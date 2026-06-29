# Package: `com.lede.telegrambots` (App Root)

**Application entry point and composition root.**

## Class Diagram

```mermaid
classDiagram
    class TelegrambotsApplication {
        +main(args String[])$
    }

    class UseCaseConfiguration {
        <<@Configuration>>
        +normalizeAndLoadStep(bots) BotUpsertStep
        +mergeStep() BotUpsertStep
        +persistStep(bots) BotUpsertStep
        +refreshCacheStep(cache) BotUpsertStep
        +publishEventStep(events) BotUpsertStep
        +upsertBotUseCase(steps) UpsertBotUseCase
        +loadStep(bots) BotDeleteStep
        +deleteActivationsStep(activations) BotDeleteStep
        +deleteBotStep(bots) BotDeleteStep
        +evictCacheStep(cache) BotDeleteStep
        +publishDeleteEventStep(events) BotDeleteStep
        +deleteBotUseCase(steps) DeleteBotUseCase
        +loadTargetsStep(activations) BroadcastStep
        +validateTargetsStep() BroadcastStep
        +deliverMessagesStep(telegram) BroadcastStep
        +broadcastUseCase(steps) BroadcastUseCase
        +groupBotActivationService(store, actSteps, deactSteps) GroupBotActivationService
        +dynamicBotManager(cache, queries, upsert, delete, activations) BotManagementUseCase
    }

    TelegrambotsApplication --> UseCaseConfiguration : loaded by Spring
```

## Design Notes

- `UseCaseConfiguration` is the **Composition Root** — the only place Spring bean wiring
  occurs for the application layer.
- Steps are declared as `@Bean` methods returning the **marker interface** type so Spring
  builds an ordered `List<MarkerInterface>` for injection into the use case.
- `@Order` on each step bean determines pipeline execution order.
- This pattern keeps all use case classes **framework-free** (plain Java constructors).
