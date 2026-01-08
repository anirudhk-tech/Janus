package io.github.anirudhk_tech.janus.output;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "janus.output")
public record OutputProperties(
    boolean sql,
    Boolean color
) {}
