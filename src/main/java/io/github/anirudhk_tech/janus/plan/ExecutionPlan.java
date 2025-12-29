package io.github.anirudhk_tech.janus.plan;

import java.util.List;

public record ExecutionPlan (
    List<PlanStep> steps,
    String mergeStrategy
) {

}


