package io.github.anirudhk_tech.janus.connectors;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import io.github.anirudhk_tech.janus.federation.ExecutionContext;
import io.github.anirudhk_tech.janus.plan.PlanStep;
import io.github.anirudhk_tech.janus.plan.SqlQueryStep;

@Service
public final class MockPostgresConnector implements Connector {
    public static final String CONNECTOR_NAME = "postgres";

    @Override
    public String name() {
        return CONNECTOR_NAME;
    }

    @Override
    public boolean supports(PlanStep step) {
        return step instanceof SqlQueryStep;
    }

    @Override
    public ConnectorResult execute(PlanStep step, ExecutionContext context) throws ConnectorException {
        Objects.requireNonNull(step, "step must not be null");
        Objects.requireNonNull(context, "context must not be null");

        if (context.isExpired()) {
            throw new ConnectorException("Request deadline exceeded before executing stepId=" + step.stepId());
        }

        if (!(step instanceof SqlQueryStep sqlStep)) {
            throw new ConnectorException("Unsupported step type for postgres connector: " + step.getClass().getName());
        }

        Map<String, Object> data = Map.of(
            "rows",
            List.of(Map.of("order_count", 42)),
            "sql",
            sqlStep.sql(),
            "params",
            sqlStep.params()
        );

        return new ConnectorResult(sqlStep.stepId(), name(), data);
    }
}