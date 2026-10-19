package com.pawaneet.fitai.ai.client;

import com.google.genai.types.*;
import com.pawaneet.fitai.ai.dto.AiPrompt;
import com.pawaneet.fitai.workout.exception.ConflictException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class GeminiAiClientTest {
    private final String document = "fileSearchStores/test/documents/target";
    private final AiPrompt prompt = new AiPrompt("system", "request", null, "fileSearchStores/test", "user_id=\"user1\"", document);

    @Test
    void rejectsUngroundedOrWrongWorkoutAnswers() {
        assertThatThrownBy(() -> GeminiAiClient.validateGrounding(prompt, GenerateContentResponse.builder().build()))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> GeminiAiClient.validateGrounding(prompt, grounded("fileSearchStores/test/documents/other")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void acceptsOnlyResponsesGroundedInRequestedWorkout() {
        assertThatCode(() -> GeminiAiClient.validateGrounding(prompt, grounded(document))).doesNotThrowAnyException();
    }

    @Test
    void allowsExecutedRetrievalWithoutCitationsForSubsequentSourceFieldVerification() {
        var response = GenerateContentResponse.builder().usageMetadata(
                GenerateContentResponseUsageMetadata.builder().toolUsePromptTokenCount(100).build()).build();
        assertThatCode(() -> GeminiAiClient.validateGrounding(prompt, response)).doesNotThrowAnyException();
    }

    private GenerateContentResponse grounded(String name) {
        return GenerateContentResponse.builder().candidates(List.of(Candidate.builder().groundingMetadata(
                GroundingMetadata.builder().groundingChunks(List.of(GroundingChunk.builder().retrievedContext(
                        GroundingChunkRetrievedContext.builder().documentName(name).build()).build())).build()).build())).build();
    }
}
