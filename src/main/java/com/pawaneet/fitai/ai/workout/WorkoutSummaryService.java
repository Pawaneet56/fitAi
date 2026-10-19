package com.pawaneet.fitai.ai.workout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawaneet.fitai.ai.dto.WorkoutSummaryResponse;
import com.pawaneet.fitai.ai.indexing.WorkoutIndexJobRepository;
import com.pawaneet.fitai.ai.indexing.CurrentWorkoutUser;
import com.pawaneet.fitai.ai.service.AiService;
import com.pawaneet.fitai.workout.entity.WorkoutStatus;
import com.pawaneet.fitai.workout.exception.ConflictException;
import com.pawaneet.fitai.workout.exception.WorkoutNotFoundException;
import com.pawaneet.fitai.workout.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutSummaryService {
    private final WorkoutRepository workoutRepository;
    private final WorkoutIndexJobRepository jobs;
    private final AiService aiService;
    private final WorkoutSummaryPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public WorkoutSummaryResponse generateSummary(UUID workoutId) {
        var workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new WorkoutNotFoundException(workoutId));
        if (workout.getStatus() != WorkoutStatus.COMPLETED) {
            throw new ConflictException("Workout summary can only be generated for completed workouts");
        }
        var job = jobs.findById(workoutId);
        if (job.isEmpty()) {
            throw new ConflictException("Workout is not indexed yet. Queue it using POST /api/workouts/" + workoutId + "/index");
        }
        if (!"READY".equals(job.get().getStatus())) {
            throw new ConflictException("Preparing insights: workout indexing status is " + job.get().getStatus() + ". Please try again shortly.");
        }
        if (!CurrentWorkoutUser.ID.equals(job.get().getUserId())) {
            throw new ConflictException("Workout index is not owned by the current user");
        }
        var historyIds = jobs.findReadyWorkoutIds(CurrentWorkoutUser.ID, workout.getEndedAt(),
                org.springframework.data.domain.PageRequest.of(0, 20));
        var response = aiService.generate(promptBuilder.build(workoutId, workout.getEndedAt(),
                job.get().getDocumentName(), historyIds));
        try {
            if (response.content() == null || response.content().isBlank()) {
                throw new ConflictException("The AI returned an empty summary. Please retry.");
            }
            String json = response.content().strip();
            if (json.startsWith("```")) {
                int lineBreak = json.indexOf('\n');
                if (lineBreak >= 0 && json.endsWith("```")) json = json.substring(lineBreak + 1, json.length() - 3).strip();
            }
            var tree = objectMapper.readTree(json);
            if (tree == null || !tree.path("summary").isTextual() || tree.path("summary").asText().isBlank()
                    || !tree.path("observations").isArray() || !tree.path("suggestions").isArray()
                    || !java.util.stream.StreamSupport.stream(tree.path("observations").spliterator(), false)
                            .allMatch(com.fasterxml.jackson.databind.JsonNode::isTextual)
                    || !java.util.stream.StreamSupport.stream(tree.path("suggestions").spliterator(), false)
                            .allMatch(com.fasterxml.jackson.databind.JsonNode::isTextual)) {
                throw new ConflictException("The AI summary did not match the expected format. Please retry.");
            }
            var source = tree.path("_source");
            boolean verified = false;
            try {
                verified = workoutId.toString().equals(source.path("workoutId").asText())
                        && workout.getStartedAt().equals(java.time.Instant.parse(source.path("startedAt").asText()));
            } catch (java.time.format.DateTimeParseException ignored) { }
            if (!verified) throw new ConflictException("The summary could not be verified against the requested workout document. Please retry.");
            ((com.fasterxml.jackson.databind.node.ObjectNode) tree).remove("_source");
            return objectMapper.treeToValue(tree, WorkoutSummaryResponse.class);
        }
        catch (JsonProcessingException e) { throw new IllegalStateException("Failed to parse AI workout summary", e); }
    }
}
