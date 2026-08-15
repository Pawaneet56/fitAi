package com.pawaneet.fitai.ai.dto;

import java.util.List;

public record WorkoutSummaryResponse(
        String summary,
        List<String> observations,
        List<String> suggestions
) {
}