package io.github.anirudhk_tech.janus.merge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.github.anirudhk_tech.janus.federation.StepExecutionResult;
import io.github.anirudhk_tech.janus.plan.ExecutionPlan;
import io.github.anirudhk_tech.janus.plan.PlanStep;


@Component
public final class JsonDeepMergeV1 implements MergeStrategy {

    @Override
    public String name() {
        return "json-deep-merge-v1";
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
            merged = deepMergeWithSuffix(merged, r.data());
        }

        return merged;
    }

    private static Map<String, Object> deepMergeWithSuffix(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (a != null) out.putAll(a);
        if (b == null) return out;

        for (Map.Entry<String, Object> e : b.entrySet()) {
            String key = e.getKey();
            Object bVal = e.getValue();

            if (!out.containsKey(key)) {
                out.put(key, bVal);
                continue;
            }

            Object aVal = out.get(key);

            if (aVal == null) {
                out.put(key, bVal);
                continue;
            }

            if (bVal == null) {
                // Keep existing non-null value.
                continue;
            }

            if (aVal instanceof Map<?, ?> aMap && bVal instanceof Map<?, ?> bMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> aCast = (Map<String, Object>) aMap;
                @SuppressWarnings("unchecked")
                Map<String, Object> bCast = (Map<String, Object>) bMap;
                out.put(key, deepMergeWithSuffix(aCast, bCast));
                continue;
            }

            if (aVal instanceof List<?> aList && bVal instanceof List<?> bList) {
                List<Object> combined = new ArrayList<>(aList.size() + bList.size());
                combined.addAll(aList);
                combined.addAll(bList);
                out.put(key, combined);
                continue;
            }

            if (aVal.equals(bVal)) {
                continue;
            }

            // Collision on a non-mergeable type: preserve both by suffixing the new value.
            String candidate = nextAvailableKey(out, key);
            out.put(candidate, bVal);
        }

        return out;
    }

    private static String nextAvailableKey(Map<String, Object> out, String key) {
        int i = 1;
        String candidate = key + "_" + i;
        while (out.containsKey(candidate)) {
            i++;
            candidate = key + "_" + i;
        }
        return candidate;
    }
}


