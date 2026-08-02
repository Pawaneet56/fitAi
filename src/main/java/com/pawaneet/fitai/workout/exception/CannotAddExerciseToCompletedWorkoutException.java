package com.pawaneet.fitai.workout.exception;

import java.util.UUID;

public class CannotAddExerciseToCompletedWorkoutException extends RuntimeException {

    public CannotAddExerciseToCompletedWorkoutException(UUID workoutId) {
        super("Cannot add exercises to completed workout with id: " + workoutId);
    }
}
