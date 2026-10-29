# AI Gateway (practice project)

A multi-tenant gateway between callers and LLM providers, built in Spring Boot 4.1 and Spring AI 2.0.
See [`spec.md`](spec.md) for the full plan and [`DECISIONS.md`](DECISIONS.md) for every decision that
changes or adds to it.

**Status: milestone 1.** One endpoint (`POST /api/chat`) sends a message to a local Ollama model and
returns the complete reply. Every call that reaches a model is metered (provider-reported tokens,
latency, status) and audited. Tenant and role come from a Keycloak JWT.

## Prerequisites

- Java 21
- Docker (Compose v2)
- Nothing else. The Maven wrapper (`./mvnw`) downloads a pinned Maven 3.10.0.

## Run locally

```bash
# 1. Infrastructure: Postgres+pgvector (5433), Keycloak (8180), Ollama (11434), and a one-shot pull
#    of llama3.2:1b (~1.3 GB, first run only)
docker compose up -d

# 2. The gateway, with the local profile (seeds the dev tenant)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**Faster Ollama on a Mac.** Ollama in Docker cannot use the Apple GPU. If you have a native install
(`ollama serve`, then `ollama pull llama3.2:1b`), start only `docker compose up -d postgres keycloak`
and point the gateway at it with `OLLAMA_BASE_URL=http://localhost:11434`.

### Try it

```bash
TOKEN=$(curl -s http://localhost:8180/realms/ai-gateway/protocol/openid-connect/token \
  -d grant_type=password -d client_id=ai-gateway-cli \
  -d username=alice -d password=alice-dev-only | jq -r .access_token)

curl -s localhost:8080/api/models -H "Authorization: Bearer $TOKEN"

curl -s localhost:8080/api/chat -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"model": "llama3.2:1b", "message": "In one sentence, what is a token?"}'
# {"reply":"...","model":"llama3.2:1b","inputTokens":38,"outputTokens":53,"latencyMs":2767}
```

| Try | Expect |
|---|---|
| `"model": "gpt-4o"` | 400 `Model not allowed`, no model called |
| Token for `orphan` | 403 `Tenant not recognized` (its tenant is not in the database) |
| No `Authorization` header | 401 |
| `docker compose stop ollama`, then chat | 502 `Model provider error`, plus an `ERROR` usage row with 0 tokens |

Errors are RFC 9457 problem details (`application/problem+json`).

### Dev-only credentials

> **These exist only for local development. Never reuse them, and never enable the password grant in a
> real realm.**

| What | Value |
|---|---|
| Keycloak admin console | http://localhost:8180, `admin` / `admin-dev-only` |
| Gateway users (realm `ai-gateway`) | `alice` (user), `ada-admin` (user + admin), `orphan` (unknown tenant); password is `<username>-dev-only` |
| Token client | `ai-gateway-cli`: public, direct access grants on so you can curl tokens |
| Postgres | `aigateway` / `aigateway-dev-only` on `localhost:5433` |

All of them can be overridden with environment variables (see [`.env.example`](.env.example)). The
repository contains no real secrets.

## Configuration

| Variable | Default | Used by |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | `localhost`, `5433`, `aigateway`, `aigateway`, `aigateway-dev-only` | app, Compose |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/ai-gateway` | app |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | app |
| `OLLAMA_MODEL` | `llama3.2:1b` | app (default model and allowlist), Compose (model pull) |

## Test

```bash
./mvnw verify
```

This runs unit tests (Surefire, `*Test`), integration tests (Failsafe, `*IT`, which need Docker for
Testcontainers Postgres and Keycloak) and the JaCoCo gate: at least **90% line and branch** coverage,
or the build fails. The report is at `target/site/jacoco/index.html`.

**No test calls a real model.** Unit and end-to-end tests use `FakeChatModel`, which returns scripted
replies and token counts and records the prompts it received. Tests assert on structure (model id,
token counts, records written), never on model wording. `ChatEndToEndIT` exercises the full stack:
real Keycloak tokens, the real security chain, real Postgres and migrations, and the fake in place of
the provider.

CI (`.github/workflows/ci.yml`) runs the same `./mvnw verify` on every push and pull request.

## Layout

```
src/main/java/dev/spasbo/aigateway/
  chat/       POST /api/chat, GET /api/models, ChatService, model allowlist
  identity/   JWT roles, tenant resolution filter, app_user upsert
  metering/   usage_record + audit_event writes
  config/     security, clock, MVC wiring
  web/        RFC 9457 problem-detail mapping
src/main/resources/db/migration   Flyway schema (V1, V2)
src/main/resources/db/local       dev tenant seed, local profile only
keycloak/ai-gateway-realm.json    realm imported by Compose and by the Keycloak Testcontainer
```
