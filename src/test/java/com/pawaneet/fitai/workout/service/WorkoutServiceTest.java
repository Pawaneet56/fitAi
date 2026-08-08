package com.pawaneet.fitai.workout.service;

import com.pawaneet.fitai.workout.dto.WorkoutResponse;
import com.pawaneet.fitai.workout.entity.Workout;
import com.pawaneet.fitai.workout.entity.WorkoutStatus;
import com.pawaneet.fitai.workout.exception.WorkoutNotFoundException;
import com.pawaneet.fitai.workout.mapper.WorkoutMapper;
import com.pawaneet.fitai.workout.producer.WorkoutEventProducer;
import com.pawaneet.fitai.workout.repository.WorkoutRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    @Mock
    private WorkoutRepository workoutRepository;

    @Mock
    private WorkoutMapper workoutMapper;

    @Mock
    private WorkoutEventProducer workoutEventProducer;

    @InjectMocks
    private WorkoutService workoutService;

    @Test
    void getWorkoutReturnsMappedWorkoutWhenWorkoutExists() {
        UUID workoutId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        Workout workout = Workout.builder()
                .id(workoutId)
                .startedAt(startedAt)
                .status(WorkoutStatus.IN_PROGRESS)
                .notes("Push Day")
                .build();
        WorkoutResponse response = new WorkoutResponse(
                workoutId,
                startedAt,
                null,
                null,
                WorkoutStatus.IN_PROGRESS,
                "Push Day"
        );

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(workoutMapper.toResponse(workout)).thenReturn(response);

        WorkoutResponse actualResponse = workoutService.getWorkout(workoutId);

        assertThat(actualResponse).isSameAs(response);
    }

    @Test
    void getWorkoutThrowsWorkoutNotFoundWhenWorkoutDoesNotExist() {
        UUID workoutId = UUID.randomUUID();
        when(workoutRepository.findById(workoutId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutService.getWorkout(workoutId))
                .isInstanceOf(WorkoutNotFoundException.class)
                .hasMessageContaining(workoutId.toString());
    }

    @Test
    void getWorkoutsReturnsAllMappedWorkoutsInRepositoryOrder() {
        UUID firstWorkoutId = UUID.randomUUID();
        UUID secondWorkoutId = UUID.randomUUID();
        Instant firstStartedAt = Instant.now();
        Instant secondStartedAt = firstStartedAt.minusSeconds(3600);
        Workout firstWorkout = Workout.builder()
                .id(firstWorkoutId)
                .startedAt(firstStartedAt)
                .status(WorkoutStatus.IN_PROGRESS)
                .build();
        Workout secondWorkout = Workout.builder()
                .id(secondWorkoutId)
                .startedAt(secondStartedAt)
                .status(WorkoutStatus.COMPLETED)
                .endedAt(secondStartedAt.plusSeconds(1800))
                .build();
        WorkoutResponse firstResponse = new WorkoutResponse(
                firstWorkoutId,
                firstStartedAt,
                null,
                null,
                WorkoutStatus.IN_PROGRESS,
                null
        );
        WorkoutResponse secondResponse = new WorkoutResponse(
                secondWorkoutId,
                secondStartedAt,
                secondStartedAt.plusSeconds(1800),
                1800L,
                WorkoutStatus.COMPLETED,
                null
        );

        when(workoutRepository.findAllByOrderByStartedAtDesc()).thenReturn(List.of(firstWorkout, secondWorkout));
        when(workoutMapper.toResponse(firstWorkout)).thenReturn(firstResponse);
        when(workoutMapper.toResponse(secondWorkout)).thenReturn(secondResponse);

        List<WorkoutResponse> responses = workoutService.getWorkouts();

        assertThat(responses).containsExactly(firstResponse, secondResponse);
    }

    @Test
    void endWorkoutCompletesWorkoutPublishesEventAndReturnsResponse() {
        UUID workoutId = UUID.randomUUID();
        Instant startedAt = Instant.now().minusSeconds(900);
        Workout workout = Workout.builder()
                .id(workoutId)
                .startedAt(startedAt)
                .status(WorkoutStatus.IN_PROGRESS)
                .notes("Push Day")
                .build();
        WorkoutResponse response = new WorkoutResponse(
                workoutId,
                startedAt,
                Instant.now(),
                900L,
                WorkoutStatus.COMPLETED,
                "Push Day"
        );

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));
        when(workoutRepository.save(any(Workout.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutMapper.toResponse(any(Workout.class))).thenReturn(response);

        WorkoutResponse actualResponse = workoutService.endWorkout(workoutId);

        assertThat(actualResponse).isSameAs(response);
        assertThat(workout.getStatus()).isEqualTo(WorkoutStatus.COMPLETED);
        assertThat(workout.getEndedAt()).isNotNull();

        ArgumentCaptor<Workout> workoutCaptor = ArgumentCaptor.forClass(Workout.class);
        verify(workoutEventProducer).publishWorkoutEnded(workoutCaptor.capture());

        Workout capturedWorkout = workoutCaptor.getValue();
        assertThat(capturedWorkout.getId()).isEqualTo(workoutId);
        assertThat(capturedWorkout.getStatus()).isEqualTo(WorkoutStatus.COMPLETED);
    }

    @Test
    void endWorkoutThrowsNotFoundWhenWorkoutDoesNotExist() {
        UUID workoutId = UUID.randomUUID();
        when(workoutRepository.findById(workoutId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutService.endWorkout(workoutId))
                .isInstanceOf(WorkoutNotFoundException.class)
                .hasMessageContaining(workoutId.toString());

        verify(workoutRepository, never()).save(any(Workout.class));
        verify(workoutEventProducer, never()).publishWorkoutEnded(any(Workout.class));
    }

    @Test
    void endWorkoutThrowsConflictWhenWorkoutIsNotInProgress() {
        UUID workoutId = UUID.randomUUID();
        Workout workout = Workout.builder()
                .id(workoutId)
                .startedAt(Instant.now().minusSeconds(900))
                .endedAt(Instant.now())
                .status(WorkoutStatus.COMPLETED)
                .build();

        when(workoutRepository.findById(workoutId)).thenReturn(Optional.of(workout));

        assertThatThrownBy(() -> workoutService.endWorkout(workoutId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(workoutRepository, never()).save(any(Workout.class));
        verify(workoutEventProducer, never()).publishWorkoutEnded(any(Workout.class));
    }
}