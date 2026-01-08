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

        sb.append(color("══════ JANUS SQL OUTPUT ══════", BRIGHT_BLUE)).append("\n");
        if (traceId != null && !traceId.isBlank()) {
            sb.append(color("traceId: ", DIM)).append(color(traceId, BRIGHT_WHITE)).append("\n");
        }
        sb.append(color("mode: text/plain • merge: off • explain: off", DIM)).append("\n");

        for (StepExecutionResult r : safe) {
            if (r == null) continue;
            Map<String, Object> data = r.data();
            sb.append("\n")
                .append(color("▼ Step ", BRIGHT_MAGENTA))
                .append(color(r.stepId(), BRIGHT_WHITE))
                .append(color(" (", BRIGHT_MAGENTA))
                .append(color(r.connector(), BRIGHT_WHITE))
                .append(color(")", BRIGHT_MAGENTA))
                .append(color(" • ", DIM))
                .append(color(r.status().name().toLowerCase(), r.status().isSuccess() ? BRIGHT_GREEN : BRIGHT_RED))
                .append(color(" • ", DIM))
                .append(color(r.durationMs() + " ms", BRIGHT_YELLOW))
                .append("\n");

            if (data != null) {
                Object sql = data.get("sql");
                if (sql instanceof String s && !s.isBlank()) {
                    sb.append(color("SQL:", BRIGHT_CYAN)).append("\n")
                      .append(color(s, BRIGHT_WHITE)).append("\n\n");
                }

                Object params = data.get("params");
                if (params instanceof Map<?, ?> m) {
                    sb.append(color("Params: ", BRIGHT_CYAN)).append(color(m.toString(), BRIGHT_WHITE)).append("\n\n");
                }

                List<Map<String, Object>> rows = extractRows(data.get("rows"));
                appendTable(sb, rows);
            } else {
                sb.append(color("No data\n", DIM));
            }
        }

        return sb.toString();
    }

    private static void appendTable(StringBuilder sb, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            sb.append(color("Rows: 0\n", DIM));
            return;
        }

        LinkedHashSet<String> columns = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            if (row != null) {
                columns.addAll(row.keySet());
            }
        }

        if (columns.isEmpty()) {
            sb.append(color("Rows: ", DIM)).append(rows.size()).append(color(" (no columns)\n", DIM));
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
        sb.append(color("(", DIM))
          .append(color(String.valueOf(rows.size()), BRIGHT_YELLOW))
          .append(color(rows.size() == 1 ? " row" : " rows", DIM))
          .append(color(")\n", DIM));
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
        sb.append(color("+", DIM));
        for (int w : widths.values()) {
            sb.append(color("-".repeat(w + 2), DIM)).append(color("+", DIM));
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String buildHeader(LinkedHashSet<String> columns, Map<String, Integer> widths) {
        StringBuilder sb = new StringBuilder();
        sb.append(color("|", DIM));
        for (String col : columns) {
            sb.append(" ")
              .append(color(pad(col, widths.get(col)), BRIGHT_YELLOW))
              .append(" ")
              .append(color("|", DIM));
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String buildRow(LinkedHashSet<String> columns, Map<String, Integer> widths, Map<String, Object> row) {
        StringBuilder sb = new StringBuilder();
        sb.append(color("|", DIM));
        for (String col : columns) {
            String val = stringify(row == null ? null : row.get(col));
            sb.append(" ")
              .append(color(pad(val, widths.get(col)), BRIGHT_WHITE))
              .append(" ")
              .append(color("|", DIM));
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

    private static String color(String text, String code) {
        return code + text + RESET;
    }

    // ANSI colors for a bit of flair in terminal output.
    private static final String RESET = "\u001B[0m";
    private static final String BRIGHT_WHITE = "\u001B[97m";
    private static final String BRIGHT_BLUE = "\u001B[94m";
    private static final String BRIGHT_CYAN = "\u001B[96m";
    private static final String BRIGHT_MAGENTA = "\u001B[95m";
    private static final String BRIGHT_YELLOW = "\u001B[93m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String DIM = "\u001B[2m";
}
