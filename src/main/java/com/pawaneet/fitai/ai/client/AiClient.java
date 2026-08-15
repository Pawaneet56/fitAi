package com.pawaneet.fitai.ai.client;

import com.pawaneet.fitai.ai.dto.AiPrompt;
import com.pawaneet.fitai.ai.dto.AiResponse;

public interface AiClient {

    AiResponse generate(AiPrompt prompt);
}