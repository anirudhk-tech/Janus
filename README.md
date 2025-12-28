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
- `POST /query` (protected; validates input and returns a stubbed JSON response with a `traceId`)

Milestone 1 target:

- Deterministic planner → `ExecutionPlan`
- Parallel execution across connectors (M1: Postgres + REST)
- Explainable JSON response (plan + timings + per-step errors)

## Architecture (M1)

At a high level, a request to `POST /query` will:

- authenticate via `X-API-Key`
- build an explicit execution plan (steps + parameters)
- execute connector steps in parallel (Postgres + REST)
- merge results into a deterministic response including timings and an explanation

## Quickstart

### Prerequisites

- Java 21+
- GNU Make (usually available via Xcode Command Line Tools on macOS)

### Setup (local only)

Create a local `.env` file in the repo root (do not commit it):

```bash
cat > .env <<'EOF'
JANUS_API_KEY=dev-secret-change-me
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

### `.env` file notes

- `.env` is a **local development convenience**. It is loaded by the Makefile and exported into the process environment before starting Spring Boot.
- Do not commit `.env` (treat it like a password).

## API (current)

- `GET /healthz` → `200 OK` with a simple body (`OK`)
- `GET /actuator/health` → `200 OK` with Actuator health JSON
- `GET /protected/ping` → `200 OK` with body `pong` (requires `X-API-Key`)
- `POST /query` → `200 OK` with a stubbed JSON response (requires `X-API-Key`)

### API key auth (curl examples)

```bash
# Missing key -> 401 JSON
curl -i localhost:8080/protected/ping

# Wrong key -> 401 JSON
curl -i -H 'X-API-Key: wrong' localhost:8080/protected/ping

# Correct key -> 200 pong
curl -i -H "X-API-Key: $JANUS_API_KEY" localhost:8080/protected/ping
```

### `POST /query` (current stub)

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
  "answer": "Not implemented yet",
  "data": {},
  "explanation": {
    "plan": [],
    "execution": [],
    "merge": "pending"
  }
}
```

Validation notes:

- If `question` is missing/blank, the API returns `400` with a small JSON body containing `field_errors`.
- If you `POST /query` without a JSON body, Spring may route the failure through `/error`; since `/error` is protected, you can see a confusing `401` with `"path":"/error"`. Easiest fix in curl: always send `Content-Type: application/json` and a body (even `{}`) while testing.

## API (Milestone 1 target)

- `/query` will evolve from today’s stub into the full M1 behavior:
  - deterministic planner output included in `explanation.plan`
  - parallel execution across connectors with per-step timings/errors in `explanation.execution`
  - deterministic merge rules and merged results in `data`

## Project structure

Source lives under the base package `io.github.anirudhk_tech.janus`:

- `api`: HTTP controllers and DTOs
- `auth`: authentication/authorization (M1: API key)
- `agent`: question → plan (deterministic for M1; pluggable later)
- `plan`: execution plan domain model (keep Spring-free)
- `connectors`: adapters for external systems (Postgres, REST, etc.)
- `federation`: parallel execution, timeouts, partial results
- `merge`: merge + explanation generation
- `obs`: trace IDs, logging/metrics helpers

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

- **M1**: API-key secured `/query` (contract + stub shipped), deterministic planning, Postgres + REST connectors, parallel federation, explainable response
- **M2**: multi-step plans, caching, improved merge rules
- **M3**: governance (tenants, per-tenant connector policies, audit log)
- **M4**: LLM-backed planner behind `QueryAgent` with safety guardrails

## Why “Janus”?

Janus (the Roman god of doors and thresholds) fits a system that sits at the boundary between multiple data sources and produces a single, explainable response. The project is also inspired by Jarvis.

## Contributing

See `CONTRIBUTING.md`.

## Security

See `SECURITY.md`. Please do not open public issues for security reports.

## License

See `LICENSE`.


