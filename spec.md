# Practice Project Spec: Multi-Provider AI Gateway

Oct 28, 2026 · @Steven Spasbo

## Purpose

Build a small, production-shaped AI gateway in Spring Boot before starting on Systone's federal AI shared-service project, so the AI-specific parts of that work are familiar on day one. Spring, React, SQL and AWS are known territory; the project exists to exercise what sits on top of them.

By the end it should have taught:

- How a chat request turns into provider calls and a streamed response
- Token accounting, and why per-user and per-tenant metering matters once a shared service charges agencies for use
- Swapping model providers behind one interface without vendor details leaking into the rest of the code
- Retrieval-augmented generation end to end: chunking, embeddings, vector search, prompt assembly
- How to hold an LLM-backed codebase to strict TDD and a coverage gate when the model's output is not deterministic

## Scope

The project is a multi-tenant chat service with document Q&A, usage metering and an admin view, runnable locally with Docker Compose. It is a learning build, so anything that teaches little new is cut.

**In scope**

- Chat with streaming responses, against at least two model providers
- Persistent conversations per user
- Per-request usage records and a monthly token quota per tenant
- Document upload and question answering over those documents (RAG), isolated per tenant
- A small admin dashboard of usage by tenant and model
- An audit log of who called which model and when
- A test suite and CI pipeline that enforce a coverage gate

**Out of scope**

- Training or fine-tuning models
- Real billing or payment
- Production hosting and a real identity provider (a local stand-in is enough)
- Polished visual design beyond accessible, standard components
- Image generation and voice

## Architecture

One Spring Boot service sits between the UI and the model providers; nothing calls a model directly.

&#91;embedded content: request path · 8 steps through one gateway\]

Steps 3 to 5 are cheap, deterministic gates, so most of the request path can be tested without calling a model. Document upload takes its own path: parse, chunk, embed, store in pgvector.

## Tech stack

The stack mirrors the posting (Java, Spring Boot, AWS, SQL, React, GitHub) and adds the pieces a Java team would most likely use for the AI layer.

| Layer | Choice | Why |
| --- | --- | --- |
| Backend | Java 21, Spring Boot 4.1, Spring AI 2.0 | Current supported line; Boot 3.5 and Spring AI 1.1 left open-source support on June 30, 2026, and Spring AI 2.0 requires Boot 4 |
| Build | Maven with a pinned wrapper and the Enforcer plugin | Declarative POM keeps the JaCoCo gate readable; the wrapper keeps CI and local builds identical |
| Model providers | AWS Bedrock (Converse API) plus Ollama locally, default model llama3.2:1b, base URL configurable | Bedrock hosts several vendors behind one AWS API; Ollama keeps day-to-day dev free and offline, and can point at a native install for speed |
| Database | PostgreSQL 16 with pgvector, Flyway migrations | One database for relational data and embeddings |
| Auth | Spring Security as an OAuth2 resource server, Keycloak in Docker | Local stand-in for agency SSO; tenant\_id and a roles claim (user, admin) come from the JWT |
| Frontend | React, TypeScript, Vite, U.S. Web Design System components | USWDS is the common federal design system and is built for accessibility |
| Backend tests | JUnit, Mockito, WireMock, Testcontainers, JaCoCo | Fake providers, recorded HTTP, a real Postgres in tests, coverage gate |
| Frontend tests | Vitest, React Testing Library, Playwright, axe-core | Component tests, end-to-end with a mocked stream, automated accessibility checks |
| CI and local | GitHub Actions, Docker Compose | Matches the posting; one command to run everything |

Spring AI 2.0 has breaking changes from 1.x, and most tutorials still show 1.x code. Use the 2.0 reference docs, not older examples.

Open question: confirm on the Friday call which providers and SDKs the real project uses, and adjust this table to match.

## Features and acceptance criteria

Eight features, each done when its criteria pass as automated tests.

| # | Feature | Acceptance criteria |
| --- | --- | --- |
| F1 | Basic chat | POST a message and model id to a temporary /api/chat endpoint (conversations arrive in milestone 4) and get a complete reply; each request writes one usage record and one audit event; a model outside the configured allowlist gets a 400; a failed model call still writes both records (status ERROR, 0 tokens, real latency) and returns a 502 |
| F2 | Streaming | Tokens reach the UI incrementally over SSE; time to first token is logged; cancelling in the UI stops the upstream provider call |
| F3 | Multiple providers | The same request works against two providers chosen by model id; adding a provider needs only config and one adapter class; a provider outage returns a clear 502, not a stack trace |
| F4 | Usage metering and quotas | Every call records input tokens, output tokens, model, user, tenant and latency, using the counts the provider reports; a tenant over its monthly token budget gets a 429 with a readable message |
| F5 | Conversations | Conversations persist and can be resumed; history sent to the model is trimmed to fit the model's context window, oldest turns first |
| F6 | Document Q&A (RAG) | Upload PDF or Markdown into a tenant collection; it is chunked and embedded; answers cite the chunks they used; tenant A can never retrieve tenant B's chunks |
| F7 | Admin dashboard | Admins see token usage by tenant and model per day; non-admins get a 403 |
| F8 | Audit log | Each request logs who, when, which model and the outcome; storing prompt text is a config flag, off by default (prompt\_text stays null until this feature lands) |

Identity rules for every feature: the tenant\_id claim holds the tenant's UUID, and a token whose tenant is not in the database gets a 403. An app\_user row is created on a user's first request, with role refreshed from the token each time. The local test tenant is seeded by a migration that only the local profile runs.

## Data model

Eight tables, every row owned by a tenant so isolation can be enforced in one place. All primary keys are UUIDs.

| Table | Key fields | Relates to |
| --- | --- | --- |
| tenant | id, name, monthly\_token\_budget | Parent of everything below |
| app\_user | id, tenant\_id, external\_subject, role | tenant |
| conversation | id, tenant\_id, user\_id, model\_id, title, created\_at | tenant, app\_user |
| message | id, conversation\_id, role (user or assistant), content, token\_count, created\_at | conversation |
| usage\_record | id, tenant\_id, user\_id, model\_id, input\_tokens, output\_tokens, latency\_ms, status, created\_at | tenant, app\_user |
| document | id, tenant\_id, filename, status, uploaded\_by, created\_at | tenant, app\_user |
| document\_chunk | id, document\_id, tenant\_id, chunk\_index, text, embedding (vector) | document; tenant\_id is duplicated so every vector query can filter on it |
| audit\_event | id, tenant\_id, user\_id, action, model\_id, outcome, prompt\_text (nullable), created\_at | tenant, app\_user |

Usage summaries for the dashboard come from a daily rollup query over usage\_record; a materialized view is a stretch goal.

## API surface

A small REST API under /api, with tenant and user taken from the JWT, never from the request body.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | /api/models | List available models and their providers |
| POST | /api/chat | Milestone 1 only: send a model id and message, get a complete reply (F1); replaced by the conversation endpoints in milestone 4 |
| POST | /api/conversations | Start a conversation with a chosen model |
| GET | /api/conversations | List the caller's conversations |
| GET | /api/conversations/{id} | Get one conversation with its messages |
| POST | /api/conversations/{id}/messages | Send a message, get a complete reply |
| POST | /api/conversations/{id}/messages/stream | Send a message, get the reply as Server-Sent Events (F2) |
| POST | /api/documents | Upload a document for indexing (F6) |
| GET | /api/documents | List the tenant's documents and indexing status |
| DELETE | /api/documents/{id} | Remove a document and its chunks |
| GET | /api/usage/me | The caller's usage this month and remaining tenant budget |
| GET | /api/admin/usage | Usage by tenant, model and day, admin only (F7) |
| GET | /api/admin/audit | Paged audit events, admin only (F8) |

Errors use RFC 9457 problem details, so 429 quota errors and 502 provider errors share one shape the UI can display.

## Testing strategy

The rule: CI never calls a real model. Everything that gates a merge is deterministic, and judging answer quality runs separately as an eval suite that reports but does not block.

| Layer | Tools | What it proves |
| --- | --- | --- |
| Unit | JUnit 5, Mockito, a fake ChatModel returning scripted replies | Services, quota math, context trimming, prompt assembly; assert on structure (which chunks went into the prompt, token counts), never on model wording |
| Provider contract | WireMock with recorded responses | Each adapter parses real response shapes, including streamed chunks, 429 throttling, 500s, timeouts and malformed payloads |
| Integration | Spring Boot tests with Testcontainers Postgres plus pgvector | Repositories, migrations, vector search ordering, and tenant isolation: seed two tenants and prove neither can see the other |
| Frontend | Vitest, React Testing Library | Components render streamed text, errors and quota messages correctly |
| End to end | Playwright against a stubbed SSE stream, axe-core | Full chat flow in a browser, keyboard-only use, no accessibility violations |
| Evals (non-gating) | A small golden set of questions run against real models | Retrieval hit rate (did the expected chunk land in the top k, which is deterministic and measurable) and answer quality, tracked over time |

Write tests first for every feature. Set the JaCoCo gate high (the posting asks for full coverage, so start at 90% line and branch and raise it), and fail the build below it. Retrieval hit rate is worth noting: it turns part of RAG quality into a number you can assert on, which is exactly the kind of thing to bring up in a team that cares about TDD.

## Security, accessibility and operations

These are the concerns a federal AI service gets judged on, so each one gets at least one test.

**Security**

- Tenant isolation lives in one place (a repository filter or Postgres row-level security), not scattered across queries
- Prompt injection: retrieved document text is wrapped and labeled as data in the prompt; a test uploads a poisoned document ("ignore previous instructions...") and checks the system prompt still holds
- Secrets come from environment variables locally and would come from AWS Secrets Manager in a deployment; nothing in the repo
- Logs redact prompt text unless the audit flag is on

**Accessibility (WCAG 2.1 AA, Section 508)**

- Streamed replies render into an aria-live="polite" region that announces the finished message, not every token
- Every action works by keyboard, with visible focus and a focus move to the new reply
- Error and quota messages are announced, not only colored

**Operations**

- Micrometer metrics per provider: latency, time to first token, tokens per second, error rate
- Structured JSON logs with a request id carried through to the provider call
- Retries with backoff for provider 429s and 5xxs, and a circuit breaker (Resilience4j) so one failing provider does not take the service down

## Milestones and stretch goals

Six milestones in build order, each ending with a demoable slice and green CI. If time before the start date runs short, stopping after milestone 4 still covers the core of what the job will involve.

1. Skeleton: Spring Boot app, Postgres via Compose, Flyway, Keycloak login, GitHub Actions with the JaCoCo gate, and F1 against Ollama through the temporary /api/chat endpoint
2. Streaming and UI: F2 with a React and USWDS chat screen, aria-live behavior, Playwright and axe-core in CI
3. Providers: F3 with Bedrock as the second provider, WireMock contract tests, retries and circuit breaker
4. Metering: F4 usage records and quotas, F8 audit log, F5 conversations with context trimming
5. RAG: F6 upload, chunking, embeddings in pgvector, cited answers, tenant isolation tests, the poisoned-document test, and the first retrieval evals
6. Admin: F7 dashboard and the usage rollup

**Stretch goals**

- Side-by-side model comparison: send one prompt to two models and show both answers with cost and latency, which is the evaluation use case USAi itself is built around
- Tool calling: let the model call a function, such as looking up the user's remaining quota
- Agency federation: add two Keycloak realms as stand-ins for two agencies' identity providers, federate them into the main realm, and map each to a tenant from the token claims, the way a multi-agency service trusts each agency's own sign-in (in production that would be agency Okta, Entra ID or Ping over SAML or OIDC, ending in PIV/CAC)
- Deploy to AWS (ECS Fargate or App Runner with RDS) behind a real domain
- Publish the repo publicly with a clear README, since the team works in the open
