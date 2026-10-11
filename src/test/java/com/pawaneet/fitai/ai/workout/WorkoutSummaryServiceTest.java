package com.pawaneet.fitai.ai.workout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawaneet.fitai.ai.dto.AiPrompt;
import com.pawaneet.fitai.ai.dto.AiResponse;
import com.pawaneet.fitai.ai.service.AiService;
import com.pawaneet.fitai.analytics.service.WorkoutMetricsService;
import com.pawaneet.fitai.workout.entity.*;
import com.pawaneet.fitai.workout.exception.ConflictException;
import com.pawaneet.fitai.workout.repository.WorkoutExerciseRepository;
import com.pawaneet.fitai.workout.repository.WorkoutRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkoutSummaryServiceTest {
    private final WorkoutRepository workouts = mock(WorkoutRepository.class);
    private final WorkoutExerciseRepository exercises = mock(WorkoutExerciseRepository.class);
    private final AiService ai = mock(AiService.class);
    private final WorkoutSummaryService service = new WorkoutSummaryService(
            new WorkoutMetricsService(workouts, exercises), workouts, exercises, ai,
            new WorkoutSummaryPromptBuilder(), new ObjectMapper());

    @Test
    void suppliesBackendMetricsToAiAndPreservesSummaryResponseContract() {
        UUID id = UUID.randomUUID();
        when(workouts.findById(id)).thenReturn(Optional.of(Workout.builder()
                .id(id).status(WorkoutStatus.COMPLETED).build()));
        when(exercises.findByWorkoutIdWithSets(id)).thenReturn(List.of(WorkoutExercise.builder()
                .exerciseName("Bench").orderIndex(1).sets(List.of(WorkoutSet.builder()
                        .weight(82.5).reps(6).rir(2).setNumber(1).build())).build()));
        when(ai.generate(any())).thenReturn(new AiResponse(
                "{\"summary\":\"Bench workout\",\"observations\":[],\"suggestions\":[]}"));

        assertThat(service.generateSummary(id).summary()).isEqualTo("Bench workout");
        ArgumentCaptor<AiPrompt> prompt = ArgumentCaptor.forClass(AiPrompt.class);
        verify(ai).generate(prompt.capture());
        assertThat(prompt.getValue().userPrompt()).contains("totalVolume=495.0", "averageRir=2.0");
        assertThat(prompt.getValue().systemPrompt()).contains("Do not recalculate", "units are unspecified");
        verify(exercises, times(1)).findByWorkoutIdWithSets(id);
    }

    @Test
    void rejectsInProgressWorkoutBeforeLoadingSetsOrCallingAi() {
        UUID id = UUID.randomUUID();
        when(workouts.findById(id)).thenReturn(Optional.of(Workout.builder()
                .id(id).status(WorkoutStatus.IN_PROGRESS).build()));
        assertThatThrownBy(() -> service.generateSummary(id)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(exercises, ai);
    }
}
