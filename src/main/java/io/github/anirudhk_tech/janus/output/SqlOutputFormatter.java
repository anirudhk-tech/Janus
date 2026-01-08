package io.github.anirudhk_tech.janus.output;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.anirudhk_tech.janus.federation.StepExecutionResult;

public final class SqlOutputFormatter {

    private SqlOutputFormatter() {}

    public static String format(String traceId, List<StepExecutionResult> execution) {
        return format(traceId, execution, true);
    }

    public static String format(String traceId, List<StepExecutionResult> execution, boolean colored) {
        List<StepExecutionResult> safe = execution == null ? List.of() : execution;
        Colorizer c = new Colorizer(colored);

        StringBuilder sb = new StringBuilder();

        sb.append(c.color("══════ JANUS SQL OUTPUT ══════", BRIGHT_BLUE)).append("\n");
        if (traceId != null && !traceId.isBlank()) {
            sb.append(c.color("traceId: ", DIM)).append(c.color(traceId, BRIGHT_WHITE)).append("\n");
        }
        sb.append(c.color("mode: text/plain • merge: off • explain: off", DIM)).append("\n");

        for (StepExecutionResult r : safe) {
            if (r == null) continue;
            Map<String, Object> data = r.data();
            sb.append("\n")
                .append(c.color("▼ Step ", BRIGHT_MAGENTA))
                .append(c.color(r.stepId(), BRIGHT_WHITE))
                .append(c.color(" (", BRIGHT_MAGENTA))
                .append(c.color(r.connector(), BRIGHT_WHITE))
                .append(c.color(")", BRIGHT_MAGENTA))
                .append(c.color(" • ", DIM))
                .append(c.color(r.status().name().toLowerCase(), r.status().isSuccess() ? BRIGHT_GREEN : BRIGHT_RED))
                .append(c.color(" • ", DIM))
                .append(c.color(r.durationMs() + " ms", BRIGHT_YELLOW))
                .append("\n");

            if (data != null) {
                Object sql = data.get("sql");
                if (sql instanceof String s && !s.isBlank()) {
                    sb.append(c.color("SQL:", BRIGHT_CYAN)).append("\n")
                      .append(c.color(s, BRIGHT_WHITE)).append("\n\n");
                }

                Object params = data.get("params");
                if (params instanceof Map<?, ?> m) {
                    sb.append(c.color("Params: ", BRIGHT_CYAN)).append(c.color(m.toString(), BRIGHT_WHITE)).append("\n\n");
                }

                List<Map<String, Object>> rows = extractRows(data.get("rows"));
                appendTable(sb, rows, c);
            } else {
                sb.append(c.color("No data\n", DIM));
            }
        }

        return sb.toString();
    }

    private static void appendTable(StringBuilder sb, List<Map<String, Object>> rows, Colorizer c) {
        if (rows.isEmpty()) {
            sb.append(c.color("Rows: 0\n", DIM));
            return;
        }

        LinkedHashSet<String> columns = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            if (row != null) {
                columns.addAll(row.keySet());
            }
        }

        if (columns.isEmpty()) {
            sb.append(c.color("Rows: ", DIM)).append(rows.size()).append(c.color(" (no columns)\n", DIM));
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

        int maxWidth = resolveMaxWidth();
        widths = clampWidths(widths, maxWidth);

        String horizontal = buildHorizontal(widths, c);
        sb.append(horizontal);
        sb.append(buildHeader(columns, widths, c));
        sb.append(horizontal);
        for (Map<String, Object> row : rows) {
            sb.append(buildRow(columns, widths, row, c));
        }
        sb.append(horizontal);
        sb.append(c.color("(", DIM))
          .append(c.color(String.valueOf(rows.size()), BRIGHT_YELLOW))
          .append(c.color(rows.size() == 1 ? " row" : " rows", DIM))
          .append(c.color(")\n", DIM));
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

    private static String buildHorizontal(Map<String, Integer> widths, Colorizer c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.color("+", DIM));
        for (int w : widths.values()) {
            sb.append(c.color("-".repeat(w + 2), DIM)).append(c.color("+", DIM));
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String buildHeader(LinkedHashSet<String> columns, Map<String, Integer> widths, Colorizer c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.color("|", DIM));
        for (String col : columns) {
            String rendered = renderCell(col, widths.get(col));
            sb.append(" ")
              .append(c.color(rendered, BRIGHT_YELLOW))
              .append(" ")
              .append(c.color("|", DIM));
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String buildRow(LinkedHashSet<String> columns, Map<String, Integer> widths, Map<String, Object> row, Colorizer c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.color("|", DIM));
        for (String col : columns) {
            String val = stringify(row == null ? null : row.get(col));
            String rendered = renderCell(val, widths.get(col));
            sb.append(" ")
              .append(c.color(rendered, BRIGHT_WHITE))
              .append(" ")
              .append(c.color("|", DIM));
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

    private static String renderCell(String val, int width) {
        if (val == null) val = "";
        if (width < 1) return "";
        if (val.length() > width) {
            if (width == 1) return "…";
            return val.substring(0, width - 1) + "…";
        }
        return pad(val, width);
    }

    private static int resolveMaxWidth() {
        String prop = System.getProperty("janus.output.maxWidth");
        if (prop != null) {
            try {
                return Math.max(40, Integer.parseInt(prop));
            } catch (NumberFormatException ignore) {}
        }
        String env = System.getenv("COLUMNS");
        if (env != null) {
            try {
                return Math.max(40, Integer.parseInt(env));
            } catch (NumberFormatException ignore) {}
        }
        return 120;
    }

    private static Map<String, Integer> clampWidths(Map<String, Integer> widths, int maxWidth) {
        final int minWidth = 6;
        Map<String, Integer> out = new LinkedHashMap<>(widths);
        while (true) {
            int total = totalTableWidth(out);
            if (total <= maxWidth) break;
            Optional<Map.Entry<String, Integer>> largest =
                out.entrySet().stream()
                    .filter(e -> e.getValue() > minWidth)
                    .max((a, b) -> Integer.compare(a.getValue(), b.getValue()));
            if (largest.isEmpty()) break;
            String key = largest.get().getKey();
            out.put(key, out.get(key) - 1);
        }
        return out;
    }

    private static int totalTableWidth(Map<String, Integer> widths) {
        int total = 1; // starting "+"
        for (int w : widths.values()) {
            total += (w + 2); // spaces around cell
            total += 1; // trailing border
        }
        return total;
    }

    private static final class Colorizer {
        private final boolean enabled;

        Colorizer(boolean enabled) {
            this.enabled = enabled;
        }

        String color(String text, String code) {
            if (!enabled) return text;
            return code + text + RESET;
        }
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
