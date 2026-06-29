# Package: `com.lede.telegrambots.telegram`

The Telegram webhook processing root types: the typed pipeline step alias, its mutable context, and the web-agnostic result.

## Class Diagram

```mermaid
classDiagram
    class TelegramWebhookStep {
        <<interface>>
        +execute(ctx TelegramWebhookContext) Optional~TelegramWebhookResult~
    }
    class TelegramWebhookContext {
        -String botUsername
        -String secret
        -Update update
        -ManagedBot bot
        -Update.Message message
        -String commandKey
        -String commandArg
        -long chatId
        -BotCommand command
        -String replyHtml
    }
    class TelegramWebhookResult {
        <<record>>
        +outcome Outcome
        +message String
        +of(outcome Outcome, message String) TelegramWebhookResult$
    }
    class Outcome {
        <<enum>>
        OK
        UNKNOWN_BOT
        BAD_SECRET
        INVALID_MESSAGE
        INVALID_COMMAND
        UNKNOWN_COMMAND
        EXECUTION_ERROR
    }

    TelegramWebhookStep ..> TelegramWebhookContext : receives
    TelegramWebhookStep ..> TelegramWebhookResult : produces
    TelegramWebhookResult --> Outcome
```

## Design Notes

- **Typed pipeline alias**: `TelegramWebhookStep extends Step<TelegramWebhookContext, TelegramWebhookResult>`, injected as `List<TelegramWebhookStep>`.
- **Mutable context, progressively enriched**: lookup sets `bot`; validation sets `message`/`chatId`; parsing sets `commandKey`/`commandArg`; command lookup sets `command`; execution sets `replyHtml`.
- **Web-agnostic result**: `TelegramWebhookResult(Outcome, message)` keeps Spring MVC types out of processing; the controller maps `Outcome` → HTTP status.
