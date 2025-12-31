package io.github.anirudhk_tech.janus.agent.llm;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {
    
    @Bean
    RestClient geminiRestClient(LlmProperties props) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(props.getTimeout())
            .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(props.getTimeout());

        return RestClient.builder()
            .baseUrl(props.getGemini().getBaseUrl().toString())
            .requestFactory((ClientHttpRequestFactory) factory)
            .build();
    }
}
