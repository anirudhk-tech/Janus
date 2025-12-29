package io.github.anirudhk_tech.janus.plan;

import java.util.Map;

public record HttpRequestStep (
    String stepId,
    String connector,
    String method,
    String url,
    Map<String, String> headers,
    Map<String, String> queryParams
) implements PlanStep {

}
