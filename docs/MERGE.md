# Merge Strategies

This document describes how Janus merges per-step outputs into `data.merged`.
For configuration, see [`CONFIG.md`](CONFIG.md). For API shapes, see [`API.md`](API.md).

## Selection

Merge selection is **server-controlled**:

- `janus.merge.strategy` (in `application.yaml` / env)

The planner/LLM does not decide merge behavior.

## Available strategies

### `json-shallow-merge-v1`

Top-level map merge with collision suffixing.

- Iterates steps in plan order.
- For each step’s output map:
  - If a key does not exist: set it.
  - If a key already exists: write to `key_1`, `key_2`, ... (first available).

This is good when each step returns a mostly disjoint top-level shape.

### `json-deep-merge-v1`

Deep merge for JSON-ish structures.

Rules:

- Map + Map: **recursive merge**
- List + List: **concatenate** (preserves order)
- Any other collision (including scalar vs object/list): **preserve both by suffixing** the incoming value (`key_1`, `key_2`, ...)

This is good when multiple steps return nested JSON and you want a single combined object without silently dropping data.

## Example

Given step A output:

```json
{ "rows": [1], "meta": { "sql": "A" } }
```

and step B output:

```json
{ "rows": [2], "meta": { "sql": "B" } }
```

Then:

- `json-shallow-merge-v1` produces:
  - `rows: [1]`, `rows_1: [2]`, `meta: {sql:"A"}`, `meta_1: {sql:"B"}`
- `json-deep-merge-v1` produces:
  - `rows: [1,2]`, and since `meta.sql` collides as a scalar, you’ll get something like:
    - `meta: { "sql": "A", "sql_1": "B" }`


