# Package: `com.lede.telegrambots.application.activation`

Group activation use cases — activate/deactivate a bot in a Telegram chat — plus the result object that command handlers render.

## Responsibility

- `GroupBotActivationService`: owns the activate/deactivate `Pipeline`s and the read queries (`isActive`, `activeGroupCount`, `activeGroups`); enforces the `*Requested` validation rules.
- `GroupActivationCommandResult`: application result object describing a command outcome (`Status` + optional `ActivationResult`).
- `ActivateContext` / `DeactivateContext`: mutable pipeline contexts.
- `ActivateGroupStep` / `DeactivateGroupStep`: named `Step` sub-interfaces for type-safe Spring list injection.

## Class Diagram

```mermaid
classDiagram
    class GroupBotActivationService {
        -ActivationRepository store
        -Pipeline~ActivateContext, ActivationResult~ activatePipeline
        -Pipeline~DeactivateContext, Boolean~ deactivatePipeline
        +activate(bot ManagedBot, chatId long) ActivationResult
        +activateRequested(bot ManagedBot, chatId long, requestedUsername String) GroupActivationCommandResult
        +deactivate(bot ManagedBot, chatId long) boolean
        +deactivateRequested(bot ManagedBot, chatId long, requestedUsername String) GroupActivationCommandResult
        +isActive(bot ManagedBot, chatId long) boolean
        +activeGroupCount(bot ManagedBot) long
        +activeGroups(bot ManagedBot) List~GroupActivation~
        +deleteByBotUsername(username String) void
    }
    class GroupActivationCommandResult {
        <<record>>
        +status Status
        +activation ActivationResult
        +missingUsername() GroupActivationCommandResult$
        +botMismatch() GroupActivationCommandResult$
        +activated(result ActivationResult) GroupActivationCommandResult$
        +alreadyActive(result ActivationResult) GroupActivationCommandResult$
        +deactivated() GroupActivationCommandResult$
        +notActive() GroupActivationCommandResult$
    }
    class Status {
        <<enum>>
        MISSING_USERNAME
        BOT_MISMATCH
        ACTIVATED
        ALREADY_ACTIVE
        DEACTIVATED
        NOT_ACTIVE
    }
    class ActivateContext {
        -ManagedBot bot
        -long chatId
        -GroupActivation existing
        -ActivationResult result
    }
    class DeactivateContext {
        -ManagedBot bot
        -long chatId
        -GroupActivation existing
        -boolean deactivated
    }
    class ActivateGroupStep {
        <<interface>>
    }
    class DeactivateGroupStep {
        <<interface>>
    }

    GroupActivationCommandResult --> Status
    GroupBotActivationService --> ActivateContext : activate pipeline input
    GroupBotActivationService --> DeactivateContext : deactivate pipeline input
    GroupBotActivationService ..> GroupActivationCommandResult : returns
    GroupBotActivationService --> ActivateGroupStep : pipeline steps
    GroupBotActivationService --> DeactivateGroupStep : pipeline steps
```

## Pipelines

- **Activate**: `LoadExistingActivationForActivateStep` → `CheckAlreadyActiveStep` → `SaveActivationStep`.
- **Deactivate**: `LoadExistingActivationForDeactivateStep` → `CheckNotActiveStep` → `SaveDeactivatedStep`.

(Concrete steps live in `application.activation.steps`.)

## Design Notes

- **Application Result Object**: `GroupActivationCommandResult` is an immutable record with named factory methods (`activated`, `alreadyActive`, `botMismatch`, …) and a `Status` enum, so command handlers render outcomes without re-deriving rules.
- **Validation in `*Requested`**: `activateRequested`/`deactivateRequested` reject a blank requested username (`MISSING_USERNAME`) or one targeting a different bot (`BOT_MISMATCH`) before touching the pipeline; matching uses `BotUsername` normalization.
- **Dual constructors**: manual (explicit step list) and Spring-autowired (`List<ActivateGroupStep>` / `List<DeactivateGroupStep>`), same as the bot use cases.
- `activate` throws if the pipeline yields nothing; `deactivate` defaults to `false`.
