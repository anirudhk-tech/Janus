## Help / Troubleshooting

This file is a quick “runbook” for common Janus setup issues and debugging tips.
For the main project overview and examples, see `README.md`. For historical gotchas, see `docs/DEVLOG.md`.

Related docs:

- `docs/API.md` (API reference)
- `docs/CONFIG.md` (configuration reference)
- `docs/MERGE.md` (merge strategies)

### Run locally

- **With Make**:

```bash
make run
```

- **Without Make**:

```bash
./mvnw spring-boot:run
```

### Required config (local dev)

- **API auth**: set `JANUS_API_KEY` and pass it in `X-API-Key` for protected endpoints.
- **LLM provider**: configure one of:
  - `OPENAI_API_KEY` (when `janus.llm.provider=openai`)
  - `GEMINI_API_KEY` (when `janus.llm.provider=gemini`)
- **Capabilities allowlist**: `janus.capabilities.sources[].sql.schema` + `tables` controls what SQL tables are allowed.
- **Connector credentials**: configure `janus.connectors.*` JDBC URL + username/password for execution (and schema introspection).

### Smoke tests (curl)

```bash
# Public health checks
curl -i localhost:8080/healthz
curl -i localhost:8080/actuator/health

# Protected endpoint: requires X-API-Key
curl -i localhost:8080/protected/ping
curl -i -H "X-API-Key: $JANUS_API_KEY" localhost:8080/protected/ping
```

```bash
curl -sS -X POST localhost:8080/query \
  -H "X-API-Key: $JANUS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"question":"How many calendar events are there?"}' \
| jq .
```

### Common issues

- **401 with `"path":"/error"` after calling `/query`**
  - **Cause**: Spring routed a bad request to `/error`, and `/error` is protected.
  - **Fix**: always send JSON with `Content-Type: application/json` and a body (even `{}`).

- **Guardrail errors (502 with `query_execution_failed`)**
  - Janus validates SQL before execution.
  - **Multi-statement SQL** (e.g. `SELECT 1; SELECT 2;`) is rejected.
    - A single trailing `;` is allowed, but multiple statements are not.
  - **Non-SELECT SQL** is rejected (DDL/DML keywords, etc.).
  - **Out-of-allowlist tables** are rejected (must be listed in `janus.capabilities.sources[].sql.tables`).
  - **Disable guardrails (dev only)**: `janus.sql.guardrails.enabled=false`

- **Schema introspection returns empty columns**
  - **Cause**: the DB user lacks permissions for `information_schema` or the table names/schema don’t match.
  - **Fix**:
    - confirm `sql.schema` and `sql.tables` match actual names
    - ensure the configured DB user can read metadata (at minimum, access to the schema/tables)

- **Postgres `UnknownHostException: user:pass@host`**
  - **Cause**: credentials were embedded in the JDBC URL like `postgresql://user:pass@host/...`.
  - **Fix**: remove userinfo from the URL; set `username` and `password` separately.

- **YAML gotcha: `#` in passwords**
  - **Cause**: `#` starts a comment in YAML.
  - **Fix**: quote any values containing `#` in YAML.

- **Hosted Postgres requires TLS**
  - **Fix**: add `?sslmode=require` to the URL if your provider requires TLS.

### Timeouts

- `/query` supports `options.timeoutMs` which is enforced by the federation executor.
- If the deadline is exceeded, a step can return `TIMEOUT` and the request will fail with `query_execution_failed`.

