# AI-powered Support Ticket Assistant

A focused Spring Boot assessment project that accepts support tickets, persists them, and performs LLM-based categorisation asynchronously. The default mock provider makes the project runnable with no external account or API key.

## Architecture and lifecycle

```
TicketController -> TicketService -> TicketRepository
                              -> after commit -> AsyncTicketAnalysisService
                                                -> TicketAnalysisProvider -> TicketAnalysisRepository
```

`POST /api/tickets` validates the JSON, creates a UUID, saves the ticket as `PENDING`, and registers work to start **after the database transaction commits**. It returns `201 Created` without waiting for the provider. The async worker reloads the ticket, changes it to `PROCESSING`, requests analysis, validates the provider JSON, saves the analysis, then marks the ticket `COMPLETED`. Provider, timeout, malformed-output, and validation errors result in `FAILED`, with a deliberately generic safe error message.

Status lifecycle: `PENDING -> PROCESSING -> COMPLETED`, or `PENDING -> PROCESSING -> FAILED`.

## Stack and layout

- Java 17 target, Spring Boot, Spring MVC, Spring Data JPA, H2, JUnit 5, Mockito, and MockMvc.
- `entity/` has the compact JPA model and lifecycle enums.
- `service/` contains transactional creation, async orchestration, and strict JSON validation.
- `llm/` contains the application-level provider port plus mock and configurable HTTP adapters.
- `controller/`, `dto/`, and `exception/` form the REST boundary.
- `src/main/resources/prompts/ticket-analysis-prompt.md` is the provider prompt template.

## Prerequisites, setup, and run

Use JDK 17+ and Maven 3.9+.

```bash
mvn spring-boot:run
```

The H2 database is in-memory and the application defaults to `llm.mock.mode=success`. No secret is required. H2 console is available at `/h2-console` while the process runs.

## Configuration and providers

Default mock properties in `application.yml`:

```yaml
llm.mock.mode: success # success, invalid, failure
llm.mock.delay-ms: 100
```

Use `invalid` or `failure` to manually demonstrate safe failed processing. The `TicketAnalysisProvider` interface keeps domain services independent of an SDK. The mock is deterministic enough for local runs/tests. Automated tests cover provider failure and invalid-output handling at the service layer; the configurable mock failure modes are not full REST integration-test profiles. To select the optional real HTTP adapter, activate the profile and supply secrets outside source control:

```bash
# Edit .env with your provider values, then run:
mvn spring-boot:run "-Dspring-boot.run.profiles=real-llm"
```

The application optionally imports a root `.env` file as Spring properties. It expects `LLM_ENDPOINT`, `LLM_API_KEY`, `LLM_MODEL` (defaults to `gpt-4.1-mini`), and optionally `LLM_TIMEOUT_SECONDS`, `LLM_AUTH_HEADER` (defaults to `api-key`), and `LLM_AUTH_PREFIX` (defaults to empty). For an OpenAI-compatible endpoint, set `LLM_AUTH_HEADER=Authorization` and `LLM_AUTH_PREFIX=Bearer `. For an Azure-style endpoint, keep the defaults and provide the deployment-specific endpoint. The file is ignored by Git; keep real credentials there and never commit them.

The adapter uses the JDK `HttpClient`, applies `llm.timeout-seconds` (default 10), renders the repository prompt template, and sends a JSON request shaped as `{ "model": "...", "input": "..." }` with the configured authentication header. It accepts either the specified analysis JSON directly or a compatible response containing `output_text`/`output[].content[].text`. The request and response envelope are intentionally isolated in this adapter so a provider-specific adapter can replace it without changing the core service. The real provider profile was startup-checked for configuration binding, but it was not verified against a live LLM service in this assessment environment.

## API

### Create a ticket

```bash
curl -i -X POST http://localhost:8080/api/tickets \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"CUST-101","subject":"Unable to import a Word document","description":"The import remains at 80% and eventually fails.","priority":"HIGH","product":"Import"}'
```

Returns `201 Created`, a `Location` header, a UUID `ticketId`, and initially `PENDING` with `analysis: null`. Invalid/missing fields or an invalid priority return `400`:

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request fields are missing or invalid."
}
```

### Retrieve a ticket

```bash
curl http://localhost:8080/api/tickets/{ticketId}
```

While pending/processing, `analysis` is `null`. On success it includes:

```json
{
  "ticketId": "...",
  "status": "COMPLETED",
  "analysis": {
    "category": "IMPORT_FAILURE",
    "summary": "...",
    "suggestedResponse": "...",
    "recommendedTeam": "SUPPORT",
    "confidence": 0.85
  }
}
```

With mock invalid/failure or a provider error it returns status `FAILED`, `analysis: null`, and a safe generic `processingError`. A missing ID returns `404` with `TICKET_NOT_FOUND`.

## Structured-output, security, and error handling

The validator rejects malformed JSON, non-object payloads, missing/null/empty fields, extra fields, non-numeric confidence, confidence outside 0–1, oversized values, and category/team values that are not uppercase identifiers. It does not use a fixed allow-list for category or team names, so otherwise well-formed but unfamiliar values remain valid. Failed output is never persisted. The prompt has explicit role/output rules and separately labels ticket content as untrusted data; embedded ticket instructions cannot override task instructions or request prompt disclosure/command execution.

Centralized REST advice consistently returns validation, not-found, and internal errors without stack traces. Async processing errors are stored as a safe state rather than exposed as REST provider details. The application intentionally does not log complete ticket bodies, provider responses, API keys, credentials, or auth headers. API keys are environment variables only. In production add PII redaction/classification, encryption and retention controls, secret management, audit policy, and access controls.

## Tests

```bash
mvn test
```

Tests cover HTTP creation/validation/404 via MockMvc, eventual completed analysis with all five fields, and Mockito-driven successful analysis, incomplete/invalid structured output, timeout, and provider failure paths. The integration test uses delayed mock analysis so the creation response can reliably assert `PENDING`; mock `invalid` and `failure` configuration modes remain manual demonstration paths rather than separate REST integration profiles.

## Assumptions and limitations

- The app is intentionally a single-node demonstration with H2 and in-process async work.
- Only one analysis job is accepted because the worker proceeds only from `PENDING`; the current design does not expose regeneration.
- There is no retry: temporary failures become `FAILED` and can be retried operationally by a future feature.
- The generic real adapter is intentionally provider-neutral, not a full vendor SDK integration.
- Approximate implementation time: 5–7 hours.
- AI tools used during development: an AI coding assistant was used to scaffold, review, and test the implementation; all design choices and code should be reviewed by the submitter.

## Production Readiness / Future Improvements

Use a persistent queue/message broker, distributed workers, retry/backoff and dead-letter handling; add authentication/authorization and rate limits; introduce structured logs, metrics, tracing, provider/model latency monitoring, and alerting; implement robust PII redaction, prompt versioning, database migrations, optimistic locking/idempotency, durable error history, and horizontal scaling. These are deliberately not included to keep the assessment solution explainable and appropriately scoped.

## Technical interview walkthrough

A POST validates `customerId`, subject, description, and enum priority. `TicketService` generates a UUID and commits the pending row. Its after-commit callback invokes a Spring `@Async` worker so the HTTP response never waits for model latency. The worker sets `PROCESSING`, calls the abstraction-selected provider, validates exactly five structured fields, saves the one-to-one analysis, and marks `COMPLETED`. GET reloads the ticket and optional analysis. A timeout, provider exception, or invalid JSON marks `FAILED` without saving analysis. The key decisions to explain are the after-commit boundary (avoids an async transaction reading uncommitted state), the provider port (testability/vendor independence), strict output validation (LLM output is untrusted), and the mock default (runnable without secrets).
