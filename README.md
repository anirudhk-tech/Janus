# Janus

Janus is an **explainable federated query and reasoning engine**. It turns a question into an execution plan, runs steps across multiple data sources in parallel, and returns merged results with traceable timings and an explanation.

## Status

This repo is under active development.

What works today:

- `GET /healthz` (app health)
- `GET /actuator/health` (Spring Boot Actuator health)
- Spring Security is enabled:
  - health endpoints are public
  - other endpoints currently use **HTTP Basic** (temporary, will be replaced by API-key auth)

Milestone 1 target:

- `POST /query` (API-key secured)
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

Note: health endpoints are public. Other endpoints are currently protected by **HTTP Basic**; this will switch to `X-API-Key` as part of Milestone 1.

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

## API (Milestone 1 target)

- `POST /query`
  - Auth header: `X-API-Key: <key>` (validated against `JANUS_API_KEY`)

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

- **M1**: API-key secured `/query`, deterministic planning, Postgres + REST connectors, parallel federation, explainable response
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


