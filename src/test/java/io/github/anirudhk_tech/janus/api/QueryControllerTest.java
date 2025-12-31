package io.github.anirudhk_tech.janus.api;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "JANUS_API_KEY=test-key",
    "janus.agent.mode=rule_based"
})
@AutoConfigureMockMvc
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
            .andExpect(jsonPath("$.explanation.plan.steps.length()").value(2))
            .andExpect(jsonPath("$.explanation.plan.steps[0].type").value("sql"))
            .andExpect(jsonPath("$.explanation.plan.steps[0].sql").exists())
            .andExpect(jsonPath("$.explanation.plan.steps[1].type").value("http"))
            .andExpect(jsonPath("$.explanation.plan.steps[1].method").value("GET"))
            .andExpect(jsonPath("$.explanation.plan.steps[1].url").exists());
    }
}