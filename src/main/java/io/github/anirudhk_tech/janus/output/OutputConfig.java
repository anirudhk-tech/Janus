package io.github.anirudhk_tech.janus.output;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OutputProperties.class)
class OutputConfig {}
