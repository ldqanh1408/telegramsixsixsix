# DELETE /admin/bots/{username} — Xóa bot

## Mục đích

Xóa bot khỏi hệ thống. Xóa luôn tất cả group activation liên quan — các group đã `/add` bot này sẽ không còn nhận thông báo.

## Request

```http
DELETE /admin/bots/{username}
X-Admin-Token: <ADMIN_TOKEN>
```

| Path param | Mô tả |
|---|---|
| `username` | Username bot cần xóa |

Không có request body.

## Response

**204 No Content** — xóa thành công (không có body).

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
    participant ActivationSvc as GroupBotActivationService
    participant Registry as ManagedBotRegistry
    participant BotUsername as BotUsername
    participant ActivationRepo as GroupActivationRepository
    participant BotRepo as ManagedBotRepository
    participant DB_Act as MongoDB<br/>group_activations
    participant DB_Bot as MongoDB<br/>managed_bots

    Admin->>Controller: DELETE /admin/bots/my_repo_bot<br/>X-Admin-Token: xxx

    Controller->>Guard: denyIfUnauthorized(adminToken)
    alt Token không hợp lệ
        Guard-->>Controller: AccessDenied
        Controller-->>Admin: 401 hoặc 404
    else Token hợp lệ
        Guard-->>Controller: Optional.empty()
    end

    Controller->>Service: delete("my_repo_bot")
    Service->>Facade: deleteBot("my_repo_bot")

    Note over Facade: Bước 1: Xóa tất cả activation của bot

    Facade->>ActivationSvc: deleteByBotUsername("my_repo_bot")
    ActivationSvc->>BotUsername: BotUsername.of("my_repo_bot")
    BotUsername-->>ActivationSvc: "my_repo_bot"
    ActivationSvc->>ActivationRepo: deleteByBotUsername("my_repo_bot")
    ActivationRepo->>DB_Act: db.group_activations.deleteMany({ botUsername: "my_repo_bot" })
    DB_Act-->>ActivationRepo: deleted count
    ActivationRepo-->>ActivationSvc: done

    Note over Facade: Bước 2: Xóa bot khỏi managed_bots + cache

    Facade->>Registry: delete("my_repo_bot")
    Registry->>BotUsername: BotUsername.of("my_repo_bot")
    BotUsername-->>Registry: "my_repo_bot"
    Registry->>BotRepo: deleteByUsername("my_repo_bot")
    BotRepo->>DB_Bot: db.managed_bots.deleteOne({ username: "my_repo_bot" })
    DB_Bot-->>BotRepo: deleted
    BotRepo-->>Registry: done
    Registry->>Registry: runningBots.remove("my_repo_bot")

    Registry-->>Facade: done
    Facade-->>Service: done
    Service-->>Controller: done
    Controller-->>Admin: 204 No Content
```

## Business rules

1. **Cascade delete**: xóa bot → xóa tất cả activation trong `group_activations` có `botUsername` khớp
2. **Thứ tự xóa**: activation trước, bot sau (tránh orphan activation)
3. **Cache cleanup**: bot bị remove khỏi `runningBots` (ConcurrentHashMap)
4. **Idempotent**: xóa bot không tồn tại không gây lỗi, vẫn trả `204`
5. **Không thể undo**: sau khi xóa, phải tạo lại bot + đăng ký lại webhook + `/add` lại trong group

## Tác động

```
Trước khi xóa:
├── managed_bots: { username: "my_repo_bot", token: "...", enabled: true }
├── group_activations: [
│     { botUsername: "my_repo_bot", chatId: -100111, active: true },
│     { botUsername: "my_repo_bot", chatId: -100222, active: true }
│   ]
└── runningBots cache: { "my_repo_bot": ManagedBot{...} }

Sau khi xóa:
├── managed_bots: (không còn document)
├── group_activations: (không còn document cho bot này)
└── runningBots cache: (không còn entry)
```
