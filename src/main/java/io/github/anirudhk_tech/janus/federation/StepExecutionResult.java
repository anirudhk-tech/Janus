package io.github.anirudhk_tech.janus.federation;

import java.util.Map;

public record StepExecutionResult (
    String stepId,
    String connector,
    StepExecutionStatus status,
    long durationMs,
    Map<String, Object> data,
    String error
) {}
