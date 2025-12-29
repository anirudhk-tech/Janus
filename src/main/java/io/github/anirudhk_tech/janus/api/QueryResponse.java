package io.github.anirudhk_tech.janus.api;

import java.util.List;
import java.util.Map;

import io.github.anirudhk_tech.janus.plan.ExecutionPlan;

public record QueryResponse (
    String traceId,
    String answer,
    Map<String, Object> data,
    Explanation explanation
) {
    public record Explanation (
        ExecutionPlan plan,
        List<Object> execution
    ) {}
}
