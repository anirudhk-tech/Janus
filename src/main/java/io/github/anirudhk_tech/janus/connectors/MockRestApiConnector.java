package io.github.anirudhk_tech.janus.connectors;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.anirudhk_tech.janus.federation.ExecutionContext;
import io.github.anirudhk_tech.janus.plan.HttpRequestStep;
import io.github.anirudhk_tech.janus.plan.PlanStep;

public final class MockRestApiConnector implements Connector {
    public static final String CONNECTOR_NAME = "rest";

    @Override
    public String name() {
        return CONNECTOR_NAME;
    }

    @Override
    public boolean supports(PlanStep step) {
        return step instanceof HttpRequestStep;
    }

    @Override
    public ConnectorResult execute(PlanStep step, ExecutionContext context) throws ConnectorException {
        Objects.requireNonNull(step, "step must not be null");
        Objects.requireNonNull(context, "context must not be null");

        if (context.isExpired()) {
            throw new ConnectorException("Request deadline exceeded before executing stepId=" + step.stepId());
        }

        if (!(step instanceof HttpRequestStep httpStep)) {
            throw new ConnectorException("Unsupported step type for rest connector: " + step.getClass().getName());
        }

        Map<String, Object> data = Map.of(
            "status",
            200,
            "events",
            List.of(
                Map.of("type", "PushEvent", "repo", "octocat/Hello-World"),
                Map.of("type", "IssueCommentEvent", "repo", "octocat/Hello-World")
            ),
            "method",
            httpStep.method(),
            "url",
            httpStep.url(),
            "headers",
            httpStep.headers(),
            "queryParams",
            httpStep.queryParams()
        );

        return new ConnectorResult(httpStep.stepId(), name(), data);
    }
}


