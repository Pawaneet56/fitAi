package com.pawaneet.fitai.workout.exception;

import java.util.UUID;

public class WorkoutNotFoundException extends RuntimeException {

    public WorkoutNotFoundException(UUID workoutId) {
        super("Workout not found with id: " + workoutId);
    }
}
