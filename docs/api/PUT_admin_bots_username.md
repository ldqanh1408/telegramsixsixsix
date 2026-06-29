# PUT /admin/bots/{username} — Cập nhật bot theo username trên path

## Mục đích

Cập nhật một hoặc nhiều field của bot đã tồn tại. Khác với `POST /admin/bots`, endpoint này lấy username từ path thay vì body — cho phép body chỉ chứa các field cần thay đổi.

## Request

```http
PUT /admin/bots/{username}
Content-Type: application/json
X-Admin-Token: <ADMIN_TOKEN>
```

| Path param | Mô tả |
|---|---|
| `username` | Username bot trên path (tự động normalize) |

**Body** (tất cả field đều optional khi bot đã tồn tại):

```json
{
  "githubRepo": "owner/new-repo",
  "enabled": true
}
```

| Field | Mô tả |
|---|---|
| `token` | Đổi token bot |
| `telegramWebhookSecret` | Đổi Telegram webhook secret |
| `githubRepo` | Đổi repo GitHub theo dõi |
| `githubWebhookSecret` | Đổi GitHub webhook secret |
| `enabled` | Bật/tắt bot |

## Response

**200 OK:**

```json
{
  "id": "685be...",
  "username": "my_repo_bot",
  "enabled": true,
  "githubRepo": "owner/new-repo",
  "hasToken": true,
  "hasTelegramWebhookSecret": true,
  "hasGithubWebhookSecret": true,
  "telegramWebhookPath": "/telegram/webhook/my_repo_bot",
  "githubWebhookPath": "/github/webhook/my_repo_bot",
  "activeGroups": 3,
  "createdAt": "2026-06-25T04:30:00Z",
  "updatedAt": "2026-06-26T09:00:00Z"
}
```

**400 Bad Request:**

```json
{ "error": "username is required" }
{ "error": "token is required for a new bot" }
```

**401 / 404:** tương tự các endpoint admin khác.

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

    Admin->>Controller: PUT /admin/bots/my_repo_bot<br/>X-Admin-Token: xxx<br/>Body: { githubRepo: "owner/new-repo" }

    Controller->>Guard: denyIfUnauthorized(adminToken)
    alt Token không hợp lệ
        Guard-->>Controller: AccessDenied
        Controller-->>Admin: 401 hoặc 404
    else Token hợp lệ
        Guard-->>Controller: Optional.empty()
    end

    Controller->>Service: save("my_repo_bot", request)
    Note over Service: usernameOverride = "my_repo_bot" (từ path)<br/>→ body.username bị bỏ qua

    Service->>Mapper: toRegistration("my_repo_bot", request)<br/>→ BotRegistration(username="my_repo_bot",<br/>   token=null, githubRepo="owner/new-repo", ...)

    Service->>Facade: upsertBot(registration)
    Facade->>Registry: upsert(registration)

    Registry->>BotUsername: BotUsername.of("my_repo_bot")
    BotUsername-->>Registry: "my_repo_bot"

    Registry->>Repo: findByUsername("my_repo_bot")
    Repo->>DB: db.managed_bots.findOne({ username: "my_repo_bot" })
    DB-->>Repo: existing ManagedBot

    Registry->>Registry: merge(existing, "my_repo_bot", registration)
    Note over Registry: Field null trong request → giữ giá trị cũ<br/>githubRepo = "owner/new-repo" (mới)<br/>token = existing.token (giữ nguyên)<br/>updatedAt = now

    Registry->>Repo: save(mergedBot)
    Repo->>DB: db.managed_bots.save(...)
    DB-->>Repo: saved ManagedBot
    Repo-->>Registry: saved

    alt enabled = true
        Registry->>Registry: cache(saved) → cập nhật runningBots
    else enabled = false
        Registry->>Registry: runningBots.remove(username)
    end

    Registry-->>Facade: saved
    Facade-->>Service: saved

    Service->>Mapper: toResponse(saved)
    Mapper-->>Service: AdminBotResponse
    Service-->>Controller: AdminBotResponse
    Controller-->>Admin: 200 OK { ... githubRepo: "owner/new-repo" }
```

## Business rules

1. **Username từ path**: `PUT /admin/bots/my_repo_bot` → username = `"my_repo_bot"`, body.username bị bỏ qua
2. **Partial update**: field `null` trong body → giữ giá trị cũ trong MongoDB (merge logic)
3. **Tạo mới cũng được**: nếu bot chưa tồn tại, `PUT` cũng tạo mới (nhưng cần `token`)
4. **Cache sync**: khi `enabled` thay đổi, registry cache (`runningBots`) được cập nhật ngay
5. **updatedAt tự cập nhật**: luôn set `updatedAt = Instant.now()`

## So sánh với POST /admin/bots

| | POST /admin/bots | PUT /admin/bots/{username} |
|---|---|---|
| Username từ | body.username | path parameter |
| Use-case chính | Tạo bot mới | Cập nhật bot có sẵn |
| Partial update | Có (nếu bot tồn tại) | Có |
