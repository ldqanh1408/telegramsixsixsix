# AdminBotController

File code chính: `telegrambots-web/src/main/java/com/lede/telegrambots/admin/impl/AdminBotController.java`

`AdminBotController` là controller quản trị kho bot trong MongoDB. Controller này không xử lý webhook Telegram/GitHub trực tiếp; nhiệm vụ của nó là thêm, cập nhật, xem và xóa các bot mà backend đang quản lý.

Khi một bot được thêm qua controller này, thông tin token và webhook secret sẽ được lưu vào collection `managed_bots`. Backend đi qua contract `BotManagementUseCase` để tự động lấy đúng token khi có request đến `/telegram/webhook/{botUsername}` hoặc `/github/webhook/{botUsername}`.

## Base URL

```text
/admin/bots
```

Tất cả endpoint trong controller đều yêu cầu header:

```text
X-Admin-Token: <ADMIN_TOKEN>
```

Giá trị `ADMIN_TOKEN` được đọc từ cấu hình:

```yaml
app:
  admin:
    token: ${ADMIN_TOKEN:}
```

Nếu `ADMIN_TOKEN` rỗng, admin API bị tắt và controller trả `404` với body:

```json
{
  "error": "admin api disabled"
}
```

Nếu thiếu hoặc sai `X-Admin-Token`, controller trả `401`:

```json
{
  "error": "unauthorized"
}
```

## Design pattern đang dùng

| Pattern / lớp | Vai trò |
|---|---|
| Controller | `AdminBotController` chỉ nhận HTTP request, trả HTTP response |
| Service | `AdminBotService` xử lý use-case quản trị bot |
| Mapper | `AdminBotMapper` chuyển `ManagedBot` entity thành response DTO |
| Guard | `AdminAccessGuard` kiểm tra `X-Admin-Token` |
| DTO | `AdminBotRequest`, `AdminBotResponse`, `ApiError` tách khỏi controller |
| Port / Interface | `BotManagementUseCase` là contract mà controller/command/service phụ thuộc vào |
| Facade | `DynamicBotManager` implement `BotManagementUseCase` và điều phối registry + activation |
| Registry | `ManagedBotRegistry` quản lý CRUD bot và cache các bot enabled |
| Use-case Service | `GroupBotActivationService` chỉ xử lý activate/deactivate/count group |
| Value Object | `BotUsername` chuẩn hóa username Telegram ở một nơi |

Mục tiêu của cách tách này là giữ controller mỏng: controller không biết chi tiết MongoDB, không tự map entity, không tự so sánh secret, và không giữ DTO lồng bên trong.

## Các dependency chính

| Dependency | Vai trò |
|---|---|
| `AdminAccessGuard access` | Kiểm tra `X-Admin-Token` |
| `AdminBotService service` | Chạy use-case list/find/save/delete bot |

Controller không gọi trực tiếp repository MongoDB. Mọi thao tác nghiệp vụ được đẩy qua `AdminBotService`, rồi service gọi contract `BotManagementUseCase`.

## Endpoint

### 1. Lấy danh sách bot

```http
GET /admin/bots
```

Ví dụ:

```bash
curl -H "X-Admin-Token: $ADMIN_TOKEN" \
  "$PUBLIC_URL/admin/bots"
```

Response là danh sách `AdminBotResponse`. Token thật không được trả ra ngoài; API chỉ trả `hasToken=true/false`.

```json
[
  {
    "id": "685be...",
    "username": "my_repo_bot",
    "enabled": true,
    "githubRepo": "owner/repo",
    "hasToken": true,
    "hasTelegramWebhookSecret": true,
    "hasGithubWebhookSecret": true,
    "telegramWebhookPath": "/telegram/webhook/my_repo_bot",
    "githubWebhookPath": "/github/webhook/my_repo_bot",
    "activeGroups": 3,
    "createdAt": "2026-06-25T04:30:00Z",
    "updatedAt": "2026-06-25T04:45:00Z"
  }
]
```

### 2. Lấy thông tin một bot

```http
GET /admin/bots/{username}
```

Ví dụ:

```bash
curl -H "X-Admin-Token: $ADMIN_TOKEN" \
  "$PUBLIC_URL/admin/bots/my_repo_bot"
```

Nếu không tìm thấy bot:

```json
{
  "error": "bot not found"
}
```

### 3. Tạo hoặc cập nhật bot

```http
POST /admin/bots
```

Ví dụ:

```bash
curl -X POST "$PUBLIC_URL/admin/bots" \
  -H "Content-Type: application/json" \
  -H "X-Admin-Token: $ADMIN_TOKEN" \
  -d '{
    "username": "my_repo_bot",
    "token": "123456789:ABC...",
    "telegramWebhookSecret": "random-telegram-secret",
    "githubRepo": "owner/repo",
    "githubWebhookSecret": "random-github-secret",
    "enabled": true
  }'
```

Luồng xử lý:

1. Controller kiểm tra `X-Admin-Token`.
2. Controller chuyển request cho `AdminBotService`.
3. `AdminBotMapper.toRegistration(...)` tạo `BotRegistration`.
4. `BotManagementUseCase.upsertBot(...)` đi qua `DynamicBotManager`, sau đó `ManagedBotRegistry` chuẩn hóa username, lưu bot vào MongoDB và cập nhật registry `runningBots`.
5. `AdminBotMapper` trả về `AdminBotResponse` đã ẩn token thật.

Nếu tạo bot mới mà không có `token`, API trả `400`:

```json
{
  "error": "token is required for a new bot"
}
```

Nếu thiếu `username`, API trả `400`:

```json
{
  "error": "username is required"
}
```

### 4. Cập nhật bot theo username trên path

```http
PUT /admin/bots/{username}
```

Ví dụ:

```bash
curl -X PUT "$PUBLIC_URL/admin/bots/my_repo_bot" \
  -H "Content-Type: application/json" \
  -H "X-Admin-Token: $ADMIN_TOKEN" \
  -d '{
    "githubRepo": "owner/new-repo",
    "enabled": true
  }'
```

Khác với `POST`, endpoint này lấy username từ path, không lấy từ body. Vì vậy body có thể chỉ chứa các field cần đổi.

Các field gửi lên là tùy chọn khi bot đã tồn tại:

| Field | Ý nghĩa |
|---|---|
| `token` | Token Telegram bot do BotFather cấp |
| `telegramWebhookSecret` | Secret dùng cho Telegram header `X-Telegram-Bot-Api-Secret-Token` |
| `githubRepo` | Repo GitHub mà bot theo dõi, dạng `owner/repo` |
| `githubWebhookSecret` | Secret dùng verify chữ ký GitHub |
| `enabled` | `true` để bật bot, `false` để gỡ khỏi registry chạy |

### 5. Xóa bot

```http
DELETE /admin/bots/{username}
```

Ví dụ:

```bash
curl -X DELETE \
  -H "X-Admin-Token: $ADMIN_TOKEN" \
  "$PUBLIC_URL/admin/bots/my_repo_bot"
```

Luồng xử lý:

1. Controller kiểm tra `X-Admin-Token`.
2. Gọi `bots.deleteBot(username)`.
3. `DynamicBotManager` điều phối `GroupBotActivationService` xóa activation liên quan trong `group_activations`, rồi `ManagedBotRegistry` xóa bot khỏi `managed_bots` và remove bot khỏi `runningBots`.
4. Controller trả `204 No Content`.

## Request model: `AdminBotRequest`

```java
public record AdminBotRequest(
        String username,
        String token,
        String telegramWebhookSecret,
        String githubRepo,
        String githubWebhookSecret,
        Boolean enabled
) {}
```

`AdminBotRequest` là DTO nhận dữ liệu từ admin API. Với `POST`, `username` và `token` cần có khi tạo bot mới. Với `PUT /admin/bots/{username}`, username lấy từ path nên body không cần chứa `username`.

## Response model: `AdminBotResponse`

```java
public record AdminBotResponse(
        String id,
        String username,
        boolean enabled,
        String githubRepo,
        boolean hasToken,
        boolean hasTelegramWebhookSecret,
        boolean hasGithubWebhookSecret,
        String telegramWebhookPath,
        String githubWebhookPath,
        long activeGroups,
        Instant createdAt,
        Instant updatedAt
) {}
```

Response này cố tình không trả token và secret thật. Thay vào đó dùng các cờ:

- `hasToken`
- `hasTelegramWebhookSecret`
- `hasGithubWebhookSecret`

Cách này giúp admin biết bot đã cấu hình đủ hay chưa mà không làm lộ credential qua response/log.

## Bảo mật

`AdminAccessGuard` dùng `MessageDigest.isEqual(...)` trong `constantTimeEquals(...)` để so sánh admin token theo kiểu constant-time. Mục đích là giảm rủi ro timing attack khi so sánh secret.

```java
private static boolean constantTimeEquals(String expected, String received) {
    if (received == null) return false;
    return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            received.getBytes(StandardCharsets.UTF_8)
    );
}
```

Trong production phải đặt `ADMIN_TOKEN` đủ mạnh và không commit vào Git.

## Controller này liên quan gì đến dynamic loading?

`AdminBotController` là cửa vào để nạp kho token. Sau khi admin tạo bot:

1. Document bot được lưu vào `managed_bots`.
2. Nếu `enabled=true`, `ManagedBotRegistry` đưa bot vào Map `runningBots`.
3. Telegram gọi `/telegram/webhook/{botUsername}`.
4. `TelegramWebhookController` dùng `{botUsername}` để tìm `ManagedBot`.
5. Command `/add @botUsername` kích hoạt group trong `group_activations`.
6. Khi GitHub có event, backend dùng token của chính bot đó để gửi tin vào group active.

Nói ngắn gọn: controller admin không gửi tin nhắn, nhưng nó là nơi chuẩn bị dữ liệu token để toàn bộ hệ thống multi-bot chạy động.

## Làm rõ flow `/add <name_bot>` trong group

Ý đúng của flow là:

```text
User trong group gõ: /add @my_repo_bot
        ↓
Telegram gửi update đến backend
        ↓
Backend nhận request tại /telegram/webhook/my_repo_bot
        ↓
Backend lấy my_repo_bot làm key để tìm token trong MongoDB
        ↓
Backend active group này cho bot my_repo_bot
        ↓
Backend dùng token vừa lấy từ MongoDB để gửi tin xác nhận
```

Điểm quan trọng: Telegram không gửi bot token trong update. Update Telegram chỉ có thông tin message, chat, user, text... chứ không có token của bot.

Vì vậy backend chỉ có thể "tự động bắt token" theo nghĩa:

1. Token đã được lưu sẵn trong collection `managed_bots`.
2. User gõ `/add @my_repo_bot`.
3. Backend lấy `my_repo_bot` từ URL webhook hoặc command argument.
4. Backend query MongoDB để lấy token tương ứng.
5. Backend lưu activation vào `group_activations`.

Backend không thể tự suy ra token chỉ từ username `@my_repo_bot`. Telegram/BotFather không cung cấp API nào cho phép lấy token của bot bằng username, vì token là secret chỉ chủ bot biết.

Ví dụ dữ liệu phải có sẵn trong MongoDB trước khi group gõ `/add`:

```json
{
  "username": "my_repo_bot",
  "token": "123456789:ABC...",
  "tgWebhookSecret": "random-telegram-secret",
  "githubRepo": "owner/repo",
  "ghWebhookSecret": "random-github-secret",
  "enabled": true
}
```

Sau đó khi group gõ:

```text
/add @my_repo_bot
```

`AddCommand` sẽ gọi:

```java
ActivationResult result = bots.activate(ctx.bot(), ctx.chatId());
```

Trong đó `bots` là `BotManagementUseCase`, còn `ctx.bot()` chính là `ManagedBot` đã được `TelegramWebhookController` lấy từ MongoDB dựa vào `{botUsername}` trên webhook URL. Nói cách khác, command `/add` không nhận token từ user; nó kích hoạt bot đã tồn tại trong kho token.

Nếu muốn user gửi token trực tiếp trong group, ví dụ `/add @my_repo_bot 123:ABC`, thì về kỹ thuật backend có thể parse và lưu token, nhưng cách này không nên dùng vì token sẽ lộ trong group chat. Cách an toàn là admin seed token qua `/admin/bots`, còn group chỉ gõ `/add @bot` để active.
