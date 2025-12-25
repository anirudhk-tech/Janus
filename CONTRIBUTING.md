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

- Keep packages aligned with architecture (`api`, `auth`, `agent`, `plan`, `connectors`, `federation`, `merge`, `obs`).
- Prefer small PRs with clear commit messages.
- Add tests for behavior changes (unit/integration as appropriate).

### Pull requests

- Describe the change and motivation.
- Include any relevant logs or screenshots (if applicable).
- Ensure `./mvnw test` passes.


