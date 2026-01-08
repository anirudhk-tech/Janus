# API Reference

This document describes the current HTTP API surface for Janus.
For configuration, see [`CONFIG.md`](CONFIG.md). For merge behavior, see [`MERGE.md`](MERGE.md).

## Auth

All protected endpoints require an API key:

- Header: `X-API-Key: <value>`
- Server checks it against `JANUS_API_KEY`

Health endpoints are public.

## Endpoints

### `GET /healthz`

- **Auth**: none
- **Response**: `200 OK` with a simple `OK` body

### `GET /actuator/health`

- **Auth**: none
- **Response**: `200 OK` with Spring Boot Actuator health JSON

### `GET /protected/ping`

- **Auth**: required (`X-API-Key`)
- **Response**: `200 OK` with `pong`

### `POST /query`

Protected endpoint that plans and executes a query and returns results.

#### Request body

```json
{
  "question": "List all providers for my calendar events",
  "options": {
    "timeoutMs": 5000,
    "explain": false,
    "debug": false
  }
}
```

Options:

- `options.timeoutMs` (integer, optional): execution timeout budget in ms
- `options.explain` (boolean, optional): include `explanation` in the response only when `true`
- `options.debug` (boolean, optional): reserved (currently unused)

#### Response body

```json
{
  "traceId": "uuid",
  "answer": "executed",
  "data": {
    "sources": {
      "supabase": {
        "step_id": {
          "rows": [],
          "sql": "SELECT ...",
          "params": {}
        }
      }
    },
    "merged": {
      "rows": [],
      "sql": "SELECT ...",
      "params": {}
    }
  }
}
```

Notes:

- `data.sources` is shaped as `connector -> stepId -> stepOutput`.
- `data.merged` is produced by the configured merge strategy (see [`MERGE.md`](MERGE.md)).
- `explanation` is **only present when** `options.explain=true`.

Text output mode:

- When `janus.output.sql=true`, the endpoint returns `text/plain` with one
  block per executed step: `traceId`, step id/connector, the SQL text, params,
  and the step’s rows as a table. The merged JSON payload and `options.explain`
  are skipped in this mode.

#### Example: without explanation (default)

```bash
curl -sS -X POST localhost:8080/query \
  -H "X-API-Key: $JANUS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"question":"List all providers for my calendar events","options":{"timeoutMs":5000}}' \
| jq .
```

#### Example: with explanation

```bash
curl -sS -X POST localhost:8080/query \
  -H "X-API-Key: $JANUS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"question":"List all providers for my calendar events","options":{"timeoutMs":5000,"explain":true}}' \
| jq .
```

When `explain=true`, the response includes:

- `explanation.plan`: execution plan (steps + merge strategy)
- `explanation.execution`: per-step execution results (timings, status, data/error)


