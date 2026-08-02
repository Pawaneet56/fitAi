package com.pawaneet.fitai.workout.event;

import java.time.Instant;
import java.util.UUID;

public record WorkoutEndedEvent(
        UUID workoutId,
        Instant startedAt,
        Instant endedAt,
        long durationSeconds
) {
}
