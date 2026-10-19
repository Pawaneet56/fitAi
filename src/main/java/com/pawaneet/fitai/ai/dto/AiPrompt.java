package com.pawaneet.fitai.ai.dto;

import com.google.genai.types.Schema;

public record AiPrompt(
        String systemPrompt,
        String userPrompt,
        Schema responseSchema,
        String fileSearchStore,
        String metadataFilter,
        String requiredDocumentName
) {
    public AiPrompt(String systemPrompt, String userPrompt, Schema responseSchema) {
        this(systemPrompt, userPrompt, responseSchema, null, null, null);
    }
}