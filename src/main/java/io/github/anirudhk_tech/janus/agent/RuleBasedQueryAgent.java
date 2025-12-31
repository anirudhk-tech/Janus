package io.github.anirudhk_tech.janus.agent;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.github.anirudhk_tech.janus.api.QueryRequest;
import io.github.anirudhk_tech.janus.plan.ExecutionPlan;
import io.github.anirudhk_tech.janus.plan.HttpRequestStep; 
import io.github.anirudhk_tech.janus.plan.PlanStep; 
import io.github.anirudhk_tech.janus.plan.SqlQueryStep; 

// Mock implementation of the QueryAgent interface.

@Service
@ConditionalOnProperty(name = "janus.agent.mode", havingValue = "rule_based", matchIfMissing = true)
public class RuleBasedQueryAgent implements QueryAgent {

    @Override
    public ExecutionPlan buildPlan(String question, QueryRequest.Options options) {
        YearMonth thisMonth = YearMonth.now();

        PlanStep postgresStep = new SqlQueryStep(
            "step_pg_orders_this_month",
            "postgres",
            """
            SELECT COUNT(*) AS order_count
            FROM orders
            WHERE created_at >= :monthStart AND created_at < :nextMonthStart
            """,
            Map.of(
                "monthStart", thisMonth.atDay(1).atStartOfDay().toString(),
                "nextMonthStart", thisMonth.plusMonths(1).atDay(1).atStartOfDay().toString()
            )
        );

        PlanStep restStep = new HttpRequestStep(
            "step_rest_github_activity",
            "rest",
            "GET",
            "https://api.github.com/users/{username}/events",
            Map.of("Accept", "application/vnd.github+json"),
            Map.of("username", "octocat")
        );

        List<PlanStep> steps = List.of(postgresStep, restStep);

        return new ExecutionPlan(steps, "template_merge_v1");
    }
}