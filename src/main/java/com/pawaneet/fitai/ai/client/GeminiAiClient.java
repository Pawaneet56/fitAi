package com.pawaneet.fitai.ai.client;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.pawaneet.fitai.ai.dto.AiPrompt;
import com.pawaneet.fitai.ai.dto.AiResponse;
import org.springframework.stereotype.Component;

@Component
public class GeminiAiClient implements AiClient {

    private final Client client;
    @org.springframework.beans.factory.annotation.Value("${fitai.ai.model:gemini-3.6-flash}")
    private String model;

    public GeminiAiClient() {
        this.client = Client.builder().httpOptions(com.google.genai.types.HttpOptions.builder().timeout(180000).build()).build();
    }

    @Override
    public AiResponse generate(AiPrompt prompt) {

        String combinedPrompt = """
                System Instructions:
                %s

                User Request:
                %s
                """.formatted(
                prompt.systemPrompt(),
                prompt.userPrompt()
        );

        GenerateContentConfig.Builder config = GenerateContentConfig.builder();
        // Keep retrieval free of generation-schema constraints; validate its JSON at the API boundary.
        if (prompt.fileSearchStore() == null) {
            config.responseMimeType("application/json").responseSchema(prompt.responseSchema());
        }
        if (prompt.fileSearchStore() != null) {
            config.tools(com.google.genai.types.Tool.builder().fileSearch(
                    com.google.genai.types.FileSearch.builder()
                            .fileSearchStoreNames(java.util.List.of(prompt.fileSearchStore()))
                            .metadataFilter(prompt.metadataFilter()).build()).build());
        }

        GenerateContentResponse response = client.models.generateContent(
                model, combinedPrompt, config.build());

        validateGrounding(prompt, response);
        return new AiResponse(response.text());
    }
    static void validateGrounding(AiPrompt prompt, GenerateContentResponse response) {
        if (prompt.requiredDocumentName() != null) {
            boolean targetRetrieved = response.candidates().orElse(java.util.List.of()).stream()
                    .flatMap(candidate -> candidate.groundingMetadata().stream())
                    .flatMap(grounding -> grounding.groundingChunks().orElse(java.util.List.of()).stream())
                    .flatMap(chunk -> chunk.retrievedContext().stream())
                    .anyMatch(context -> prompt.requiredDocumentName().equals(context.documentName().orElse(null))
                            || prompt.requiredDocumentName().equals(context.uri().orElse(null)));
            boolean usedRetrieval = response.usageMetadata()
                    .flatMap(com.google.genai.types.GenerateContentResponseUsageMetadata::toolUsePromptTokenCount)
                    .orElse(0) > 0;
            if (!targetRetrieved && !usedRetrieval) {
                throw new com.pawaneet.fitai.workout.exception.ConflictException(
                        "The requested workout was not retrieved from File Search. Please retry shortly.");
            }
            // When citations are omitted, WorkoutSummaryService verifies unprompted source fields.

        }
    }
}