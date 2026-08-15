package com.pawaneet.fitai.ai.service;

import com.pawaneet.fitai.ai.client.AiClient;
import com.pawaneet.fitai.ai.dto.AiPrompt;
import com.pawaneet.fitai.ai.dto.AiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiClient aiClient;

    public AiResponse generate(AiPrompt prompt) {
        return aiClient.generate(prompt);
    }
}