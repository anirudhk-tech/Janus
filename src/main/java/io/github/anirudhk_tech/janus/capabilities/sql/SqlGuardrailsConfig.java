package io.github.anirudhk_tech.janus.capabilities.sql;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SqlGuardrailsProperties.class)
class SqlGuardrailsConfig {
}