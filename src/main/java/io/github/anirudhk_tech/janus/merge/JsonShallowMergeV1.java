package io.github.anirudhk_tech.janus.merge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.github.anirudhk_tech.janus.federation.StepExecutionResult;
import io.github.anirudhk_tech.janus.plan.ExecutionPlan;
import io.github.anirudhk_tech.janus.plan.PlanStep;

@Component
public final class JsonShallowMergeV1 implements MergeStrategy {
    
    @Override
    public String name() {
        return "json-shallow-merge-v1";
    }

    @Override
    public Map<String, Object> merge(ExecutionPlan plan, List<StepExecutionResult> execution) {
        Map<String, StepExecutionResult> byStepId = new LinkedHashMap<>();
        for (StepExecutionResult r : execution) {
            byStepId.put(r.stepId(), r);
        }

        Map<String, Object> merged = new LinkedHashMap<>();

        for (PlanStep step : plan.steps()) {
            StepExecutionResult r = byStepId.get(step.stepId());
            if (r == null || r.data() == null) continue;
            merged = mergeWithSuffix(merged, r.data());
        }

        return merged;
    }

    private static Map<String, Object> mergeWithSuffix(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (a != null) out.putAll(a);
        if (b == null) return out;

        for (Map.Entry<String, Object> e : b.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();

            if (!out.containsKey(key)) {
            out.put(key, val);
            continue;
            }

            int i = 1;
            String candidate = key + "_" + i;
            while (out.containsKey(candidate)) {
            i++;
            candidate = key + "_" + i;
            }
            out.put(candidate, val);
        }

        return out;
    }
}
