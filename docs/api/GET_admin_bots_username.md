# GET /admin/bots/{username} — Lấy thông tin một bot

## Mục đích

Trả về thông tin chi tiết của một bot theo username. Dùng để admin kiểm tra trạng thái cấu hình, xác nhận bot đã có đủ token/secret chưa.

## Request

```http
GET /admin/bots/{username}
X-Admin-Token: <ADMIN_TOKEN>
```

| Path param | Mô tả |
|---|---|
| `username` | Username bot (vd: `my_repo_bot`). Tự động normalize: strip `@`, lowercase |

## Response

**200 OK:**

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

**404 Not Found** — bot không tồn tại:

```json
{ "error": "bot not found" }
```

**401 Unauthorized:**

```json
{ "error": "unauthorized" }
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

    Admin->>Controller: GET /admin/bots/my_repo_bot<br/>X-Admin-Token: xxx

    Controller->>Guard: denyIfUnauthorized(adminToken)
    alt Token không hợp lệ
        Guard-->>Controller: AccessDenied
        Controller-->>Admin: 401 hoặc 404
    else Token hợp lệ
        Guard-->>Controller: Optional.empty()
    end

    Controller->>Service: find("my_repo_bot")
    Service->>Facade: findAny("my_repo_bot")
    Facade->>Registry: findAny("my_repo_bot")
    Registry->>BotUsername: BotUsername.of("my_repo_bot")
    BotUsername-->>Registry: normalized = "my_repo_bot"

    Registry->>Repo: findByUsername("my_repo_bot")
    Repo->>DB: db.managed_bots.findOne({ username: "my_repo_bot" })

    alt Bot tồn tại
        DB-->>Repo: ManagedBot
        Repo-->>Registry: Optional.of(bot)
        Registry-->>Facade: Optional.of(bot)
        Facade-->>Service: Optional.of(bot)

        Service->>Mapper: toResponse(bot)
        Mapper->>Facade: activeGroupCount(bot)
        Facade-->>Mapper: count
        Mapper-->>Service: AdminBotResponse

        Service-->>Controller: Optional.of(response)
        Controller-->>Admin: 200 OK { id, username, ... }
    else Bot không tồn tại
        DB-->>Repo: null
        Repo-->>Registry: Optional.empty()
        Registry-->>Facade: Optional.empty()
        Facade-->>Service: Optional.empty()
        Service-->>Controller: Optional.empty()
        Controller-->>Admin: 404 { error: "bot not found" }
    end
```

## Business rules

1. **findAny**: tìm cả bot enabled lẫn disabled (khác với `findEnabled` chỉ dùng cho webhook)
2. **Username normalize**: path `/@My_Bot` hay `/my_repo_bot` đều được chuẩn hóa trước khi query
3. **Token ẩn**: response chỉ trả `hasToken`, `hasTelegramWebhookSecret`, `hasGithubWebhookSecret`
4. **Active groups**: kèm đếm số group đang nhận thông báo từ bot này
