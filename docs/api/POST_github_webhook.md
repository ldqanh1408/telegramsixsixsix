# POST /github/webhook/{botUsername} — Nhận GitHub event

## Mục đích

Endpoint mà GitHub gọi khi repo có event (push, PR, issue...). Backend verify chữ ký HMAC-SHA256, kiểm tra repo khớp, format message, và broadcast đến tất cả Telegram group đã kích hoạt bot.

## Request

```http
POST /github/webhook/{botUsername}
Content-Type: application/json
X-GitHub-Event: push
X-Hub-Signature-256: sha256=abc123...
X-GitHub-Delivery: 72d3162e-cc78-11e3-81ab-4c9367dc0958
```

| Header | Mô tả |
|---|---|
| `X-GitHub-Event` | Loại event: `push`, `pull_request`, `issues`, `issue_comment`, `release`, `star`, `workflow_run` |
| `X-Hub-Signature-256` | Chữ ký HMAC-SHA256 của body, verify bằng `githubWebhookSecret` |
| `X-GitHub-Delivery` | UUID duy nhất cho mỗi delivery (dùng log/debug) |

| Path param | Mô tả |
|---|---|
| `botUsername` | Username bot. Key để lookup bot + secret trong MongoDB |

**Body**: JSON payload từ GitHub (khác nhau theo event type).

## Response

| Status | Body | Điều kiện |
|---|---|---|
| `200` | `"pong"` | Event = `ping` (GitHub test delivery) |
| `200` | `"ok"` | Event xử lý thành công |
| `200` | `"ignored: repo mismatch"` | Repo trong payload không khớp `githubRepo` của bot |
| `400` | `"missing X-GitHub-Event"` | Thiếu header event |
| `400` | `"invalid json"` | Body không parse được JSON |
| `401` | `"bad signature"` | Chữ ký HMAC-SHA256 sai |
| `404` | `"unknown bot"` | Bot không tồn tại hoặc disabled |

## Sequence Diagram — Luồng chính

```mermaid
sequenceDiagram
    actor GH as GitHub
    participant Controller as GitHubWebhookController +<br/>GitHubWebhookProcessor
    participant Facade as BotManagementUseCase
    participant Registry as ManagedBotRegistry
    participant Verifier as WebhookSignatureVerifier<br/>(HmacSha256SignatureVerifier)
    participant NotifSvc as NotificationService
    participant Formatter as EventFormatter<br/>(vd: PushEventFormatter)
    participant ActivationSvc as GroupBotActivationService
    participant Client as TelegramClient
    participant DB_Bot as MongoDB<br/>managed_bots
    participant DB_Act as MongoDB<br/>group_activations
    participant TG as Telegram API

    GH->>Controller: POST /github/webhook/my_repo_bot<br/>X-GitHub-Event: push<br/>X-Hub-Signature-256: sha256=...<br/>Body: { repository, commits, ... }

    Note over Controller: Bước 1: Lookup bot

    Controller->>Facade: findEnabled("my_repo_bot")
    Facade->>Registry: findEnabled("my_repo_bot")
    Registry->>DB_Bot: findByUsername (cache hoặc query)

    alt Bot không tồn tại hoặc disabled
        Registry-->>Controller: Optional.empty()
        Controller-->>GH: 404 "unknown bot"
    end

    DB_Bot-->>Registry: ManagedBot
    Registry-->>Controller: Optional.of(bot)

    Note over Controller: Bước 2: Verify chữ ký

    Controller->>Verifier: verify(bot.ghWebhookSecret, signatureHeader, rawBody)

    Verifier->>Verifier: HMAC-SHA256(secret, body)<br/>→ computed = "sha256=abc..."
    Verifier->>Verifier: MessageDigest.isEqual(computed, received)<br/>(constant-time compare)

    alt Chữ ký sai
        Verifier-->>Controller: false
        Controller-->>GH: 401 "bad signature"
    else Secret rỗng (verify disabled)
        Verifier-->>Controller: true
    else Chữ ký đúng
        Verifier-->>Controller: true
    end

    Note over Controller: Bước 3: Check event type

    alt Thiếu X-GitHub-Event header
        Controller-->>GH: 400 "missing X-GitHub-Event"
    else event = "ping"
        Controller-->>GH: 200 "pong"
    end

    Note over Controller: Bước 4: Parse JSON

    Controller->>Controller: mapper.readTree(body) → JsonNode payload
    alt JSON invalid
        Controller-->>GH: 400 "invalid json"
    end

    Note over Controller: Bước 5: Check repo match

    Controller->>Controller: matchesConfiguredRepo(bot, payload)
    Controller->>Controller: payload.repository.full_name == bot.githubRepo?

    alt Repo không khớp
        Controller-->>GH: 200 "ignored: repo mismatch"
    end

    Note over Controller: Bước 6: Dispatch notification

    Controller->>NotifSvc: dispatch(bot, "push", payload)

    NotifSvc->>NotifSvc: formatters.get("push")

    alt Không có formatter cho event type
        NotifSvc-->>Controller: (ignored, no formatter)
        Controller-->>GH: 200 "ok"
    end

    NotifSvc->>Formatter: format(payload)

    alt Formatter drop payload (vd: push 0 commits)
        Formatter-->>NotifSvc: Optional.empty()
        NotifSvc-->>Controller: (dropped)
        Controller-->>GH: 200 "ok"
    else Formatter render HTML
        Formatter-->>NotifSvc: Optional.of("🔔 3 commits lên owner/repo @ main...")
    end

    Note over NotifSvc: Bước 7: Broadcast

    NotifSvc->>Facade: activeGroups(bot)
    Facade->>ActivationSvc: activeGroups(bot)
    ActivationSvc->>DB_Act: findByBotUsernameAndActiveTrue("my_repo_bot")
    DB_Act-->>ActivationSvc: List<GroupActivation>

    alt Không có group active
        ActivationSvc-->>NotifSvc: []
        Note over NotifSvc: Log "no activated groups - dropped"
    else Có groups
        ActivationSvc-->>NotifSvc: [group1, group2, group3]

        loop Với mỗi GroupActivation
            NotifSvc->>Client: sendHtml(bot.token, activation.chatId, html)
            Client->>TG: POST /bot<TOKEN>/sendMessage<br/>{ chat_id, text, parse_mode: "HTML" }
            TG-->>Client: ok
        end
    end

    NotifSvc-->>Controller: done
    Controller-->>GH: 200 "ok"
```

---

## Ví dụ: Push event flow chi tiết

```mermaid
sequenceDiagram
    actor Dev as Developer
    participant GH as GitHub
    participant App as telegrambots
    participant TG as Telegram API
    actor Group as Telegram Group

    Dev->>GH: git push origin main (3 commits)

    GH->>App: POST /github/webhook/my_repo_bot<br/>X-GitHub-Event: push<br/>Body: { ref, commits[3], pusher, compare }

    App->>App: Verify signature ✓
    App->>App: Repo match ✓
    App->>App: PushEventFormatter.format()

    Note over App: Render HTML:<br/>🔔 3 commits lên owner/repo @ main bởi dev<br/>• abc1234 Fix login bug — dev<br/>• def5678 Add tests — dev<br/>• ghi9012 Update docs — dev

    App->>App: Query active groups → [group_A, group_B]

    par Gửi song song (trong vòng lặp)
        App->>TG: sendMessage(chatId_A, html)
        TG-->>Group: 🔔 3 commits lên owner/repo @ main...
    and
        App->>TG: sendMessage(chatId_B, html)
        TG-->>Group: 🔔 3 commits lên owner/repo @ main...
    end

    App-->>GH: 200 "ok"
```

---

## Các event formatter có sẵn

| Event | Formatter | Mô tả | Drop khi |
|---|---|---|---|
| `push` | `PushEventFormatter` | Liệt kê commits, branch, pusher | 0 commits (branch create/delete) |
| `pull_request` | `PullRequestEventFormatter` | PR opened/closed/merged, title, URL | — |
| `issues` | `IssueEventFormatter` | Issue opened/closed, title, URL | — |
| `issue_comment` | `IssueCommentEventFormatter` | Comment trên issue, nội dung, URL | — |
| `release` | `ReleaseEventFormatter` | Release published, tag, URL | — |
| `star` | `StarEventFormatter` | User star/unstar repo | — |
| `workflow_run` | `WorkflowRunEventFormatter` | CI/CD workflow completed, status | — |

## Business rules

1. **Signature verification**: HMAC-SHA256 + constant-time compare. Secret rỗng → skip verify (dev mode)
2. **Repo match**: nếu `bot.githubRepo` rỗng → accept all repos. Nếu có giá trị → phải khớp `payload.repository.full_name` (case-insensitive)
3. **Ping event**: GitHub gửi `ping` khi đăng ký webhook lần đầu → trả `"pong"`, không dispatch
4. **No formatter**: event type không có formatter (vd: `fork`) → bỏ qua, trả `200`
5. **Formatter drop**: formatter trả `Optional.empty()` → bỏ qua (vd: push 0 commits)
6. **No active groups**: có message nhưng không có group nào `/add` → log, không gửi
7. **Broadcast failure isolation**: nếu gửi thất bại cho 1 group, vẫn tiếp tục gửi các group còn lại
8. **Luôn trả 200/ok**: sau khi verify thành công, mọi lỗi xử lý đều catch → trả 200 để GitHub không retry
