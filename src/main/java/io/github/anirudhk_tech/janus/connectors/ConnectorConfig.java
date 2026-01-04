package io.github.anirudhk_tech.janus.connectors;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ConnectorProperties.class)
class ConnectorConfig {
    
}
