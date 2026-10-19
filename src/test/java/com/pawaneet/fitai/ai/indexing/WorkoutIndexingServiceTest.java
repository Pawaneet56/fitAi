package com.pawaneet.fitai.ai.indexing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.*;
import com.pawaneet.fitai.ai.client.GeminiFileSearchClient;
import com.pawaneet.fitai.ai.config.FileSearchProperties;
import com.pawaneet.fitai.analytics.service.WorkoutMetricsService;
import com.pawaneet.fitai.workout.entity.*;
import com.pawaneet.fitai.workout.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkoutIndexingServiceTest {
    private final WorkoutIndexJobRepository jobs = mock(WorkoutIndexJobRepository.class);
    private final WorkoutRepository workouts = mock(WorkoutRepository.class);
    private final WorkoutExerciseRepository exercises = mock(WorkoutExerciseRepository.class);
    private final GeminiFileSearchClient google = mock(GeminiFileSearchClient.class);
    private final WorkoutIndexingService service = new WorkoutIndexingService(jobs, workouts, exercises,
            new WorkoutDocumentBuilder(new WorkoutMetricsService(workouts, exercises), new ObjectMapper()),
            google, new FileSearchProperties("fileSearchStores/test"));
    private final UUID id = UUID.randomUUID();
    private final WorkoutIndexJob job = new WorkoutIndexJob(id);

    @Test
    void readyJobsAreIdempotent() {
        when(jobs.findLockedById(id)).thenReturn(Optional.of(job)); job.setStatus("READY");
        service.process(id);
        verifyNoInteractions(google, workouts, exercises);
    }

    @Test
    void existingUploadIsPolledWithoutUploadingAgainAndOnlyReadyWhenActive() {
        prepare(); job.setOperationName("operations/one");
        when(google.getUploadOperation("operations/one")).thenReturn(UploadToFileSearchStoreOperation.builder()
                .name("operations/one").done(true).response(UploadToFileSearchStoreResponse.builder()
                        .documentName("one").build()).build());
        when(google.getDocument(anyString())).thenReturn(validDocument("STATE_PENDING"));
        service.process(id);
        assertThat(job.getStatus()).isEqualTo("INDEXING");
        assertThat(job.getIndexedAt()).isNull();
        when(google.getDocument(anyString())).thenReturn(validDocument("STATE_ACTIVE"));
        service.process(id);
        assertThat(job.getStatus()).isEqualTo("READY");
        assertThat(job.getIndexedAt()).isNotNull();
        verify(google, atLeastOnce()).getDocument("fileSearchStores/test/documents/one");
        verify(google, never()).uploadWorkout(anyString(), any(), anyString(), anyList());
    }

    @Test
    void failurePersistsRetryStateWithoutLosingOperation() {
        prepare(); job.setOperationName("operations/one");
        when(google.getUploadOperation(anyString())).thenThrow(new IllegalStateException("temporary failure"));
        service.process(id);
        assertThat(job.getStatus()).isEqualTo("FAILED");
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getOperationName()).isEqualTo("operations/one");
    }

    @Test
    void recoveredDocumentAvoidsDuplicateUpload() {
        prepare();
        @SuppressWarnings("unchecked") com.google.genai.Pager<Document> pager = mock(com.google.genai.Pager.class);
        when(google.listDocuments(anyString())).thenReturn(pager);
        when(pager.iterator()).thenAnswer(invocation -> List.of(validDocument("STATE_ACTIVE").toBuilder()
                .name("fileSearchStores/test/documents/recovered").build()).iterator());
        when(google.getDocument(anyString())).thenReturn(validDocument("STATE_ACTIVE"));
        service.process(id);
        assertThat(job.getStatus()).isEqualTo("READY");
        verify(google, never()).uploadWorkout(anyString(), any(), anyString(), anyList());
    }

    @Test
    void uploadsUserTaggedDatedDocumentWithVerifiedMetricsOnce() {
        prepare();
        @SuppressWarnings("unchecked") com.google.genai.Pager<Document> pager = mock(com.google.genai.Pager.class);
        when(google.listDocuments(anyString())).thenReturn(pager);
        when(pager.iterator()).thenAnswer(invocation -> Collections.emptyIterator());
        when(exercises.findByWorkoutIdWithSets(id)).thenReturn(List.of(WorkoutExercise.builder()
                .exerciseName("Bench").sets(List.of(WorkoutSet.builder().weight(80.0).reps(8).rir(2).build())).build()));
        when(google.uploadWorkout(anyString(), any(), anyString(), anyList())).thenReturn(
                UploadToFileSearchStoreOperation.builder().name("operations/new").done(false).build());
        service.process(id);
        ArgumentCaptor<byte[]> content = ArgumentCaptor.forClass(byte[].class);
        @SuppressWarnings({"unchecked", "rawtypes"}) ArgumentCaptor<List<CustomMetadata>> metadata = ArgumentCaptor.forClass((Class)List.class);
        verify(google).uploadWorkout(eq("fileSearchStores/test"), content.capture(), eq("fitai-user1-"+id), metadata.capture());
        assertThat(new String(content.getValue(), StandardCharsets.UTF_8)).contains("user1", "Bench", "640.0", "2026-10-19");
        assertThat(metadata.getValue()).anySatisfy(item -> {
            assertThat(item.key()).contains("user_id"); assertThat(item.stringValue()).contains("user1");
        });
        assertThat(job.getOperationName()).isEqualTo("operations/new");
        assertThat(job.getStatus()).isEqualTo("INDEXING");
    }

    @Test
    void activeDocumentWithoutUserMetadataIsReplacedRatherThanMarkedReady() {
        prepare(); job.setDocumentName("fileSearchStores/test/documents/broken");
        when(google.getDocument(anyString())).thenReturn(Document.builder().state(new DocumentState("STATE_ACTIVE")).build());
        service.process(id);
        assertThat(job.getStatus()).isEqualTo("FAILED");
        assertThat(job.getDocumentName()).isNull();
        assertThat(job.getOperationName()).isNull();
        verify(google).deleteDocument("fileSearchStores/test/documents/broken");
    }

    @Test
    void resolvesFinalImportResourceUsingVerifiedTagsWhenOperationReturnsDifferentId() {
        prepare(); job.setDocumentName("fileSearchStores/test/documents/operation-id");
        when(google.getDocument(anyString())).thenThrow(new IllegalStateException("not found"));
        @SuppressWarnings("unchecked") com.google.genai.Pager<Document> pager = mock(com.google.genai.Pager.class);
        when(google.listDocuments(anyString())).thenReturn(pager);
        when(pager.iterator()).thenAnswer(invocation -> List.of(validDocument("STATE_ACTIVE").toBuilder()
                .name("fileSearchStores/test/documents/final-resource").build()).iterator());
        service.process(id);
        assertThat(job.getStatus()).isEqualTo("READY");
        assertThat(job.getDocumentName()).isEqualTo("fileSearchStores/test/documents/final-resource");
        verify(google, never()).uploadWorkout(anyString(), any(), anyString(), anyList());
    }

    private Document validDocument(String state) {
        return Document.builder().state(new DocumentState(state)).displayName("fitai-user1-" + id)
                .customMetadata(List.of(
                        CustomMetadata.builder().key("user_id").stringValue("user1").build(),
                        CustomMetadata.builder().key("workout_id").stringValue(id.toString()).build(),
                        CustomMetadata.builder().key("workout_date").stringValue("2026-10-19T07:00:00.000000000Z").build()))
                .build();
    }

    private void prepare() {
        when(jobs.findLockedById(id)).thenReturn(Optional.of(job));
        when(workouts.findById(id)).thenReturn(Optional.of(Workout.builder().id(id)
                .startedAt(Instant.parse("2026-10-19T06:00:00Z")).endedAt(Instant.parse("2026-10-19T07:00:00Z"))
                .status(WorkoutStatus.COMPLETED).build()));
    }
}
