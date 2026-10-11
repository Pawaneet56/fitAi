package com.pawaneet.fitai.analytics.dto;

import java.util.List;
import java.util.UUID;

public record WorkoutMetrics(
        int totalExercises,
        int totalSets,
        long totalReps,
        double totalVolume,
        Double highestWeight,
        Double averageRir,
        List<ExerciseMetrics> exercises
) {
    public record ExerciseMetrics(
            UUID exerciseId,
            String exerciseName,
            int totalSets,
            long totalReps,
            double totalVolume,
            Double highestWeight,
            Double averageRir
    ) {}
}
