# CONFIG — chạy multi-bot bằng MongoDB

Checklist này dùng cho mô hình dynamic loading: token của từng bot nằm trong MongoDB, app không còn đọc `BOT_TOKEN`, `BOT_USERNAME`, `GITHUB_REPO` hay `GROUPS_FILE` từ env.

## 1. Chuẩn bị hạ tầng chung

| Giá trị | Nguồn | Ghi chú |
|---|---|---|
| `MONGODB_URI` | MongoDB local/Atlas/VPS | Default local: `mongodb://localhost:27017/telegrambots` |
| `ADMIN_TOKEN` | tự sinh random | Bảo vệ `/admin/**`. Nếu rỗng, admin API bị tắt |
| `PUBLIC_URL` | domain/ngrok HTTPS | Telegram và GitHub cần endpoint public HTTPS |

Sinh `ADMIN_TOKEN`:

```powershell
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 48 | ForEach-Object {[char]$_})
```

Chạy app:

```powershell
$env:MONGODB_URI = "mongodb://localhost:27017/telegrambots"
$env:ADMIN_TOKEN = "<random-admin-token>"
$env:PORT = "8080"
./mvnw spring-boot:run
```

## 2. Tạo bot và secret

Với mỗi bot:

1. Chat với [@BotFather](https://t.me/BotFather), tạo bot bằng `/newbot`, lưu token.
2. Chạy `/setprivacy` cho bot và chọn **Disable** để bot đọc được lệnh trong group.
3. Sinh 2 secret riêng:
   - `telegramWebhookSecret`: gửi qua header `X-Telegram-Bot-Api-Secret-Token`.
   - `githubWebhookSecret`: dùng để verify `X-Hub-Signature-256`.

## 3. Nạp bot vào kho MongoDB

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

Các API quản trị hữu ích:

```bash
curl -H "X-Admin-Token: $ADMIN_TOKEN" "$PUBLIC_URL/admin/bots"
curl -H "X-Admin-Token: $ADMIN_TOKEN" "$PUBLIC_URL/admin/bots/my_repo_bot"
curl -X DELETE -H "X-Admin-Token: $ADMIN_TOKEN" "$PUBLIC_URL/admin/bots/my_repo_bot"
```

## 4. Đăng ký Telegram webhook cho bot

```bash
curl -X POST "https://api.telegram.org/bot<TOKEN>/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "<PUBLIC_URL>/telegram/webhook/my_repo_bot",
    "secret_token": "random-telegram-secret",
    "allowed_updates": ["message"]
  }'
```

Verify:

```bash
curl "https://api.telegram.org/bot<TOKEN>/getWebhookInfo"
```

## 5. Đăng ký GitHub webhook

Trong repo `owner/repo`: **Settings → Webhooks → Add webhook**

| Trường | Giá trị |
|---|---|
| Payload URL | `<PUBLIC_URL>/github/webhook/my_repo_bot` |
| Content type | `application/json` |
| Secret | `githubWebhookSecret` trong MongoDB |
| SSL verification | Enable |
| Events | Pushes, Pull requests, Issues, Issue comments, Releases, Stars, Workflow runs |

GitHub gửi `ping`; app trả `200 OK pong` nếu webhook đúng.

## 6. Kích hoạt trong Telegram group

1. Add `@my_repo_bot` vào group.
2. Cấp quyền gửi tin nhắn cho bot.
3. Gõ:

```text
/add @my_repo_bot
```

Bot sẽ tạo/cập nhật document trong `group_activations` với `active=true`. Tắt bằng:

```text
/remove @my_repo_bot
```

## 7. Test nhanh

| Bước | Kết quả mong đợi |
|---|---|
| Gõ `/id` trong group | Bot trả Chat ID |
| Gõ `/status` | Bot báo group hiện tại đã active hay chưa |
| Push commit lên repo đúng `githubRepo` | Bot gửi thông báo vào group đã `/add` |
| Gửi event repo khác | App trả `ignored: repo mismatch` |

Nếu không có tin nhắn:

- `404 unknown bot`: chưa có document `managed_bots` enabled cho username trên URL.
- `401 bad signature`: secret GitHub không khớp.
- Log `no activated groups`: chưa `/add @bot` trong group.
