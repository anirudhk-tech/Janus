package io.github.anirudhk_tech.janus.api;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import io.github.anirudhk_tech.janus.merge.MergeService;
import io.github.anirudhk_tech.janus.merge.MergeProperties;
import io.github.anirudhk_tech.janus.plan.ExecutionPlan;
import jakarta.validation.Valid;

@RestController
public class QueryController {

    private final QueryAgent queryAgent;
    private final FederationExecutor federationExecutor;
    private final Clock clock;
    private final MergeService mergeService;
    private final MergeProperties mergeProperties;

    public QueryController(
        QueryAgent queryAgent,
        FederationExecutor federationExecutor,
        MergeService mergeService,
        MergeProperties mergeProperties
    ) {
        this.queryAgent = queryAgent;
        this.federationExecutor = federationExecutor;
        this.clock = Clock.systemUTC();
        this.mergeService = mergeService;
        this.mergeProperties = mergeProperties;
    }

    @PostMapping("/query")
    public QueryResponse query(@Valid @RequestBody QueryRequest request) {
        String traceId = UUID.randomUUID().toString();
        // Merge strategy is server-controlled (from config), not planner-controlled.
        ExecutionPlan plannerPlan = queryAgent.buildPlan(request.question(), request.options());
        String effectiveMergeStrategy =
            (mergeProperties.strategy() == null || mergeProperties.strategy().isBlank())
                ? "json-shallow-merge-v1"
                : mergeProperties.strategy();
        ExecutionPlan plan = new ExecutionPlan(plannerPlan.steps(), effectiveMergeStrategy);
        ExecutionContext context = new ExecutionContext(traceId, Instant.now(clock), clock);
        List<StepExecutionResult> execution = federationExecutor.execute(plan, context, request.options() == null ? null : request.options().timeoutMs());
        Map<String, Object> data = new HashMap<>();
        boolean shouldExplain = request.options() != null && Boolean.TRUE.equals(request.options().explain());
        if (shouldExplain) {
            data.put("sources", buildSources(execution));
        }
        data.put("merged", mergeService.merge(plan, execution));
        QueryResponse.Explanation explanation = shouldExplain ? new QueryResponse.Explanation(plan, execution) : null;
        return new QueryResponse(traceId, "executed", data, explanation);
    }

    private Map<String, Object> buildSources(List<StepExecutionResult> execution) {
        Map<String, Object> sources = new LinkedHashMap<>();
      
        for (StepExecutionResult result : execution) {
          @SuppressWarnings("unchecked")
          Map<String, Object> perConnector =
            (Map<String, Object>) sources.computeIfAbsent(result.connector(), k -> new LinkedHashMap<>());
      
          perConnector.put(result.stepId(), result.data());
        }
      
        return sources;
      }
}
