package com.pawaneet.fitai.ai.client;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.pawaneet.fitai.ai.dto.AiPrompt;
import com.pawaneet.fitai.ai.dto.AiResponse;
import org.springframework.stereotype.Component;

@Component
public class GeminiAiClient implements AiClient {

    private final Client client;

    public GeminiAiClient() {
        this.client = new Client();
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

        GenerateContentResponse response = client.models.generateContent(
                "gemini-3.6-flash",
                combinedPrompt,
                null
        );

        return new AiResponse(response.text());
    }
}