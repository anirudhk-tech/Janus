package io.github.anirudhk_tech.janus.capabilities;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "janus.capabilities")
public record CapabilitiesProperties (
    List<Source> sources
) {
    public record Source(
        String sourceId,
        String connector,
        String description,
        SqlHints sql
    ) {}

    public record SqlHints(
        String schema,
        List<String> tables
    ) {}
}
