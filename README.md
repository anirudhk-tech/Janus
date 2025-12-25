# Janus

Janus is an **explainable federated query and reasoning engine**. It turns a question into an execution plan, runs steps across multiple data sources in parallel, and returns merged results with traceable timings and an explanation.

## Status

This repo is under active development. The current scaffold provides:

- `GET /healthz` (app health)
- `GET /actuator/health` (Spring Boot Actuator health)

Milestone 1 target:

- `POST /query` (API-key secured)
- Deterministic planner → `ExecutionPlan`
- Parallel execution across connectors (M1: Postgres + REST)
- Explainable JSON response (plan + timings + per-step errors)

## Why “Janus”?

Janus (the Roman god of doors and thresholds) fits a system that sits at the boundary between multiple data sources and produces a single, explainable response. The project is also inspired by Jarvis.

## Quickstart

### Prerequisites

- Java 21+

### Run locally

```bash
./mvnw spring-boot:run
```

### Health checks

```bash
curl -i localhost:8080/healthz
curl -i localhost:8080/actuator/health
```

Note: Spring Security is enabled. Health endpoints are publicly accessible; everything else will require authentication until API-key auth is implemented.

## API (current)

- `GET /healthz` → `200 OK` with a simple body (`OK`)
- `GET /actuator/health` → `200 OK` with Actuator health JSON

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

## Roadmap (high level)

- **M1**: API-key secured `/query`, deterministic planning, Postgres + REST connectors, parallel federation, explainable response
- **M2**: multi-step plans, caching, improved merge rules
- **M3**: governance (tenants, per-tenant connector policies, audit log)
- **M4**: LLM-backed planner behind `QueryAgent` with safety guardrails

## Contributing

See `CONTRIBUTING.md`.

## Security

See `SECURITY.md`. Please do not open public issues for security reports.

## License

See `LICENSE`.


