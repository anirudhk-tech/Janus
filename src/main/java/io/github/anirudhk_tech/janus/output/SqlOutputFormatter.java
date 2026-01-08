package io.github.anirudhk_tech.janus.output;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.anirudhk_tech.janus.federation.StepExecutionResult;

public final class SqlOutputFormatter {

    private SqlOutputFormatter() {}

    public static String format(String traceId, List<StepExecutionResult> execution) {
        List<StepExecutionResult> safe = execution == null ? List.of() : execution;

        StringBuilder sb = new StringBuilder();

        if (traceId != null && !traceId.isBlank()) {
            sb.append("traceId: ").append(traceId).append("\n");
        }

        for (StepExecutionResult r : safe) {
            if (r == null) continue;
            Map<String, Object> data = r.data();
            sb.append("\nStep ").append(r.stepId()).append(" (").append(r.connector()).append(")\n");

            if (data != null) {
                Object sql = data.get("sql");
                if (sql instanceof String s && !s.isBlank()) {
                    sb.append("SQL:\n").append(s).append("\n\n");
                }

                Object params = data.get("params");
                if (params instanceof Map<?, ?> m) {
                    sb.append("Params: ").append(m).append("\n\n");
                }

                List<Map<String, Object>> rows = extractRows(data.get("rows"));
                appendTable(sb, rows);
            } else {
                sb.append("No data\n");
            }
        }

        return sb.toString();
    }

    private static void appendTable(StringBuilder sb, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            sb.append("Rows: 0\n");
            return;
        }

        LinkedHashSet<String> columns = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            if (row != null) {
                columns.addAll(row.keySet());
            }
        }

        if (columns.isEmpty()) {
            sb.append("Rows: ").append(rows.size()).append(" (no columns)\n");
            return;
        }

        Map<String, Integer> widths = new LinkedHashMap<>();
        for (String col : columns) {
            widths.put(col, col.length());
        }
        for (Map<String, Object> row : rows) {
            if (row == null) continue;
            for (String col : columns) {
                String val = stringify(row.get(col));
                widths.put(col, Math.max(widths.get(col), val.length()));
            }
        }

        String horizontal = buildHorizontal(widths);
        sb.append(horizontal);
        sb.append(buildHeader(columns, widths));
        sb.append(horizontal);
        for (Map<String, Object> row : rows) {
            sb.append(buildRow(columns, widths, row));
        }
        sb.append(horizontal);
        sb.append("(").append(rows.size()).append(rows.size() == 1 ? " row" : " rows").append(")\n");
    }

    private static List<Map<String, Object>> extractRows(Object candidate) {
        if (!(candidate instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map<?, ?> map) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    row.put(Objects.toString(e.getKey(), ""), e.getValue());
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private static String buildHorizontal(Map<String, Integer> widths) {
        StringBuilder sb = new StringBuilder();
        sb.append("+");
        for (int w : widths.values()) {
            sb.append("-".repeat(w + 2)).append("+");
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String buildHeader(LinkedHashSet<String> columns, Map<String, Integer> widths) {
        StringBuilder sb = new StringBuilder();
        sb.append("|");
        for (String col : columns) {
            sb.append(" ").append(pad(col, widths.get(col))).append(" |");
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String buildRow(LinkedHashSet<String> columns, Map<String, Integer> widths, Map<String, Object> row) {
        StringBuilder sb = new StringBuilder();
        sb.append("|");
        for (String col : columns) {
            String val = stringify(row == null ? null : row.get(col));
            sb.append(" ").append(pad(val, widths.get(col))).append(" |");
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String pad(String val, int width) {
        if (val == null) val = "";
        if (val.length() >= width) return val;
        return val + " ".repeat(width - val.length());
    }
}
