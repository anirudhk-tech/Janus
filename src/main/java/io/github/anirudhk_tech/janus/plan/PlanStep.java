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
})

public sealed interface PlanStep 
    permits SqlQueryStep {
    
        String stepId();

        String connector();

}
