package com.pawaneet.fitai.ai.indexing;

import com.google.genai.types.*;
import com.pawaneet.fitai.ai.client.GeminiFileSearchClient;
import com.pawaneet.fitai.ai.config.FileSearchProperties;
import com.pawaneet.fitai.workout.entity.Workout;
import com.pawaneet.fitai.workout.entity.WorkoutStatus;
import com.pawaneet.fitai.workout.exception.ConflictException;
import com.pawaneet.fitai.workout.exception.WorkoutNotFoundException;
import com.pawaneet.fitai.workout.repository.WorkoutRepository;
import com.pawaneet.fitai.workout.repository.WorkoutExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkoutIndexingService {
    private final WorkoutIndexJobRepository jobs;
    private final WorkoutRepository workouts;
    private final WorkoutExerciseRepository exercises;
    private final WorkoutDocumentBuilder documentBuilder;
    private final GeminiFileSearchClient google;
    private final FileSearchProperties properties;

    @Transactional
    public IndexStatus enqueue(UUID workoutId) {
        Workout workout = workouts.findById(workoutId).orElseThrow(() -> new WorkoutNotFoundException(workoutId));
        if (workout.getStatus() != WorkoutStatus.COMPLETED) throw new ConflictException("Only completed workouts can be indexed");
        // Serialize manual enqueue requests for this workout with a database row lock.
        workouts.lockById(workoutId);
        WorkoutIndexJob job = jobs.findById(workoutId).orElseGet(() -> jobs.save(new WorkoutIndexJob(workoutId)));
        if ("FAILED".equals(job.getStatus())) {
            job.setStatus("PENDING");
            job.setIndexedAt(null);
            job.setNextDispatchAt(Instant.now());
            job.setLastError(null);
        }
        return status(job);
    }

    public IndexStatus getStatus(UUID workoutId) {
        if (!workouts.existsById(workoutId)) throw new WorkoutNotFoundException(workoutId);
        return jobs.findById(workoutId).map(this::status)
                .orElse(new IndexStatus(workoutId, CurrentWorkoutUser.ID, "NOT_QUEUED", null, 0));
    }

    @Transactional
    public void process(UUID workoutId) {
        WorkoutIndexJob job = jobs.findLockedById(workoutId).orElseThrow();
        if ("READY".equals(job.getStatus())) return;
        if (!CurrentWorkoutUser.ID.equals(job.getUserId())) throw new IllegalStateException("Unsupported workout owner");
        job.setIndexedAt(null);
        try {
            if (properties.storeName() == null || properties.storeName().isBlank()) throw new IllegalStateException("Configure the Gemini File Search store");
            Workout workout = workouts.findById(workoutId).orElseThrow(() -> new WorkoutNotFoundException(workoutId));
            if (workout.getStatus() != WorkoutStatus.COMPLETED) throw new IllegalStateException("Workout is not completed");
            if (job.getDocumentName() == null) {
                UploadToFileSearchStoreOperation operation;
                if (job.getOperationName() == null) {
                    String displayName = "fitai-" + job.getUserId() + "-" + workoutId;
                    // Recover an upload accepted by Google before a local crash saved its operation name.
                    for (Document existing : google.listDocuments(properties.storeName())) {
                        if (metadataMatches(existing, workout, job.getUserId())) {
                            job.setDocumentName(existing.name().orElseThrow());
                            break;
                        }
                    }
                    if (job.getDocumentName() == null) {
                        operation = google.uploadWorkout(properties.storeName(),
                                documentBuilder.build(workout, exercises.findByWorkoutIdWithSets(workoutId)).getBytes(StandardCharsets.UTF_8),
                                displayName, List.of(metadata("user_id", job.getUserId()), metadata("workout_id", workoutId.toString()),
                                        metadata("workout_date", CurrentWorkoutUser.documentDate(workout.getEndedAt()))));
                        job.setOperationName(operation.name().orElseThrow());
                        acceptOperation(job, operation);
                    }
                } else {
                    operation = google.getUploadOperation(job.getOperationName());
                    acceptOperation(job, operation);
                }
            }
            if (job.getDocumentName() != null) {
                job.setDocumentName(normalizeDocumentName(job.getDocumentName()));
                Document document = resolveDocument(job, workout);
                if (!metadataMatches(document, workout, job.getUserId())) {
                    // A document without these tags is invisible to the user/date retrieval filter.
                    google.deleteDocument(job.getDocumentName());
                    job.setDocumentName(null);
                    job.setOperationName(null);
                    throw new IllegalStateException("Index metadata missing or mismatched; re-uploading workout");
                }
                String state = document.state().map(Object::toString).orElse("");
                if ("STATE_ACTIVE".equals(state)) {
                    job.setStatus("READY"); job.setIndexedAt(Instant.now());
                } else if ("STATE_FAILED".equals(state)) {
                    // Only discard a terminally failed document owned by this indexing job.
                    // Transient request failures retain the operation/document IDs for resuming.
                    google.deleteDocument(job.getDocumentName());
                    job.setDocumentName(null);
                    job.setOperationName(null);
                    throw new IllegalStateException("Google could not index the workout document; retrying upload");
                } else { job.setStatus("INDEXING"); }
            }
            job.setLastError(null);
        } catch (Exception e) {
            job.setStatus("FAILED"); job.setAttempts(job.getAttempts() + 1);
            job.setLastError(e.getClass().getSimpleName());
            log.warn("Workout indexing attempt failed for {} ({})", workoutId, e.getClass().getSimpleName());
        }
    }

    private void acceptOperation(WorkoutIndexJob job, UploadToFileSearchStoreOperation operation) {
        if (operation.error().isPresent()) {
            job.setOperationName(null);
            throw new IllegalStateException("Google upload operation failed");
        }
        if (operation.done().orElse(false)) {
            job.setDocumentName(normalizeDocumentName(operation.response()
                    .flatMap(UploadToFileSearchStoreResponse::documentName).orElseThrow()));
        }
        job.setStatus("INDEXING");
    }

    private Document resolveDocument(WorkoutIndexJob job, Workout workout) {
        try { return google.getDocument(job.getDocumentName()); }
        catch (RuntimeException lookupFailure) {
            // Import operation IDs can differ from the final document resource ID.
            // Resolve by verified immutable metadata instead of guessing the final ID.
            for (Document document : google.listDocuments(properties.storeName())) {
                if (metadataMatches(document, workout, job.getUserId())) {
                    job.setDocumentName(document.name().orElseThrow());
                    return document;
                }
            }
            throw lookupFailure;
        }
    }

    private String normalizeDocumentName(String name) {
        if (name.startsWith("fileSearchStores/")) return name;
        return properties.storeName() + (name.startsWith("documents/") ? "/" : "/documents/") + name;
    }

    private boolean metadataMatches(Document document, Workout workout, String userId) {
        var expected = java.util.Map.of("user_id", userId, "workout_id", workout.getId().toString(),
                "workout_date", CurrentWorkoutUser.documentDate(workout.getEndedAt()));
        var actual = new java.util.HashMap<String, String>();
        document.customMetadata().orElse(List.of()).forEach(item -> {
            if (item.key().isPresent() && item.stringValue().isPresent()) {
                actual.put(item.key().get(), item.stringValue().get());
            }
        });
        return actual.entrySet().containsAll(expected.entrySet());
    }

    private CustomMetadata metadata(String key, String value) {
        return CustomMetadata.builder().key(key).stringValue(value).build();
    }

    private IndexStatus status(WorkoutIndexJob job) {
        return new IndexStatus(job.getWorkoutId(), job.getUserId(), job.getStatus(), job.getIndexedAt(), job.getAttempts());
    }

    public record IndexStatus(UUID workoutId, String userId, String status, Instant indexedAt, int failedAttempts) {}
}
