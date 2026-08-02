package com.pawaneet.fitai.workout.dto;

import java.util.UUID;

public record WorkoutSetResponse(
        UUID id,
        Integer setNumber,
        Double weight,
        Integer reps,
        Integer rir,
        Integer durationSeconds,
        String notes
) {
}
