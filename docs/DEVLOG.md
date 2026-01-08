# Devlog / Notes

This is a lightweight running log of build/run/debug notes and “gotchas” encountered while developing Janus.
It’s intentionally informal and optimized for future-you and contributors trying to reproduce fixes quickly.

## Project history (backfilled from early commits)

This section is a quick “how we got here” timeline so the devlog doesn’t start abruptly.

### Thu Dec 25, 2025 — Bootstrap

- Scaffolded the Spring Boot service and OSS hygiene docs (security policy, contributing/code of conduct, etc.).

### Fri Dec 26, 2025 — API key auth + initial API surface

- Added `X-API-Key` auth and a protected smoke endpoint (`/protected/ping`), plus documentation around local `.env` usage.

### Sat–Mon Dec 27–29, 2025 — `/query` scaffold

- Introduced a protected `POST /query` endpoint with DTOs + tests.
- Began returning a structured response that includes an execution plan (foundation for explainability).

### Tue Dec 30, 2025 — Federation execution skeleton

- Built the parallel execution skeleton (`federation`) and the typed result model.
- Added HTTP error mapping for execution failures (later refined to include better detail in dev).
- Updated README examples to reflect the evolving response format.

### Wed–Fri Dec 31, 2025 – Jan 2, 2026 — LLM planner + provider support

- Added pluggable LLM planning support (Gemini client first; later refined).
- Updated Gemini model/API integration as versions changed.
- Introduced OpenAI configuration and tightened the plan validation to reject unsupported step types.
- Enhanced `LlmQueryAgent` to incorporate capabilities in prompts and require `sourceId` for SQL steps.
- Added JDBC + Postgres driver dependencies to support real SQL execution paths.

## Sat Jun 3, 5:54 PM

### Today’s problems + fixes

- **LLM plan generation failed with `ExecutionPlan.steps is required`**
  - **Symptom**: `/query` threw `IllegalArgumentException: ExecutionPlan.steps is required`.
  - **Root cause**: LLM can legally return `{}` (still valid JSON), which deserializes into `ExecutionPlan(steps=null, ...)`.
  - **Also hit**: `janus.capabilities.sources` YAML was initially shaped as an object instead of a list, so capabilities bound as empty.
  - **Fixes/notes**:
    - Ensure `janus.capabilities.sources` is a YAML list:
      - `sources:`
      - `  - sourceId: ...`
    - Log the raw LLM JSON during debugging so schema issues are obvious.

- **Supabase/Postgres connection failures**
  - **Symptom**: `CannotGetJdbcConnectionException: Failed to obtain JDBC Connection`, later `UnknownHostException`.
  - **Root cause**: `jdbc-url` included credentials like `postgresql://user:pass@host/db`.
    - Postgres JDBC does **not** treat URI userinfo the same way; it can mis-parse host and attempt DNS lookup for `user:pass@host`.
  - **Fix**: Provide credentials separately (`username`, `password`), and keep `jdbc-url` host-only:
    - Example: `postgresql://db.<project>.supabase.co:5432/postgres?sslmode=require`
  - **Hardening**: add a guard that rejects URLs with `user:pass@host` and throws a clear error.

- **YAML gotcha: `#` in passwords**
  - In YAML, `#` begins a comment unless the value is quoted.
  - **Fix**: quote any passwords/strings containing `#` (and other special characters) in `application.yaml`.

- **SSL required for hosted Postgres**
  - Many hosted Postgres providers require TLS.
  - **Fix**: add `?sslmode=require` to the connection URL when needed.

### Design/architecture observations

- **Connectors vs capabilities**
  - **Capabilities** inform the planner (LLM) what sources exist (`connector` + `sourceId` + hints).
  - **Connector config** contains execution credentials/settings (JDBC URL, username, password, etc.).

- **Keeping `supabase` as a logical connector**
  - We kept `connector: supabase` for clarity, but routed execution to the Postgres connector implementation.
  - This keeps “business semantics” separate while reusing the same execution engine.

- **Error visibility**
  - If the API returns generic `query_execution_failed`, debugging gets painful fast.
  - Better dev ergonomics: include a safe `detail` field and log stack traces server-side (redacting secrets).

### Developer UX tips

- Terminal JSON is hard to read at scale. Prefer:
  - `curl ... | jq . | less -R`
  - or write to a file: `curl ... | jq . > out.json` and open in an editor.

## Sun Jan 4, 2026 — Schema-aware SQL planning + execution guardrails

### What we added

- **Schema introspection for the LLM (allowlisted)**
  - The LLM previously only saw `schema` + `tables` in capabilities, which encouraged `COUNT(*)` / `SELECT *`.
  - Added a small Postgres introspector that queries `information_schema.columns` for the allowlisted tables in `janus.capabilities.sources[].sql.tables`.
  - The LLM prompt now includes **table → columns (+ type/nullable)** so it can choose specific columns.

- **In-memory caching**
  - Introspected schemas are cached in-process (per `(connector, sourceId, schema, tables[])` key) to avoid re-hitting `information_schema` every request.
  - Restarting the server clears the cache (expected).

- **SQL guardrails (execution-time)**
  - Added a strict validation/rewrite layer that runs before connector execution.
  - Enforces single-statement, `SELECT`-only queries and blocks obvious dangerous keywords (DDL/DML).
  - Enforces allowlist by rejecting references to tables not present in capabilities for that `(connector, sourceId)`.
  - Rewrites `SELECT *` to an explicit column list when it is safe to do so (single-table; no JOIN).

### Gotchas encountered

- **Semicolons**
  - Many tools/LLMs emit a trailing `;` even for a single statement.
  - Guardrails were adjusted to allow a trailing semicolon while still rejecting true multi-statement SQL.

- **Tests**
  - Guardrails depend on real capabilities + schema introspection. MVC tests are kept hermetic by disabling guardrails via:
    - `janus.sql.guardrails.enabled=false`

### Follow-ups / next iterations

- Consider a TTL/refresh mechanism for schema cache (or keep it simple until we have many sources).
- Make `SELECT *` rewrite more capable (joins/aliases) or add a better SQL parser if needed.

## Mon Jan 5, 2026 — Merge strategies + output shape direction

### What we added

- **Merge strategy plugin point**
  - Introduced `MergeStrategy` + `MergeService` under `io.github.anirudhk_tech.janus.merge`.
  - Added first strategy: `json-shallow-merge-v1` (top-level map merge; key collisions get suffixed: `key`, `key_1`, `key_2`, ...).

- **Server-controlled merge selection**
  - Merge selection is now configured via `janus.merge.strategy` in `application.yaml`.
  - Planner/LLM no longer emits or decides merge strategy (it only emits `steps`).

- **Response structure tweak to avoid overwrites**
  - `data.sources` is now shaped as `connector -> stepId -> data` so multi-step plans against the same connector do not overwrite each other.
  - `data.merged` contains the merged output (strategy-driven).

### Next: make the API response compact by default

- Current responses are too verbose/ugly for typical clients.
- Desired direction:
  - Default `options.explain=false` (or omit `explanation`) for the common case.
  - Return a compact, domain-shaped `data` payload (e.g. keys like `"providers"`, `"links"`, `"events"`) with **no raw SQL/params/rows metadata**.
  - Move execution details (raw SQL, params, per-step rows, timings, etc.) under `explanation` when `explain=true`.

## Tue Jan 6, 2026 — Explain gating + deep merge strategy

### What we added

- **`options.explain` now controls response verbosity**
  - `POST /query` now only includes the `explanation` object when `options.explain=true`.
  - When not requested, `explanation` is omitted from the JSON response (not present as `null`).

- **New merge strategy: `json-deep-merge-v1`**
  - Added `JsonDeepMergeV1` as a `MergeStrategy`.
  - Semantics:
    - map + map: deep/recursive merge
    - list + list: concatenate
    - non-mergeable collisions: preserve both values by suffixing keys (`key`, `key_1`, `key_2`, ...)
  - Enable via `janus.merge.strategy=json-deep-merge-v1`.

## Wed Jan 7, 2026 — SQL-only output mode (no merge)

### What we added

- **Config flag `janus.output.sql`**
  - When `true`, `POST /query` returns `text/plain` with one block per executed step (step id/connector, SQL, params, rows rendered as a table).
  - Skips merge + explanation entirely; response is per-step raw outputs only.
  - Default remains JSON (with merge and optional explanation).

### Tests/docs

- Added `QueryControllerSqlOutputTest` to assert plaintext shape (step header, SQL, params, table, row count).
- Updated `CONFIG.md` and `API.md` to clarify SQL mode behavior and JSON default.

### Notes

- If you need to pipe to `jq`, keep `janus.output.sql=false` (JSON mode). Text mode is for terminal-friendly inspection.

### Observability + polish

- Added `TraceContextFilter` to propagate/generate `X-Trace-Id` and put it in MDC for log correlation (also echoed back in responses).
- Added `janus.output.color` to toggle ANSI color in SQL text output (disable for pipelines/tests).
