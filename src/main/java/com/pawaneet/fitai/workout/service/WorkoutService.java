package com.pawaneet.fitai.workout.service;

import com.pawaneet.fitai.ai.indexing.WorkoutIndexJob;
import com.pawaneet.fitai.ai.indexing.WorkoutIndexJobRepository;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.pawaneet.fitai.workout.dto.StartWorkoutRequest;
import com.pawaneet.fitai.workout.dto.WorkoutResponse;
import com.pawaneet.fitai.workout.entity.Workout;
import com.pawaneet.fitai.workout.entity.WorkoutExercise;
import com.pawaneet.fitai.workout.entity.WorkoutStatus;
import com.pawaneet.fitai.workout.event.WorkoutEndedEvent;
import com.pawaneet.fitai.workout.event.WorkoutStartedEvent;
import com.pawaneet.fitai.workout.exception.WorkoutNotFoundException;
import com.pawaneet.fitai.workout.mapper.WorkoutMapper;
import com.pawaneet.fitai.workout.producer.WorkoutEventProducer;
import com.pawaneet.fitai.workout.repository.WorkoutRepository;
import com.pawaneet.fitai.workout.repository.WorkoutSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutIndexJobRepository indexingJobs;

    private final WorkoutMapper workoutMapper;

    private final WorkoutEventProducer workoutEventProducer;

    private final WorkoutSetRepository workoutSetRepository;

    public WorkoutResponse startWorkout(StartWorkoutRequest request) {
        Workout workout = Workout.builder().startedAt(Instant.now()).status(WorkoutStatus.IN_PROGRESS).notes(request.notes()).build();
        Workout savedWorkout = workoutRepository.save(workout);
        workoutEventProducer.publishWorkoutStarted(savedWorkout);

        return workoutMapper.toResponse(savedWorkout);
    }

    @Transactional(readOnly = true)
    public WorkoutResponse getWorkout(UUID workoutId) {
        Workout workout = workoutRepository
                .findWithExercisesById(workoutId)
                .orElseThrow(() -> new WorkoutNotFoundException(workoutId));

        return workoutMapper.toResponse(workout);
    }

    public List<WorkoutResponse> getWorkouts() {
        return workoutRepository.findAllByOrderByStartedAtDesc()
                .stream()
                .map(workoutMapper::toResponse)
                .toList();
    }

    @Transactional
    public WorkoutResponse endWorkout(UUID workoutId) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new WorkoutNotFoundException(workoutId));

        if (workout.getStatus() != WorkoutStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only in-progress workouts can be ended");
        }

        Instant endedAt = Instant.now();
        workout.setEndedAt(endedAt);
        workout.setStatus(WorkoutStatus.COMPLETED);

        Workout savedWorkout = workoutRepository.save(workout);

        indexingJobs.save(new WorkoutIndexJob(savedWorkout.getId()));
        // Analytics publication stays independent; indexing is durably queued in this transaction.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override public void afterCommit() { workoutEventProducer.publishWorkoutEnded(savedWorkout); }
                    });
        } else {
            workoutEventProducer.publishWorkoutEnded(savedWorkout);
        }

        return workoutMapper.toResponse(savedWorkout);
    }
}
