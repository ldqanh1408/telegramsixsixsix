# GET /admin/bots — Lấy danh sách tất cả bot

## Mục đích

Trả về danh sách tất cả bot đang được quản lý trong MongoDB, bao gồm cả bot enabled và disabled. Dùng để admin kiểm tra trạng thái hệ thống.

## Request

```http
GET /admin/bots
X-Admin-Token: <ADMIN_TOKEN>
```

Không có request body.

## Response

**200 OK:**

```json
[
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
  },
  {
    "id": "685bf...",
    "username": "another_bot",
    "enabled": false,
    "githubRepo": "owner/other-repo",
    "hasToken": true,
    "hasTelegramWebhookSecret": false,
    "hasGithubWebhookSecret": true,
    "telegramWebhookPath": "/telegram/webhook/another_bot",
    "githubWebhookPath": "/github/webhook/another_bot",
    "activeGroups": 0,
    "createdAt": "2026-06-25T05:00:00Z",
    "updatedAt": "2026-06-25T05:00:00Z"
  }
]
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
    participant Repo as ManagedBotRepository
    participant Mapper as AdminBotMapper
    participant DB as MongoDB<br/>managed_bots

    Admin->>Controller: GET /admin/bots<br/>X-Admin-Token: xxx

    Controller->>Guard: denyIfUnauthorized(adminToken)
    alt Token không hợp lệ
        Guard-->>Controller: AccessDenied
        Controller-->>Admin: 401 hoặc 404
    else Token hợp lệ
        Guard-->>Controller: Optional.empty()
    end

    Controller->>Service: list()
    Service->>Facade: listBots()
    Facade->>Registry: listBots()
    Registry->>Repo: findAll()
    Repo->>DB: db.managed_bots.find({})
    DB-->>Repo: List<ManagedBot>
    Repo-->>Registry: List<ManagedBot>
    Registry-->>Facade: List<ManagedBot>
    Facade-->>Service: List<ManagedBot>

    loop Với mỗi ManagedBot
        Service->>Mapper: toResponse(bot)
        Mapper->>Facade: activeGroupCount(bot)
        Facade->>Facade: activations.countByBotUsernameAndActiveTrue()
        Facade-->>Mapper: count
        Mapper-->>Service: AdminBotResponse (token ẩn)
    end

    Service-->>Controller: List<AdminBotResponse>
    Controller-->>Admin: 200 OK [...]
```

## Business rules

1. **Trả tất cả bot**: bao gồm cả `enabled=false`, admin cần thấy toàn cảnh
2. **Active group count**: mỗi bot response kèm số group đang active (`activeGroups`)
3. **Token ẩn**: response không bao giờ chứa token/secret thật
4. **Danh sách rỗng hợp lệ**: nếu chưa có bot nào → trả `[]`
