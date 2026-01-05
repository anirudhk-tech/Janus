package io.github.anirudhk_tech.janus.agent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.anirudhk_tech.janus.agent.llm.LlmClient;
import io.github.anirudhk_tech.janus.api.QueryRequest;
import io.github.anirudhk_tech.janus.capabilities.CapabilitiesProperties;
import io.github.anirudhk_tech.janus.capabilities.CapabilitiesService;
import io.github.anirudhk_tech.janus.capabilities.sql.SqlSchema;
import io.github.anirudhk_tech.janus.capabilities.sql.SqlSchemaService;
import io.github.anirudhk_tech.janus.plan.ExecutionPlan;
import io.github.anirudhk_tech.janus.plan.PlanStep;
import io.github.anirudhk_tech.janus.plan.SqlQueryStep;

@Service
@ConditionalOnProperty(name = "janus.agent.mode", havingValue = "llm")
public final class LlmQueryAgent implements QueryAgent {
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final CapabilitiesService capabilitiesService;
    private final SqlSchemaService sqlSchemaService;

    public LlmQueryAgent(LlmClient llmClient, ObjectMapper objectMapper, CapabilitiesService capabilitiesService, SqlSchemaService sqlSchemaService
    ) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
        this.capabilitiesService = Objects.requireNonNull(capabilitiesService, "capabilitiesService is required");
        this.sqlSchemaService = Objects.requireNonNull(sqlSchemaService, "sqlSchemaService is required");
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
                if (sql.sourceId() == null || sql.sourceId().isBlank()) throw new IllegalArgumentException("SqlQueryStep.sourceId is required");
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
            "steps": [ ... one or more PlanStep objects ... ]
            }

            Each steps[i] MUST be one of:
            - type="sql": { "type":"sql", "stepId":"...", "connector":"<from capabilities>", "sourceId":"<from capabilities>", "sql":"...", "params":{...} }

            You will receive Capabilities JSON in the user message:
            { "sources": [ { "sourceId":"...", "connector":"...", ... }, ... ] }

            Rules:
            - connector MUST equal one of Capabilities.sources[*].connector
            - sourceId MUST equal one of Capabilities.sources[*].sourceId
            - The (connector, sourceId) pair MUST match a single item in Capabilities.sources

            You may return 1..N steps depending on the question. Prefer fewer steps.
            """
        ));
    }

    private String userPrompt(String question, QueryRequest.Options options) {
        String timeoutMs = (options == null || options.timeoutMs() == null) ? "null" : options.timeoutMs().toString();
        String capabilitiesJson;

        try {
            Map<String, Object> capabilities = new HashMap<>();
            capabilities.put("sources", buildPromptSources());
            capabilitiesJson = objectMapper.writeValueAsString(capabilities);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize capabilities for LLM prompt", e);
        }
        return String.join("\n", List.of(
            "Question:",
            question,
            "",
            "Options:",
            "timeoutMs=" + timeoutMs,
            "",
            "Capabilities (JSON):",
            capabilitiesJson
        ));
    }

    private List<Map<String, Object>> buildPromptSources() {
        List<CapabilitiesProperties.Source> sources = capabilitiesService.sources();

        return sources.stream().map(s -> {
            Map<String, Object> src = new HashMap<>();
            src.put("sourceId", s.sourceId());
            src.put("connector", s.connector());
            src.put("description", s.description());

            if (s.sql() != null) {
                SqlSchema schema = sqlSchemaService.describe(s.connector(), s.sourceId());

                Map<String, Object> sql = new HashMap<>();
                sql.put("schema", schema.schema());
                sql.put("tables", schema.tables());
                src.put("sql", sql);
            }

            return src;
        }).toList();
    }
}