# Hướng dẫn cấu hình Webhook (Webhook Configuration Guide)

Hệ thống đã được cập nhật để tự động hóa việc tạo và đăng ký webhook tối đa. Dưới đây là phân tích chi tiết về những gì được thực hiện tự động bởi backend và những gì bạn cần cấu hình thủ công.

---

## 1. Tự động hóa ở Backend (Automated Actions)

Khi bạn thực hiện tạo mới hoặc cập nhật một bot qua API `POST/PUT /admin/bots`:

### Tự động sinh khóa bí mật (Secrets Generation)
- Nếu bạn không truyền (hoặc để trống/null) `telegramWebhookSecret` hoặc `githubWebhookSecret`, hệ thống sẽ **tự động tạo ra một khóa bí mật ngẫu nhiên bảo mật (32 ký tự hex)** và lưu vào Database.
- Các khóa bí mật này dùng để xác thực webhook gửi đến, tránh giả mạo:
  - `tgWebhookSecret` được gửi sang Telegram làm `secret_token` cho header `X-Telegram-Bot-Api-Secret-Token`.
  - `ghWebhookSecret` dùng để kiểm tra chữ ký HMAC SHA-256 (`X-Hub-Signature-256`) từ GitHub.

### Tự động đăng ký Webhook Telegram (`setWebhook` & `deleteWebhook`)
Nếu biến môi trường `PUBLIC_URL` (hoặc `app.public-url` trong `application.yaml`) được cấu hình:
- **Khi Bot được BẬT (`enabled: true`)**: Backend tự động gửi yêu cầu `setWebhook` đến API Telegram để cấu hình URL nhận tin nhắn:
  - **URL đăng ký**: `https://<PUBLIC_URL>/telegram/webhook/<bot_username>`
  - **Secret Token**: `tgWebhookSecret` tự động sinh ở trên.
- **Khi Bot bị TẮT (`enabled: false`) hoặc bị XÓA**: Backend tự động gọi `deleteWebhook` đến Telegram để huỷ đăng ký.

---

## 2. Cấu hình Thủ công (Manual Configurations)

Một số cấu hình bắt buộc bạn phải thao tác thủ công vì lý do bảo mật hoặc giới hạn của các dịch vụ bên thứ ba:

### Bước A: Cấu hình URL công khai của ứng dụng (`PUBLIC_URL`)
Để backend có thể tự động gọi `setWebhook` đến Telegram, nó cần biết domain công khai của nó (ví dụ: URL ngrok). Bạn có 2 cách để khởi chạy ngrok:

#### Cách 1: Chạy ngrok qua Docker Compose (Khuyên dùng)
Dịch vụ `ngrok` đã được cấu hình sẵn trong `docker-compose.yml`.
1. Copy file `config.properties.example` thành `.env`.
2. Lấy **Authtoken** từ trang quản trị ngrok của bạn (truy cập `https://dashboard.ngrok.com/get-started/your-authtoken`).
3. Cấu hình biến `NGROK_AUTHTOKEN` trong file `.env`:
   ```env
   NGROK_AUTHTOKEN=your-ngrok-authtoken-here
   ```
4. Khởi chạy Docker Compose:
   ```bash
   docker compose up -d
   ```
5. Truy cập giao diện kiểm tra của ngrok tại: [http://localhost:4040](http://localhost:4040) để lấy URL HTTPS công khai được cấp (ví dụ: `https://xxxx.ngrok-free.app`).
6. Điền URL đó vào `PUBLIC_URL` trong `.env`:
   ```env
   PUBLIC_URL=https://xxxx.ngrok-free.app
   ```
7. Khởi động lại container ứng dụng để nhận cấu hình mới:
   ```bash
   docker compose restart app
   ```

#### Cách 2: Chạy ngrok thủ công bên ngoài Docker
1. Khởi chạy ngrok để expose cổng của backend (mặc định `8080`):
   ```bash
   ngrok http 8080
   ```
2. Lấy URL HTTPS được cấp (ví dụ: `https://xxxx.ngrok-free.app`).
3. Cấu hình biến `PUBLIC_URL` trong file `.env`:
   ```env
   PUBLIC_URL=https://xxxx.ngrok-free.app
   ```
4. Khởi chạy/Khởi động lại ứng dụng.

### Bước B: Cấu hình Webhook trên GitHub Repository
Vì backend không lưu trữ token truy cập cá nhân (GitHub Personal Access Token) của bạn, nó **không thể** tự gọi API của GitHub để tạo webhook tự động cho repo của bạn. Bạn phải cấu hình thủ công:
1. Truy cập vào GitHub Repository của bạn.
2. Vào **Settings** -> **Webhooks** -> **Add webhook**.
3. Điền các thông tin sau:
   - **Payload URL**: `https://<PUBLIC_URL>/github/webhook/<bot_username>` (Ví dụ: `https://xxxx.ngrok-free.app/github/webhook/my_repo_bot`)
   - **Content type**: `application/json`
   - **Secret**: Lấy giá trị `githubWebhookSecret` mà backend tự động sinh và trả về khi bạn tạo bot.
   - **Which events...**: Chọn *Let me select individual events* và chọn: `Pushes`, `Pull requests`, `Issues`, `Issue comments`, `Releases`, `Stars`, `Workflow runs`.
4. Nhấn **Add webhook**.

---

> [!TIP]
> Bạn có thể gọi `GET /admin/bots` hoặc `GET /admin/bots/<username>` để lấy các giá trị `tgWebhookSecret` và `ghWebhookSecret` tự động sinh nhằm cấu hình thủ công cho GitHub hoặc kiểm tra trạng thái webhook.
