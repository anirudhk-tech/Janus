package io.github.anirudhk_tech.janus.plan;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)

@JsonSubTypes({
    @JsonSubTypes.Type(value = SqlQueryStep.class, name = "sql"),
    @JsonSubTypes.Type(value = HttpRequestStep.class, name = "http")
})

public sealed interface PlanStep 
    permits SqlQueryStep, HttpRequestStep {
    
        String stepId();

        String connector();

}
