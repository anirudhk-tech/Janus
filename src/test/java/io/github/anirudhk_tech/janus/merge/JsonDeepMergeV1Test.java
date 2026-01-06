package io.github.anirudhk_tech.janus.merge;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.anirudhk_tech.janus.federation.StepExecutionResult;
import io.github.anirudhk_tech.janus.federation.StepExecutionStatus;
import io.github.anirudhk_tech.janus.plan.ExecutionPlan;
import io.github.anirudhk_tech.janus.plan.SqlQueryStep;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonDeepMergeV1Test {

    private final JsonDeepMergeV1 merge = new JsonDeepMergeV1();

    @Test
    void mergesNestedMapsAndConcatsLists_andSuffixesScalarCollisions() {
        ExecutionPlan plan = new ExecutionPlan(
            List.of(
                new SqlQueryStep("s1", "postgres", "test", "SELECT 1", Map.of()),
                new SqlQueryStep("s2", "postgres", "test", "SELECT 2", Map.of())
            ),
            "json-deep-merge-v1"
        );

        StepExecutionResult r1 = new StepExecutionResult(
            "s1",
            "postgres",
            StepExecutionStatus.SUCCESS,
            1,
            Map.of(
                "rows", List.of(1),
                "meta", Map.of("sql", "A")
            ),
            null
        );

        StepExecutionResult r2 = new StepExecutionResult(
            "s2",
            "postgres",
            StepExecutionStatus.SUCCESS,
            1,
            Map.of(
                "rows", List.of(2),
                "meta", Map.of("sql", "B")
            ),
            null
        );

        Map<String, Object> merged = merge.merge(plan, List.of(r1, r2));

        assertEquals(
            Map.of(
                "rows", List.of(1, 2),
                "meta", Map.of("sql", "A", "sql_1", "B")
            ),
            merged
        );
    }

    @Test
    void suffixesNonMergeableCollisionsAtTopLevel() {
        ExecutionPlan plan = new ExecutionPlan(
            List.of(
                new SqlQueryStep("s1", "postgres", "test", "SELECT 1", Map.of()),
                new SqlQueryStep("s2", "postgres", "test", "SELECT 2", Map.of())
            ),
            "json-deep-merge-v1"
        );

        StepExecutionResult r1 = new StepExecutionResult(
            "s1",
            "postgres",
            StepExecutionStatus.SUCCESS,
            1,
            Map.of("x", 1),
            null
        );

        StepExecutionResult r2 = new StepExecutionResult(
            "s2",
            "postgres",
            StepExecutionStatus.SUCCESS,
            1,
            Map.of("x", 2),
            null
        );

        Map<String, Object> merged = merge.merge(plan, List.of(r1, r2));

        assertEquals(Map.of("x", 1, "x_1", 2), merged);
    }
}


