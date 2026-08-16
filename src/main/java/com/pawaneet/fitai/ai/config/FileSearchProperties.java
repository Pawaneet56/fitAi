package com.pawaneet.fitai.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fitai.ai.file-search")
public record FileSearchProperties(
        String storeName
) {
}