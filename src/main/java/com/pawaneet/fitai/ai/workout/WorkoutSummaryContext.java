package com.pawaneet.fitai.ai.workout;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkoutSummaryContext(
        UUID workoutId,
        Instant startedAt,
        Instant endedAt,
        String notes,
        List<ExerciseContext> exercises
) {

    public record ExerciseContext(
            String exerciseName,
            Integer orderIndex,
            List<SetContext> sets
    ) {
    }

    public record SetContext(
            Integer setNumber,
            Double weight,
            Integer reps,
            Integer rir,
            Integer durationSeconds,
            String notes
    ) {
    }
}