package com.pawaneet.fitai.ai.dto;

import com.google.genai.types.Schema;

public record AiPrompt(
        String systemPrompt,
        String userPrompt,
        Schema responseSchema
) {
}