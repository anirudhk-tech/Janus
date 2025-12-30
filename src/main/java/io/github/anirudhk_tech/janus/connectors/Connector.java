package io.github.anirudhk_tech.janus.connectors;

import io.github.anirudhk_tech.janus.federation.ExecutionContext;
import io.github.anirudhk_tech.janus.plan.PlanStep;

public interface Connector {
    String name();

    boolean supports(PlanStep step);

    ConnectorResult execute(PlanStep step, ExecutionContext context) throws ConnectorException;
}


