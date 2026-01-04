package io.github.anirudhk_tech.janus.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import io.github.anirudhk_tech.janus.TestSupportConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class ApiKeyAuthTest {

    @Autowired
    MockMvc mvc;

    @Test
    void missingKey_is401() throws Exception {
        mvc.perform(get("/protected/ping"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongKey_is401() throws Exception {
        mvc.perform(get("/protected/ping").header("X-API-Key", "nope"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void correctKey_is200() throws Exception {
        mvc.perform(get("/protected/ping").header("X-API-Key", "test-key"))
            .andExpect(status().isOk())
            .andExpect(content().string("pong"));
    }
}


