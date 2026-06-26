# GITHUB_SETUP — Cấu hình GitHub Webhook

Hướng dẫn từng bước cấu hình GitHub webhook để repo gửi event đến backend `telegrambots`.

---

## Điều kiện tiên quyết

- Bot đã được seed vào MongoDB (xem [telegram.md](telegram.md) bước 3)
- Backend đang chạy và có HTTPS public URL
- Bạn có quyền **Admin** hoặc **Maintain** trên repo GitHub

---

## Bước 1: Sinh GitHub webhook secret

Secret này dùng để backend verify request thực sự đến từ GitHub (HMAC-SHA256).

**PowerShell:**

```powershell
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 48 | ForEach-Object {[char]$_})
```

**Bash:**

```bash
openssl rand -hex 32
```

**Ví dụ output:** `e4f8a2b1c9d7e3f6a0b5c8d2e1f4a7b9c3d6e0f5a8b2c4d7`

> **Lưu lại** — đây là `githubWebhookSecret`. Giá trị này phải khớp với field `githubWebhookSecret` đã seed vào MongoDB.

---

## Bước 2: Seed bot vào backend (nếu chưa)

Đảm bảo bot trong MongoDB có đúng `githubRepo` và `githubWebhookSecret`:

```bash
curl -X POST "$PUBLIC_URL/admin/bots" \
  -H "Content-Type: application/json" \
  -H "X-Admin-Token: $ADMIN_TOKEN" \
  -d '{
    "username": "my_repo_bot",
    "token": "<TELEGRAM_TOKEN>",
    "telegramWebhookSecret": "<TG_SECRET>",
    "githubRepo": "owner/repo",
    "githubWebhookSecret": "e4f8a2b1c9d7e3f6a0b5c8d2e1f4a7b9c3d6e0f5a8b2c4d7",
    "enabled": true
  }'
```

Nếu bot đã tồn tại, chỉ cần update field GitHub:

```bash
curl -X PUT "$PUBLIC_URL/admin/bots/my_repo_bot" \
  -H "Content-Type: application/json" \
  -H "X-Admin-Token: $ADMIN_TOKEN" \
  -d '{
    "githubRepo": "owner/repo",
    "githubWebhookSecret": "e4f8a2b1c9d7e3f6a0b5c8d2e1f4a7b9c3d6e0f5a8b2c4d7"
  }'
```

> **Quan trọng**: `githubRepo` phải khớp chính xác `full_name` của repo (vd: `ledequocanh/my-project`). Case-insensitive.

---

## Bước 3: Tạo webhook trên GitHub

### Cách 1: Qua giao diện GitHub (khuyến nghị)

1. Vào repo trên GitHub
2. **Settings** → **Webhooks** → **Add webhook**
3. Điền thông tin:

| Field | Giá trị |
|---|---|
| **Payload URL** | `https://<PUBLIC_URL>/github/webhook/my_repo_bot` |
| **Content type** | `application/json` |
| **Secret** | `e4f8a2b1c9d7e3f6a0b5c8d2e1f4a7b9c3d6e0f5a8b2c4d7` |
| **SSL verification** | ✅ Enable SSL verification |
| **Which events?** | ◉ Let me select individual events |

4. Chọn các events:

| Event | Checkbox | Mô tả |
|---|---|---|
| **Pushes** | ☑ | Push commits lên branch |
| **Pull requests** | ☑ | Mở/đóng/merge PR |
| **Issues** | ☑ | Mở/đóng issue |
| **Issue comments** | ☑ | Comment trên issue |
| **Releases** | ☑ | Publish release |
| **Stars** | ☑ | Star/unstar repo |
| **Workflow runs** | ☑ | CI/CD workflow hoàn thành |

5. ☑ **Active** → **Add webhook**

### Cách 2: Qua GitHub API

```bash
curl -X POST "https://api.github.com/repos/owner/repo/hooks" \
  -H "Authorization: Bearer <GITHUB_PERSONAL_ACCESS_TOKEN>" \
  -H "Accept: application/vnd.github+json" \
  -d '{
    "name": "web",
    "active": true,
    "events": [
      "push",
      "pull_request",
      "issues",
      "issue_comment",
      "release",
      "star",
      "workflow_run"
    ],
    "config": {
      "url": "https://<PUBLIC_URL>/github/webhook/my_repo_bot",
      "content_type": "json",
      "secret": "e4f8a2b1c9d7e3f6a0b5c8d2e1f4a7b9c3d6e0f5a8b2c4d7",
      "insecure_ssl": "0"
    }
  }'
```

> GitHub Personal Access Token cần permission: `admin:repo_hook` (hoặc fine-grained token với `Webhooks: Read and write`).

---

## Bước 4: Verify webhook

### 4.1 Kiểm tra ping event

Ngay sau khi tạo webhook, GitHub gửi `ping` event. Backend trả `200 "pong"`.

Kiểm tra trên GitHub:
1. **Settings** → **Webhooks** → click webhook vừa tạo
2. Tab **Recent Deliveries**
3. Tìm delivery có `X-GitHub-Event: ping`
4. **Response** tab phải là `200` với body `pong`

### 4.2 Kiểm tra qua API

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/owner/repo/hooks"
```

### 4.3 Test bằng push thật

```bash
# Tạo commit nhỏ và push
echo "test" >> test.txt
git add test.txt
git commit -m "test webhook"
git push
```

Kiểm tra:
- GitHub webhook delivery tab: response `200 "ok"`
- Telegram group đã `/add @my_repo_bot`: nhận tin nhắn thông báo push

---

## Bước 5: (Tùy chọn) Test delivery lại

GitHub cho phép redeliver bất kỳ webhook nào:

1. **Settings** → **Webhooks** → click webhook
2. **Recent Deliveries** → chọn delivery
3. Click **Redeliver**

Hữu ích khi debug mà không cần tạo event thật.

---

## Events được hỗ trợ

Backend có formatter cho 7 event types sau:

| GitHub Event | Formatter | Telegram message |
|---|---|---|
| `push` | `PushEventFormatter` | 🔔 **N commits** lên `repo` @ `branch` bởi `user`<br/>• `sha` message — author |
| `pull_request` | `PullRequestEventFormatter` | PR opened/closed/merged + title + URL |
| `issues` | `IssueEventFormatter` | Issue opened/closed + title + URL |
| `issue_comment` | `IssueCommentEventFormatter` | Comment trên issue + nội dung + URL |
| `release` | `ReleaseEventFormatter` | Release published + tag + URL |
| `star` | `StarEventFormatter` | User starred/unstarred repo |
| `workflow_run` | `WorkflowRunEventFormatter` | Workflow completed + status |

**Events khác** (fork, watch, deployment...): backend nhận nhưng không có formatter → bỏ qua, trả `200 "ok"`.

---

## Cấu hình cho Organization repo

Tương tự repo cá nhân. Lưu ý:
- Cần quyền **Admin** trên repo (hoặc Org owner)
- Webhook URL vẫn dùng `full_name` của repo: `org-name/repo-name`
- `githubRepo` trong MongoDB phải khớp: `"org-name/repo-name"`

---

## Nhiều bot cho nhiều repo

Mỗi repo có webhook riêng trỏ tới bot riêng:

```
Repo A (owner/repo-a)
  └── Webhook → https://domain.com/github/webhook/bot_a
       Secret = secret_a

Repo B (owner/repo-b)
  └── Webhook → https://domain.com/github/webhook/bot_b
       Secret = secret_b
```

MongoDB:

```json
[
  { "username": "bot_a", "githubRepo": "owner/repo-a", "ghWebhookSecret": "secret_a" },
  { "username": "bot_b", "githubRepo": "owner/repo-b", "ghWebhookSecret": "secret_b" }
]
```

---

## GitHub webhook security

### Cách verify hoạt động

```
GitHub tạo request:
  1. Body = JSON payload
  2. HMAC = SHA256(secret, body)
  3. Header: X-Hub-Signature-256 = "sha256=" + hex(HMAC)

Backend verify:
  1. Nhận raw body bytes
  2. Tính HMAC-SHA256 bằng secret trong MongoDB
  3. So sánh constant-time với header
  4. Khớp → xử lý. Sai → 401
```

### Tại sao cần secret?

- Không có secret → bất kỳ ai biết URL webhook đều có thể gửi event giả
- Với secret → chỉ GitHub (biết secret) mới tạo được signature hợp lệ
- Constant-time compare → chống timing attack

---

## Troubleshooting

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| Delivery response `404 "unknown bot"` | `botUsername` trên URL không khớp bot trong MongoDB | Kiểm tra username chính xác, bot `enabled: true` |
| Delivery response `401 "bad signature"` | Secret trên GitHub ≠ `githubWebhookSecret` trong MongoDB | Đồng bộ lại secret |
| Delivery response `200 "ignored: repo mismatch"` | `full_name` trong payload ≠ `githubRepo` trong MongoDB | Kiểm tra `githubRepo` khớp format `owner/repo` |
| Delivery response `200 "ok"` nhưng group không nhận tin | Chưa `/add @bot` trong group, hoặc không có formatter cho event type | Gõ `/add @bot`, kiểm tra event type được hỗ trợ |
| Delivery response `400 "invalid json"` | Body không phải JSON hợp lệ | Content type phải là `application/json` |
| Webhook hiện ⚠️ trên GitHub | URL không reachable hoặc trả non-2xx | Kiểm tra app đang chạy, URL public HTTPS |
| Webhook bị disabled tự động | Quá nhiều delivery thất bại liên tiếp | GitHub auto-disable sau ~N failures. Fix lỗi → Re-enable |

### Re-enable webhook bị disabled

1. **Settings** → **Webhooks** → click webhook
2. Nếu thấy banner "This webhook is disabled" → scroll xuống
3. Click **Enable** hoặc edit → save lại

### Xóa webhook

1. **Settings** → **Webhooks** → click webhook → **Delete webhook**

Hoặc qua API:

```bash
curl -X DELETE "https://api.github.com/repos/owner/repo/hooks/<HOOK_ID>" \
  -H "Authorization: Bearer <TOKEN>"
```

---

## Checklist tổng hợp

```
☐ Sinh githubWebhookSecret (random, đủ mạnh)
☐ Seed bot vào MongoDB với đúng githubRepo + githubWebhookSecret
☐ Tạo webhook trên GitHub repo:
    ☐ Payload URL = https://<domain>/github/webhook/<username>
    ☐ Content type = application/json
    ☐ Secret = khớp với MongoDB
    ☐ SSL verification = enabled
    ☐ Events: push, pull_request, issues, issue_comment, release, star, workflow_run
☐ Verify ping delivery = 200 "pong"
☐ /add @bot trong Telegram group
☐ Push test commit → tin nhắn xuất hiện trong group
```
