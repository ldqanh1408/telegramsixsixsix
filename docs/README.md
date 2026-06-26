# Documentation

Tài liệu chi tiết của dự án `telegrambots`.

## Mục lục

| Tài liệu | Nội dung |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Kiến trúc tổng quan: package, design pattern, luồng dữ liệu, MongoDB schema, API reference, bảo mật, cách mở rộng |
| [CONFIG.md](CONFIG.md) | Checklist cấu hình runtime, seed bot vào MongoDB, đăng ký Telegram/GitHub webhook, test end-to-end |
| [DOCKER.md](DOCKER.md) | Chạy app + MongoDB bằng Docker Compose |
| [ADMIN_CONTROLLER.md](ADMIN_CONTROLLER.md) | Giải thích Admin Bot API, flow `/add @bot`, security, DTO, SOLID/design pattern trong admin module |
| [HELP.md](HELP.md) | Tài liệu tham chiếu Spring Boot/Maven sinh từ scaffold ban đầu |

## Luồng chính

1. Seed bot/token vào MongoDB qua `/admin/bots`.
2. Đăng ký Telegram webhook: `/telegram/webhook/{botUsername}`.
3. Đăng ký GitHub webhook: `/github/webhook/{botUsername}`.
4. Add bot vào Telegram group và gõ `/add @botUsername`.
5. GitHub event sẽ được backend gửi về các group đã active bằng token của bot tương ứng.
