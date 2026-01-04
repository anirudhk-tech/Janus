package io.github.anirudhk_tech.janus.connectors;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "janus.connectors")
public record ConnectorProperties (
    DbGroup supabase
) {
    public record DbGroup(
        Map<String, JbdcSource> sources
    ) {}

    public record JbdcSource(
        String jdbcUrl,
        String username,
        String password
    ) {}
}
