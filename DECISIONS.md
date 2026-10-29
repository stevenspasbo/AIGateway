# Decisions

Decisions that change or add to `spec.md`. Newest last.

| # | Date | Decision | Why |
|---|------|----------|-----|
| D1 | 2026-10-28 | Versions pinned: Spring Boot 4.1.2, Spring AI 2.0.2, Maven 3.10.0 via wrapper 3.3.4 (distribution SHA-256 pinned). Library versions come from the Boot and Spring AI BOMs and are not overridden. | Latest patch releases on Maven Central at project start; Spring AI 2.0.0 was built on Boot 4.1. |
| D2 | 2026-10-28 | Java package and groupId: `dev.spasbo.aigateway` / `dev.spasbo`. | Not specified in the spec. |
| D3 | 2026-10-28 | JaCoCo gate: 90% LINE and BRANCH covered ratio over the whole bundle, Surefire and Failsafe feeding one agent, checked in `verify`. The only exclusion is `AiGatewayApplication` (its `main` is never invoked by `@SpringBootTest`). `-Dcoverage.minimum` raises it later. | Spec asks for 90/90 to start; excluding bootstrap code avoids a test that starts and leaks a full application. |
| D4 | 2026-10-28 | Milestone 1 runs only infrastructure in Compose (Postgres, Keycloak, Ollama + a one-shot model pull); the gateway runs on the host with `./mvnw spring-boot:run`. Postgres is published on host port 5433 by default. | Running the app in Compose makes token issuers differ (`localhost:8180` vs `keycloak:8080`), which is solvable but teaches nothing for M1. 5433 avoids clashing with a locally installed Postgres. |
| D5 | 2026-10-28 | M1 creates only the four tables F1 writes to (tenant, app_user, usage_record, audit_event); the rest arrive with their milestones. usage_record and audit_event reference app_user by the composite (tenant_id, user_id), so a row can never pair a user with another tenant. Status/outcome are `SUCCESS` or `ERROR`; `app_user` is unique on (tenant_id, external_subject). | Schema grows with features; the composite key makes tenant consistency a database guarantee, not a convention. |
| D6 | 2026-10-28 | The local dev tenant (`7f3c1e2a-5b8d-4c6e-9a0f-3d2b1c4e5f60`, "Local Dev Agency", 1,000,000 tokens/month) is a repeatable Flyway migration in `db/local`, added to `spring.flyway.locations` only by the `local` profile. | A versioned migration in a second location would cause out-of-order errors once later `V` migrations are added. |
