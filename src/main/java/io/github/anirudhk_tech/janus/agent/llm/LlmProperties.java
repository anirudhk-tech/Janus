package io.github.anirudhk_tech.janus.agent.llm;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "janus.llm")
public class LlmProperties {
    
    private String provider = "gemini";
    private Duration timeout = Duration.ofSeconds(10);
    private final Gemini gemini = new Gemini();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }

    public Gemini getGemini() { return gemini; }

    public static final class Gemini {

        private URI baseUrl = URI.create("https://generativelanguage.googleapis.com");
        private String model = "gemini-2.0-flash";
        private String apiKey;

        public URI getBaseUrl() { return baseUrl; }
        public void setBaseUrl(URI baseUrl) { this.baseUrl = baseUrl; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }
}
