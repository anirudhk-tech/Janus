package io.github.anirudhk_tech.janus.capabilities.sql;

import java.util.Objects;

public record SqlColumn(
    String name,
    String type,
    boolean nullable
) {
    public SqlColumn {
        Objects.requireNonNull(name, "name is required");
        type = (type == null) ? "" : type;
    }
}