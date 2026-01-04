## Contributing

Thanks for helping improve Janus.

### Development setup

Prereqs:

- Java 21+

Run:

```bash
./mvnw spring-boot:run
```

Test:

```bash
./mvnw test
```

### Coding conventions

- Keep packages aligned with architecture:
  - `api`, `auth`, `agent`, `plan`, `federation`, `connectors`
  - `capabilities` (planner metadata / allowlists)
  - `capabilities/sql` (schema introspection + SQL guardrails)
- Prefer small PRs with clear commit messages.
- Add tests for behavior changes (unit/integration as appropriate).
- Do not commit secrets (API keys, DB credentials). Use local `.env` files or environment variables.

### Pull requests

- Describe the change and motivation.
- Include any relevant logs or screenshots (if applicable).
- Ensure `./mvnw test` passes.

### Notes for SQL-related changes

- Capabilities are the **source of truth allowlist** for SQL:
  - tables are allowlisted via `janus.capabilities.sources[].sql.tables`
  - schema/column metadata is introspected from Postgres for allowlisted tables only
- SQL guardrails run before execution:
  - single-statement `SELECT` only (no DDL/DML; no multi-statement)
  - out-of-allowlist tables should fail fast with a clear error
- Tests should be hermetic:
  - if a test context does not provide real capabilities + DB access, disable guardrails via `janus.sql.guardrails.enabled=false`


