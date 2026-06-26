# MVP Summary — telegrambots

> Tài liệu tóm tắt quá trình khởi tạo MVP dự án `telegrambots`.
> Ngày khởi tạo: 2026-06-26.

---

## 1. Bài toán cần giải

**Vấn đề**: Một team có nhiều repo GitHub, muốn nhận thông báo real-time (push, PR, issue, release...) vào các Telegram group khác nhau. Mỗi repo cần một bot riêng, nhưng việc deploy N instance riêng biệt cho N bot là lãng phí.

**Giải pháp MVP**: Một backend duy nhất phục vụ N bot Telegram cùng lúc. Token/config của từng bot lưu trong MongoDB, quản lý động qua REST API — không cần redeploy khi thêm/xóa bot.

---

## 2. Scope MVP

### Có trong MVP

| Feature | Trạng thái |
|---|---|
| Multi-bot dynamic loading từ MongoDB | ✅ Done |
| Admin REST API (CRUD bot) | ✅ Done |
| Telegram webhook nhận command từ group | ✅ Done |
| GitHub webhook nhận event từ repo | ✅ Done |
| 6 Telegram command: `/start`, `/help`, `/id`, `/add`, `/remove`, `/status` | ✅ Done |
| 7 GitHub event formatter: push, PR, issues, issue_comment, release, star, workflow_run | ✅ Done |
| Group activation/deactivation per bot | ✅ Done |
| HMAC-SHA256 verify GitHub webhook | ✅ Done |
| Telegram webhook secret verify per bot | ✅ Done |
| Admin token guard (constant-time compare) | ✅ Done |
| Response ẩn token (chỉ trả `hasToken` boolean) | ✅ Done |
| Docker Compose (app + MongoDB) | ✅ Done |
| Unit tests cho domain services | ✅ Done |

### Chưa có trong MVP (có thể làm sau)

| Feature | Lý do chưa làm |
|---|---|
| Webhook auto-registration (tự gọi Telegram/GitHub API đăng ký webhook) | MVP để admin tự đăng ký, đơn giản hơn |
| Rate limiting | Chưa cần ở quy mô nhỏ |
| Retry khi gửi Telegram thất bại | Telegram Bot API khá ổn định, log lỗi là đủ cho MVP |
| Web UI quản trị | REST API + curl đủ dùng giai đoạn đầu |
| Authentication phức tạp (OAuth, JWT) | Admin token đơn giản đủ cho internal use |
| Multi-repo per bot (1 bot theo dõi nhiều repo) | MVP: 1 bot = 1 repo, rõ ràng |
| Notification filter (chọn event type per group) | MVP gửi tất cả event, filter sau |
| Message queue (Kafka/RabbitMQ) | Broadcast trực tiếp đủ cho quy mô nhỏ |
| Health check / metrics endpoint | Có thể thêm Spring Actuator sau |
| Integration tests với Testcontainers MongoDB | Unit test mock đủ cho MVP |

---

## 3. Quyết định kiến trúc

### 3.1 Tại sao multi-bot trên 1 backend?

**Vấn đề**: deploy 1 instance per bot → N container, N config, N deployment pipeline.

**Quyết định**: 1 backend, N bot. Token lưu MongoDB, lookup bằng `{botUsername}` trên webhook URL.

**Trade-off**: phức tạp hơn single-bot, nhưng scale tốt hơn khi thêm bot (chỉ cần 1 API call, không cần redeploy).

### 3.2 Tại sao MongoDB?

- Schema linh hoạt — bot config có thể thêm field mà không cần migration
- Spring Data MongoDB tích hợp sẵn, ít boilerplate
- Đủ cho workload đọc nhiều ghi ít (webhook lookup >> admin CRUD)
- Free tier MongoDB Atlas dùng được cho production nhỏ

### 3.3 Tại sao webhook thay vì polling?

- Telegram khuyến nghị webhook cho production
- GitHub chỉ hỗ trợ webhook (không có polling API cho event)
- Webhook = real-time, không tốn resource polling liên tục
- Trade-off: cần HTTPS public URL (giải quyết bằng ngrok khi dev)

### 3.4 Tại sao không dùng Telegram Bot SDK?

- SDK (vd: TelegramBots Java) thiết kế cho single-bot, bind token lúc khởi tạo
- MVP cần multi-bot dynamic → `RestClient` gọi trực tiếp Telegram Bot API, truyền token per-call
- `TelegramClient` stateless, 1 bean phục vụ N bot

### 3.5 Tại sao Jackson 3?

- Spring Boot 4.1 mặc định dùng Jackson 3 (`tools.jackson.databind`)
- Không chủ động chọn, follow framework default
- **Lưu ý**: import `tools.jackson.databind.JsonNode`, không phải `com.fasterxml.jackson`

---

## 4. Cấu trúc code

```
49 Java files (39 main + 10 test)
8 packages
1 commit (MVP single-shot)
```

### Package map

```
com.lede.telegrambots
├── config          (1 file)   Cấu hình: AppProperties
├── bot             (5 files)  Domain core: use-case interface, facade, registry, value object
├── activation      (2 files)  Group activation logic
├── admin           (6 files)  Admin REST API: controller, service, guard, mapper, DTOs
├── telegram        (10 files) Telegram integration: webhook, client, router, 6 commands
├── github          (9 files)  GitHub integration: webhook, verifier, 7 formatters
├── notification    (1 file)   Broadcast service
└── mongo           (4 files)  Entity records + repositories
```

### Design patterns áp dụng

| Pattern | Class | Lý do chọn |
|---|---|---|
| **Facade** | `DynamicBotManager` | Controller chỉ cần 1 interface, không biết chi tiết bên dưới |
| **Strategy** | `EventFormatter` | Thêm event type = thêm 1 class, không sửa code cũ (OCP) |
| **Command** | `BotCommand` | Thêm Telegram command = thêm 1 class, router tự phát hiện |
| **Registry** | `ManagedBotRegistry` | Cache bot enabled để webhook lookup nhanh O(1) |
| **Value Object** | `BotUsername` | Chuẩn hóa username 1 nơi, tránh bug so sánh |
| **Guard** | `AdminAccessGuard` | Tách logic auth khỏi controller, constant-time compare |

---

## 5. Data model

### 2 MongoDB collections

```
managed_bots (1 document = 1 bot)
┌──────────────────────────────────────────────────┐
│ _id, username (unique), token, tgWebhookSecret,  │
│ githubRepo, ghWebhookSecret, enabled,            │
│ createdAt, updatedAt                             │
└──────────────────────────────────────────────────┘

group_activations (1 document = 1 bot-group binding)
┌──────────────────────────────────────────────────┐
│ _id, botId, botUsername, chatId,                  │
│ active, activatedAt, updatedAt                   │
│ unique index: (botUsername, chatId)               │
└──────────────────────────────────────────────────┘
```

### Quan hệ

```
managed_bots 1 ──── N group_activations
    (1 bot có thể active trong nhiều group)
```

---

## 6. API surface

| # | Method | Path | Mục đích |
|---|---|---|---|
| 1 | `POST` | `/admin/bots` | Tạo/cập nhật bot |
| 2 | `GET` | `/admin/bots` | List tất cả bot |
| 3 | `GET` | `/admin/bots/{username}` | Lấy thông tin 1 bot |
| 4 | `PUT` | `/admin/bots/{username}` | Update bot |
| 5 | `DELETE` | `/admin/bots/{username}` | Xóa bot + activations |
| 6 | `POST` | `/telegram/webhook/{botUsername}` | Nhận Telegram update |
| 7 | `POST` | `/github/webhook/{botUsername}` | Nhận GitHub event |

**Tổng: 7 endpoints, 3 lớp bảo mật** (admin token, Telegram secret, GitHub HMAC-SHA256).

---

## 7. Luồng end-to-end

```
                         ┌──────────────┐
                         │  Admin seed  │
                         │  bot via API │
                         └──────┬───────┘
                                │ POST /admin/bots
                                ▼
                         ┌──────────────┐
                         │   MongoDB    │
                         │ managed_bots │
                         └──────┬───────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
                ▼               ▼               ▼
        Đăng ký webhook   Đăng ký webhook   Add bot vào
        Telegram API      GitHub repo       Telegram group
                │               │               │
                ▼               ▼               ▼
        Telegram gọi      GitHub gọi      User gõ
        /telegram/webhook  /github/webhook  /add @bot
                │               │               │
                ▼               ▼               ▼
        CommandRouter      NotificationSvc  GroupActivation
        dispatch command   format + broadcast  upsert
                │               │               │
                ▼               ▼               ▼
        TelegramClient ◄───────┘         group_activations
        sendHtml()                        { active: true }
                │
                ▼
        Tin nhắn xuất hiện
        trong Telegram group
```

---

## 8. Security checklist MVP

| Mục | Cách làm | Đủ cho MVP? |
|---|---|---|
| Admin API auth | Static token + constant-time compare | ✅ Đủ cho internal |
| Token exposure | Response chỉ trả `hasToken` boolean | ✅ |
| GitHub webhook verify | HMAC-SHA256 signature | ✅ Industry standard |
| Telegram webhook verify | Secret token per-bot qua header | ✅ |
| Admin API disable | `ADMIN_TOKEN` rỗng → 404 toàn bộ | ✅ |
| Secrets trong env | Không commit `.env`, có `.env.docker.example` | ✅ |

---

## 9. Deliverables

| Artifact | Đường dẫn |
|---|---|
| Source code | `telegrambots-[module]/src/main/java/com/lede/telegrambots/` |
| Unit tests | `telegrambots-[module]/src/test/java/com/lede/telegrambots/` |
| Application config | `telegrambots-app/src/main/resources/application.yaml` |
| Docker setup | `Dockerfile`, `docker-compose.yml`, `.env.docker.example` |
| Env example | `config.properties.example` |
| Project docs | `docs/` (ARCHITECTURE, CONFIG, DOCKER, ADMIN_CONTROLLER, API/) |
| API sequence diagrams | `docs/api/` (7 files, mỗi file 1 endpoint) |
| Build | `./mvnw clean package` → `telegrambots-app/target/telegrambots-app-0.0.1-SNAPSHOT.jar` |

---

## 10. Chạy thử MVP trong 5 phút

```bash
# 1. Start infra
docker compose up --build -d

# 2. Seed bot
curl -X POST "http://localhost:8080/admin/bots" \
  -H "Content-Type: application/json" \
  -H "X-Admin-Token: change-me-local-admin-token" \
  -d '{
    "username": "my_repo_bot",
    "token": "<TOKEN_TỪ_BOTFATHER>",
    "telegramWebhookSecret": "tg-secret-123",
    "githubRepo": "owner/repo",
    "githubWebhookSecret": "gh-secret-456",
    "enabled": true
  }'

# 3. Expose local (cần ngrok)
ngrok http 8080

# 4. Register Telegram webhook
curl -X POST "https://api.telegram.org/bot<TOKEN>/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://<NGROK_URL>/telegram/webhook/my_repo_bot",
    "secret_token": "tg-secret-123",
    "allowed_updates": ["message"]
  }'

# 5. Register GitHub webhook
#    Repo Settings → Webhooks → Add webhook
#    Payload URL: https://<NGROK_URL>/github/webhook/my_repo_bot
#    Secret: gh-secret-456

# 6. Test trong Telegram group
#    - Add @my_repo_bot vào group
#    - Gõ /add @my_repo_bot
#    - Push commit lên repo → thông báo xuất hiện trong group
```

---

## 11. Kết luận

MVP hoàn thành **1 commit, 49 Java files, 7 API endpoints**, đủ để:

1. **Quản lý N bot** qua REST API mà không cần redeploy
2. **Nhận GitHub event** real-time và broadcast vào Telegram group
3. **Bảo mật** 3 lớp: admin token, Telegram secret, GitHub HMAC-SHA256
4. **Mở rộng** dễ dàng: thêm command = 1 class, thêm event formatter = 1 class, thêm bot = 1 API call

Hệ thống sẵn sàng chạy production nhỏ hoặc làm nền tảng cho các feature tiếp theo.
