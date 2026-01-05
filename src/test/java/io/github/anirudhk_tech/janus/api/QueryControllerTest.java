package io.github.anirudhk_tech.janus.api;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.github.anirudhk_tech.janus.TestSupportConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "JANUS_API_KEY=test-key",
    // Disable LlmQueryAgent in tests; use TestSupportConfig's deterministic QueryAgent instead.
    "janus.agent.mode=test",
    // Guardrails require real capabilities + schema introspection; keep tests hermetic.
    "janus.sql.guardrails.enabled=false"
})
@AutoConfigureMockMvc
@Import(TestSupportConfig.class)
class QueryControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void missingKey_is401() throws Exception {
        mvc.perform(
            post("/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"hello\"}")
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void correctKey_validBody_is200_andReturnsTraceIdAndAnswer() throws Exception {
        mvc.perform(
            post("/query")
                .header("X-API-Key", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"hello\"}")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.traceId").isNotEmpty())
            .andExpect(jsonPath("$.answer").value("executed"))
            .andExpect(jsonPath("$.explanation.plan.steps").isArray())
            .andExpect(jsonPath("$.explanation.plan.steps.length()").value(1))
            .andExpect(jsonPath("$.explanation.plan.steps[0].type").value("sql"))
            .andExpect(jsonPath("$.explanation.plan.steps[0].sql").exists())
            .andExpect(jsonPath("$.explanation.plan.mergeStrategy").value("json-shallow-merge-v1"))
            .andExpect(jsonPath("$.data.sources.postgres.step_test_sql").exists())
            .andExpect(jsonPath("$.data.sources.postgres.step_test_sql.rows[0].ok").value(1))
            .andExpect(jsonPath("$.data.merged.rows[0].ok").value(1));
    }
}