# Package: `domain.activation`

**Group activation** aggregate — binding between a bot and a Telegram chat.

## Class Diagram

```mermaid
classDiagram
    class GroupActivation {
        -String id
        -String botId
        -String botUsername
        -long chatId
        -boolean active
        -Instant activatedAt
        -Instant updatedAt
        -Pipeline~ValidationContext String~ validationPipeline$
        +activate() void
        +deactivate() void
        -requireValid(botId, botUsername, chatId)$
    }

    class ValidationContext {
        <<record>>
        -String botId
        -String botUsername
        -long chatId
    }

    class ValidateBotIdStep {
        +execute(ctx) Optional~String~
    }

    class ValidateBotUsernameStep {
        +execute(ctx) Optional~String~
    }

    class ValidateChatIdStep {
        +execute(ctx) Optional~String~
    }

    class ActivationResult {
        <<record>>
        +activation() GroupActivation
        +newlyActivated() boolean
    }

    GroupActivation --> ValidationContext : uses
    GroupActivation --> ValidateBotIdStep : runs
    GroupActivation --> ValidateBotUsernameStep : runs
    GroupActivation --> ValidateChatIdStep : runs
    ActivationResult --> GroupActivation : contains
```

## Design Notes

- **Rich Domain Model & Local Pipelines**: All business validations are enforced through a static `Pipeline` (`validationPipeline`) of granular verification steps, throwing an `IllegalArgumentException` with the first encountered error.
- `activate()` / `deactivate()` mutate `active` state and stamp `updatedAt` locally.
- **`ActivationResult`** distinguishes "newly activated now" from "was already active".
