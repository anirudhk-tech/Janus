package io.github.anirudhk_tech.janus.plan;

import java.util.Map;

public record SqlQueryStep (
    String stepId,
    String connector,
    String sql,
    Map<String, Object> params
) implements PlanStep {
    
}
