# DECOUPLING — Giảm coupling bằng design pattern

Tài liệu này tóm tắt các refactor đã áp dụng để hạ coupling trong `telegrambots`, kèm before/after.

---

## Nguyên tắc

> **Code phụ thuộc vào abstraction (interface), không phụ thuộc vào implementation cụ thể.**
> Module trong (domain/application) không được biết tới chi tiết hạ tầng (HTTP client, crypto, Spring MVC).

Áp dụng: **Dependency Inversion Principle (DIP)** + **Ports & Adapters (Hexagonal)** + **Command pattern (pure)** + **Application Service / Result Object**.

---

## 4 refactor chính

### 1. Command không còn phụ thuộc transport

**Vấn đề (before)**: cả 7 command inject `TelegramClient` (lớp HTTP cụ thể) và tự gọi `sendHtml(token, chatId, ...)`. Mỗi command phải biết về token, transport — khó test, đổi transport phải sửa 7 file.

```
Before:
  StartCommand   ─┐
  HelpCommand    ─┤
  IdCommand      ─┼──▶ TelegramClient (concrete HTTP)
  AddCommand     ─┤
  RemoveCommand  ─┤
  StatusCommand  ─┘
```

**Giải pháp (after)**: `BotCommand.execute()` trả `Optional<String>` (nội dung reply). `CommandRouter` (invoker) là nơi duy nhất gửi tin. Command trở thành hàm thuần: `context → message`.

```
After:
  StartCommand/HelpCommand/IdCommand  ──▶ (không phụ thuộc gì)
  AddCommand/RemoveCommand/StatusCommand ──▶ BotManagementUseCase (domain port)

  CommandRouter ──▶ TelegramGateway (port)
```

**Kết quả**:
- `StartCommand`, `HelpCommand`, `IdCommand`: từ phụ thuộc `TelegramClient` → **phụ thuộc 0 dependency**
- Command còn lại chỉ phụ thuộc domain port `BotManagementUseCase` (hợp lý)
- Test command không cần mock transport (xem `AddCommandTest`)

### 2. Outbound port `TelegramGateway`

**Vấn đề**: `NotificationService` và `CommandRouter` bind cứng vào `TelegramClient`.

**Giải pháp**: thêm interface `TelegramGateway` (port). `TelegramClient` `implements TelegramGateway` (adapter). Callers phụ thuộc port.

```
NotificationService ─┐
                     ├──▶ TelegramGateway (interface) ◀── TelegramClient (adapter)
CommandRouter       ─┘
```

Đổi transport (vd: gửi qua queue, hay fake trong test) chỉ cần adapter mới, không đụng caller.

### 3. Port `WebhookSignatureVerifier` thay cho static util

**Vấn đề (before)**: `SignatureVerifier.verify(...)` là **static method** → `GitHubWebhookController` bị bind cứng (compile-time) vào một impl crypto, không thể thay/test-double.

**Giải pháp (after)**:
- Interface `WebhookSignatureVerifier` (port)
- `HmacSha256SignatureVerifier` `@Component implements WebhookSignatureVerifier`
- Consumer phụ thuộc interface

```
Before: Controller ──▶ SignatureVerifier.verify()   (static, cứng)
After:  Processor  ──▶ WebhookSignatureVerifier (port) ◀── HmacSha256SignatureVerifier
```

### 4. Tách business logic khỏi controller

**Vấn đề (before)**: `GitHubWebhookController` ôm tất cả: lookup bot, verify chữ ký, check event, parse JSON, match repo, dispatch — đồng thời phụ thuộc `BotManagementUseCase`, `NotificationService`, `ObjectMapper`, static verifier, và Spring MVC. Logic nghiệp vụ trộn với tầng web → không test được nếu không dựng MVC.

**Giải pháp (after)**:
- `GitHubWebhookProcessor` (Application Service) sở hữu toàn bộ pipeline, trả về `GitHubWebhookResult` (result object **không chứa kiểu Spring web**)
- `GitHubWebhookController` chỉ còn: nhận HTTP request → gọi processor → map `Outcome` sang `HttpStatus`

```
Before:
  GitHubWebhookController  ──▶ BotManagementUseCase
       (HTTP + toàn bộ        ──▶ NotificationService
        business logic)       ──▶ ObjectMapper
                              ──▶ SignatureVerifier (static)

After:
  GitHubWebhookController ──▶ GitHubWebhookProcessor ──▶ BotManagementUseCase
   (thin HTTP adapter)            (pipeline thuần)    ──▶ WebhookSignatureVerifier (port)
        │                                             ──▶ NotificationService
        └─ map GitHubWebhookResult.Outcome → HttpStatus──▶ ObjectMapper
```

**Kết quả**: pipeline test được hoàn toàn không cần Spring MVC (xem `GitHubWebhookProcessorTest`).

---

## Bảng tổng hợp coupling

| Thành phần | Before phụ thuộc | After phụ thuộc |
|---|---|---|
| `StartCommand` / `HelpCommand` / `IdCommand` | `TelegramClient` | — (không) |
| `AddCommand` / `RemoveCommand` / `StatusCommand` | `TelegramClient`, `BotManagementUseCase` | `BotManagementUseCase` |
| `CommandRouter` | `TelegramClient` (gián tiếp qua command) | `TelegramGateway` (port) |
| `NotificationService` | `TelegramClient` | `TelegramGateway` (port) |
| `GitHubWebhookController` | 4 collaborator + static verifier + MVC | `GitHubWebhookProcessor` (+ MVC) |
| `GitHubWebhookProcessor` (mới) | — | abstraction (`BotManagementUseCase`, `WebhookSignatureVerifier`) |

---

## Pattern đã dùng

| Pattern | Áp dụng |
|---|---|
| **Dependency Inversion Principle** | `TelegramGateway`, `WebhookSignatureVerifier` ports |
| **Ports & Adapters (Hexagonal)** | `TelegramClient`/`HmacSha256SignatureVerifier` là adapter của port |
| **Command (pure / return-value)** | `BotCommand.execute()` trả reply thay vì gửi |
| **Application Service** | `GitHubWebhookProcessor` |
| **Result Object** | `GitHubWebhookResult` (web-agnostic) |
| **Single Responsibility** | controller = adapter, processor = rule engine, command = pure logic |

---

## Lợi ích đo được

- **Testability**: thêm 13 unit test mới chạy **không cần Spring/HTTP** (`GitHubWebhookProcessorTest`, `HmacSha256SignatureVerifierTest`, `AddCommandTest`). Tổng test 9 → 22, tất cả pass.
- **Open/Closed**: thêm command mới không tăng bề mặt coupling (không kéo theo transport).
- **Thay thế hạ tầng**: đổi cách gửi Telegram hoặc thuật toán verify chỉ cần thêm 1 adapter.
- **Controller mỏng**: `GitHubWebhookController` từ ~95 dòng logic → ~45 dòng adapter thuần.
