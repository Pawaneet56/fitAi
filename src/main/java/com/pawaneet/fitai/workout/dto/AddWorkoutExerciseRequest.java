package com.pawaneet.fitai.workout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddWorkoutExerciseRequest(
        @NotBlank(message = "Exercise name is required")
        @Size(max = 255, message = "Exercise name must be at most 255 characters")
        String exerciseName
) {
}
