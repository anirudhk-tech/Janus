package io.github.anirudhk_tech.janus.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record QueryRequest (
    @NotBlank(message = "Question is required")
    String question,

    @Valid
    Options options
) {
    public record Options (
        Integer timeoutMs,
        Boolean explain,
        Boolean debug
    ) {}
}
