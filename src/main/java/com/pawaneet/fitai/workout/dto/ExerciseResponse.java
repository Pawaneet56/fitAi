package com.pawaneet.fitai.workout.dto;

import java.time.Instant;
import java.util.UUID;

public record ExerciseResponse(
        UUID id,
        UUID workoutId,
        String name,
        Integer orderIndex,
        Instant createdAt
) {
}
