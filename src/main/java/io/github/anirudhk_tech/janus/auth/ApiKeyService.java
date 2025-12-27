package io.github.anirudhk_tech.janus.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


@Service
public class ApiKeyService {
    private final String expectedApiKey;

    public ApiKeyService(@Value("${JANUS_API_KEY}") String expectedApiKey) {
        if (!StringUtils.hasText(expectedApiKey)) {
            throw new IllegalStateException("JANUS_API_KEY is not set (required for API-key auth).");
        }
        this.expectedApiKey = expectedApiKey;
    }

    public boolean isValid(String presentedApiKey) {
        return StringUtils.hasText(presentedApiKey) && presentedApiKey.equals(expectedApiKey);
    }
}
