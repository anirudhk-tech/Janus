package io.github.anirudhk_tech.janus.federation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import io.github.anirudhk_tech.janus.connectors.Connector;
import io.github.anirudhk_tech.janus.plan.ExecutionPlan;
import io.github.anirudhk_tech.janus.plan.PlanStep;

@Service
public class FederationExecutor {
    
    private static final int DEFAULT_TIMEOUT_MS = 5_000;
    private final List<Connector> connectors;
    private final Executor executor;

    public FederationExecutor(List<Connector> connectors) {
        this.connectors = List.copyOf(connectors);
        this.executor = Executors.newFixedThreadPool(8);
    }

    public List<StepExecutionResult> execute(ExecutionPlan plan, ExecutionContext context, Integer timeoutMs) {
        Objects.requireNonNull(plan, "plan is required");
        Objects.requireNonNull(context, "context is required");

        int effectiveTimeoutMs = Optional.ofNullable(timeoutMs).orElse(DEFAULT_TIMEOUT_MS);
        Instant effectiveDeadline = context.now().plusMillis(effectiveTimeoutMs);
        ExecutionContext effectiveContext = new ExecutionContext(context.traceId(), effectiveDeadline, context.clock());

        List<CompletableFuture<StepExecutionResult>> futures = new ArrayList<>();

        for (PlanStep step : plan.steps()) {
            futures.add(executeOne(step, effectiveContext));
        }

        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        try {
            long waitMs = effectiveContext.remainingMillis();
            all.get(waitMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new FederationExecutionException("Failed to execute plan or timed out", e);
        }

        List<StepExecutionResult> results = new ArrayList<>();

        for (CompletableFuture<StepExecutionResult> f : futures) {
            StepExecutionResult r = f.join();
            if (r.status() != StepExecutionStatus.SUCCESS) {
                throw new FederationExecutionException("Step failed: stepId=" + r.stepId() + ", connector=" + r.connector() + ", error=" + r.error());
            }
            results.add(r);
        }

        return results;
    }

    // Todo: executeOne, etc
}
