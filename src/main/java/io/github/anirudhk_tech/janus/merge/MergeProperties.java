package io.github.anirudhk_tech.janus.merge;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "janus.merge")
public record MergeProperties(
    String strategy
) {}


