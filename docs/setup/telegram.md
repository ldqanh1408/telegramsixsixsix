# TELEGRAM_SETUP — Cấu hình Telegram Bot

Hướng dẫn từng bước tạo bot trên Telegram và kết nối với backend `telegrambots`.

---

## Bước 1: Tạo bot trên BotFather

Mở Telegram, tìm [@BotFather](https://t.me/BotFather) và gửi các lệnh sau:

### 1.1 Tạo bot mới

```
/newbot
```

BotFather sẽ hỏi:
1. **Tên hiển thị** (vd: `My Repo Notifier`)
2. **Username** (phải kết thúc bằng `bot`, vd: `my_repo_bot`)

Sau khi tạo xong, BotFather trả về:

```
Done! Congratulations on your new bot.

Use this token to access the HTTP API:
123456789:ABCdefGHIjklMNOpqrSTUvwxYZ

Keep your token secure and store it safely.
```

> **Lưu lại token này** — đây là `token` sẽ dùng khi seed bot vào MongoDB.

### 1.2 Tắt privacy mode (bắt buộc cho group)

```
/setprivacy
```

1. Chọn bot vừa tạo
2. Chọn **Disable**

> **Tại sao?** Mặc định bot chỉ nhận message bắt đầu bằng `/` gửi trực tiếp cho nó. Khi disable privacy, bot nhận được tất cả message trong group — cần thiết để xử lý command `/add @bot`.

### 1.3 (Tùy chọn) Đặt description

```
/setdescription
```

Gợi ý:
```
Thông báo GitHub repo events (push, PR, issues...) vào Telegram group.
Dùng /help để xem danh sách lệnh.
```

### 1.4 (Tùy chọn) Đặt danh sách command

```
/setcommands
```

Gửi danh sách:
```
start - Chào và hiển thị repo đang theo dõi
help - Liệt kê lệnh có sẵn
id - Hiển thị Chat ID
add - Kích hoạt bot trong group
remove - Tắt thông báo trong group
status - Trạng thái bot và số group active
```

> Sau bước này, user gõ `/` trong group sẽ thấy menu command gợi ý.

---

## Bước 2: Sinh Telegram webhook secret

Secret này dùng để backend verify request thực sự đến từ Telegram.

**PowerShell:**

```powershell
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | ForEach-Object {[char]$_})
```

**Bash:**

```bash
openssl rand -hex 16
```

**Ví dụ output:** `a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6`

> **Lưu lại** — đây là `telegramWebhookSecret` khi seed bot vào MongoDB.

---

## Bước 3: Seed bot vào backend

Gọi Admin API để lưu bot vào MongoDB:

```bash
curl -X POST "$PUBLIC_URL/admin/bots" \
  -H "Content-Type: application/json" \
  -H "X-Admin-Token: $ADMIN_TOKEN" \
  -d '{
    "username": "my_repo_bot",
    "token": "123456789:ABCdefGHIjklMNOpqrSTUvwxYZ",
    "telegramWebhookSecret": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
    "githubRepo": "owner/repo",
    "githubWebhookSecret": "gh-secret-riêng",
    "enabled": true
  }'
```

**PowerShell (Windows):**

```powershell
curl.exe -X POST "http://localhost:8080/admin/bots" `
  -H "Content-Type: application/json" `
  -H "X-Admin-Token: $env:ADMIN_TOKEN" `
  -d '{"username":"my_repo_bot","token":"123456789:ABCdefGHIjklMNOpqrSTUvwxYZ","telegramWebhookSecret":"a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6","githubRepo":"owner/repo","githubWebhookSecret":"gh-secret","enabled":true}'
```

**Verify response:**

```json
{
  "username": "my_repo_bot",
  "enabled": true,
  "hasToken": true,
  "hasTelegramWebhookSecret": true,
  "telegramWebhookPath": "/telegram/webhook/my_repo_bot"
}
```

---

## Bước 4: Đăng ký webhook với Telegram (Tự động hoặc Thủ công)

### 4.1 Tự động (Khuyên dùng)

Nếu bạn cấu hình biến môi trường `PUBLIC_URL` khi chạy backend (xem chi tiết tại [webhook.md](webhook.md)), backend sẽ **tự động gọi Telegram API để đăng ký webhook** ngay khi bạn gửi request tạo mới hoặc cập nhật bot thành công ở **Bước 3**. Bạn không cần làm bất cứ thao tác gì khác.

### 4.2 Thủ công (Dùng khi cần debug / fallback)

Nếu không cấu hình `PUBLIC_URL`, bạn phải tự gửi request đăng ký đến Telegram Bot API bằng tay:

```bash
curl -X POST "https://api.telegram.org/bot123456789:ABCdefGHIjklMNOpqrSTUvwxYZ/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://your-domain.com/telegram/webhook/my_repo_bot",
    "secret_token": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
    "allowed_updates": ["message"]
  }'
```

| Tham số | Giá trị | Lưu ý |
|---|---|---|
| `url` | `https://<PUBLIC_URL>/telegram/webhook/<username>` | **Phải HTTPS**. Username phải khớp với đã seed |
| `secret_token` | Cùng giá trị `telegramWebhookSecret` trong MongoDB | Telegram gửi header `X-Telegram-Bot-Api-Secret-Token` |
| `allowed_updates` | `["message"]` | Chỉ cần nhận message (command text) |

**Response thành công:**

```json
{
  "ok": true,
  "result": true,
  "description": "Webhook was set"
}
```

---

## Bước 5: Verify webhook đã hoạt động

```bash
curl "https://api.telegram.org/bot123456789:ABCdefGHIjklMNOpqrSTUvwxYZ/getWebhookInfo"
```

**Response mong đợi:**

```json
{
  "ok": true,
  "result": {
    "url": "https://your-domain.com/telegram/webhook/my_repo_bot",
    "has_custom_certificate": false,
    "pending_update_count": 0,
    "last_error_date": null,
    "last_error_message": null,
    "max_connections": 40,
    "allowed_updates": ["message"]
  }
}
```

**Kiểm tra:**
- `url` đúng
- `last_error_message` là `null` (không có lỗi)
- `pending_update_count` = 0 hoặc số nhỏ

---

## Bước 6: Test trong Telegram group

### 6.1 Add bot vào group

1. Mở group Telegram
2. Nhấn tên group → **Add Members** → tìm `@my_repo_bot` → Add
3. Nếu group yêu cầu, cấp quyền **Send Messages** cho bot

### 6.2 Test command

| Gõ trong group | Kết quả mong đợi |
|---|---|
| `/start` | Bot chào + hiển thị repo đang theo dõi |
| `/help` | Danh sách lệnh |
| `/id` | Chat ID của group (số âm, vd: `-1001234567890`) |
| `/status` | Trạng thái: chưa active, 0 group |
| `/add @my_repo_bot` | "✅ Đã kích hoạt" |
| `/status` | Trạng thái: ✅ activated, 1 group |
| `/remove @my_repo_bot` | "✅ Đã tắt thông báo" |

---

## Bước 7: (Dev local) Expose localhost qua ngrok

Telegram yêu cầu HTTPS public URL. Khi dev local:

```bash
ngrok http 8080
```

Lấy URL dạng `https://xxxx.ngrok-free.app`, dùng thay `<PUBLIC_URL>` ở bước 4.

> **Lưu ý**: mỗi lần restart ngrok, URL thay đổi → phải gọi lại `setWebhook`.

---

## Troubleshooting

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| Bot không phản hồi command trong group | Privacy mode đang Enabled | `/setprivacy` → Disable trên BotFather |
| `getWebhookInfo` có `last_error_message` | URL không reachable hoặc trả lỗi | Kiểm tra URL public, app đang chạy |
| Bot trả lời trong chat riêng nhưng không trong group | Bot chưa được add vào group hoặc thiếu quyền | Add bot + cấp quyền Send Messages |
| `setWebhook` trả "bad request: bad webhook: HTTPS url must be provided" | URL không dùng HTTPS | Dùng HTTPS domain hoặc ngrok |
| `/add @my_repo_bot` không có phản hồi | Username gõ khác với username trong MongoDB | Kiểm tra username khớp chính xác |
| Log backend: "bad/missing secret token" | `secret_token` khi `setWebhook` khác với `telegramWebhookSecret` trong MongoDB | Đồng bộ lại 2 giá trị |

---

## Xóa webhook (nếu cần)

```bash
curl -X POST "https://api.telegram.org/bot<TOKEN>/deleteWebhook"
```

## Quản lý nhiều bot

Lặp lại bước 1-6 cho mỗi bot. Mỗi bot có:
- Token riêng (từ BotFather)
- Secret riêng
- Webhook URL riêng: `/telegram/webhook/<username-riêng>`
- Document riêng trong `managed_bots`

Backend phục vụ tất cả trên cùng 1 instance.
