# API Documentation — telegrambots

Mỗi file mô tả business flow của một API endpoint, kèm sequence diagram.

## Admin API

Tất cả endpoint admin yêu cầu header `X-Admin-Token`.

| Tài liệu | Method | Path | Mô tả |
|---|---|---|---|
| [POST_admin_bots.md](POST_admin_bots.md) | `POST` | `/admin/bots` | Tạo hoặc cập nhật bot |
| [GET_admin_bots.md](GET_admin_bots.md) | `GET` | `/admin/bots` | Lấy danh sách tất cả bot |
| [GET_admin_bots_username.md](GET_admin_bots_username.md) | `GET` | `/admin/bots/{username}` | Lấy thông tin một bot |
| [PUT_admin_bots_username.md](PUT_admin_bots_username.md) | `PUT` | `/admin/bots/{username}` | Cập nhật bot theo username trên path |
| [DELETE_admin_bots_username.md](DELETE_admin_bots_username.md) | `DELETE` | `/admin/bots/{username}` | Xóa bot và toàn bộ activation |

## Webhook API

| Tài liệu | Method | Path | Mô tả |
|---|---|---|---|
| [POST_telegram_webhook.md](POST_telegram_webhook.md) | `POST` | `/telegram/webhook/{botUsername}` | Nhận Telegram update, dispatch command |
| [POST_github_webhook.md](POST_github_webhook.md) | `POST` | `/github/webhook/{botUsername}` | Nhận GitHub event, broadcast thông báo |
