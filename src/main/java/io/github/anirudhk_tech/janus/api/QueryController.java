package io.github.anirudhk_tech.janus.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.anirudhk_tech.janus.agent.QueryAgent;
import io.github.anirudhk_tech.janus.plan.ExecutionPlan;
import jakarta.validation.Valid;

@RestController
public class QueryController {

    private final QueryAgent queryAgent;

    public QueryController(QueryAgent queryAgent) {
        this.queryAgent = queryAgent;
    }

    @PostMapping("/query")
    public QueryResponse query(@Valid @RequestBody QueryRequest request) {
        String traceId = UUID.randomUUID().toString(); // mock for now

        ExecutionPlan plan = queryAgent.buildPlan(request.question(), request.options());

        return new QueryResponse(
            traceId,
            "planned",
            Map.of(),
            new QueryResponse.Explanation(
                plan,
                List.of()            // TODO: add execution results
            )
        );
    }
}
