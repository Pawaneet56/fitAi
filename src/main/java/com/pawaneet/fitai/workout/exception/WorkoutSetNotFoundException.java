package com.pawaneet.fitai.workout.exception;

import java.util.UUID;

public class WorkoutSetNotFoundException extends RuntimeException {
    public WorkoutSetNotFoundException(UUID setId, UUID exerciseId) {
        super("Set not found with id: " + setId + " for exercise: "+ exerciseId);
    }
}
