# Package: `com.lede.telegrambots.github`

The GitHub webhook processing root types: the typed pipeline step alias, its mutable context, the web-agnostic result, and the event renderer.

## Class Diagram

```mermaid
classDiagram
    class GitHubWebhookStep {
        <<interface>>
        +execute(ctx GitHubWebhookContext) Optional~GitHubWebhookResult~
    }
    class GitHubWebhookContext {
        -String botUsername
        -String event
        -String signature
        -String delivery
        -byte[] body
        -ManagedBot bot
        -JsonNode payload
        +setBot(bot ManagedBot) void
        +setPayload(payload JsonNode) void
    }
    class GitHubWebhookResult {
        <<record>>
        +outcome Outcome
        +body String
        +of(outcome Outcome, body String) GitHubWebhookResult$
    }
    class Outcome {
        <<enum>>
        UNKNOWN_BOT
        BAD_SIGNATURE
        MISSING_EVENT
        PONG
        INVALID_JSON
        REPO_MISMATCH
        OK
    }
    class GitHubEventRenderer {
        -Map~String, EventFormatter~ formatters
        +render(event String, payload JsonNode) Optional~String~
    }

    GitHubWebhookStep ..> GitHubWebhookContext : receives
    GitHubWebhookStep ..> GitHubWebhookResult : produces
    GitHubWebhookResult --> Outcome
    GitHubEventRenderer --> EventFormatter : dispatches by eventName
```

## Design Notes

- **Typed pipeline alias**: `GitHubWebhookStep extends Step<GitHubWebhookContext, GitHubWebhookResult>` so the webhook steps form a distinct bean group Spring injects as `List<GitHubWebhookStep>`.
- **Mutable context**: `GitHubWebhookContext` carries immutable request inputs plus `bot` and `payload`, which steps populate progressively.
- **Web-agnostic result**: `GitHubWebhookResult(Outcome, body)` keeps Spring MVC types out of the processing rules; only the controller maps `Outcome` → HTTP status.
- **Renderer (Strategy dispatcher)**: `GitHubEventRenderer` builds a `Map<eventName, EventFormatter>` from the injected formatter beans and returns the formatted HTML (or empty when no formatter matches / the formatter drops the payload).
