# Docker

Tài liệu này chạy app Spring Boot và MongoDB bằng Docker Compose.

## File liên quan

| File | Vai trò |
|---|---|
| `Dockerfile` | Build app thành image `telegrambots:local` |
| `docker-compose.yml` | Chạy app + MongoDB |
| `.env.docker.example` | Mẫu biến môi trường cho Docker Compose |

## Chạy local

Tạo file `.env` từ mẫu:

```bash
cp .env.docker.example .env
```

Đổi `ADMIN_TOKEN` trong `.env` thành chuỗi random mạnh.

Chạy:

```bash
docker compose up --build
```

App sẽ chạy tại:

```text
http://localhost:8080
```

MongoDB sẽ chạy tại:

```text
mongodb://localhost:27017/telegrambots
```

Bên trong Docker network, app kết nối MongoDB bằng:

```text
mongodb://mongo:27017/telegrambots
```

Compose truyền connection string này qua `MONGODB_URI` và `SPRING_MONGODB_URI`.

## Kiểm tra container

```bash
docker compose ps
docker compose logs -f app
docker compose logs -f mongo
```

## Seed bot vào MongoDB qua app

```bash
curl -X POST "http://localhost:8080/admin/bots" \
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

Trên PowerShell, nếu chưa export `$ADMIN_TOKEN`, dùng trực tiếp giá trị trong `.env`:

```powershell
curl.exe -X POST "http://localhost:8080/admin/bots" `
  -H "Content-Type: application/json" `
  -H "X-Admin-Token: replace-with-strong-random-value" `
  -d '{ "username": "my_repo_bot", "token": "123456789:ABC...", "enabled": true }'
```

## Public webhook khi chạy local

Telegram và GitHub cần HTTPS public. Khi app chạy trong Docker local, dùng ngrok:

```bash
ngrok http 8080
```

Sau đó dùng public URL cho webhook:

```text
https://<ngrok-domain>/telegram/webhook/my_repo_bot
https://<ngrok-domain>/github/webhook/my_repo_bot
```

## Dừng và xóa dữ liệu

Dừng container nhưng giữ dữ liệu MongoDB:

```bash
docker compose down
```

Dừng container và xóa luôn volume MongoDB:

```bash
docker compose down -v
```
