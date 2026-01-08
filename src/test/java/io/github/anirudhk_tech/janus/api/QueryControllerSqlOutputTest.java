package io.github.anirudhk_tech.janus.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.github.anirudhk_tech.janus.TestSupportConfig;

@SpringBootTest(properties = {
    "JANUS_API_KEY=test-key",
    "janus.agent.mode=test",
    "janus.merge.strategy=json-shallow-merge-v1",
    "janus.sql.guardrails.enabled=false",
    "janus.output.sql=true",
    "janus.output.color=false"
})
@AutoConfigureMockMvc
@Import(TestSupportConfig.class)
class QueryControllerSqlOutputTest {

    @Autowired
    MockMvc mvc;

    @Test
    void sqlOutputEnabled_returnsPlainTextTable() throws Exception {
        mvc.perform(
            post("/query")
                .header("X-API-Key", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"hello\"}")
        )
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(containsString("traceId:")))
            .andExpect(content().string(containsString("Step step_test_sql (postgres)")))
            .andExpect(content().string(containsString("SQL:")))
            .andExpect(content().string(containsString("| ok |")))
            .andExpect(content().string(containsString("(1 row)")));
    }
}
