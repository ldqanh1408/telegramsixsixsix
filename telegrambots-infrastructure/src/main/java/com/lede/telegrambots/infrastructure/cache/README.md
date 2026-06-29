# Package: `com.lede.telegrambots.infrastructure.cache`

The in-memory registry of enabled bots — the infrastructure adapter for the `BotCache` port and the fast path for webhook request routing.

## Class Diagram

```mermaid
classDiagram
    class BotCache {
        <<interface>>
        +findEnabled(username String) Optional~ManagedBot~
        +put(bot ManagedBot) void
        +evict(username String) void
    }
    class ManagedBotRegistry {
        -BotRepository bots
        -ConcurrentMap~String, ManagedBot~ runningBots
        +findEnabled(username String) Optional~ManagedBot~
        +put(bot ManagedBot) void
        +evict(username String) void
        ~loadEnabledBots() void
    }
    ManagedBotRegistry ..|> BotCache
    ManagedBotRegistry --> BotRepository : DB fallback on miss
```

## Design Notes

- **Registry / cache-aside**: `loadEnabledBots()` (`@PostConstruct`) warms the map at startup with all enabled bots; on a `findEnabled` miss it falls back to `BotRepository` and caches the result, returning only enabled bots.
- **Thread-safety**: backed by a `ConcurrentHashMap` keyed by normalized `BotUsername`.
- **Kept in sync** by the bot write use cases: `put` on upsert (`RefreshCacheStep`), `evict` on delete (`EvictCacheStep`).
- **Visibility**: package-private `@Component` — only the Spring container and the port abstraction see it.
