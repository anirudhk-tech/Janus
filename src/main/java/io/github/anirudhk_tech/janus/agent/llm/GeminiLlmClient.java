package io.github.anirudhk_tech.janus.agent.llm;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "janus.llm.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiLlmClient implements LlmClient {
    
    private final RestClient restClient;
    private final LlmProperties props;

    public GeminiLlmClient(@Qualifier("geminiRestClient") RestClient geminiRestClient, ObjectMapper objectMapper, LlmProperties props) {
        this.restClient = Objects.requireNonNull(geminiRestClient, "geminiRestClient is required");
        Objects.requireNonNull(objectMapper, "objectMapper is required");
        this.props = Objects.requireNonNull(props, "props is required");
    }

    @Override
    public String generateJson(String systemPrompt, String userPrompt) {
        Objects.requireNonNull(systemPrompt, "systemPrompt is required");
        Objects.requireNonNull(userPrompt, "userPrompt is required");

        String apiKey = props.getGemini().getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("janus.llm.gemini.api-key is required");
        }

        GeminiGenerateContentRequest body = GeminiGenerateContentRequest.forPrompts(systemPrompt, userPrompt);

        try {
            GeminiGenerateContentResponse response = restClient.post()
                .uri("/v1/models/{model}:generateContent", props.getGemini().getModel())
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(GeminiGenerateContentResponse.class);

            String text = extractFirstText(response);
            String json = stripCodeFences(text);
            assertLooksLikeJson(json);
            return json;
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Gemini API call failed: status=" + e.getStatusCode() + " body=" + safeBody(e.getResponseBodyAsString()), e);
        } catch (Exception e) {
            throw new IllegalStateException("Gemini client failed", e);
        }
    }

    private static String extractFirstText(GeminiGenerateContentResponse response) {
        if (response == null || response.candidates == null || response.candidates.isEmpty()) {
            throw new IllegalStateException("Gemini returned no candidates");
        }
        Candidate c0 = response.candidates.getFirst();
        if (c0 == null || c0.content == null || c0.content.parts == null || c0.content.parts.isEmpty()) {
            throw new IllegalStateException("Gemini returned empty content");
        }
        Part p0 = c0.content.parts.getFirst();
        if (p0 == null || p0.text == null || p0.text.isBlank()) {
            throw new IllegalStateException("Gemini returned empty text");
        }
        return p0.text;
    }

    private static String stripCodeFences(String s) {
        String x = s.trim();
        if (x.startsWith("```")) {
            x = x.replaceFirst("^```[a-zA-Z]*\\s*", "");
            x = x.replaceFirst("\\s*```\\s*$", "");
        }
        return x.trim();
    }

    private static void assertLooksLikeJson(String s) {
        String x = s.trim();
        if (!(x.startsWith("{") && x.endsWith("}"))) {
            throw new IllegalStateException("LLM did not return a JSON object");
        }
    }

    private static String safeBody(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 2_000 ? body.substring(0, 2_000) + "...(truncated)" : body;
    }

    record GeminiGenerateContentRequest(
        // Content systemPrompt,
        List<Content> contents,
        GenerationConfig generationConfig
    ) {
        static GeminiGenerateContentRequest forPrompts(String systemPrompt, String userPrompt) {
            Content sys = Content.system(systemPrompt);
            Content user = Content.user(userPrompt);
            GenerationConfig cfg = new GenerationConfig(0.0);
            return new GeminiGenerateContentRequest(List.of(sys, user), cfg);
        }
    }

    record GenerationConfig(
        double temperature
    ) {}

    record Content(
        String role,
        List<Part> parts
    ) {
        static Content system(String systemPrompt) {
            return new Content("system", List.of(new Part(systemPrompt)));
        }

        static Content user(String userPrompt) {
            return new Content("user", List.of(new Part(userPrompt)));
        }
    }

    record Part(
        String text
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class GeminiGenerateContentResponse {
        public List<Candidate> candidates;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Candidate {
        public Content content;
    }
}
