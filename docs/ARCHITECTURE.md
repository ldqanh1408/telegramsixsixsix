# ARCHITECTURE — Kiến trúc dự án telegrambots

Tài liệu này giải thích toàn bộ kiến trúc, luồng dữ liệu, design pattern, và cách mở rộng dự án `telegrambots`.

---

## 1. Tổng quan

`telegrambots` là một Spring Boot backend cho phép quản lý **nhiều Telegram bot** cùng lúc, mỗi bot theo dõi một GitHub repo riêng và gửi thông báo vào các Telegram group đã kích hoạt.

**Đặc điểm chính:**

- Multi-bot: một backend duy nhất phục vụ N bot, mỗi bot có token/repo/secret riêng
- Dynamic loading: token bot lưu trong MongoDB, không hardcode trong env
- Webhook-based: cả Telegram lẫn GitHub đều giao tiếp qua webhook (không polling)
- Stateless per-request: `TelegramClient` nhận token theo từng lời gọi, không bind vào một bot cố định

---

## 2. Tech stack

| Thành phần | Công nghệ | Phiên bản |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 4.1.0 |
| Build tool | Maven (wrapper) | 3.9.9 |
| Database | MongoDB | 7 (Docker) |
| ORM | Spring Data MongoDB | (theo Spring Boot) |
| JSON | Jackson 3 | `tools.jackson.databind` |
| HTTP Client | Spring RestClient | (theo Spring Boot) |
| Container | Docker + Docker Compose | multi-stage build |

> **Lưu ý Jackson**: Dự án dùng Jackson 3 với package `tools.jackson.databind`, **không phải** `com.fasterxml.jackson`. Khi import `ObjectMapper`, `JsonNode` phải dùng đúng package.

---

## 3. Cấu trúc package

```
com.lede.telegrambots
├── TelegrambotsApplication.java          # Entry point
├── config/
│   └── AppProperties.java               # @ConfigurationProperties(prefix = "app")
├── bot/                                  # Core domain
│   ├── BotManagementUseCase.java         # Port interface (contract)
│   ├── DynamicBotManager.java            # Facade implementation
│   ├── ManagedBotRegistry.java           # Bot CRUD + in-memory cache
│   ├── BotRegistration.java              # Command object for upsert
│   └── BotUsername.java                  # Value object (normalize username)
├── activation/                           # Group activation domain
│   ├── GroupBotActivationService.java    # Activate/deactivate groups
│   └── ActivationResult.java            # Result record
├── admin/                                # Admin REST API
│   ├── AdminBotController.java           # REST controller
│   ├── AdminBotService.java              # Service layer
│   ├── AdminBotMapper.java               # Entity → Response DTO
│   ├── AdminAccessGuard.java             # Token guard (constant-time)
│   └── dto/
│       ├── AdminBotRequest.java          # Input DTO
│       ├── AdminBotResponse.java         # Output DTO (ẩn token)
│       └── ApiError.java                 # Error DTO
├── telegram/                             # Telegram integration
│   ├── TelegramWebhookController.java    # POST /telegram/webhook/{bot}
│   ├── TelegramClient.java              # Stateless HTTP → Telegram API
│   ├── CommandRouter.java                # Dispatch command → handler
│   ├── MessageFormatter.java            # HTML escape utilities
│   ├── dto/
│   │   └── Update.java                  # Telegram Update model
│   └── command/
│       ├── BotCommand.java              # Command interface
│       ├── CommandContext.java           # Immutable context record
│       ├── StartCommand.java            # /start
│       ├── HelpCommand.java             # /help
│       ├── IdCommand.java               # /id
│       ├── AddCommand.java              # /add @bot
│       ├── RemoveCommand.java           # /remove @bot
│       └── StatusCommand.java           # /status
├── github/                               # GitHub integration
│   ├── GitHubWebhookController.java     # POST /github/webhook/{bot}
│   ├── SignatureVerifier.java           # HMAC-SHA256 verification
│   └── formatter/
│       ├── EventFormatter.java          # Formatter interface
│       ├── PushEventFormatter.java      # push events
│       ├── PullRequestEventFormatter.java
│       ├── IssueEventFormatter.java
│       ├── IssueCommentEventFormatter.java
│       ├── ReleaseEventFormatter.java
│       ├── StarEventFormatter.java
│       └── WorkflowRunEventFormatter.java
├── notification/
│   └── NotificationService.java         # Render + fan-out to groups
└── mongo/
    ├── entity/
    │   ├── ManagedBot.java              # managed_bots document
    │   └── GroupActivation.java         # group_activations document
    └── repo/
        ├── ManagedBotRepository.java
        └── GroupActivationRepository.java
```

---

## 4. Design patterns

### 4.1 Facade — `DynamicBotManager`

`BotManagementUseCase` là interface (port) mà tất cả controller, command, service đều phụ thuộc vào. `DynamicBotManager` là implementation duy nhất, đóng vai trò facade điều phối hai service bên dưới:

```
BotManagementUseCase (interface)
        ↑
DynamicBotManager (facade)
   ├── ManagedBotRegistry      → CRUD bot + in-memory cache
   └── GroupBotActivationService → activate/deactivate groups
```

Lợi ích: controller không cần biết có bao nhiêu service bên dưới, chỉ gọi một interface duy nhất.

### 4.2 Strategy — `EventFormatter`

Mỗi loại GitHub event (push, pull_request, issues...) có một formatter riêng implement `EventFormatter`. `NotificationService` tự động thu thập tất cả formatter beans và index theo `eventName()`:

```java
this.formatters = formatters.stream()
    .collect(toUnmodifiableMap(EventFormatter::eventName, identity()));
```

Thêm formatter mới = thêm `@Component` mới, không sửa code cũ (Open/Closed Principle).

### 4.3 Command — `BotCommand`

Tương tự formatter, mỗi Telegram command là một bean implement `BotCommand`. `CommandRouter` thu thập và dispatch:

```java
this.commands = commands.stream()
    .collect(toUnmodifiableMap(BotCommand::name, identity()));
```

### 4.4 Registry — `ManagedBotRegistry`

Load tất cả bot enabled từ MongoDB vào `ConcurrentHashMap` khi app khởi động (`@PostConstruct`). Khi admin thêm/sửa/xóa bot, registry được cập nhật đồng thời. Lookup từ cache trước, fallback về MongoDB nếu cache miss.

### 4.5 Value Object — `BotUsername`

Chuẩn hóa username ở một nơi duy nhất:
- Strip `@` prefix
- Lowercase
- Trim whitespace

Mọi chỗ so sánh/lưu username đều đi qua `BotUsername.of(...)`.

### 4.6 Guard — `AdminAccessGuard`

So sánh admin token bằng `MessageDigest.isEqual()` (constant-time) để chống timing attack. Nếu `ADMIN_TOKEN` rỗng → trả 404 (admin API disabled hoàn toàn).

---

## 5. Luồng dữ liệu

### 5.1 Admin tạo bot

```
Admin
  │  POST /admin/bots  (X-Admin-Token header)
  │  Body: { username, token, telegramWebhookSecret, githubRepo, githubWebhookSecret, enabled }
  ▼
AdminBotController
  → AdminAccessGuard.denyIfUnauthorized()     // check token
  → AdminBotService.save()
    → AdminBotRequest.toRegistration()        // DTO → command object
    → BotManagementUseCase.upsertBot()
      → ManagedBotRegistry.upsert()
        → BotUsername.of() normalize          // chuẩn hóa
        → ManagedBotRepository.save()         // MongoDB
        → cache vào runningBots (nếu enabled)
    → AdminBotMapper.toResponse()             // entity → response DTO (ẩn token)
  ▼
Response: AdminBotResponse (hasToken: true, không lộ token thật)
```

### 5.2 User kích hoạt bot trong group

```
User trong Telegram group gõ: /add @my_repo_bot
  ▼
Telegram gửi Update tới: POST /telegram/webhook/my_repo_bot
  ▼
TelegramWebhookController
  → bots.findEnabled("my_repo_bot")           // lookup từ registry/MongoDB
  → secretMatches(bot.tgWebhookSecret, header) // verify secret
  → CommandRouter.handle(bot, update)
    → parse "/add @my_repo_bot" → key="/add", arg="@my_repo_bot"
    → AddCommand.execute(CommandContext)
      → stripAt("@my_repo_bot") → "my_repo_bot"
      → bots.activate(bot, chatId)
        → GroupBotActivationService.activate()
          → upsert vào group_activations (active=true)
      → TelegramClient.sendHtml(token, chatId, "✅ Đã kích hoạt...")
```

### 5.3 GitHub event → thông báo Telegram

```
GitHub repo có push event
  ▼
GitHub gửi POST /github/webhook/my_repo_bot
  Headers: X-GitHub-Event: push, X-Hub-Signature-256: sha256=...
  ▼
GitHubWebhookController
  → bots.findEnabled("my_repo_bot")
  → SignatureVerifier.verify(secret, signature, body)  // HMAC-SHA256
  → matchesConfiguredRepo(bot, payload)                // so sánh repo
  → notifications.dispatch(bot, "push", payload)
    ▼
NotificationService
  → formatters.get("push") → PushEventFormatter
  → PushEventFormatter.format(payload) → Optional<String> HTML message
  → broadcast(bot, html, "push")
    → bots.activeGroups(bot) → List<GroupActivation>
    → for each group:
        TelegramClient.sendHtml(bot.token(), chatId, html)
  ▼
Tin nhắn HTML xuất hiện trong mỗi group đã /add @my_repo_bot
```

---

## 6. MongoDB schema

### Collection: `managed_bots`

```json
{
  "_id": "ObjectId",
  "username": "my_repo_bot",           // unique index, lowercase
  "token": "123456789:ABC...",         // Telegram bot token (secret)
  "tgWebhookSecret": "random-string", // verify Telegram webhook
  "githubRepo": "owner/repo",         // GitHub repo full name
  "ghWebhookSecret": "random-string", // verify GitHub webhook signature
  "enabled": true,
  "createdAt": "2026-06-25T04:30:00Z",
  "updatedAt": "2026-06-25T04:45:00Z"
}
```

Indexes:
- `username`: unique

### Collection: `group_activations`

```json
{
  "_id": "ObjectId",
  "botId": "ObjectId ref → managed_bots._id",
  "botUsername": "my_repo_bot",
  "chatId": -1001234567890,            // Telegram group chat ID (negative)
  "active": true,
  "activatedAt": "2026-06-25T05:00:00Z",
  "updatedAt": "2026-06-25T05:00:00Z"
}
```

Indexes:
- `botId`: indexed
- `botUsername`: indexed
- compound unique: `{ botUsername: 1, chatId: 1 }`

---

## 7. REST API reference

### Admin API (`/admin/bots`)

Tất cả endpoint yêu cầu header `X-Admin-Token`.

| Method | Path | Body | Response |
|---|---|---|---|
| `GET` | `/admin/bots` | — | `AdminBotResponse[]` |
| `GET` | `/admin/bots/{username}` | — | `AdminBotResponse` hoặc `404` |
| `POST` | `/admin/bots` | `AdminBotRequest` | `AdminBotResponse` |
| `PUT` | `/admin/bots/{username}` | `AdminBotRequest` (partial) | `AdminBotResponse` |
| `DELETE` | `/admin/bots/{username}` | — | `204 No Content` |

**AdminBotRequest:**

```json
{
  "username": "my_repo_bot",
  "token": "123456789:ABC...",
  "telegramWebhookSecret": "random-telegram-secret",
  "githubRepo": "owner/repo",
  "githubWebhookSecret": "random-github-secret",
  "enabled": true
}
```

**AdminBotResponse** (token được ẩn):

```json
{
  "id": "685be...",
  "username": "my_repo_bot",
  "enabled": true,
  "githubRepo": "owner/repo",
  "hasToken": true,
  "hasTelegramWebhookSecret": true,
  "hasGithubWebhookSecret": true,
  "telegramWebhookPath": "/telegram/webhook/my_repo_bot",
  "githubWebhookPath": "/github/webhook/my_repo_bot",
  "activeGroups": 3,
  "createdAt": "2026-06-25T04:30:00Z",
  "updatedAt": "2026-06-25T04:45:00Z"
}
```

### Webhook endpoints

| Method | Path | Auth Header | Purpose |
|---|---|---|---|
| `POST` | `/telegram/webhook/{botUsername}` | `X-Telegram-Bot-Api-Secret-Token` | Nhận Telegram update |
| `POST` | `/github/webhook/{botUsername}` | `X-Hub-Signature-256` | Nhận GitHub event |

---

## 8. Bảo mật

| Lớp bảo mật | Cơ chế | Vị trí |
|---|---|---|
| Admin API | Token header + constant-time comparison | `AdminAccessGuard` |
| Telegram webhook | Secret token per-bot | `TelegramWebhookController.secretMatches()` |
| GitHub webhook | HMAC-SHA256 signature | `SignatureVerifier.verify()` |
| Token exposure | Response chỉ trả `hasToken` boolean | `AdminBotMapper` / `AdminBotResponse` |
| Admin API disabled | `ADMIN_TOKEN` rỗng → toàn bộ admin API trả 404 | `AdminAccessGuard` |

---

## 9. Cách mở rộng

### Thêm Telegram command mới

1. Tạo class mới trong `telegram/command/`, implement `BotCommand`, đánh `@Component`
2. `name()` trả tên command (vd: `"/ping"`)
3. `execute(CommandContext ctx)` xử lý logic, dùng `ctx.bot()` lấy bot info
4. Không cần sửa `CommandRouter` — Spring DI tự phát hiện

### Thêm GitHub event formatter mới

1. Tạo class mới trong `github/formatter/`, implement `EventFormatter`, đánh `@Component`
2. `eventName()` trả tên event GitHub (vd: `"deployment"`)
3. `format(JsonNode payload)` trả `Optional<String>` HTML message, `Optional.empty()` để bỏ qua
4. Không cần sửa `NotificationService` — Spring DI tự phát hiện

### Thêm bot mới (runtime)

Không cần deploy lại. Chỉ cần gọi Admin API:

```bash
curl -X POST "$PUBLIC_URL/admin/bots" \
  -H "Content-Type: application/json" \
  -H "X-Admin-Token: $ADMIN_TOKEN" \
  -d '{ "username": "new_bot", "token": "...", "githubRepo": "owner/repo2", ... }'
```

Sau đó đăng ký webhook Telegram + GitHub cho bot mới.

---

## 10. Deployment

### Local (không Docker)

```bash
# Cần MongoDB chạy sẵn ở localhost:27017
export MONGODB_URI=mongodb://localhost:27017/telegrambots
export ADMIN_TOKEN=<random-token>
./mvnw spring-boot:run
```

### Docker Compose

```bash
cp .env.docker.example .env
# Sửa ADMIN_TOKEN trong .env
docker compose up --build
```

Compose khởi động:
- `telegrambots-app` (port 8080)
- `telegrambots-mongo` (MongoDB 7, port 27017, healthcheck trước khi app start)

### Public webhook (dev)

Telegram và GitHub yêu cầu HTTPS public URL. Khi dev local dùng ngrok:

```bash
ngrok http 8080
# Dùng URL https://<ngrok>.ngrok.io cho webhook
```

---

## 11. Testing

Dự án có unit tests cho các domain service:

| Test class | Kiểm tra |
|---|---|
| `BotUsernameTest` | Normalize username (strip @, lowercase) |
| `ManagedBotRegistryTest` | CRUD bot, cache, upsert merge logic |
| `GroupBotActivationServiceTest` | Activate/deactivate/count groups |
| `AdminAccessGuardTest` | Token guard: disabled, valid, invalid |
| `TelegrambotsApplicationTests` | Spring context loads |

Chạy tests:

```bash
./mvnw test
```
