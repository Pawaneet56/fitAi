package com.pawaneet.fitai.workout.event;

import java.time.Instant;
import java.util.UUID;

public record ExerciseAddedEvent(
        UUID workoutId,
        UUID exerciseId,
        String exerciseName,
        Integer orderIndex
) {
}
