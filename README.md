# Janus

Janus is an **explainable federated query and reasoning engine**. It turns a question into an execution plan, runs steps across multiple data sources in parallel, and returns merged results with traceable timings and an explanation.

Docs:

- [`docs/API.md`](docs/API.md) — HTTP endpoints + request/response shapes
- [`docs/CONFIG.md`](docs/CONFIG.md) — configuration + environment variables
- [`docs/MERGE.md`](docs/MERGE.md) — merge strategies and semantics
- [`HELP.md`](HELP.md) — troubleshooting runbook
- [`docs/DEVLOG.md`](docs/DEVLOG.md) — historical notes / gotchas

## Status

This repo is under active development.

What works today:

- `GET /healthz` (app health)
- `GET /actuator/health` (Spring Boot Actuator health)
- Spring Security is enabled:
  - health endpoints are public
  - other endpoints require `X-API-Key` (validated against `JANUS_API_KEY`) and return JSON `401` when missing/invalid
- `GET /protected/ping` (protected smoke-test endpoint for API-key auth)
- `POST /query` (protected; LLM planner → SQL execution plan → guardrails → Postgres execution → JSON response with `data` and optional `explanation` when `options.explain=true`)
- Schema-aware SQL planning:
  - the LLM is given allowlisted tables **and their columns** (introspected from Postgres) so it can select specific fields
- SQL guardrails:
  - only single-statement `SELECT` is allowed (no DDL/DML; no multi-statement)
  - only allowlisted tables (from capabilities) are allowed
  - `SELECT *` is rewritten to explicit columns when safe (single-table, no JOIN)

Current constraints (by design for now):

- **Plan steps**: only `type="sql"` (`SqlQueryStep`) is supported today.
- **Execution connectors**: Postgres via JDBC. A logical `connector="supabase"` is supported and routed to the Postgres executor internally.

## Architecture (M1)

At a high level, a request to `POST /query` will:

- authenticate via `X-API-Key`
- build an explicit execution plan (currently: SQL steps only) using an LLM + configured capabilities (tables + columns)
- validate and (when safe) rewrite SQL before execution (single `SELECT`, allowlisted tables; rewrite `SELECT *`)
- execute steps in parallel via connector implementations (currently: Postgres via JDBC)
- return a JSON response including results (`data`) and optional traceability (`explanation.plan` + `explanation.execution`) when `options.explain=true`

## Quickstart

### Prerequisites

- Java 21+
- GNU Make (usually available via Xcode Command Line Tools on macOS)

### Setup (local only)

Create a local `.env` file in the repo root (do not commit it):

```bash
cat > .env <<'EOF'
JANUS_API_KEY=dev-secret-change-me
# Choose one provider:
# OPENAI_API_KEY=...
# GEMINI_API_KEY=...
EOF
```

### Run locally

```bash
make run
```

Fallback (if you don't want `make`):

```bash
./mvnw spring-boot:run
```

### Health checks

```bash
curl -i localhost:8080/healthz
curl -i localhost:8080/actuator/health
```

Note: health endpoints are public. Other endpoints require `X-API-Key` (validated against `JANUS_API_KEY`).

## API quick usage

See the full API reference in [`docs/API.md`](docs/API.md).

```bash
curl -sS -X POST localhost:8080/query \
  -H "X-API-Key: $JANUS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"question":"How many calendar events are there?","options":{"timeoutMs":5000}}' \
| jq .
```

Text vs JSON output:

- Default: JSON (merge + optional explanation); great with `jq`.
- Set `janus.output.sql=true` to get `text/plain` per-step SQL blocks (SQL, params, rows as a table) with no merge/explanation. Handy for terminal inspection; skip piping to `jq` in this mode.

## Project structure

Source lives under the base package `io.github.anirudhk_tech.janus`:

- `api`: HTTP controllers and DTOs
- `auth`: authentication/authorization (M1: API key)
- `agent`: question → plan (current: LLM-backed planner)
- `plan`: execution plan domain model (keep Spring-free)
- `capabilities`: planner metadata (sources, SQL hints)
- `capabilities/sql`: schema introspection + SQL guardrails (allowlist enforcement, `SELECT *` rewrite)
- `connectors`: adapters for external systems (currently: Postgres JDBC executor)
- `federation`: parallel execution + timeouts + step result model

## Development

### Common commands

Run:

```bash
make run
```

Test:

```bash
make test
```

## Roadmap

- **Next**: use `options.debug` to control response size/verbosity; add safety/redaction for logs and errors
- **M2**: multi-step plans (N>1 SQL steps), improved error reporting without failing the entire request on the first step failure
- **M3**: more connectors + merge rules + caching (TTL/persistent schema cache, caching query results)
- **M4**: governance (tenants, per-tenant connector policies, audit log)

## Why “Janus”?

Janus (the Roman god of doors and thresholds) fits a system that sits at the boundary between multiple data sources and produces a single, explainable response. The project is also inspired by Jarvis.

## Contributing

See `CONTRIBUTING.md`.

## Security

See `SECURITY.md`. Please do not open public issues for security reports.

## License

See `LICENSE`.


