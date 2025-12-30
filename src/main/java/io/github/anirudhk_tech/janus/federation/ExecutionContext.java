package io.github.anirudhk_tech.janus.federation;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public record ExecutionContext (
    String traceId,
    Instant deadline,
    Clock clock
) {

    public ExecutionContext {
        Objects.requireNonNull(traceId, "traceId is required");
        Objects.requireNonNull(deadline, "deadline is required");
        Objects.requireNonNull(clock, "clock is required");
    }

    public Instant now() {
        return Instant.now(clock);
    }

    public boolean isExpired() {
        return now().isAfter(deadline);
    }

    public long remainingMillis() {
        long millis = deadline.toEpochMilli() - now().toEpochMilli();
        return Math.max(0, millis);
    }
}
