package com.pawaneet.fitai.ai.workout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawaneet.fitai.ai.config.FileSearchProperties;
import com.pawaneet.fitai.ai.dto.AiPrompt;
import com.pawaneet.fitai.ai.dto.AiResponse;
import com.pawaneet.fitai.ai.service.AiService;
import com.pawaneet.fitai.ai.indexing.WorkoutIndexJob;
import com.pawaneet.fitai.ai.indexing.WorkoutIndexJobRepository;
import com.pawaneet.fitai.workout.entity.*;
import com.pawaneet.fitai.workout.exception.ConflictException;
import com.pawaneet.fitai.workout.repository.WorkoutRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkoutSummaryServiceTest {
    private final WorkoutRepository workouts = mock(WorkoutRepository.class);
    private final WorkoutIndexJobRepository jobs = mock(WorkoutIndexJobRepository.class);
    private final AiService ai = mock(AiService.class);
    private final WorkoutSummaryService service = new WorkoutSummaryService(workouts, jobs, ai,
            new WorkoutSummaryPromptBuilder(new FileSearchProperties("fileSearchStores/test")), new ObjectMapper());

    @Test
    void retrievesIndexedWorkoutWithoutEmbeddingWorkoutOrHistoryInPrompt() {
        UUID id = completedWorkout();
        WorkoutIndexJob job = new WorkoutIndexJob(id);
        job.setStatus("READY"); job.setDocumentName("fileSearchStores/test/documents/one");
        when(jobs.findById(id)).thenReturn(Optional.of(job));
        when(ai.generate(any())).thenReturn(new AiResponse("{\"summary\":\"Bench workout\",\"observations\":[],\"suggestions\":[],\"_source\":{\"workoutId\":\"" + id + "\",\"startedAt\":\"2026-10-19T05:00:00Z\"}}"));
        assertThat(service.generateSummary(id).summary()).isEqualTo("Bench workout");
        ArgumentCaptor<AiPrompt> prompt = ArgumentCaptor.forClass(AiPrompt.class);
        verify(ai).generate(prompt.capture());
        assertThat(prompt.getValue().userPrompt()).contains(id.toString(), "user1").doesNotContain("private workout notes", "totalVolume", "sets=", "2026-10-19T05:00:00Z");
        assertThat(prompt.getValue().fileSearchStore()).isEqualTo("fileSearchStores/test");
        assertThat(prompt.getValue().metadataFilter()).isEqualTo("user_id=\"user1\" AND (workout_id=\"" + id + "\")");
        assertThat(prompt.getValue().requiredDocumentName()).isEqualTo(job.getDocumentName());
    }

    @Test
    void blocksSummaryUntilIndexingCompletes() {
        UUID id = completedWorkout();
        when(jobs.findById(id)).thenReturn(Optional.of(new WorkoutIndexJob(id)));
        assertThatThrownBy(() -> service.generateSummary(id)).isInstanceOf(ConflictException.class).hasMessageContaining("Preparing insights");
        verifyNoInteractions(ai);
    }

    @Test
    void oldWorkoutNeedsExplicitQueueRatherThanSilentlyUploadingInSummaryRequest() {
        UUID id = completedWorkout();
        assertThatThrownBy(() -> service.generateSummary(id)).isInstanceOf(ConflictException.class).hasMessageContaining("POST");
        verifyNoInteractions(ai);
    }

    @Test
    void rejectsInProgressWorkoutBeforeCallingAi() {
        UUID id = UUID.randomUUID();
        when(workouts.findById(id)).thenReturn(Optional.of(Workout.builder().id(id).status(WorkoutStatus.IN_PROGRESS).build()));
        assertThatThrownBy(() -> service.generateSummary(id)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(jobs, ai);
    }

    @Test
    void rejectsSummaryThatCannotEchoUnpromptedDocumentTimestamp() {
        UUID id = completedWorkout();
        WorkoutIndexJob job = new WorkoutIndexJob(id);
        job.setStatus("READY"); job.setDocumentName("fileSearchStores/test/documents/one");
        when(jobs.findById(id)).thenReturn(Optional.of(job));
        when(ai.generate(any())).thenReturn(new AiResponse("{\"summary\":\"Unverified answer\",\"observations\":[],\"suggestions\":[],\"_source\":{\"workoutId\":\"" + id + "\",\"startedAt\":\"2026-10-18T05:00:00Z\"}}"));
        assertThatThrownBy(() -> service.generateSummary(id)).isInstanceOf(ConflictException.class)
                .hasMessageContaining("could not be verified");
    }

    private UUID completedWorkout() {
        UUID id = UUID.randomUUID();
        when(workouts.findById(id)).thenReturn(Optional.of(Workout.builder().id(id).status(WorkoutStatus.COMPLETED)
                .startedAt(Instant.parse("2026-10-19T05:00:00Z")).endedAt(Instant.parse("2026-10-19T06:00:00Z")).notes("private workout notes").build()));
        return id;
    }
}
