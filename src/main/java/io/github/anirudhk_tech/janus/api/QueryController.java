package io.github.anirudhk_tech.janus.api;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.anirudhk_tech.janus.agent.QueryAgent;
import io.github.anirudhk_tech.janus.federation.ExecutionContext;
import io.github.anirudhk_tech.janus.federation.FederationExecutor;
import io.github.anirudhk_tech.janus.federation.StepExecutionResult;
import io.github.anirudhk_tech.janus.plan.ExecutionPlan;
import jakarta.validation.Valid;

@RestController
public class QueryController {

    private final QueryAgent queryAgent;
    private final FederationExecutor federationExecutor;
    private final Clock clock;

    public QueryController(QueryAgent queryAgent, FederationExecutor federationExecutor) {
        this.queryAgent = queryAgent;
        this.federationExecutor = federationExecutor;
        this.clock = Clock.systemUTC();
    }

    @PostMapping("/query")
    public QueryResponse query(@Valid @RequestBody QueryRequest request) {
        String traceId = UUID.randomUUID().toString();
        ExecutionPlan plan = queryAgent.buildPlan(request.question(), request.options());
        ExecutionContext context = new ExecutionContext(traceId, Instant.now(clock), clock);
        List<StepExecutionResult> execution = federationExecutor.execute(plan, context, request.options() == null ? null : request.options().timeoutMs());
        Map<String, Object> data = new HashMap<>();
        data.put("sources", buildSources(execution));
        return new QueryResponse(traceId, "executed", data, new QueryResponse.Explanation(plan, execution));
    }

    private Map<String, Object> buildSources(List<StepExecutionResult> execution) {
        Map<String, Object> sources = new HashMap<>();
        for (StepExecutionResult result : execution) {
            sources.put(result.connector(), result.data());
        }
        return sources;
    }
}
