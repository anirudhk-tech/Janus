package io.github.anirudhk_tech.janus.capabilities.sql;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.github.anirudhk_tech.janus.connectors.ConnectorException;
import io.github.anirudhk_tech.janus.plan.SqlQueryStep;

@Component
@ConditionalOnProperty(name = "janus.sql.guardrails.enabled", havingValue = "true", matchIfMissing = true)
public final class SqlStepGuardrail {

    private static final Pattern FORBIDDEN =
        Pattern.compile("(?i)\\b(insert|update|delete|drop|alter|create|grant|revoke|truncate|call|do|copy|execute|merge)\\b");

    private static final Pattern TABLE_REF =
        Pattern.compile("(?i)\\b(from|join)\\s+([a-zA-Z0-9_\\.\\\"]+)");

    private static final Pattern SELECT_LIST =
        Pattern.compile("(?is)^\\s*select\\s+(.*?)\\s+from\\s+");

    private static final Pattern FROM_ONE_TABLE =
        Pattern.compile("(?is)\\bfrom\\s+([a-zA-Z0-9_\\.\\\"]+)\\s*(?:as\\s+)?([a-zA-Z0-9_\\\"]+)?");

    private static final Pattern REWRITE_STAR =
        Pattern.compile("(?is)^(\\s*select\\s+)(\\*|[a-zA-Z0-9_\\\"]+\\s*\\.\\s*\\*)(\\s+from\\s+)");

    private final SqlSchemaService sqlSchemaService;

    public SqlStepGuardrail(SqlSchemaService sqlSchemaService) {
        this.sqlSchemaService = Objects.requireNonNull(sqlSchemaService, "sqlSchemaService is required");
    }

    public SqlQueryStep apply(SqlQueryStep step) {
        Objects.requireNonNull(step, "step is required");
        if (step.sql() == null || step.sql().isBlank()) {
            throw new ConnectorException("Guardrail: SqlQueryStep.sql is blank");
        }

        String sql = stripComments(step.sql()).trim();

        while (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }

        if (sql.contains(";")) {
            throw new ConnectorException("Guardrail: multi-statement SQL is not allowed");
        }

        // SELECT-only
        String lower = sql.toLowerCase(Locale.ROOT).trim();
        if (!lower.startsWith("select")) {
            throw new ConnectorException("Guardrail: only SELECT statements are allowed");
        }

        // forbid obvious dangerous keywords (defense-in-depth)
        if (FORBIDDEN.matcher(sql).find()) {
            throw new ConnectorException("Guardrail: forbidden SQL keyword detected");
        }

        SqlSchema schema = sqlSchemaService.describe(step.connector(), step.sourceId());

        // Validate referenced tables against allowlist
        Set<String> referencedTables = extractTables(sql, schema.schema());
        if (referencedTables.isEmpty()) {
            throw new ConnectorException("Guardrail: could not determine referenced tables (FROM/JOIN)");
        }

        for (String t : referencedTables) {
            if (schema.table(t).isEmpty()) {
                throw new ConnectorException("Guardrail: table not allowlisted: " + t);
            }
        }

        // Rewrite SELECT * only when it is safe (single-table, no joins, no mixed select list)
        String rewritten = rewriteSelectStarIfSafe(sql, schema, referencedTables);

        if (rewritten.equals(step.sql())) {
            return step;
        }

        return new SqlQueryStep(step.stepId(), step.connector(), step.sourceId(), rewritten, step.params());
    }

    private static String rewriteSelectStarIfSafe(String sql, SqlSchema schema, Set<String> referencedTables) {
        Matcher listM = SELECT_LIST.matcher(sql);
        if (!listM.find()) return sql;

        String selectList = listM.group(1).trim();
        boolean isStarOnly = "*".equals(selectList);
        boolean isAliasStarOnly = selectList.matches("(?is)^[a-zA-Z0-9_\\\"]+\\s*\\.\\s*\\*$");

        if (!isStarOnly && !isAliasStarOnly) return sql;

        if (referencedTables.size() != 1) {
            throw new ConnectorException("Guardrail: SELECT * is only allowed for single-table queries (rewrite requires 1 table)");
        }

        String lower = sql.toLowerCase(Locale.ROOT);
        if (lower.contains(" join ")) {
            throw new ConnectorException("Guardrail: SELECT * with JOIN is not allowed; select explicit columns");
        }

        Matcher fromM = FROM_ONE_TABLE.matcher(sql);
        if (!fromM.find()) return sql;

        String rawIdent = fromM.group(1);
        String alias = fromM.group(2);

        String tableName = normalizeTableName(rawIdent, schema.schema());
        SqlTable table = schema.table(tableName)
            .orElseThrow(() -> new ConnectorException("Guardrail: table not allowlisted: " + tableName));

        if (table.columns().isEmpty()) {
            throw new ConnectorException("Guardrail: no column metadata available to rewrite SELECT * for table: " + tableName);
        }

        String prefix = null;
        if (isAliasStarOnly) {
            prefix = selectList.split("\\.")[0].trim().replace("\"", "");
        }

        List<String> cols = new ArrayList<>();
        for (SqlColumn c : table.columns()) {
            String colName = c.name();
            if (prefix != null && !prefix.isBlank()) {
                cols.add(prefix + "." + colName);
            } else if (alias != null && !alias.isBlank() && selectList.contains(".")) {
                // If the query used alias.*, prefer keeping alias qualification.
                String a = alias.replace("\"", "");
                cols.add(a + "." + colName);
            } else {
                cols.add(colName);
            }
        }

        String colList = String.join(", ", cols);

        Matcher rw = REWRITE_STAR.matcher(sql);
        if (!rw.find()) return sql;

        return rw.replaceFirst("$1" + Matcher.quoteReplacement(colList) + "$3");
    }

    private static Set<String> extractTables(String sql, String defaultSchema) {
        String s = stripComments(sql);

        Matcher m = TABLE_REF.matcher(s);
        Set<String> out = new LinkedHashSet<>();
        while (m.find()) {
            String ident = m.group(2);
            String t = normalizeTableName(ident, defaultSchema);
            if (t != null && !t.isBlank()) out.add(t);
        }
        return out;
    }

    private static String normalizeTableName(String ident, String defaultSchema) {
        if (ident == null) return null;
        String x = ident.trim();
        if (x.isBlank()) return x;

        // drop quotes
        x = x.replace("\"", "");

        // remove schema qualifier if it matches default schema
        // examples: public.calendar_events -> calendar_events
        int dot = x.indexOf('.');
        if (dot >= 0) {
            String left = x.substring(0, dot);
            String right = x.substring(dot + 1);
            if (defaultSchema != null && left.equalsIgnoreCase(defaultSchema)) {
                return right;
            }
            // otherwise, keep right-most token as table name (strict allowlist is table-name based)
            return right;
        }

        return x;
    }

    private static String stripComments(String sql) {
        String x = sql;

        // Remove /* ... */ comments (non-nested)
        x = x.replaceAll("(?s)/\\*.*?\\*/", " ");

        // Remove -- ... end-of-line comments
        x = x.replaceAll("(?m)--.*?$", " ");

        return x;
    }
}