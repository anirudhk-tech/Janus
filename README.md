# Janus

Janus is an **explainable federated query and reasoning engine**. It turns a question into an execution plan, runs steps across multiple data sources in parallel, and returns merged results with traceable timings and an explanation.

## Status

This repo is under active development.

What works today:

- `GET /healthz` (app health)
- `GET /actuator/health` (Spring Boot Actuator health)
- Spring Security is enabled:
  - health endpoints are public
  - other endpoints require `X-API-Key` (validated against `JANUS_API_KEY`) and return JSON `401` when missing/invalid
- `GET /protected/ping` (protected smoke-test endpoint for API-key auth)
- `POST /query` (protected; LLM planner → SQL execution plan → guardrails → Postgres execution → JSON response with `data` + `explanation`)
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
- return a JSON response including results (`data`) and traceability (`explanation.plan` + `explanation.execution`)

## Quickstart

### Prerequisites

- Java 21+
- GNU Make (usually available via Xcode Command Line Tools on macOS)

### Devlog (notes as the project evolves)

If you’re working on the codebase (or debugging local setup), see:

- `docs/DEVLOG.md`

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

## Configuration

### Environment variables

- `JANUS_API_KEY` (required for M1)
  - Used to validate the `X-API-Key` header for protected endpoints (including `POST /query`).
  - Local dev: store it in `.env` (this repo’s `.gitignore` already ignores `.env`).

### Planner mode (required)

Janus requires a `QueryAgent` bean. Today, the shipped agent is `LlmQueryAgent`, enabled by:

- `janus.agent.mode=llm`

If you set a different value, the app will fail to start because there is no other `QueryAgent` implementation registered.

### LLM provider configuration

Configure one provider:

- **OpenAI**:
  - `janus.llm.provider=openai`
  - `OPENAI_API_KEY` (via `janus.llm.openai.api-key: ${OPENAI_API_KEY:}`)
- **Gemini**:
  - `janus.llm.provider=gemini`
  - `GEMINI_API_KEY` (via `janus.llm.gemini.api-key: ${GEMINI_API_KEY:}`)

Models are configurable via `janus.llm.openai.model` / `janus.llm.gemini.model`.

### Capabilities (planner metadata)

The planner (LLM) needs a list of available sources. This is **separate** from connector credentials.

For SQL sources, capabilities also act as an **allowlist**:

- Only tables listed under `sql.tables` may be queried.
- Column metadata for those tables is introspected from Postgres (`information_schema.columns`) using the connector credentials, and included in the LLM prompt so it can choose specific columns.

Example:

```yaml
janus:
  capabilities:
    sources:
      - sourceId: cackle
        connector: supabase
        description: "Calendar events in Supabase"
        sql:
          schema: public
          tables: ["calendar_events"]
```

Note: `janus.capabilities.sources` must be a **list** (use `-`), not an object.

### SQL guardrails (recommended)

By default, Janus enforces strict SQL safety checks before execution:

- only a single `SELECT` statement is allowed (a trailing `;` is allowed, but multiple statements are rejected)
- obvious DDL/DML keywords are rejected
- referenced tables must be present in the capabilities allowlist for that `(connector, sourceId)`

To disable guardrails (not recommended outside tests/dev):

- `janus.sql.guardrails.enabled=false`

### Connector config (execution credentials)

**Warning**: Do **NOT** commit database credentials (or any secrets) into `application.yaml`.
Use environment variables (or a local `.env`) and reference them from YAML with `${...}` placeholders.

Example Supabase (Postgres) source:

```yaml
janus:
  connectors:
    supabase:
      sources:
        cackle:
          # NEVER commit secrets into application.yaml. Prefer env vars:
          #   jdbc-url: ${JANUS_CONNECTORS_SUPABASE_SOURCES_CACKLE_JDBC_URL:}
          #   username: ${JANUS_CONNECTORS_SUPABASE_SOURCES_CACKLE_USERNAME:}
          #   password: ${JANUS_CONNECTORS_SUPABASE_SOURCES_CACKLE_PASSWORD:}
          #
          # Do NOT embed credentials in the URL (no user:pass@host).
          # Keep username/password separate.
          #
          # Also: quote values with '#' since YAML treats it as a comment otherwise.
          jdbc-url: "postgresql://db.<project>.supabase.co:5432/postgres?sslmode=require"
          username: "postgres"
          password: "your-password-with-#-must-be-quoted"
```

Troubleshooting tips:

- If you see `UnknownHostException: user:pass@host`, your `jdbc-url` likely includes credentials. Move them to `username`/`password`.
- If you see password truncation, make sure any value containing `#` is quoted in YAML.
- If the hosted DB requires TLS, add `?sslmode=require` to the URL.

### `.env` file notes

- `.env` is a **local development convenience**. It is loaded by the Makefile and exported into the process environment before starting Spring Boot.
- Do not commit `.env` (treat it like a password).

## API (current)

- `GET /healthz` → `200 OK` with a simple body (`OK`)
- `GET /actuator/health` → `200 OK` with Actuator health JSON
- `GET /protected/ping` → `200 OK` with body `pong` (requires `X-API-Key`)
- `POST /query` → `200 OK` with plan + execution + data (requires `X-API-Key`)

### API key auth (curl examples)

```bash
# Missing key -> 401 JSON
curl -i localhost:8080/protected/ping

# Wrong key -> 401 JSON
curl -i -H 'X-API-Key: wrong' localhost:8080/protected/ping

# Correct key -> 200 pong
curl -i -H "X-API-Key: $JANUS_API_KEY" localhost:8080/protected/ping
```

### `POST /query` (current: LLM plan + SQL execution)

Request body (minimal):

```json
{
  "question": "hello"
}
```

Example:

```bash
curl -i -X POST localhost:8080/query \
  -H "X-API-Key: $JANUS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"question":"hello"}'
```

Response (example):

```json
{
  "traceId": "0d76ddc6-5bf8-4f8d-91d1-f6cfb6abbcfb",
  "answer": "executed",
  "data": {
    "sources": {
      "supabase": {
        "count_calendar_events": {
          "rows": [
            { "event_count": 16 }
          ],
          "sql": "SELECT COUNT(*) AS event_count FROM public.calendar_events;",
          "params": {}
        }
      }
    },
    "merged": {
      "rows": [
        { "event_count": 16 }
      ],
      "sql": "SELECT COUNT(*) AS event_count FROM public.calendar_events;",
      "params": {}
    }
  },
  "explanation": {
    "plan": {
      "steps": [
        {
          "type": "sql",
          "stepId": "count_calendar_events",
          "connector": "supabase",
          "sourceId": "cackle",
          "sql": "SELECT COUNT(*) AS event_count FROM public.calendar_events;",
          "params": {}
        }
      ],
      "mergeStrategy": "json-shallow-merge-v1"
    },
    "execution": [
      {
        "stepId": "count_calendar_events",
        "connector": "supabase",
        "status": "SUCCESS",
        "durationMs": 911,
        "data": {
          "rows": [
            { "event_count": 16 }
          ],
          "sql": "SELECT COUNT(*) AS event_count FROM public.calendar_events;",
          "params": {}
        },
        "error": null
      }
    ]
  }
}
```

Notes:

- `options.timeoutMs` is currently wired through to execution timeouts.
- `options.explain` / `options.debug` exist in the request schema but are not yet used to trim/expand responses (the API currently always includes `explanation`).
- Merge:
  - Merge strategy is **server-controlled** via `janus.merge.strategy` (see `application.yaml`).
  - The planner/LLM does **not** decide merge strategy; it only emits plan steps.
  - `data.sources` is keyed by `connector` and then `stepId` to avoid overwrites for multi-step plans.
- SQL safety:
  - multi-statement SQL is rejected by guardrails
  - `SELECT *` may be rewritten to an explicit column list when safe (single-table, no JOIN)

Validation notes:

- If `question` is missing/blank, the API returns `400` with a small JSON body containing `field_errors`.
- If you `POST /query` without a JSON body, Spring may route the failure through `/error`; since `/error` is protected, you can see a confusing `401` with `"path":"/error"`. Easiest fix in curl: always send `Content-Type: application/json` and a body (even `{}`) while testing.

### Reading big JSON responses locally

Pretty-print and page:

```bash
curl -sS -X POST localhost:8080/query \
  -H "X-API-Key: $JANUS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"question":"List full data on all calendar events","options":{"explain":true}}' \
| jq . | less -R
```

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

- **Next**: use `options.explain` / `options.debug` to control response size; add safety/redaction for logs and errors
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


