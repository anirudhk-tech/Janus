package io.github.anirudhk_tech.janus.merge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import io.github.anirudhk_tech.janus.federation.StepExecutionResult;
import io.github.anirudhk_tech.janus.plan.ExecutionPlan;

@Service
public final class MergeService {
    private final Map<String, MergeStrategy> byName;
    private final MergeProperties props;

    public MergeService(List<MergeStrategy> strategies, MergeProperties props) {
        Map<String, MergeStrategy> m = new LinkedHashMap<>();

        for (MergeStrategy s : strategies) {
            String name = Objects.requireNonNull(s.name(), "name is required");
            if (m.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate merge strategy name: " + name);
            }
            m.put(name, s);
        }

        this.byName = Map.copyOf(m);
        this.props = Objects.requireNonNull(props, "props is required");
    }

    public Map<String, Object> merge(ExecutionPlan plan, List<StepExecutionResult> execution) {
        String name = (props.strategy() == null || props.strategy().isBlank())
            ? "json-shallow-merge-v1"
            : props.strategy();

        MergeStrategy strat = byName.get(name);

        if (strat == null) {
            throw new IllegalArgumentException("Unknown merge strategy: " + name);
        }

        return strat.merge(plan, execution);
    }
}
