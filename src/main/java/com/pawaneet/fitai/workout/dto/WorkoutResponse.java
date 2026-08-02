package com.pawaneet.fitai.workout.dto;

import com.pawaneet.fitai.workout.entity.WorkoutStatus;

import java.time.Instant;
import java.util.UUID;

public record WorkoutResponse(
        UUID id,
        Instant startedAt,
        Instant endedAt,
        Long durationSeconds,
        WorkoutStatus status,
        String notes
) {
}
