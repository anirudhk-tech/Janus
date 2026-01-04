package io.github.anirudhk_tech.janus.capabilities.sql;

import java.util.List;
import java.util.Objects;

public record SqlTable (
    String name,
    List<SqlColumn> columns
) {
    public SqlTable{
        Objects.requireNonNull(name, "name is required");
        columns = (columns == null) ? List.of() : columns;
    }
}
