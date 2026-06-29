# Package: `com.lede.telegrambots.telegram.dto`

The inbound Telegram `Update` DTO — mirrors the subset of the Telegram Bot API webhook payload the bot consumes.

## Class Diagram

```mermaid
classDiagram
    class Update {
        <<record>>
        +update_id long
        +message Message
    }
    class Message {
        <<record>>
        +message_id long
        +chat Chat
        +from User
        +text String
    }
    class Chat {
        <<record>>
        +id long
        +type String
        +title String
    }
    class User {
        <<record>>
        +id long
        +username String
        +first_name String
    }
    Update --> Message
    Message --> Chat
    Message --> User
```

## Design Notes

- **Nested records**: `Update` and its `Message` / `Chat` / `User` are immutable records using snake_case component names matching the wire JSON.
- **Lenient parsing**: every record is annotated `@JsonIgnoreProperties(ignoreUnknown = true)` so unmodeled Telegram fields are dropped instead of failing deserialization.
- Only the fields needed for command processing are modeled (chat id/type, sender, message text).
