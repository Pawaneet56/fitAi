package com.pawaneet.fitai.workout.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AddWorkoutSetRequest(
        @NotNull(message = "Weight is required")
        @PositiveOrZero(message = "Weight must be greater than or equal to 0")
        Double weight,

        @NotNull(message = "Reps are required")
        @Positive(message = "Reps must be greater than 0")
        Integer reps,

        @Min(value = 0, message = "RIR must be greater than or equal to 0")
        Integer rir,

        @Min(value = 0, message = "Duration seconds must be greater than or equal to 0")
        Integer durationSeconds,

        @Size(max = 1000, message = "Notes must be at most 1000 characters")
        String notes
) {
}
