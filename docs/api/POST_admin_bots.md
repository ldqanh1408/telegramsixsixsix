# POST /admin/bots — Tạo hoặc cập nhật bot

## Mục đích

Tạo bot mới hoặc cập nhật bot đã tồn tại trong kho MongoDB. Đây là bước đầu tiên để đưa bot vào hệ thống — trước khi đăng ký webhook Telegram/GitHub.

## Request

```http
POST /admin/bots
Content-Type: application/json
X-Admin-Token: <ADMIN_TOKEN>
```

**Body:**

```json
{
  "username": "my_repo_bot",
  "token": "123456789:ABC-def-ghi",
  "telegramWebhookSecret": "random-telegram-secret",
  "githubRepo": "owner/repo",
  "githubWebhookSecret": "random-github-secret",
  "enabled": true
}
```

| Field | Bắt buộc | Mô tả |
|---|---|---|
| `username` | Có (tạo mới) | Username Telegram bot (không có `@`) |
| `token` | Có (tạo mới) | Token từ BotFather |
| `telegramWebhookSecret` | Không | Secret verify Telegram webhook header |
| `githubRepo` | Không | Repo GitHub dạng `owner/repo` |
| `githubWebhookSecret` | Không | Secret verify chữ ký GitHub |
| `enabled` | Không | Default `true`. `false` để tắt bot |

## Response

**200 OK** — Bot đã được tạo/cập nhật:

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
  "activeGroups": 0,
  "createdAt": "2026-06-25T04:30:00Z",
  "updatedAt": "2026-06-25T04:30:00Z"
}
```

**400 Bad Request:**

```json
{ "error": "username is required" }
{ "error": "token is required for a new bot" }
```

**401 Unauthorized:**

```json
{ "error": "unauthorized" }
```

**404 Not Found** (admin API disabled):

```json
{ "error": "admin api disabled" }
```

## Sequence Diagram

```mermaid
sequenceDiagram
    actor Admin
    participant Controller as AdminBotController
    participant Guard as AdminAccessGuard
    participant Service as AdminBotService
    participant Facade as DynamicBotManager
    participant Registry as ManagedBotRegistry
    participant BotUsername as BotUsername
    participant Repo as ManagedBotRepository
    participant Mapper as AdminBotMapper
    participant DB as MongoDB<br/>managed_bots

    Admin->>Controller: POST /admin/bots<br/>X-Admin-Token: xxx<br/>Body: { username, token, ... }

    Controller->>Guard: denyIfUnauthorized(adminToken)
    alt ADMIN_TOKEN rỗng (chưa cấu hình)
        Guard-->>Controller: AccessDenied(404, "admin api disabled")
        Controller-->>Admin: 404 { error: "admin api disabled" }
    else Token sai hoặc thiếu
        Guard-->>Controller: AccessDenied(401, "unauthorized")
        Controller-->>Admin: 401 { error: "unauthorized" }
    else Token đúng (constant-time compare)
        Guard-->>Controller: Optional.empty() → authorized
    end

    Controller->>Service: save(null, request)
    Service->>Service: request.toRegistration(null)<br/>→ BotRegistration

    Service->>Facade: upsertBot(registration)
    Facade->>Registry: upsert(registration)

    Registry->>BotUsername: BotUsername.of(username)<br/>normalize: strip @, lowercase
    BotUsername-->>Registry: "my_repo_bot"

    alt Username rỗng
        Registry-->>Service: throw IllegalArgumentException("username is required")
        Service-->>Controller: exception
        Controller-->>Admin: 400 { error: "username is required" }
    end

    Registry->>Repo: findByUsername("my_repo_bot")
    Repo->>DB: db.managed_bots.findOne({ username: "my_repo_bot" })

    alt Bot chưa tồn tại + không có token
        DB-->>Repo: null
        Repo-->>Registry: Optional.empty()
        Registry-->>Service: throw IllegalArgumentException("token is required for a new bot")
        Service-->>Controller: exception
        Controller-->>Admin: 400 { error: "token is required for a new bot" }
    else Bot chưa tồn tại + có token
        DB-->>Repo: null
        Repo-->>Registry: Optional.empty()
        Registry->>Registry: merge(null, username, registration)<br/>→ tạo ManagedBot mới
    else Bot đã tồn tại
        DB-->>Repo: existing ManagedBot
        Repo-->>Registry: Optional.of(existing)
        Registry->>Registry: merge(existing, username, registration)<br/>→ giữ field cũ nếu field mới null
    end

    Registry->>Repo: save(mergedBot)
    Repo->>DB: db.managed_bots.save(...)
    DB-->>Repo: saved ManagedBot
    Repo-->>Registry: saved

    alt enabled = true
        Registry->>Registry: cache(saved)<br/>→ runningBots.put(username, saved)
    else enabled = false
        Registry->>Registry: runningBots.remove(username)
    end

    Registry-->>Facade: saved ManagedBot
    Facade-->>Service: saved ManagedBot

    Service->>Mapper: toResponse(saved)
    Mapper->>Facade: activeGroupCount(bot)
    Facade-->>Mapper: count (vd: 0)
    Mapper-->>Service: AdminBotResponse<br/>(hasToken=true, token ẩn)
    Service-->>Controller: AdminBotResponse

    Controller-->>Admin: 200 OK { id, username, hasToken, ... }
```

## Business rules

1. **Username chuẩn hóa**: `@My_Bot` → `my_bot` (strip `@`, lowercase)
2. **Upsert logic**: nếu bot cùng username đã tồn tại → merge field mới vào, field `null` giữ giá trị cũ
3. **Token bắt buộc khi tạo mới**: bot lần đầu phải có token, lần sau update có thể bỏ qua
4. **Cache registry**: bot enabled được cache vào `ConcurrentHashMap` để lookup nhanh khi webhook đến
5. **Response ẩn token**: không bao giờ trả `token`, `tgWebhookSecret`, `ghWebhookSecret` thật — chỉ trả `hasToken`, `hasTelegramWebhookSecret`, `hasGithubWebhookSecret`
