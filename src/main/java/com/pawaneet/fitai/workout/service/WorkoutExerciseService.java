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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutExerciseService {

    private final WorkoutRepository workoutRepository;

    private final WorkoutExerciseRepository workoutExerciseRepository;

    private final WorkoutExerciseMapper workoutExerciseMapper;

    private final ExerciseEventProducer exerciseEventProducer;

    @Transactional
    public WorkoutExerciseResponse addExercise(UUID workoutId, AddWorkoutExerciseRequest request) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new WorkoutNotFoundException(workoutId));

        if (workout.getStatus() == WorkoutStatus.COMPLETED) {
            throw new CannotAddExerciseToCompletedWorkoutException(workoutId);
        }

        int nextOrderIndex = workoutExerciseRepository.findMaxOrderIndexByWorkoutId(workoutId) + 1;
        WorkoutExercise workoutExercise = WorkoutExercise.builder()
                .workout(workout)
                .exerciseName(request.exerciseName())
                .orderIndex(nextOrderIndex)
                .build();

        WorkoutExercise savedWorkoutExercise = workoutExerciseRepository.save(workoutExercise);
        publishAfterCommit(savedWorkoutExercise);

        return workoutExerciseMapper.toResponse(savedWorkoutExercise);
    }

    private void publishAfterCommit(WorkoutExercise event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            exerciseEventProducer.publishExerciseAdded(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                exerciseEventProducer.publishExerciseAdded(event);
            }
        });
    }
}
