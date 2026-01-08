# Configuration Reference

This document describes Janus configuration knobs and environment variables.
For API usage, see [`API.md`](API.md). For merge behavior, see [`MERGE.md`](MERGE.md).

## Overview

Janus is configured via:

- `src/main/resources/application.yaml` (checked in; should not contain secrets)
- Environment variables (recommended for secrets)
- Optional local `.env` (loaded by the Makefile; should not be committed)

## Required environment variables

### `JANUS_API_KEY`

Required for protected endpoints. Incoming `X-API-Key` must match this value.

### LLM provider API key

Configure one provider:

- OpenAI: `OPENAI_API_KEY` (when `janus.llm.provider=openai`)
- Gemini: `GEMINI_API_KEY` (when `janus.llm.provider=gemini`)

## Key `application.yaml` settings

### Planner mode

- `janus.agent.mode`: currently must be `llm`

### LLM provider

- `janus.llm.provider`: `openai` or `gemini`
- `janus.llm.openai.api-key`: usually `${OPENAI_API_KEY:}`
- `janus.llm.openai.model`: model name
- `janus.llm.gemini.api-key`: usually `${GEMINI_API_KEY:}`
- `janus.llm.gemini.model`: model name

### Merge strategy

Server-controlled merge selection:

- `janus.merge.strategy`: e.g. `json-shallow-merge-v1` or `json-deep-merge-v1`

See [`MERGE.md`](MERGE.md) for semantics and available strategies.

### Connectors (execution credentials)

Connector credentials are used for:

- query execution
- schema introspection (for SQL capabilities)

Example (Supabase/Postgres):

```yaml
janus:
  connectors:
    supabase:
      sources:
        cackle:
          jdbc-url: ${JANUS_CONNECTORS_SUPABASE_SOURCES_CACKLE_JDBC_URL:}
          username: ${JANUS_CONNECTORS_SUPABASE_SOURCES_CACKLE_USERNAME:}
          password: ${JANUS_CONNECTORS_SUPABASE_SOURCES_CACKLE_PASSWORD:}
```

Notes:

- Do **not** embed credentials in the URL (`postgresql://user:pass@host/...`). Keep `username` / `password` separate.
- If your hosted Postgres requires TLS, add `?sslmode=require` to the URL.
- If a value contains `#`, quote it in YAML or it will be treated as a comment.

### Capabilities (planner allowlist)

Capabilities tell the planner what sources exist and, for SQL, which tables are allowed.

Example:

```yaml
janus:
  capabilities:
    sources:
      - sourceId: cackle
        connector: supabase
        description: "My calendar events"
        sql:
          schema: public
          tables: ["calendar_events", "links"]
```

Notes:

- `janus.capabilities.sources` must be a YAML **list** (`- ...`), not an object.
- For SQL sources, the listed tables are enforced as an allowlist at execution-time.

### Output format

- `janus.output.sql` (boolean, default `false`): when `true`, `POST /query`
  returns `text/plain` with one block per executed step (SQL, params, rows),
  skipping merged JSON and explanation. Useful for terminal-friendly output.


