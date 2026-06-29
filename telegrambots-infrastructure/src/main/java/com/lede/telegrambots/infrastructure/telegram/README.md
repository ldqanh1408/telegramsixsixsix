# Package: `com.lede.telegrambots.infrastructure.telegram`

The Telegram Bot API HTTP client (the `TelegramGateway` adapter) plus the event-driven webhook lifecycle manager.

## Class Diagram

```mermaid
classDiagram
    class TelegramGateway {
        <<interface>>
        +sendHtml(token String, chatId long, html String) void
    }
    class TelegramClient {
        -RestClient http
        -Pipeline~SendHtmlContext, Boolean~ sendMessagePipeline
        -Pipeline~SetWebhookContext, Boolean~ setWebhookPipeline
        -Pipeline~DeleteWebhookContext, Boolean~ deleteWebhookPipeline
        +sendHtml(token String, chatId long, html String) void
        +setWebhook(token String, url String, secretToken String) boolean
        +deleteWebhook(token String) boolean
    }
    class TelegramWebhookManager {
        -String publicUrl
        -Pipeline~SyncSavedBotContext, Boolean~ savedPipeline
        -Pipeline~SyncDeletedBotContext, Boolean~ deletedPipeline
        +onBotSaved(event BotSavedEvent) void
        +onBotDeleted(event BotDeletedEvent) void
    }
    class SyncSavedBotContext {
        -BotSavedEvent event
        -String publicUrl
        -String webhookUrl
        -boolean success
    }
    class SyncDeletedBotContext {
        -BotDeletedEvent event
    }

    TelegramClient ..|> TelegramGateway
    TelegramWebhookManager --> TelegramClient : setWebhook / deleteWebhook
    TelegramWebhookManager --> SyncSavedBotContext : creates
    TelegramWebhookManager --> SyncDeletedBotContext : creates
```

## Design Notes

- **Stateless multi-tenant client**: `TelegramClient` holds one `RestClient` (base `https://api.telegram.org`); the bot token is passed per call (`/bot{token}/…`), so a single bean serves every dynamically registered bot.
- **Internal pipelines**: `sendMessage` / `setWebhook` / `deleteWebhook` are each a small `Pipeline` of private inner-record steps, every one guarded first by `ValidateTokenStep` (refuses a blank token). HTTP failures are caught and downgraded to `false` rather than thrown.
- **Event-driven sync**: `TelegramWebhookManager` listens (`@EventListener`) for `BotSavedEvent` / `BotDeletedEvent` and runs the saved/deleted pipelines (steps in `telegram.steps`); `publicUrl` comes from `app.public-url`. Side-effecting external calls justify its place in infrastructure.
- **Dual constructors**: manual (explicit steps + URL) and Spring-autowired (`List<Step<…>>` + `@Value("${app.public-url:}")`).
