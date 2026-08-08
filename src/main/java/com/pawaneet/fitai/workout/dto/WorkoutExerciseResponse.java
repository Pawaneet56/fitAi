package com.pawaneet.fitai.workout.dto;

import java.util.List;
import java.util.UUID;

public record WorkoutExerciseResponse(
        UUID id,
        String exerciseName,
        Integer orderIndex,
        List<WorkoutSetResponse> sets
) {
}
