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


