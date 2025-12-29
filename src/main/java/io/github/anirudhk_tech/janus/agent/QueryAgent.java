package io.github.anirudhk_tech.janus.agent;

import io.github.anirudhk_tech.janus.api.QueryRequest;
import io.github.anirudhk_tech.janus.plan.ExecutionPlan;

public interface QueryAgent {
    ExecutionPlan buildPlan(String question, QueryRequest.Options options);
}
