package io.github.anirudhk_tech.janus.api;

import java.util.List;
import java.util.Map;

public record QueryResponse (
    String traceId,
    String answer,
    Map<String, Object> data,
    Explanation explanation
) {
    public record Explanation (
        List<Object> plan,
        List<Object> execution,
        String merge
    ) {}
}
