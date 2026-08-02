package com.pawaneet.fitai.workout.service;

import com.pawaneet.fitai.workout.dto.AddWorkoutSetRequest;
import com.pawaneet.fitai.workout.dto.WorkoutSetResponse;
import com.pawaneet.fitai.workout.entity.Workout;
import com.pawaneet.fitai.workout.entity.WorkoutExercise;
import com.pawaneet.fitai.workout.entity.WorkoutSet;
import com.pawaneet.fitai.workout.entity.WorkoutStatus;
import com.pawaneet.fitai.workout.exception.ConflictException;
import com.pawaneet.fitai.workout.exception.ExerciseNotFoundException;
import com.pawaneet.fitai.workout.exception.WorkoutNotFoundException;
import com.pawaneet.fitai.workout.mapper.WorkoutSetMapper;
import com.pawaneet.fitai.workout.repository.WorkoutExerciseRepository;
import com.pawaneet.fitai.workout.repository.WorkoutRepository;
import com.pawaneet.fitai.workout.repository.WorkoutSetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutSetServiceTest {

    @Mock
    private WorkoutRepository workoutRepository;

    @Mock
    private WorkoutExerciseRepository workoutExerciseRepository;

    @Mock
    private WorkoutSetRepository workoutSetRepository;

    @Mock
    private WorkoutSetMapper workoutSetMapper;

    @InjectMocks
    private WorkoutSetService workoutSetService;

    @Test
    void addSetCreatesSetWithNextSetNumber() {
        UUID workoutId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        UUID setId = UUID.randomUUID();
        Workout workout = Workout.builder()
                .id(workoutId)
                .startedAt(Instant.now())
                .status(WorkoutStatus.IN_PROGRESS)
                .build();
        WorkoutExercise exercise = WorkoutExercise.builder()
                .id(exerciseId)
                .workout(workout)
                .exerciseName("Bench Press")
                .orderIndex(1)
                .build();
        AddWorkoutSetRequest request = new AddWorkoutSetRequest(80.0, 8, 2, null, "Felt easy");
        WorkoutSetResponse response = new WorkoutSetResponse(setId, 4, 80.0, 8, 2, null, "Felt easy");

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(workoutExerciseRepository.findByIdAndWorkoutId(exerciseId, workoutId)).thenReturn(Optional.of(exercise));
        when(workoutSetRepository.findMaxSetNumberByExerciseId(exerciseId)).thenReturn(3);
        when(workoutSetRepository.save(any(WorkoutSet.class))).thenAnswer(invocation -> {
            WorkoutSet workoutSet = invocation.getArgument(0);
            workoutSet.setId(setId);
            return workoutSet;
        });
        when(workoutSetMapper.toResponse(any(WorkoutSet.class))).thenReturn(response);

        WorkoutSetResponse actualResponse = workoutSetService.addSet(workoutId, exerciseId, request);

        assertThat(actualResponse).isSameAs(response);

        ArgumentCaptor<WorkoutSet> workoutSetCaptor = ArgumentCaptor.forClass(WorkoutSet.class);
        verify(workoutSetRepository).save(workoutSetCaptor.capture());

        WorkoutSet savedWorkoutSet = workoutSetCaptor.getValue();
        assertThat(savedWorkoutSet.getExercise()).isSameAs(exercise);
        assertThat(savedWorkoutSet.getSetNumber()).isEqualTo(4);
        assertThat(savedWorkoutSet.getWeight()).isEqualTo(80.0);
        assertThat(savedWorkoutSet.getReps()).isEqualTo(8);
        assertThat(savedWorkoutSet.getRir()).isEqualTo(2);
        assertThat(savedWorkoutSet.getNotes()).isEqualTo("Felt easy");
        assertThat(exercise.getSets()).contains(savedWorkoutSet);
    }

    @Test
    void addSetStartsSetNumberAtOneForFirstSet() {
        UUID workoutId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        Workout workout = Workout.builder()
                .id(workoutId)
                .startedAt(Instant.now())
                .status(WorkoutStatus.IN_PROGRESS)
                .build();
        WorkoutExercise exercise = WorkoutExercise.builder()
                .id(exerciseId)
                .workout(workout)
                .exerciseName("Bench Press")
                .orderIndex(1)
                .build();
        AddWorkoutSetRequest request = new AddWorkoutSetRequest(80.0, 8, null, null, null);
        WorkoutSetResponse response = new WorkoutSetResponse(UUID.randomUUID(), 1, 80.0, 8, null, null, null);

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(workoutExerciseRepository.findByIdAndWorkoutId(exerciseId, workoutId)).thenReturn(Optional.of(exercise));
        when(workoutSetRepository.findMaxSetNumberByExerciseId(exerciseId)).thenReturn(0);
        when(workoutSetRepository.save(any(WorkoutSet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutSetMapper.toResponse(any(WorkoutSet.class))).thenReturn(response);

        WorkoutSetResponse actualResponse = workoutSetService.addSet(workoutId, exerciseId, request);

        assertThat(actualResponse.setNumber()).isEqualTo(1);
    }

    @Test
    void addSetThrowsWorkoutNotFoundWhenWorkoutDoesNotExist() {
        UUID workoutId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        AddWorkoutSetRequest request = new AddWorkoutSetRequest(80.0, 8, null, null, null);

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSetService.addSet(workoutId, exerciseId, request))
                .isInstanceOf(WorkoutNotFoundException.class)
                .hasMessageContaining(workoutId.toString());

        verify(workoutSetRepository, never()).save(any(WorkoutSet.class));
    }

    @Test
    void addSetThrowsConflictWhenWorkoutIsCompleted() {
        UUID workoutId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        Workout workout = Workout.builder()
                .id(workoutId)
                .startedAt(Instant.now().minusSeconds(1800))
                .endedAt(Instant.now())
                .status(WorkoutStatus.COMPLETED)
                .build();
        AddWorkoutSetRequest request = new AddWorkoutSetRequest(80.0, 8, null, null, null);

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));

        assertThatThrownBy(() -> workoutSetService.addSet(workoutId, exerciseId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(workoutId.toString());

        verify(workoutSetRepository, never()).save(any(WorkoutSet.class));
    }

    @Test
    void addSetThrowsExerciseNotFoundWhenExerciseDoesNotBelongToWorkout() {
        UUID workoutId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        Workout workout = Workout.builder()
                .id(workoutId)
                .startedAt(Instant.now())
                .status(WorkoutStatus.IN_PROGRESS)
                .build();
        AddWorkoutSetRequest request = new AddWorkoutSetRequest(80.0, 8, null, null, null);

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(workoutExerciseRepository.findByIdAndWorkoutId(exerciseId, workoutId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutSetService.addSet(workoutId, exerciseId, request))
                .isInstanceOf(ExerciseNotFoundException.class)
                .hasMessageContaining(exerciseId.toString());

        verify(workoutSetRepository, never()).save(any(WorkoutSet.class));
    }
}
