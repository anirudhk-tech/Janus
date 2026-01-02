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
import com.fasterxml.jackson.annotation.JsonProperty;

@Component
@ConditionalOnProperty(name = "janus.llm.provider", havingValue = "openai")
public final class OpenAiLlmClient implements LlmClient {

    private final RestClient restClient;
    private final LlmProperties props;

    public OpenAiLlmClient(@Qualifier("openaiRestClient") RestClient openAiRestClient, LlmProperties props) {
        this.restClient = Objects.requireNonNull(openAiRestClient, "openAiRestClient is required");
        this.props = Objects.requireNonNull(props, "props is required");
    }

    @Override
    public String generateJson(String systemPrompt, String userPrompt) {
        Objects.requireNonNull(systemPrompt, "systemPrompt is required");
        Objects.requireNonNull(userPrompt, "userPrompt is required");

        String apiKey = props.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("janus.llm.openai.api-key is required");
        }

        ChatCompletionsRequest body = new ChatCompletionsRequest(
            props.getOpenai().getModel(),
            List.of(
                new Message("system", systemPrompt),
                new Message("user", userPrompt)
            ),
            0.0,
            new ResponseFormat("json_object")
        );

        try {
            ChatCompletionsResponse resp = restClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ChatCompletionsResponse.class);

            String text = extractFirstText(resp);
            String json = stripCodeFences(text);
            assertLooksLikeJson(json);
            return json;
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("OpenAI API call failed: status=" + e.getStatusCode() + " body=" + safeBody(e.getResponseBodyAsString()), e);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI client failed", e);
        }
    }

    private static String extractFirstText(ChatCompletionsResponse resp) {
        if (resp == null || resp.choices == null || resp.choices.isEmpty()) {
            throw new IllegalStateException("OpenAI returned no choices");
        }
        Choice c0 = resp.choices.getFirst();
        if (c0 == null || c0.message == null || c0.message.content == null || c0.message.content.isBlank()) {
            throw new IllegalStateException("OpenAI returned empty content");
        }
        return c0.message.content;
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
        if (body == null) return "";
        return body.length() > 2_000 ? body.substring(0, 2_000) + "...(truncated)" : body;
    }

    record ChatCompletionsRequest(
        String model,
        List<Message> messages,
        double temperature,
        @JsonProperty("response_format") ResponseFormat responseFormat
    ) {}

    record ResponseFormat(
        @JsonProperty("type") String type
    ) {}

    record Message(
        String role,
        String content
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class ChatCompletionsResponse {
        public List<Choice> choices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Choice {
        public Message message;
    }
}