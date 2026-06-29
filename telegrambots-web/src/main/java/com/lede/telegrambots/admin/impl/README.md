# Package: `com.lede.telegrambots.admin.impl`

The admin REST API implementation: controller, constant-time access guard, service, and DTO mapper.

## Class Diagram

```mermaid
classDiagram
    class AdminBotController {
        -AdminAccessGuard access
        -AdminBotService service
        +list(adminToken String) ResponseEntity
        +get(username String, adminToken String) ResponseEntity
        +createOrUpdate(request AdminBotRequest, adminToken String) ResponseEntity
        +update(username String, request AdminBotRequest, adminToken String) ResponseEntity
        +delete(username String, adminToken String) ResponseEntity
    }
    class AdminAccessGuard {
        +HEADER_NAME String$
        -String expectedToken
        +denyIfUnauthorized(adminToken String) Optional~AccessDenied~
    }
    class AccessDenied {
        <<record>>
        +status HttpStatus
        +message String
    }
    class AdminBotService {
        -BotManagementUseCase bots
        -AdminBotMapper mapper
        +list() List~AdminBotResponse~
        +find(username String) Optional~AdminBotResponse~
        +save(usernameOverride String, request AdminBotRequest) AdminBotResponse
        +delete(username String) void
    }
    class AdminBotMapper {
        -BotManagementUseCase bots
        +toResponse(bot ManagedBot) AdminBotResponse
        +toRegistration(usernameOverride String, request AdminBotRequest) BotRegistration
    }

    AdminAccessGuard +.. AccessDenied
    AdminBotController --> AdminAccessGuard : guards every endpoint
    AdminBotController --> AdminBotService : delegates
    AdminBotService --> BotManagementUseCase : inbound port
    AdminBotService --> AdminBotMapper : maps DTOs
    AdminBotMapper --> BotManagementUseCase : activeGroupCount
```

## Endpoint Map

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/bots` | List all bots |
| `GET` | `/admin/bots/{username}` | Get one bot (404 `ApiError` if absent) |
| `POST` | `/admin/bots` | Create/update (username from body) |
| `PUT` | `/admin/bots/{username}` | Update (path username overrides body) |
| `DELETE` | `/admin/bots/{username}` | Delete bot + activations + webhook (204) |

## Design Notes

- **Guard pattern**: every handler runs through `authorized(...)`, which calls `AdminAccessGuard.denyIfUnauthorized`. A blank `app.admin.token` returns **404** ("admin api disabled"); a wrong token returns **401**; otherwise the action proceeds. The header is `X-Admin-Token`.
- **Constant-time comparison**: the guard uses `MessageDigest.isEqual` over UTF-8 bytes to avoid timing attacks; `AccessDenied` is a nested record carrying the `HttpStatus` + message.
- **Thin controller**: returns `ResponseEntity<?>`, mapping `IllegalArgumentException` from `save` to `400 ApiError`; all bot logic lives behind the `BotManagementUseCase` inbound port.
- **Mapper is a bean**: `AdminBotMapper` is injected (not static) because `toResponse` calls `BotManagementUseCase.activeGroupCount` to populate `activeGroups`.
- **Visibility**: `AdminBotService` (`@Service`) and `AdminBotMapper` (`@Component`) are package-private; `AdminBotController` and `AdminAccessGuard` are public.
