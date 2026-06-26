# CLAUDE.md — telegrambots

## Project overview

Spring Boot 4.1 (Java 21) multi-bot Telegram notifier backed by MongoDB.
Receives GitHub webhook events and broadcasts formatted notifications to Telegram groups.
Bots are managed dynamically via Admin REST API — no per-bot env config.

## Tech stack

- Java 21, Spring Boot 4.1.0, Maven (wrapper: `./mvnw`)
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

Config binding: `application.yaml` → `AppProperties` record (`@ConfigurationProperties(prefix = "app")`).

## Architecture

### Package structure

| Package | Responsibility |
|---|---|
| `bot` | Use-case port (`BotManagementUseCase`), facade (`DynamicBotManager`), registry (`ManagedBotRegistry`), value object (`BotUsername`), command (`BotRegistration`) |
| `activation` | Group activation/deactivation (`GroupBotActivationService`, `ActivationResult`) |
| `admin` | REST API for bot CRUD (`AdminBotController`), guard, service, mapper, DTOs |
| `telegram` | Webhook controller, `TelegramClient` (stateless HTTP), `CommandRouter`, command handlers |
| `github` | Webhook controller, `SignatureVerifier` (HMAC-SHA256), event formatters |
| `notification` | `NotificationService` — renders GitHub events and fans out to active groups |
| `mongo` | Entity records and Spring Data repositories |
| `config` | `AppProperties` configuration record |

### Key patterns

- **Facade**: `DynamicBotManager` implements `BotManagementUseCase`, coordinates registry + activation
- **Strategy**: `EventFormatter` interface — each GitHub event type has its own formatter bean
- **Command**: `BotCommand` interface — each Telegram command is a separate `@Component`
- **Registry**: `ManagedBotRegistry` loads enabled bots into `ConcurrentHashMap` at startup, updates on CRUD
- **Value Object**: `BotUsername` normalizes `@Bot` → `bot` (lowercase, strip @)
- **Guard**: `AdminAccessGuard` uses constant-time comparison for admin token

### Data flow

```
GitHub repo event
  → POST /github/webhook/{botUsername}
  → SignatureVerifier (HMAC-SHA256)
  → repo match check
  → NotificationService.dispatch()
  → EventFormatter.format() → HTML message
  → broadcast to all active groups via TelegramClient
```

```
Telegram user command
  → POST /telegram/webhook/{botUsername}
  → secret token check
  → CommandRouter.handle()
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
- GitHub signatures verified with HMAC-SHA256 (`SignatureVerifier`)
- Telegram webhook secret checked per-bot
- Admin API response never exposes raw tokens — uses `hasToken` boolean flags
- If `ADMIN_TOKEN` is empty, entire admin API returns 404

## Adding a new Telegram command

1. Create a `@Component` class implementing `BotCommand`
2. Return command name (e.g., `/mycommand`) from `name()`
3. Implement `execute(CommandContext ctx)` — use `ctx.bot()` for bot identity/token
4. CommandRouter auto-discovers it via Spring DI

## Adding a new GitHub event formatter

1. Create a `@Component` class implementing `EventFormatter`
2. Return event name (e.g., `"deployment"`) from `eventName()`
3. Implement `format(JsonNode payload)` — return `Optional.empty()` to drop
4. NotificationService auto-discovers it via Spring DI
