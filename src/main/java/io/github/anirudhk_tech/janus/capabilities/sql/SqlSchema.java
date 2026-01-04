package io.github.anirudhk_tech.janus.capabilities.sql;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record SqlSchema (
    String schema,
    List<SqlTable> tables
) {
    public SqlSchema{
        Objects.requireNonNull(schema, "schema is required");
        tables = (tables == null) ? List.of() : tables;
    }

    public Optional<SqlTable> table(String tableName) {
        if (tableName == null) return Optional.empty();
        for (SqlTable t : tables) {
            if (t != null && tableName.equalsIgnoreCase(t.name())) return Optional.of(t);
        }
        return Optional.empty();
    }
}
