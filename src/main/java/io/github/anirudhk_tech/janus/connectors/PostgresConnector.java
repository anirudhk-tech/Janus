package io.github.anirudhk_tech.janus.connectors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import io.github.anirudhk_tech.janus.federation.ExecutionContext;
import io.github.anirudhk_tech.janus.plan.PlanStep;
import io.github.anirudhk_tech.janus.plan.SqlQueryStep;

@Component
public final class PostgresConnector implements Connector {
    private final ConnectorProperties props;
    private final ConcurrentHashMap<String, NamedParameterJdbcTemplate> templates = new ConcurrentHashMap<>();

    public PostgresConnector(ConnectorProperties props) {
        this.props = props;
    }

    @Override
    public String name() {
        return "postgres";
    }

    @Override
    public boolean supports(PlanStep step) {
        return step instanceof SqlQueryStep;
    }

    @Override
    public ConnectorResult execute(PlanStep step, ExecutionContext context) throws ConnectorException {
        SqlQueryStep sql = (SqlQueryStep) step;

        ConnectorProperties.JbdcSource cfg = resolveSource(sql.connector(), sql.sourceId());
        NamedParameterJdbcTemplate jdbc = templateFor(sql.connector(), sql.sourceId(), cfg);
    
        Map<String, Object> params = (sql.params() == null) ? Map.of() : sql.params();
        List<Map<String, Object>> rows = jdbc.queryForList(sql.sql(), params);
    
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("sql", sql.sql());
        data.put("params", params);
    
        return new ConnectorResult(sql.stepId(), sql.connector(), data);
    }



    private ConnectorProperties.JbdcSource resolveSource(String connector, String sourceId) {
        Map<String, ConnectorProperties.JbdcSource> sources =
            "supabase".equals(connector)
                ? (props.supabase() == null ? null : props.supabase().sources())
                : null;

        if (sources == null || !sources.containsKey(sourceId)) {
            throw new ConnectorException("Unknown sourceId for connector=" + connector + ": " + sourceId);
        }

        return sources.get(sourceId);
    }

    private NamedParameterJdbcTemplate templateFor(String connector, String sourceId, ConnectorProperties.JbdcSource cfg) {
        String key = connector + ":" + sourceId;

        return templates.computeIfAbsent(key, k -> {
            String url = normalizeJdbcUrl(cfg.jdbcUrl());
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setUrl(url);
            ds.setUsername(cfg.username());
            ds.setPassword(cfg.password());
            return new NamedParameterJdbcTemplate(ds);
        });
    }

    private static String normalizeJdbcUrl(String url) {
        if (url == null) return null;
        String x = url.trim();
        if (x.isBlank()) return x;

        // Hardening: Postgres JDBC does NOT support embedding credentials in the URL like:
        //   postgresql://user:pass@host:5432/db
        if (hasUserInfoInAuthority(x)) {
            throw new ConnectorException("Invalid jdbc-url: do not embed credentials (user:pass@host). Set username/password fields separately.");
        }

        // allow supabase-style "postgresql://..." in config
        if (x.startsWith("postgresql://")) return "jdbc:" + x;
        return x;
    }

    private static boolean hasUserInfoInAuthority(String url) {
        // Accept either:
        // - postgresql://host:port/db
        // - jdbc:postgresql://host:port/db
        // If the authority section contains '@', the URL likely contains userinfo.
        String x = url.trim();
        if (x.startsWith("jdbc:")) {
            x = x.substring("jdbc:".length());
        }

        String schemePrefix = "postgresql://";
        if (!x.startsWith(schemePrefix)) {
            return false;
        }

        int authorityStart = schemePrefix.length();
        int authorityEnd = x.length();
        for (int i = authorityStart; i < x.length(); i++) {
            char c = x.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                authorityEnd = i;
                break;
            }
        }

        String authority = x.substring(authorityStart, authorityEnd);
        return authority.contains("@");
    }
}

