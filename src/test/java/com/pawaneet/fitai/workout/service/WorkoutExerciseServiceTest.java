package com.pawaneet.fitai.workout.service;

import com.pawaneet.fitai.workout.dto.AddWorkoutExerciseRequest;
import com.pawaneet.fitai.workout.dto.WorkoutExerciseResponse;
import com.pawaneet.fitai.workout.entity.Workout;
import com.pawaneet.fitai.workout.entity.WorkoutExercise;
import com.pawaneet.fitai.workout.entity.WorkoutStatus;
import com.pawaneet.fitai.workout.event.ExerciseAddedEvent;
import com.pawaneet.fitai.workout.exception.CannotAddExerciseToCompletedWorkoutException;
import com.pawaneet.fitai.workout.exception.WorkoutNotFoundException;
import com.pawaneet.fitai.workout.mapper.WorkoutExerciseMapper;
import com.pawaneet.fitai.workout.producer.ExerciseEventProducer;
import com.pawaneet.fitai.workout.repository.WorkoutExerciseRepository;
import com.pawaneet.fitai.workout.repository.WorkoutRepository;
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
class WorkoutExerciseServiceTest {

    @Mock
    private WorkoutRepository workoutRepository;

    @Mock
    private WorkoutExerciseRepository workoutExerciseRepository;

    @Mock
    private WorkoutExerciseMapper workoutExerciseMapper;

    @Mock
    private ExerciseEventProducer exerciseEventProducer;

    @InjectMocks
    private WorkoutExerciseService workoutExerciseService;

    @Test
    void addExerciseCreatesExerciseWithNextOrderIndex() {
        UUID workoutId = UUID.randomUUID();
        UUID workoutExerciseId = UUID.randomUUID();
        Workout workout = Workout.builder()
                .id(workoutId)
                .startedAt(Instant.now())
                .status(WorkoutStatus.IN_PROGRESS)
                .build();
        AddWorkoutExerciseRequest request = new AddWorkoutExerciseRequest("Bench Press");
        WorkoutExerciseResponse response = new WorkoutExerciseResponse(
                workoutExerciseId,
                "Bench Press",
                3
        );

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(workoutExerciseRepository.findMaxOrderIndexByWorkoutId(workoutId)).thenReturn(2);
        when(workoutExerciseRepository.save(any(WorkoutExercise.class))).thenAnswer(invocation -> {
            WorkoutExercise workoutExercise = invocation.getArgument(0);
            workoutExercise.setId(workoutExerciseId);
            return workoutExercise;
        });
        when(workoutExerciseMapper.toResponse(any(WorkoutExercise.class))).thenReturn(response);

        WorkoutExerciseResponse actualResponse = workoutExerciseService.addExercise(workoutId, request);

        assertThat(actualResponse).isSameAs(response);

        ArgumentCaptor<WorkoutExercise> workoutExerciseCaptor = ArgumentCaptor.forClass(WorkoutExercise.class);
        verify(workoutExerciseRepository).save(workoutExerciseCaptor.capture());

        WorkoutExercise savedWorkoutExercise = workoutExerciseCaptor.getValue();
        assertThat(savedWorkoutExercise.getWorkout()).isSameAs(workout);
        assertThat(savedWorkoutExercise.getExerciseName()).isEqualTo("Bench Press");
        assertThat(savedWorkoutExercise.getOrderIndex()).isEqualTo(3);

        ArgumentCaptor<ExerciseAddedEvent> eventCaptor = ArgumentCaptor.forClass(ExerciseAddedEvent.class);
        verify(exerciseEventProducer).publishExerciseAdded(eventCaptor.capture());

        ExerciseAddedEvent event = eventCaptor.getValue();
        assertThat(event.workoutId()).isEqualTo(workoutId);
        assertThat(event.exerciseId()).isEqualTo(workoutExerciseId);
        assertThat(event.exerciseName()).isEqualTo("Bench Press");
        assertThat(event.orderIndex()).isEqualTo(3);
        assertThat(event.createdAt()).isNotNull();
    }

    @Test
    void addExerciseStartsOrderIndexAtOneForFirstExercise() {
        UUID workoutId = UUID.randomUUID();
        Workout workout = Workout.builder()
                .id(workoutId)
                .startedAt(Instant.now())
                .status(WorkoutStatus.IN_PROGRESS)
                .build();
        AddWorkoutExerciseRequest request = new AddWorkoutExerciseRequest("Squat");
        WorkoutExerciseResponse response = new WorkoutExerciseResponse(UUID.randomUUID(), "Squat", 1);

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(workoutExerciseRepository.findMaxOrderIndexByWorkoutId(workoutId)).thenReturn(0);
        when(workoutExerciseRepository.save(any(WorkoutExercise.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutExerciseMapper.toResponse(any(WorkoutExercise.class))).thenReturn(response);

        WorkoutExerciseResponse actualResponse = workoutExerciseService.addExercise(workoutId, request);

        assertThat(actualResponse.orderIndex()).isEqualTo(1);
    }

    @Test
    void addExerciseThrowsWorkoutNotFoundWhenWorkoutDoesNotExist() {
        UUID workoutId = UUID.randomUUID();
        AddWorkoutExerciseRequest request = new AddWorkoutExerciseRequest("Bench Press");

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutExerciseService.addExercise(workoutId, request))
                .isInstanceOf(WorkoutNotFoundException.class)
                .hasMessageContaining(workoutId.toString());

        verify(workoutExerciseRepository, never()).save(any(WorkoutExercise.class));
        verify(exerciseEventProducer, never()).publishExerciseAdded(any(ExerciseAddedEvent.class));
    }

    @Test
    void addExerciseThrowsWhenWorkoutIsCompleted() {
        UUID workoutId = UUID.randomUUID();
        Workout workout = Workout.builder()
                .id(workoutId)
                .startedAt(Instant.now().minusSeconds(1800))
                .endedAt(Instant.now())
                .status(WorkoutStatus.COMPLETED)
                .build();
        AddWorkoutExerciseRequest request = new AddWorkoutExerciseRequest("Bench Press");

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));

        assertThatThrownBy(() -> workoutExerciseService.addExercise(workoutId, request))
                .isInstanceOf(CannotAddExerciseToCompletedWorkoutException.class)
                .hasMessageContaining(workoutId.toString());

        verify(workoutExerciseRepository, never()).save(any(WorkoutExercise.class));
        verify(exerciseEventProducer, never()).publishExerciseAdded(any(ExerciseAddedEvent.class));
    }
}
