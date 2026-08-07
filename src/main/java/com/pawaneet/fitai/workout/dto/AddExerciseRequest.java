package com.pawaneet.fitai.workout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddExerciseRequest(
        @NotBlank String name,
        @NotNull Integer orderIndex
) {
}
