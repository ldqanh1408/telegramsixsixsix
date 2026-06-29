# Package: `domain.bot`

Core **Bot** aggregate of the domain layer.

## Responsibility

Defines the `ManagedBot` entity (root aggregate), input value object `BotRegistration`,
and domain events emitted when a bot's lifecycle changes.

## Class Diagram

```mermaid
classDiagram
    class ManagedBot {
        -String id
        -String username
        -String token
        -String tgWebhookSecret
        -String githubRepo
        -String ghWebhookSecret
        -boolean enabled
        -Instant createdAt
        -Instant updatedAt
        -Pipeline~ValidationContext String~ validationPipeline$
        +create(username, registration) ManagedBot$
        +merge(registration) void
        -requireValid(username, registration, existingToken)$
        -resolveSecret(incoming, existing) String$
        -getOrExisting(incoming, existing) T$
        -generateSecret() String$
    }

    class ValidationContext {
        <<record>>
        -String username
        -String token
        -String existingToken
    }

    class ValidateUsernameStep {
        +execute(ctx) Optional~String~
    }

    class ValidateTokenStep {
        +execute(ctx) Optional~String~
    }

    class BotRegistration {
        <<record>>
        +tokenOrNull() String
        +githubRepoOrNull() String
        +enabled() Boolean
        +telegramWebhookSecretOrNull() String
        +githubWebhookSecretOrNull() String
    }

    class BotSavedEvent {
        <<record>>
        +token() String
        +username() String
        +enabled() boolean
    }

    class BotDeletedEvent {
        <<record>>
        +token() String
        +username() String
    }

    ManagedBot --> ValidationContext : uses
    ManagedBot --> ValidateUsernameStep : runs
    ManagedBot --> ValidateTokenStep : runs
    ManagedBot ..> BotRegistration : uses for create/merge
    ManagedBot ..> BotSavedEvent : emitted after save
    ManagedBot ..> BotDeletedEvent : emitted after delete
```

## Design Notes

- **Rich Domain Model & Local Pipelines**: Business rules and validation are enforced through an internal `Pipeline` instance (`validationPipeline`). Any failure in the validation chain triggers a short-circuit error string, throwing an `IllegalArgumentException` to prevent the entity from entering an invalid state.
- **`BotRegistration`** uses nullable fields for partial-update semantics (`null` = keep existing).
- Secrets are auto-generated via `UUID` if not provided by the caller.
- Domain events are plain records; publishing is delegated to `DomainEventPublisher`.
