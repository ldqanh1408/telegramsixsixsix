# Package: `com.lede.telegrambots.infrastructure.persistence.mongo`

The MongoDB adapters that implement the application's `BotRepository` and `ActivationRepository` outbound ports.

## Class Diagram

```mermaid
classDiagram
    class BotRepository {
        <<interface>>
    }
    class ActivationRepository {
        <<interface>>
    }
    class MongoBotRepository {
        -ManagedBotDocumentRepository repo
        +findAll() List~ManagedBot~
        +findByUsername(username String) Optional~ManagedBot~
        +save(bot ManagedBot) ManagedBot
        +deleteByUsername(username String) void
    }
    class MongoActivationRepository {
        -GroupActivationDocumentRepository repo
        +find(botUsername String, chatId long) Optional~GroupActivation~
        +save(activation GroupActivation) GroupActivation
        +isActive(botUsername String, chatId long) boolean
        +countActive(botUsername String) long
        +findActive(botUsername String) List~GroupActivation~
        +deleteAllFor(botUsername String) void
    }
    MongoBotRepository ..|> BotRepository
    MongoActivationRepository ..|> ActivationRepository
    MongoBotRepository --> ManagedBotDocumentRepository : delegates
    MongoActivationRepository --> GroupActivationDocumentRepository : delegates
    MongoBotRepository ..> ManagedBotMapper : toDomain / toDocument
    MongoActivationRepository ..> GroupActivationMapper : toDomain / toDocument
```

## Design Notes

- **Adapter (Ports & Adapters)**: each `@Component` is the only place that touches its Spring Data `*DocumentRepository`; it delegates queries and maps documents ↔ domain entities via the **static** `*Mapper` methods (mappers are not injected).
- **No business rules**: pure delegation + mapping; the domain entity is never persisted directly (see the `impl` sub-package for the `@Document` twins).
- **Visibility**: package-private `@Component`s — callers depend only on the ports.
