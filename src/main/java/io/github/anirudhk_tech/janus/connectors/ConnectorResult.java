package io.github.anirudhk_tech.janus.connectors;

import java.util.Map;

public record ConnectorResult(
    String stepId,
    String connector,
    Map<String, Object> data
) {}


