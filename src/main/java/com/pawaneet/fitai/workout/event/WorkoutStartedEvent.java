package com.pawaneet.fitai.workout.event;

import java.time.Instant;
import java.util.UUID;

public record WorkoutStartedEvent(
        UUID workoutId,
        Instant startedAt
) {}