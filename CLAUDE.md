# CLAUDE.md — telegrambots

## Project overview

Spring Boot 4.1 (Java 21) multi-bot Telegram notifier backed by MongoDB.
Receives GitHub webhook events and broadcasts formatted notifications to Telegram groups.
Bots are managed dynamically via Admin REST API — no per-bot env config.

## Tech stack

- Java 21, Spring Boot 4.1.0, Maven (wrapper: `./mvnw`)
- **Clean Architecture (Onion)** as a Maven multi-module reactor: `telegrambots-domain` → `telegrambots-application` → `telegrambots-infrastructure` / `telegrambots-web` → `telegrambots-app`
- MongoDB (Spring Data MongoDB) — collections: `managed_bots`, `group_activations`
- Jackson 3 (`tools.jackson.databind`) — NOT `com.fasterxml.jackson`
- RestClient (Spring 6) for Telegram Bot API calls
- Docker multi-stage build (Maven + Eclipse Temurin 21 JRE Alpine)

## Build & run

```bash
# Build
./mvnw clean package

# Run locally (needs MongoDB on localhost:27017)
./mvnw spring-boot:run

# Docker
cp .env.docker.example .env
docker compose up --build

# Tests
./mvnw test
```

On Windows PowerShell, if `./mvnw` fails, use `mvnw.cmd` directly.

## Configuration

All config is via environment variables — no per-bot env vars.

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `MONGODB_URI` | recommended | `mongodb://localhost:27017/telegrambots` | MongoDB connection |
| `ADMIN_TOKEN` | yes (production) | empty (disables admin API) | Guards `/admin/**` endpoints |
| `PORT` | no | `8080` | HTTP server port |

Config binding: `application.yaml` values are injected where needed via `@Value` in the web/infrastructure adapters (e.g. `app.admin.token` → `AdminAccessGuard`, `app.public-url` → `TelegramWebhookManager`).

## Architecture

### Package structure

Clean Architecture (Onion). Each layer is a **Maven module**; the dependency rule is enforced by the Maven reactor graph (a module only sees the layers its `pom.xml` declares). See [docs/architecture/modules.md](docs/architecture/modules.md).

| Maven module | Root package | Responsibility | Depends on |
|---|---|---|---|
| `telegrambots-domain` | `…domain.*` | Pure entities/value objects/shared abstractions: `ManagedBot`, `GroupActivation`, `ActivationResult`, `BotRegistration`, `BotUsername`, `MessageFormatter`, `Bot*Event`, `pipeline.Step`/`pipeline.Pipeline`. No framework. | — |
| `telegrambots-application` | `…application.*` | Use cases + ports (pure Java). Inbound port `BotManagementUseCase` (facade `DynamicBotManager`); use cases `UpsertBotUseCase`/`DeleteBotUseCase` (pipeline), `BotQueryService`, `GroupBotActivationService`, `BroadcastUseCase`; outbound ports `BotRepository`, `ActivationRepository`, `BotCache`, `TelegramGateway`, `WebhookSignatureVerifier`, `DomainEventPublisher`. | `domain` |
| `telegrambots-infrastructure` | `…infrastructure.*` | Driven adapters: Mongo (`*Document`, Spring Data repos, `*Mapper`, `Mongo*Repository`), `ManagedBotRegistry` (cache), `TelegramClient` + `TelegramWebhookManager`, `HmacSha256SignatureVerifier`, `SpringDomainEventPublisher`. | `application` |
| `telegrambots-web` | `…admin` / `…github` / `…telegram` | Driving adapters: REST controllers, DTOs, webhook pipelines (`*Step`/`*Processor`/`*Context`/`*Result`), `GitHubEventRenderer` + formatters, pure command handlers. | `application`, `domain` |
| `telegrambots-app` | `…telegrambots` | Spring Boot bootstrap + composition root (`UseCaseConfiguration` wires the pure use cases as `@Bean`). | `web`, `infrastructure` |

### Key patterns

- **Clean Architecture (Onion)**: dependency rule points inward; `domain` and `application` are framework-free, enforced by the Maven module graph (no `web`↔`infrastructure` dependency). No Spring Modulith.
- **Pipeline (shared)**: `domain.pipeline.Step<C,R>` + `Pipeline<C,R>` back **both** webhook processing (`GitHubWebhookStep`/`TelegramWebhookStep` are typed aliases) **and** multi-step application workflows (`UpsertBotUseCase`, `DeleteBotUseCase`, activation, broadcast)
- **Facade**: `DynamicBotManager` implements `BotManagementUseCase`, routing to the focused use cases
- **Strategy**: `EventFormatter` interface — each GitHub event type has its own formatter bean
- **Command (pure)**: `BotCommand.execute()` returns `Optional<String>` (the reply); the command-execution step is the only sender. Commands have zero transport coupling
- **Application Result Object**: group activation command rules return `GroupActivationCommandResult`; web commands only render those outcomes
- **Ports & Adapters (DIP)**: outbound ports in `application.port.out` (`BotRepository`, `ActivationRepository`, `BotCache`, `TelegramGateway`, `WebhookSignatureVerifier`, `DomainEventPublisher`) implemented by `infrastructure.*` `@Component`s
- **Entity ↔ Document split**: domain records are persistence-free; `infrastructure.persistence.mongo` holds the `@Document` twins + `*Mapper`
- **Application Service + Result Object**: `GitHubWebhookProcessor` owns the webhook pipeline and returns web-agnostic `GitHubWebhookResult`; the controller only maps it to HTTP
- **Registry**: `ManagedBotRegistry` (infra, impl of `BotCache`) loads enabled bots into `ConcurrentHashMap` at startup, updated by the bot use cases
- **Value Object**: `BotUsername` normalizes `@Bot` → `bot` (lowercase, strip @)
- **Guard**: `AdminAccessGuard` uses constant-time comparison for admin token

### Data flow

```
GitHub repo event
  → POST /github/webhook/{botUsername}
  → GitHubWebhookProcessor: WebhookSignatureVerifier (HMAC-SHA256)
  → repo match check
  → GitHubEventRenderer.render() → EventFormatter.format() → HTML message
  → BroadcastUseCase.broadcast() → fan out to all active groups via TelegramGateway
```

```
Telegram user command
  → POST /telegram/webhook/{botUsername}
  → secret token check
  → TelegramWebhookProcessor pipeline (lookup → secret → validate → parse → lookup cmd → execute)
  → BotCommand.execute(CommandContext)
```

### MongoDB collections

**`managed_bots`** — one document per bot:
- `username` (unique index), `token`, `tgWebhookSecret`, `githubRepo`, `ghWebhookSecret`, `enabled`, timestamps

**`group_activations`** — one document per bot-chat binding:
- `botId`, `botUsername`, `chatId` (compound unique index on botUsername+chatId), `active`, timestamps

### REST endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/telegram/webhook/{botUsername}` | `X-Telegram-Bot-Api-Secret-Token` | Telegram webhook |
| POST | `/github/webhook/{botUsername}` | `X-Hub-Signature-256` | GitHub webhook |
| GET | `/admin/bots` | `X-Admin-Token` | List all bots |
| GET | `/admin/bots/{username}` | `X-Admin-Token` | Get one bot |
| POST | `/admin/bots` | `X-Admin-Token` | Create/update bot |
| PUT | `/admin/bots/{username}` | `X-Admin-Token` | Update bot by path username |
| DELETE | `/admin/bots/{username}` | `X-Admin-Token` | Delete bot + its activations |

### Telegram commands

| Command | Handler class | Description |
|---|---|---|
| `/start` | `StartCommand` | Greeting + show tracked repo |
| `/help` | `HelpCommand` | List available commands |
| `/id` | `IdCommand` | Return chat ID |
| `/add @bot` | `AddCommand` | Activate bot in this group |
| `/remove @bot` | `RemoveCommand` | Deactivate bot in this group |
| `/status` | `StatusCommand` | Show bot status + active group count |

### GitHub event formatters

| Event | Formatter class |
|---|---|
| `push` | `PushEventFormatter` |
| `pull_request` | `PullRequestEventFormatter` |
| `issues` | `IssueEventFormatter` |
| `issue_comment` | `IssueCommentEventFormatter` |
| `release` | `ReleaseEventFormatter` |
| `star` | `StarEventFormatter` |
| `workflow_run` | `WorkflowRunEventFormatter` |

## Code conventions

- Java records for entities, DTOs, value objects, and config properties
- No Lombok — pure records with compact constructors
- Constructor injection only (no `@Autowired`)
- Package-private classes where possible (e.g., `ManagedBotRegistry`, command implementations)
- `MessageFormatter` utility for Telegram HTML escaping (`bold()`, `code()`, `esc()`, `link()`)
- Vietnamese language in user-facing Telegram messages and documentation

## Security notes

- Admin token compared with `MessageDigest.isEqual()` (constant-time)
- GitHub signatures verified with HMAC-SHA256 (`WebhookSignatureVerifier` port → `HmacSha256SignatureVerifier`)
- Telegram webhook secret checked per-bot
- Admin API response never exposes raw tokens — uses `hasToken` boolean flags
- If `ADMIN_TOKEN` is empty, entire admin API returns 404

## Adding a new Telegram command

1. In `telegrambots-web`, create a `@Component` class in `telegram.command` implementing `BotCommand`
2. Return command name (e.g., `/mycommand`) from `name()`
3. Implement `execute(CommandContext ctx)` — return `Optional<String>` (the HTML reply, or `Optional.empty()` to stay silent). Do NOT inject any sender; `TelegramCommandExecutionStep` performs the single send
4. `TelegramCommandLookupStep` auto-discovers it via the injected `List<BotCommand>`

## Adding a new GitHub event formatter

1. In `telegrambots-web`, create a `@Component` class in `github.formatter` implementing `EventFormatter`
2. Return event name (e.g., `"deployment"`) from `eventName()`
3. Implement `format(JsonNode payload)` — return `Optional.empty()` to drop
4. `GitHubEventRenderer` auto-discovers it via the injected `List<EventFormatter>`; `EventExecutionStep` renders and hands non-empty messages to `BroadcastUseCase`

## Adding a new outbound dependency (DB/HTTP/etc.)

1. Declare an outbound port in `telegrambots-application` (`application.port.out`)
2. Implement it as a `@Component` adapter in `telegrambots-infrastructure`
3. Use cases depend only on the port; never let `domain`/`application` import Spring/Mongo/Jackson
