# Decisions

Decisions that change or add to `spec.md`. Newest last.

| # | Date | Decision | Why |
|---|------|----------|-----|
| D1 | 2026-10-28 | Versions pinned: Spring Boot 4.1.2, Spring AI 2.0.2, Maven 3.10.0 via wrapper 3.3.4 (distribution SHA-256 pinned). Library versions come from the Boot and Spring AI BOMs and are not overridden. | Latest patch releases on Maven Central at project start; Spring AI 2.0.0 was built on Boot 4.1. |
| D2 | 2026-10-28 | Java package and groupId: `dev.spasbo.aigateway` / `dev.spasbo`. | Not specified in the spec. |
| D3 | 2026-10-28 | JaCoCo gate: 90% LINE and BRANCH covered ratio over the whole bundle, Surefire and Failsafe feeding one agent, checked in `verify`. The only exclusion is `AiGatewayApplication` (its `main` is never invoked by `@SpringBootTest`). `-Dcoverage.minimum` raises it later. | Spec asks for 90/90 to start; excluding bootstrap code avoids a test that starts and leaks a full application. |
