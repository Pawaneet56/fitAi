package com.pawaneet.fitai.workout.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateWorkoutSetRequest(
        @Positive Double weight,
        @Positive Integer reps,
        @PositiveOrZero Integer rir,
        @PositiveOrZero Integer durationSeconds,
        String notes
) {
}