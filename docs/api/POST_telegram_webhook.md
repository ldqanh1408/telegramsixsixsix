# POST /telegram/webhook/{botUsername} — Nhận Telegram update

## Mục đích

Endpoint mà Telegram gọi khi có message mới gửi đến bot. Backend lookup bot từ URL path, verify secret, parse command, và dispatch đến handler tương ứng.

## Request

```http
POST /telegram/webhook/{botUsername}
Content-Type: application/json
X-Telegram-Bot-Api-Secret-Token: <telegramWebhookSecret>
```

| Path param | Mô tả |
|---|---|
| `botUsername` | Username bot (vd: `my_repo_bot`). Key để lookup bot trong registry |

**Body** (Telegram Update):

```json
{
  "update_id": 123456789,
  "message": {
    "message_id": 1,
    "chat": {
      "id": -1001234567890,
      "type": "group"
    },
    "from": {
      "id": 111222333,
      "first_name": "Admin"
    },
    "text": "/add @my_repo_bot",
    "date": 1719300000
  }
}
```

## Response

- **200 OK** — luôn trả 200 (kể cả khi xử lý lỗi) để Telegram không retry
- **404 Not Found** — bot không tồn tại hoặc disabled
- **401 Unauthorized** — secret token sai

## Sequence Diagram — Luồng chung

```mermaid
sequenceDiagram
    actor User as Telegram User
    participant TG as Telegram Server
    participant Controller as TelegramWebhookController
    participant Facade as BotManagementUseCase
    participant Registry as ManagedBotRegistry
    participant Router as CommandRouter
    participant DB as MongoDB<br/>managed_bots

    User->>TG: Gõ "/add @my_repo_bot" trong group
    TG->>Controller: POST /telegram/webhook/my_repo_bot<br/>X-Telegram-Bot-Api-Secret-Token: xxx<br/>Body: Update { message.text: "/add @my_repo_bot" }

    Controller->>Facade: findEnabled("my_repo_bot")
    Facade->>Registry: findEnabled("my_repo_bot")

    alt Bot có trong cache (runningBots)
        Registry-->>Facade: Optional.of(cachedBot)
    else Cache miss → query MongoDB
        Registry->>DB: findByUsername("my_repo_bot")
        alt Bot tồn tại + enabled
            DB-->>Registry: ManagedBot
            Registry->>Registry: cache(bot)
            Registry-->>Facade: Optional.of(bot)
        else Bot không tồn tại hoặc disabled
            DB-->>Registry: null / disabled
            Registry-->>Facade: Optional.empty()
        end
    end

    alt Bot không tìm thấy
        Facade-->>Controller: Optional.empty()
        Controller-->>TG: 404 Not Found
    else Bot tìm thấy
        Controller->>Controller: secretMatches(bot.tgWebhookSecret, header)
        alt Secret sai
            Controller-->>TG: 401 Unauthorized
        else Secret đúng (hoặc bot không set secret)
            Controller->>Router: handle(bot, update)
            Note over Router: Parse command, dispatch handler<br/>(xem diagram từng command bên dưới)
            Router-->>Controller: done
            Controller-->>TG: 200 OK
        end
    end
```

---

## Command: /start

Gửi lời chào và repo bot đang theo dõi.

```mermaid
sequenceDiagram
    actor User as Telegram User
    participant Router as CommandRouter
    participant Cmd as StartCommand
    participant Client as TelegramClient
    participant TG as Telegram API

    User->>Router: /start

    Router->>Router: parse text → key="/start", arg=""
    Router->>Cmd: execute(CommandContext { chatId, arg="", bot })

    Cmd->>Client: sendHtml(bot.token, chatId,<br/>"Chào! Bot đang theo dõi repo owner/repo")
    Client->>TG: POST /bot<TOKEN>/sendMessage<br/>{ chat_id, text, parse_mode: "HTML" }
    TG-->>Client: ok
    Client-->>Cmd: done
```

---

## Command: /help

Liệt kê danh sách lệnh có sẵn.

```mermaid
sequenceDiagram
    actor User as Telegram User
    participant Router as CommandRouter
    participant Cmd as HelpCommand
    participant Client as TelegramClient
    participant TG as Telegram API

    User->>Router: /help

    Router->>Router: parse text → key="/help", arg=""
    Router->>Cmd: execute(CommandContext { chatId, arg="", bot })

    Cmd->>Client: sendHtml(bot.token, chatId,<br/>"Danh sách lệnh: /start /help /id /add /remove /status")
    Client->>TG: POST /bot<TOKEN>/sendMessage
    TG-->>Client: ok
```

---

## Command: /id

Trả về Telegram chat ID của group/user hiện tại.

```mermaid
sequenceDiagram
    actor User as Telegram User
    participant Router as CommandRouter
    participant Cmd as IdCommand
    participant Client as TelegramClient
    participant TG as Telegram API

    User->>Router: /id

    Router->>Router: parse text → key="/id", arg=""
    Router->>Cmd: execute(CommandContext { chatId, arg="", bot })

    Cmd->>Client: sendHtml(bot.token, chatId,<br/>"Chat ID: -1001234567890")
    Client->>TG: POST /bot<TOKEN>/sendMessage
    TG-->>Client: ok
```

---

## Command: /add @bot — Kích hoạt bot trong group

```mermaid
sequenceDiagram
    actor User as Telegram User
    participant Router as CommandRouter
    participant Cmd as AddCommand
    participant Facade as BotManagementUseCase
    participant ActivationSvc as GroupBotActivationService
    participant Repo as GroupActivationRepository
    participant Client as TelegramClient
    participant DB as MongoDB<br/>group_activations
    participant TG as Telegram API

    User->>Router: /add @my_repo_bot

    Router->>Router: parse → key="/add", arg="@my_repo_bot"
    Router->>Cmd: execute(CommandContext { chatId, arg="@my_repo_bot", bot })

    Cmd->>Cmd: stripAt("@my_repo_bot") → "my_repo_bot"

    alt arg rỗng (user gõ "/add" không có tên)
        Cmd->>Client: sendHtml("Cú pháp: /add @my_repo_bot")
        Client->>TG: sendMessage
    else requested ≠ bot.username (tên bot khác)
        Note over Cmd: Im lặng, return<br/>(bot không phải của mình)
    else requested = bot.username
        Cmd->>Facade: activate(bot, chatId)
        Facade->>ActivationSvc: activate(bot, chatId)

        ActivationSvc->>Repo: findByBotUsernameAndChatId("my_repo_bot", chatId)
        Repo->>DB: db.group_activations.findOne(...)

        alt Đã active từ trước
            DB-->>Repo: { active: true }
            Repo-->>ActivationSvc: existing (active=true)
            ActivationSvc-->>Facade: ActivationResult(existing, newlyActivated=false)
            Facade-->>Cmd: result
            Cmd->>Client: sendHtml("ℹ️ Group này đã được kích hoạt từ trước.")
            Client->>TG: sendMessage
        else Từng deactivate (active=false)
            DB-->>Repo: { active: false }
            Repo-->>ActivationSvc: existing (active=false)
            ActivationSvc->>Repo: save({ ...existing, active: true, updatedAt: now })
            Repo->>DB: db.group_activations.save(...)
            DB-->>Repo: saved
            ActivationSvc-->>Facade: ActivationResult(saved, newlyActivated=true)
            Facade-->>Cmd: result
            Cmd->>Client: sendHtml("✅ Đã kích hoạt. Sẽ gửi thông báo repo owner/repo")
            Client->>TG: sendMessage
        else Chưa có document
            DB-->>Repo: null
            ActivationSvc->>Repo: save(new GroupActivation { botId, botUsername, chatId, active: true })
            Repo->>DB: db.group_activations.insertOne(...)
            DB-->>Repo: saved
            ActivationSvc-->>Facade: ActivationResult(saved, newlyActivated=true)
            Facade-->>Cmd: result
            Cmd->>Client: sendHtml("✅ Đã kích hoạt. Sẽ gửi thông báo repo owner/repo")
            Client->>TG: sendMessage
        end
    end
```

---

## Command: /remove @bot — Tắt thông báo trong group

```mermaid
sequenceDiagram
    actor User as Telegram User
    participant Router as CommandRouter
    participant Cmd as RemoveCommand
    participant Facade as BotManagementUseCase
    participant ActivationSvc as GroupBotActivationService
    participant Repo as GroupActivationRepository
    participant Client as TelegramClient
    participant DB as MongoDB<br/>group_activations
    participant TG as Telegram API

    User->>Router: /remove @my_repo_bot

    Router->>Router: parse → key="/remove", arg="@my_repo_bot"
    Router->>Cmd: execute(CommandContext { chatId, arg="@my_repo_bot", bot })

    Cmd->>Cmd: stripAt("@my_repo_bot") → "my_repo_bot"

    alt requested ≠ bot.username
        Note over Cmd: Im lặng, return
    else requested = bot.username
        Cmd->>Facade: deactivate(bot, chatId)
        Facade->>ActivationSvc: deactivate(bot, chatId)

        ActivationSvc->>Repo: findByBotUsernameAndChatId("my_repo_bot", chatId)
        Repo->>DB: db.group_activations.findOne(...)

        alt Không có document hoặc đã inactive
            DB-->>Repo: null hoặc { active: false }
            ActivationSvc-->>Facade: false
            Facade-->>Cmd: false
            Cmd->>Client: sendHtml("ℹ️ Group này chưa kích hoạt bot.")
            Client->>TG: sendMessage
        else Đang active
            DB-->>Repo: { active: true }
            ActivationSvc->>Repo: save({ ...existing, active: false, updatedAt: now })
            Repo->>DB: db.group_activations.save(...)
            ActivationSvc-->>Facade: true
            Facade-->>Cmd: true
            Cmd->>Client: sendHtml("✅ Đã tắt thông báo.")
            Client->>TG: sendMessage
        end
    end
```

---

## Command: /status — Trạng thái bot trong group

```mermaid
sequenceDiagram
    actor User as Telegram User
    participant Router as CommandRouter
    participant Cmd as StatusCommand
    participant Facade as BotManagementUseCase
    participant ActivationSvc as GroupBotActivationService
    participant ActivationRepo as GroupActivationRepository
    participant Client as TelegramClient
    participant DB as MongoDB<br/>group_activations
    participant TG as Telegram API

    User->>Router: /status

    Router->>Router: parse → key="/status", arg=""
    Router->>Cmd: execute(CommandContext { chatId, arg="", bot })

    Cmd->>Facade: isActive(bot, chatId)
    Facade->>ActivationSvc: isActive(bot, chatId)
    ActivationSvc->>ActivationRepo: existsByBotUsernameAndChatIdAndActiveTrue(...)
    ActivationRepo->>DB: db.group_activations.exists(...)
    DB-->>ActivationRepo: true/false
    ActivationRepo-->>Cmd: active = true/false

    Cmd->>Facade: activeGroupCount(bot)
    Facade->>ActivationSvc: activeGroupCount(bot)
    ActivationSvc->>ActivationRepo: countByBotUsernameAndActiveTrue(...)
    ActivationRepo->>DB: db.group_activations.count(...)
    DB-->>ActivationRepo: count
    ActivationRepo-->>Cmd: total = 5

    Cmd->>Client: sendHtml(<br/>"Bot status — @my_repo_bot<br/>Repo: owner/repo<br/>Group này: ✅ activated<br/>Tổng group đang nhận: 5")
    Client->>TG: POST /bot<TOKEN>/sendMessage
    TG-->>Client: ok
```

## Business rules

1. **Luôn trả 200**: kể cả khi command handler throw exception — tránh Telegram retry vô hạn
2. **Secret optional**: nếu bot không set `tgWebhookSecret` → bỏ qua verify (dev mode)
3. **Command routing**: chỉ xử lý message bắt đầu bằng `/`, các message thường bị bỏ qua
4. **Bot suffix strip**: Telegram gửi `/start@my_repo_bot` trong group → strip `@my_repo_bot` để match key `/start`
5. **Multi-bot isolation**: `/add @bot_a` chỉ kích hoạt `bot_a`, `bot_b` nhận cùng message nhưng im lặng
6. **Unknown command**: command không có handler → log debug, không trả lời user
