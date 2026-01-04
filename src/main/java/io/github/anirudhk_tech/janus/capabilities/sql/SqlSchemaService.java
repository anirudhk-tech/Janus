package io.github.anirudhk_tech.janus.capabilities.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import io.github.anirudhk_tech.janus.capabilities.CapabilitiesProperties;
import io.github.anirudhk_tech.janus.connectors.ConnectorException;
import io.github.anirudhk_tech.janus.connectors.ConnectorProperties;

@Service
public final class SqlSchemaService {
    private final CapabilitiesProperties capabilities;
    private final ConnectorProperties connectorProperties;

    private final ConcurrentHashMap<String, SqlSchema> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, NamedParameterJdbcTemplate> templates = new ConcurrentHashMap<>();

    public SqlSchemaService(CapabilitiesProperties capabilities, ConnectorProperties connectorProperties) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities is required");
        this.connectorProperties = Objects.requireNonNull(connectorProperties, "connectorProperties is required");
    }

    public SqlSchema describe(String connector, String sourceId) {
        CapabilitiesProperties.Source src = findCapabilitySource(connector, sourceId);
        CapabilitiesProperties.SqlHints sql = (src == null) ? null : src.sql();
        
        if (sql == null) {
            throw new ConnectorException("No SQL capabilities configured for connector=" + connector + " sourceId=" + sourceId);
        }
        if (sql.schema() == null || sql.schema().isBlank()) {
            throw new ConnectorException("Capabilities.sql.schema is required for connector=" + connector + " sourceId=" + sourceId);
        }
        List<String> tables = (sql.tables() == null) ? List.of() : sql.tables();
        if (tables.isEmpty()) {
            throw new ConnectorException("Capabilities.sql.tables must not be empty for connector=" + connector + " sourceId=" + sourceId);
        }

        String key = connector + ":" + sourceId + ":" + sql.schema() + ":" + String.join(",", tables);
        return cache.computeIfAbsent(key, k -> introspect(connector, sourceId, sql.schema(), tables));
    }

    private SqlSchema introspect(String connector, String sourceId, String schema, List<String> tables) {
        ConnectorProperties.JbdcSource cfg = resolveJdbcSource(connector, sourceId);
        NamedParameterJdbcTemplate jdbc = templateFor(connector, sourceId, cfg);

        String q = """
            select
              c.table_name,
              c.column_name,
              c.data_type,
              c.is_nullable,
              c.ordinal_position
            from information_schema.columns c
            where c.table_schema = :schema
              and c.table_name in (:tables)
            order by c.table_name, c.ordinal_position
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("schema", schema);
        params.put("tables", tables);

        List<Map<String, Object>> rows = jdbc.queryForList(q, params);

        Map<String, List<SqlColumn>> byTable = new HashMap<>();
        for (Map<String, Object> r : rows) {
            String tableName = Objects.toString(r.get("table_name"), "");
            String columnName = Objects.toString(r.get("column_name"), "");
            String dataType = Objects.toString(r.get("data_type"), "");
            String isNullable = Objects.toString(r.get("is_nullable"), "");
            boolean nullable = "YES".equalsIgnoreCase(isNullable);

            if (tableName.isBlank() || columnName.isBlank()) continue;

            byTable.computeIfAbsent(tableName, __ -> new ArrayList<>())
                .add(new SqlColumn(columnName, dataType, nullable));
        }

        List<SqlTable> outTables = new ArrayList<>();
        for (String t : tables) {
            List<SqlColumn> cols = byTable.getOrDefault(t, List.of());
            outTables.add(new SqlTable(t, cols));
        }

        return new SqlSchema(schema, outTables);
    }

    private CapabilitiesProperties.Source findCapabilitySource(String connector, String sourceId) {
        List<CapabilitiesProperties.Source> sources = capabilities.sources();
        if (sources == null) return null;

        for (CapabilitiesProperties.Source s : sources) {
            if (s == null) continue;
            if (connector.equals(s.connector()) && sourceId.equals(s.sourceId())) return s;
        }
        return null;
    }

    private ConnectorProperties.JbdcSource resolveJdbcSource(String connector, String sourceId) {
        Map<String, ConnectorProperties.JbdcSource> sources =
            "supabase".equals(connector)
                ? (connectorProperties.supabase() == null ? null : connectorProperties.supabase().sources())
                : null;

        if (sources == null || !sources.containsKey(sourceId)) {
            throw new ConnectorException("Unknown JDBC source for connector=" + connector + " sourceId=" + sourceId);
        }
        return sources.get(sourceId);
    }

    private NamedParameterJdbcTemplate templateFor(String connector, String sourceId, ConnectorProperties.JbdcSource cfg) {
        String key = connector + ":" + sourceId;

        return templates.computeIfAbsent(key, __ -> {
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