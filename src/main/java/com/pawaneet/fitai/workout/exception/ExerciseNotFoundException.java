package com.pawaneet.fitai.workout.exception;

import java.util.UUID;

public class ExerciseNotFoundException extends RuntimeException {

    public ExerciseNotFoundException(UUID exerciseId) {
        super("Exercise not found with id: " + exerciseId);
    }
}
