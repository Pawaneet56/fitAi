package com.pawaneet.fitai.workout.event;

import java.time.Instant;
import java.util.UUID;

public record SetAddedEvent(
        UUID workoutId,
        UUID exerciseId,
        UUID setId,
        Integer setNumber,
        Double weight,
        Integer reps,
        Integer rir,
        Integer durationSeconds,
        String notes,
        Instant createdAt
) {
}