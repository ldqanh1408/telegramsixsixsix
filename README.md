# telegrambots — MongoDB-backed multi-bot notifier

Spring Boot service nhận webhook từ Telegram/GitHub cho nhiều bot, lấy token động từ MongoDB, rồi gửi thông báo GitHub vào các Telegram group đã kích hoạt bằng `/add @<bot-username>`.

## Tài liệu

- [docs/README.md](docs/README.md): mục lục tài liệu.
- [docs/DOCKER.md](docs/DOCKER.md): chạy app + MongoDB bằng Docker Compose.
- [docs/CONFIG.md](docs/CONFIG.md): cấu hình runtime, MongoDB, Telegram webhook, GitHub webhook.
- [docs/ADMIN_CONTROLLER.md](docs/ADMIN_CONTROLLER.md): giải thích Admin Bot API, flow `/add @bot`, SOLID/design pattern.

## Kiến trúc

- `managed_bots`: kho bot/token. Mỗi document chứa `username`, `token`, `tgWebhookSecret`, `githubRepo`, `ghWebhookSecret`, `enabled`.
- `group_activations`: trạng thái bot nào đang active trong group nào. Mỗi document chứa `botId`, `botUsername`, `chatId`, `active`, `activatedAt`, `updatedAt`.
- Telegram webhook dùng URL động: `/telegram/webhook/{botUsername}`.
- GitHub webhook dùng URL động: `/github/webhook/{botUsername}`.
- `BotManagementUseCase` là interface chính mà controller, command và service phụ thuộc vào.
- `DynamicBotManager` là facade implement `BotManagementUseCase`, điều phối `ManagedBotRegistry` và `GroupBotActivationService`.
- `ManagedBotRegistry` load các bot enabled từ MongoDB vào registry Map khi app khởi động, và cập nhật registry khi admin thêm/sửa/xóa bot.
- `GroupBotActivationService` quản lý activate/deactivate/count group.
- `BotUsername` là value object chuẩn hóa username Telegram (`@Bot` -> `bot`).

## Module code

| Package | Trách nhiệm |
|---|---|
| `bot` | Use-case port/facade, registry bot, value object username, registration command |
| `activation` | Kích hoạt/tắt bot theo group và kết quả activation |
| `admin` | Admin REST API, guard, mapper, DTO |
| `telegram` | Telegram webhook, Bot API client, command router và command handlers |
| `github` | GitHub webhook, signature verification, formatter event |
| `notification` | Render + broadcast GitHub event tới các group active |
| `mongo` | Entity và repository MongoDB |
| `config` | Application properties |

## Cấu hình runtime

Chỉ cấu hình hạ tầng chung qua env, không cấu hình token từng bot trong env nữa.

| Var | Bắt buộc | Mô tả |
|---|---|---|
| `MONGODB_URI` | khuyến nghị | MongoDB connection string, default `mongodb://localhost:27017/telegrambots` |
| `SPRING_MONGODB_URI` | không | Override trực tiếp property `spring.mongodb.uri`, dùng trong Docker Compose |
| `ADMIN_TOKEN` | có nếu dùng `/admin/**` | Token bảo vệ API quản lý kho bot |
| `PORT` | không | HTTP port, default `8080` |

## Nạp bot vào MongoDB

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

API này không trả token thô; response chỉ báo `hasToken` và hai webhook path cần dùng.

## Đăng ký webhook

Telegram webhook cho từng bot:

```bash
curl -X POST "https://api.telegram.org/bot<TOKEN>/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "<PUBLIC_URL>/telegram/webhook/my_repo_bot",
    "secret_token": "random-telegram-secret",
    "allowed_updates": ["message"]
  }'
```

GitHub webhook trong repo tương ứng:

- Payload URL: `<PUBLIC_URL>/github/webhook/my_repo_bot`
- Content type: `application/json`
- Secret: `githubWebhookSecret`
- Events: Pushes, Pull requests, Issues, Issue comments, Releases, Stars, Workflow runs

## Kích hoạt trong group

1. Add bot vào Telegram group.
2. Cấp quyền gửi tin nhắn cho bot.
3. Trong group gõ `/add @my_repo_bot`.
4. Khi GitHub gửi event đúng repo, backend dùng token trong `managed_bots` để gửi tin tới các group active của bot đó.

Tắt nhận thông báo bằng `/remove @my_repo_bot`.

## Lệnh Telegram

| Lệnh | Mô tả |
|---|---|
| `/start` | Chào và hiển thị repo bot đang theo dõi |
| `/help` | Liệt kê lệnh |
| `/id` | Trả về Telegram chat ID |
| `/status` | Trạng thái group hiện tại và số group active của bot |
| `/add @bot` | Kích hoạt bot trong group |
| `/remove @bot` | Tắt thông báo trong group |

## Build

```bash
./mvnw clean package
java -jar target/telegrambots-0.0.1-SNAPSHOT.jar
```

Nếu Maven wrapper lỗi trên Windows PowerShell, chạy trực tiếp Maven distribution trong `.m2/wrapper/dists/.../bin/mvn.cmd`.

## Docker

```bash
cp .env.docker.example .env
docker compose up --build
```

Xem chi tiết tại [docs/DOCKER.md](docs/DOCKER.md).
