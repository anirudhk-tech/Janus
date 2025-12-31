package io.github.anirudhk_tech.janus.agent.llm;

public interface LlmClient {
    
    String generateJson(String systemPrompt, String userPrompt);

}
