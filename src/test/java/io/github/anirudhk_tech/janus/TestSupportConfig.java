package io.github.anirudhk_tech.janus;

import java.util.List;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import io.github.anirudhk_tech.janus.agent.QueryAgent;
import io.github.anirudhk_tech.janus.connectors.Connector;
import io.github.anirudhk_tech.janus.connectors.ConnectorResult;
import io.github.anirudhk_tech.janus.federation.ExecutionContext;
import io.github.anirudhk_tech.janus.plan.ExecutionPlan;
import io.github.anirudhk_tech.janus.plan.PlanStep;
import io.github.anirudhk_tech.janus.plan.SqlQueryStep;

/**
 * Shared test-only beans so MVC tests can load the full Spring context without:
 * - requiring real LLM API keys
 * - requiring a real Postgres connection
 *
 * This keeps tests focused on controller/auth wiring and response structure.
 */
@TestConfiguration
public class TestSupportConfig {

    @Bean
    public QueryAgent testQueryAgent() {
        return (question, options) -> new ExecutionPlan(
            List.of(new SqlQueryStep(
                "step_test_sql",
                "postgres",
                "test",
                "SELECT 1 AS ok",
                Map.of()
            )),
            "json-shallow-merge-v1"
        );
    }

    /**
     * Highest precedence "postgres" connector stub. This ensures {@code FederationExecutor}
     * does not attempt to use the real {@code PostgresConnector} in tests.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public Connector testPostgresConnector() {
        return new Connector() {
            @Override
            public String name() {
                return "postgres";
            }

            @Override
            public boolean supports(PlanStep step) {
                return step instanceof SqlQueryStep;
            }

            @Override
            public ConnectorResult execute(PlanStep step, ExecutionContext context) {
                SqlQueryStep sql = (SqlQueryStep) step;
                return new ConnectorResult(
                    sql.stepId(),
                    sql.connector(),
                    Map.of(
                        "rows", List.of(Map.of("ok", 1)),
                        "sql", sql.sql(),
                        "params", sql.params()
                    )
                );
            }
        };
    }
}


