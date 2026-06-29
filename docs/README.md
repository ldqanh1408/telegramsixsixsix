# Tài liệu — telegrambots

> **telegrambots** là backend Spring Boot nhận webhook GitHub và gửi thông báo vào các nhóm Telegram, quản lý **nhiều bot trên một server** (token lưu MongoDB, thêm/sửa qua REST API, không cần deploy lại).

Tài liệu được sắp theo **mục tiêu của bạn**. Chọn nhánh phù hợp rồi đọc theo thứ tự.

---

## 🧭 Bắt đầu từ đâu?

### 1. Tôi mới biết app — muốn hiểu nó làm gì
1. [overview/business-example.md](overview/business-example.md) — ví dụ thực tế end-to-end (không đụng code)
2. [overview/mvp-summary.md](overview/mvp-summary.md) — phạm vi, quyết định kiến trúc, data model

### 2. Tôi muốn cài đặt & chạy
1. [setup/configuration.md](setup/configuration.md) — biến môi trường, chạy app, seed bot vào MongoDB
2. [setup/telegram.md](setup/telegram.md) — tạo bot BotFather + đăng ký Telegram webhook
3. [setup/github.md](setup/github.md) — tạo GitHub webhook + chọn events
4. [setup/webhook.md](setup/webhook.md) — hướng dẫn cấu hình webhook (Tự động & Thủ công)
5. [setup/docker.md](setup/docker.md) — chạy app + MongoDB bằng Docker Compose

### 3. Tôi muốn hiểu kiến trúc / sửa code
1. [architecture/overview.md](architecture/overview.md) — package, design pattern, luồng dữ liệu, schema
2. [architecture/modules.md](architecture/modules.md) — cấu trúc module hóa Maven, phân tách package sạch (Onion)
3. [architecture/decoupling.md](architecture/decoupling.md) — các refactor hạ coupling (DIP, Ports & Adapters)
4. [architecture/admin-api.md](architecture/admin-api.md) — Admin API deep-dive + flow `/add @bot`
5. [architecture/async-broadcast-proposal.md](architecture/async-broadcast-proposal.md) — *(đề xuất)* tách broadcast sang async event để bỏ bottleneck
6. [architecture/uml-design.md](architecture/uml-design.md) — sơ đồ thiết kế UML toàn diện (Class, Component, Sequence, State)

### 4. Tôi cần tra API cụ thể
- [api/README.md](api/README.md) — mỗi endpoint một file: request/response + sequence diagram

---

## 📁 Bản đồ thư mục

| Thư mục | Nội dung | Dành cho |
|---|---|---|
| [`overview/`](overview/) | App làm gì, vì sao, phạm vi MVP | Người mới, stakeholder |
| [`setup/`](setup/) | Cài đặt, cấu hình, đăng ký webhook, Docker | Người vận hành |
| [`architecture/`](architecture/) | Kiến trúc, module, design pattern, Admin API | Developer |
| [`api/`](api/) | Tham chiếu từng REST endpoint | Người tích hợp API |
| [`reference/`](reference/) | Tài liệu Spring Boot/Maven sinh tự động | Tra cứu |

---

## 🔗 Luồng chính (tóm tắt 1 phút)

```
Admin seed bot  →  POST /admin/bots         (token + repo lưu MongoDB)
       │
       ├─ Đăng ký Telegram webhook  →  /telegram/webhook/{botUsername}
       ├─ Đăng ký GitHub webhook    →  /github/webhook/{botUsername}
       │
Nhóm Telegram   →  gõ /add @bot              (tự kích hoạt, lưu DB)
       │
GitHub có event →  app render + broadcast    (gửi đúng nhóm đã /add)
```

Chi tiết từng bước: xem nhánh **Cài đặt** ở trên.

---

## ⚠️ Doc nào là "nguồn chuẩn" khi nội dung trùng nhau

| Chủ đề | Đọc file này (canonical) |
|---|---|
| Đăng ký Telegram webhook | [setup/telegram.md](setup/telegram.md) |
| Đăng ký GitHub webhook | [setup/github.md](setup/github.md) |
| Tổng quan webhook (Tự động & Thủ công) | [setup/webhook.md](setup/webhook.md) |
| Biến môi trường & chạy app | [setup/configuration.md](setup/configuration.md) |
| Đặc tả từng API endpoint | [api/](api/README.md) |
| Kiến trúc tổng thể | [architecture/overview.md](architecture/overview.md) |
