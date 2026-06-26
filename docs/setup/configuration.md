# Cấu hình & chạy

File này lo **cấu hình runtime + nạp bot**. Phần đăng ký webhook có guide riêng:
[telegram.md](telegram.md) và [github.md](github.md).

> Mô hình dynamic loading: token của từng bot nằm trong MongoDB. App **không** đọc `BOT_TOKEN`, `BOT_USERNAME`, `GITHUB_REPO`, `GROUPS_FILE` từ env.

---

## Lộ trình cài đặt đầy đủ

```
1. Cấu hình env + chạy app        ← file này (mục 1)
2. Seed bot vào MongoDB           ← file này (mục 2)
3. Đăng ký Telegram webhook       → telegram.md
4. Đăng ký GitHub webhook         → github.md
5. /add @bot trong group + test   ← file này (mục 3-4)
```

---

## 1. Biến môi trường & chạy app

| Biến | Bắt buộc | Mặc định | Mục đích |
|---|---|---|---|
| `MONGODB_URI` | khuyến nghị | `mongodb://localhost:27017/telegrambots` | Kết nối MongoDB |
| `ADMIN_TOKEN` | có (production) | rỗng → tắt admin API | Bảo vệ `/admin/**` |
| `PORT` | không | `8080` | Cổng HTTP |
| `PUBLIC_URL` | khi đăng ký webhook | — | Domain/ngrok HTTPS công khai |

Sinh `ADMIN_TOKEN` mạnh:

```powershell
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 48 | ForEach-Object {[char]$_})
```

Chạy app (local, cần MongoDB ở `localhost:27017`):

```powershell
$env:MONGODB_URI = "mongodb://localhost:27017/telegrambots"
$env:ADMIN_TOKEN = "<random-admin-token>"
$env:PORT = "8080"
./mvnw spring-boot:run -pl telegrambots-app
```

Chạy bằng Docker: xem [docker.md](docker.md).

---

## 2. Seed bot vào MongoDB

Mỗi bot cần: token (từ BotFather), repo theo dõi, và 2 secret webhook. Tạo bot bằng 1 lệnh:

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

Các lệnh quản trị khác:

```bash
curl -H "X-Admin-Token: $ADMIN_TOKEN" "$PUBLIC_URL/admin/bots"               # liệt kê
curl -H "X-Admin-Token: $ADMIN_TOKEN" "$PUBLIC_URL/admin/bots/my_repo_bot"   # xem 1 bot
curl -X DELETE -H "X-Admin-Token: $ADMIN_TOKEN" "$PUBLIC_URL/admin/bots/my_repo_bot"  # xóa
```

> Cách lấy `token` và sinh 2 secret: xem [telegram.md](telegram.md) mục "Tạo bot".
> Đặc tả đầy đủ từng endpoint: xem [../api/](../api/README.md).

---

## 3. Kích hoạt trong Telegram group

Sau khi đã đăng ký webhook (telegram.md + github.md):

1. Add `@my_repo_bot` vào group, cấp quyền gửi tin nhắn.
2. Gõ `/add @my_repo_bot` → bot tạo document `group_activations` với `active=true`.
3. Tắt bằng `/remove @my_repo_bot`.

---

## 4. Test nhanh end-to-end

| Bước | Kết quả mong đợi |
|---|---|
| Gõ `/id` trong group | Bot trả Chat ID |
| Gõ `/status` | Bot báo group đã active hay chưa |
| Push commit lên repo đúng `githubRepo` | Bot gửi thông báo vào group đã `/add` |
| Gửi event repo khác | App trả `ignored: repo mismatch` |

**Không thấy tin nhắn?**

| Triệu chứng | Nguyên nhân |
|---|---|
| `404 unknown bot` | Chưa có bot `enabled` cho username trên URL |
| `401 bad signature` | Secret GitHub không khớp `githubWebhookSecret` |
| Log `no activated groups` | Chưa `/add @bot` trong group |

Troubleshooting chi tiết hơn: [telegram.md](telegram.md) và [github.md](github.md) (mục cuối mỗi file).
