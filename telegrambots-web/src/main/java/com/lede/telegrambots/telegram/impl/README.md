# Package: `telegram.impl`

**Telegram webhook controller and processor.**

## Class Diagram

```mermaid
classDiagram
    class TelegramWebhookController {
        -TelegramWebhookProcessor processor
        +handleUpdate(botUsername, secret, update) ResponseEntity
    }

    class TelegramWebhookProcessor {
        -Pipeline~TelegramWebhookContext TelegramWebhookResult~ pipeline
        +process(botUsername, secret, update) TelegramWebhookResult
    }

    TelegramWebhookController --> TelegramWebhookProcessor : delegates
    TelegramWebhookProcessor --> TelegramWebhookStep : pipeline steps
```

## Design Notes

- Entry URL: `POST /telegram/webhook/{botUsername}`
- `X-Telegram-Bot-Api-Secret-Token` header is extracted and passed as `secret`.
- Controller maps outcome to appropriate HTTP status codes.
