package io.github.anirudhk_tech.janus.agent;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.anirudhk_tech.janus.agent.llm.LlmClient;
import io.github.anirudhk_tech.janus.api.QueryRequest;
import io.github.anirudhk_tech.janus.plan.ExecutionPlan;
import io.github.anirudhk_tech.janus.plan.PlanStep;
import io.github.anirudhk_tech.janus.plan.SqlQueryStep;

@Service
@ConditionalOnProperty(name = "janus.agent.mode", havingValue = "llm")
public final class LlmQueryAgent implements QueryAgent {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public LlmQueryAgent(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
    }

    @Override
    public ExecutionPlan buildPlan(String question, QueryRequest.Options options) {
        Objects.requireNonNull(question, "question is required");

        String systemPrompt = systemPrompt();
        String userPrompt = userPrompt(question, options);

        String json = llmClient.generateJson(systemPrompt, userPrompt);
        ExecutionPlan plan = parsePlan(json);
        validatePlan(plan);
        return plan;
    }

    private ExecutionPlan parsePlan(String json) {
        try {
            return objectMapper.readValue(json, ExecutionPlan.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("LLM returned invalid ExecutionPlan JSON", e);
        }
    }

    private static void validatePlan(ExecutionPlan plan) {
        if (plan == null) throw new IllegalArgumentException("ExecutionPlan is null");
        if (plan.steps() == null || plan.steps().isEmpty()) throw new IllegalArgumentException("ExecutionPlan.steps is required");

        for (PlanStep step : plan.steps()) {
            if (step == null) throw new IllegalArgumentException("PlanStep must not be null");
            if (step.stepId() == null || step.stepId().isBlank()) throw new IllegalArgumentException("PlanStep.stepId is required");
            if (step.connector() == null || step.connector().isBlank()) throw new IllegalArgumentException("PlanStep.connector is required");

            if (step instanceof SqlQueryStep sql) {
                if (sql.sql() == null || sql.sql().isBlank()) throw new IllegalArgumentException("SqlQueryStep.sql is required");
            } else {
                throw new IllegalArgumentException("Unsupported PlanStep type: " + step.getClass().getName());
            }
        }
    }

    private static String systemPrompt() {
        return String.join("\n", List.of(
            """
            Return ONLY a JSON object that matches this schema:
            {
            "steps": [ ... one or more PlanStep objects ... ],
            "mergeStrategy": "template_merge_v1"
            }

            Each steps[i] MUST be one of:
            - type="sql": { "type":"sql", "stepId":"...", "connector":"postgres", "sql":"...", "params":{...} }

            You may return 1..N steps depending on the question. Prefer fewer steps.
            """
        ));
    }

    private static String userPrompt(String question, QueryRequest.Options options) {
        String timeoutMs = (options == null || options.timeoutMs() == null) ? "null" : options.timeoutMs().toString();
        return String.join("\n", List.of(
            "Question:",
            question,
            "",
            "Options:",
            "timeoutMs=" + timeoutMs
        ));
    }
}