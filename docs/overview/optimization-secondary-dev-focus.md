# Optimization / Secondary Dev Focus — telegrambots

> **Trạng thái**: Lộ trình phát triển sau MVP.  
> **Ngày cập nhật**: 2026-06-29.  
> Tài liệu này liệt kê các cải tiến kỹ thuật và tính năng thứ cấp cần làm sau khi hệ thống vận hành ổn định ở quy mô nhỏ.

---

## Mục lục

1. [Performance Optimization](#1-performance-optimization)
2. [Resilience & Error Handling](#2-resilience--error-handling)
3. [Observability (Metrics, Logging, Tracing)](#3-observability-metrics-logging-tracing)
4. [Security Hardening](#4-security-hardening)
5. [Feature Expansion](#5-feature-expansion)
6. [Testing & Quality](#6-testing--quality)
7. [DevOps & Infra](#7-devops--infra)
8. [Priority Matrix](#8-priority-matrix)

---

## 1. Performance Optimization

### 1.1 Async Broadcast (⚠️ Bottleneck chính — High Priority)

**Vấn đề**: `BroadcastUseCase.broadcast()` gửi Telegram **tuần tự, đồng bộ** ngay trên thread webhook GitHub.  
Kết quả: N group × ~150ms RTT → có thể vượt timeout 10s của GitHub → **GitHub tự tắt webhook**.

**Giải pháp**: Tách broadcast sang luồng nền qua application event (đã có đề xuất chi tiết tại [`async-broadcast-proposal.md`](../architecture/async-broadcast-proposal.md)).

```
Trước:  GitHubWebhookProcessor ──broadcast()──▶ BroadcastUseCase   (đồng bộ, cùng thread)

Sau:    GitHubWebhookProcessor ──publishEvent──▶ [BroadcastRequestedEvent]
                                                        │  (async, virtual thread)
                                    BroadcastUseCase ◀──@Async @EventListener
```

**Checklist**:
```
☐ Thêm domain event: BroadcastRequestedEvent(botUsername, html)
☐ GitHubWebhookProcessor: thay BroadcastUseCase → ApplicationEventPublisher
☐ BroadcastUseCase: thêm @Async @EventListener onBroadcastRequested()
☐ config/AsyncConfig.java: @EnableAsync
☐ application.yaml: spring.threads.virtual.enabled=true  (Java 21 virtual threads)
☐ Cập nhật GitHubWebhookProcessorTest: verify(eventPublisher).publishEvent(...)
☐ Thêm BroadcastListenerTest
```

---

### 1.2 Parallel Fan-out trong Broadcast

**Vấn đề**: Ngay cả khi async, broadcast vẫn gửi **tuần tự** (`for` loop) → tổng thời gian = N × RTT.

**Giải pháp**: Fan-out song song có giới hạn luồng.

```java
// Trong BroadcastUseCase (sau khi đã async)
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    activeGroups.forEach(group ->
        scope.fork(() -> telegramGateway.sendHtml(bot.token(), group.chatId(), html))
    );
    scope.join();
}
```

> Dùng **Structured Concurrency** (Java 21 Preview → GA trong 23). Fallback: `ExecutorService` với pool size 10 nếu chưa dùng Java 23+.

**Kết quả kỳ vọng**: Giảm thời gian broadcast N group từ `N×150ms` xuống còn `~150ms`.

**Checklist**:
```
☐ Thêm ExecutorService bean giới hạn 10 luồng (hoặc dùng virtual thread per-task)
☐ Refactor BroadcastUseCase.broadcast() sang parallel fan-out
☐ Benchmark: đo thời gian gửi 10 group trước/sau
☐ Test: assert tất cả group nhận message dù 1 group lỗi
```

---

### 1.3 Telegram Rate Limit Handling (429)

**Vấn đề hiện tại**: `TelegramClient` bắt `Exception` tổng quát, log lỗi rồi **bỏ qua** → **mất tin nhắn** khi Telegram trả 429 Too Many Requests.

**Giải pháp**:
```java
// Trong TelegramClient
if (response.statusCode() == 429) {
    int retryAfter = extractRetryAfter(responseBody); // parse "retry_after" từ JSON
    Thread.sleep(Duration.ofSeconds(retryAfter + 1));
    // retry lần 2
}
```

**Giới hạn Telegram Bot API**:
- ~30 messages/second per bot
- ~1 message/second per chat (group)
- Bulk: 20 messages/minute per group

**Checklist**:
```
☐ Parse "retry_after" từ 429 response body
☐ Thêm retry với exponential backoff (tối đa 3 lần)
☐ Throttle: 30 msg/s/bot, 1 msg/s/chat
☐ Log rõ ràng khi retry và khi drop (sau max retry)
☐ Test: mock 429 → assert retry được gọi đúng số lần
```

---

### 1.4 Bot Registry Warm-up & Refresh

**Vấn đề**: `ManagedBotRegistry` load bot lúc startup và chỉ cập nhật khi CRUD qua API. Nếu MongoDB thay đổi trực tiếp (không qua API), cache sẽ stale.

**Giải pháp**:
```java
// Scheduled refresh mỗi 5 phút để phòng trường hợp đồng bộ
@Scheduled(fixedDelay = 300_000)
void refreshRegistry() {
    botRepository.findAllEnabled().forEach(registry::register);
}
```

**Checklist**:
```
☐ Thêm @Scheduled refresh vào ManagedBotRegistry
☐ Đảm bảo refresh atomic (không race condition với webhook lookup)
☐ Thêm config: app.registry.refresh-interval-ms (default 300000)
```

---

## 2. Resilience & Error Handling

### 2.1 Retry khi Telegram gửi thất bại

> Liên quan đến mục 1.3. Mở rộng thêm: retry cho các lỗi mạng tạm thời (5xx, timeout).

**Chiến lược**:

| Loại lỗi | Hành động |
|---|---|
| `429 Too Many Requests` | Retry sau `retry_after` giây |
| `5xx Server Error` | Retry tối đa 3 lần, backoff 1s/2s/4s |
| `400 Bad Request` | Log lỗi + skip (message không hợp lệ, không thể retry) |
| `401/403 Unauthorized` | Alert admin, disable bot tạm thời |
| Timeout / Connection error | Retry tối đa 2 lần |

**Checklist**:
```
☐ Tạo TelegramSendException hierarchy (RetryableException vs NonRetryableException)
☐ Implement RetryPolicy trong TelegramClient
☐ Tích hợp với async broadcast: lỗi không retry được → log + metric
☐ Test từng loại lỗi
```

---

### 2.2 Dead Letter / Alert khi Broadcast thất bại hoàn toàn

**Vấn đề**: Sau 3 lần retry thất bại, tin nhắn bị drop **im lặng**.

**Giải pháp tối thiểu**:
```java
// Publish thêm event BroadcastFailed → ghi log structured + metric counter
log.error("Broadcast failed after {} retries: bot={}, chatId={}, error={}",
    maxRetries, botUsername, chatId, errorMessage);
metrics.counter("broadcast.failed").increment();
```

**Giải pháp nâng cao** (có persistent event outbox):  
Dùng `spring-modulith-starter-mongodb` để lưu event publication vào MongoDB → at-least-once delivery.

**Checklist**:
```
☐ Structured log khi drop message sau max retry
☐ Metric counter: broadcast.success, broadcast.retry, broadcast.failed
☐ (Nâng cao) Outbox pattern với Spring Modulith + MongoDB
```

---

### 2.3 Webhook Auto-Registration

**Vấn đề hiện tại**: Admin phải tự gọi Telegram API và cấu hình GitHub repo webhook sau khi tạo bot.

**Giải pháp**: Tự động đăng ký webhook khi bot được tạo.

```java
// Trong UpsertBotUseCase pipeline — thêm step:
// RegisterTelegramWebhookStep: gọi POST https://api.telegram.org/bot{token}/setWebhook
// RegisterGitHubWebhookStep: gọi POST https://api.github.com/repos/{owner}/{repo}/hooks
```

**Yêu cầu**: cần GitHub Personal Access Token (PAT) có quyền `admin:repo_hook`.

**Checklist**:
```
☐ Thêm outbound port: WebhookRegistrationGateway
☐ Implement TelegramWebhookRegistrationAdapter (gọi setWebhook API)
☐ Implement GitHubWebhookRegistrationAdapter (gọi GitHub Hooks API, cần PAT)
☐ Thêm config: app.public-url (đã có), app.github.pat (PAT)
☐ Thêm pipeline step trong UpsertBotUseCase
☐ Rollback: nếu đăng ký webhook thất bại → log warning (không rollback bot creation)
☐ Test: mock API calls, verify step được gọi đúng thứ tự
```

---

## 3. Observability (Metrics, Logging, Tracing)

### 3.1 Spring Actuator + Micrometer Metrics

**Thêm dependency**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Metrics cần track**:

| Metric | Type | Mô tả |
|---|---|---|
| `webhook.github.received` | Counter | Số lần nhận GitHub webhook, tag: `bot`, `event` |
| `webhook.github.processed` | Counter | Số webhook xử lý thành công |
| `webhook.github.failed` | Counter | Số webhook xử lý thất bại |
| `broadcast.sent` | Counter | Số tin nhắn Telegram gửi thành công, tag: `bot` |
| `broadcast.failed` | Counter | Số tin nhắn Telegram gửi thất bại |
| `broadcast.retry` | Counter | Số lần retry |
| `broadcast.latency` | Timer | Thời gian gửi 1 tin nhắn |
| `bot.registry.size` | Gauge | Số bot đang enabled trong registry |
| `group.activation.count` | Gauge | Tổng số group activation active |

**Checklist**:
```
☐ Thêm spring-boot-starter-actuator
☐ Expose: /actuator/health, /actuator/metrics, /actuator/info
☐ Inject MeterRegistry vào BroadcastUseCase, TelegramClient, GitHubWebhookProcessor
☐ Custom metrics theo bảng trên
☐ Bảo vệ /actuator bằng admin token (cấu hình management.endpoint.*)
```

---

### 3.2 Structured Logging

**Vấn đề**: Log hiện tại thiếu correlation ID → khó trace một request qua nhiều layer.

**Giải pháp**: Thêm MDC context cho mỗi webhook request.

```java
// Trong GitHubWebhookController (filter/interceptor)
MDC.put("requestId", UUID.randomUUID().toString());
MDC.put("botUsername", botUsername);
MDC.put("githubEvent", eventType);
// ... process ...
MDC.clear();
```

**Log format chuẩn** (JSON structured với Logback):
```xml
<!-- logback-spring.xml -->
<encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
```

**Checklist**:
```
☐ Thêm MDC filter/interceptor cho GitHub webhook và Telegram webhook
☐ Log fields chuẩn: requestId, botUsername, event, chatId, latencyMs
☐ Cấu hình Logback JSON encoder (logstash-logback-encoder)
☐ Log level: INFO cho happy path, WARN cho retry, ERROR cho failure
```

---

### 3.3 Health Check Endpoint

```yaml
# application.yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

**Custom health indicator**:
```java
@Component
class MongoHealthIndicator implements HealthIndicator {
    // ping MongoDB, report UP/DOWN
}

@Component
class BotRegistryHealthIndicator implements HealthIndicator {
    // report số bot enabled, cảnh báo nếu = 0
}
```

**Checklist**:
```
☐ Cấu hình actuator endpoints
☐ Custom health indicator cho MongoDB
☐ Custom health indicator cho BotRegistry (cảnh báo nếu 0 bot)
☐ Bảo vệ /actuator bằng Spring Security hoặc admin token filter
```

---

## 4. Security Hardening

### 4.1 Rate Limiting Admin API

**Vấn đề**: Admin API không có rate limiting → brute-force token.

**Giải pháp**: Bucket4j in-memory rate limiter.

```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
</dependency>
```

```java
// AdminRateLimitFilter: 100 req/min per IP
Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
```

**Checklist**:
```
☐ Thêm Bucket4j dependency
☐ Implement AdminRateLimitFilter (Spring OncePerRequestFilter)
☐ Rate limit per IP: 100 req/min (configurable)
☐ Response 429 khi vượt giới hạn
☐ Test: vượt giới hạn → 429
```

---

### 4.2 Input Validation

**Vấn đề**: Các DTO admin API không có validation annotation → có thể nhận dữ liệu invalid.

**Giải pháp**: Bean Validation.

```java
public record CreateBotRequest(
    @NotBlank @Size(max = 64) String username,
    @NotBlank String token,
    @Pattern(regexp = "[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+") String githubRepo,
    // ...
) {}
```

**Checklist**:
```
☐ Thêm @Valid vào controller method parameters
☐ Thêm validation annotations vào tất cả request DTOs
☐ GlobalExceptionHandler: xử lý MethodArgumentNotValidException → 400 + validation errors
☐ Test: invalid input → 400 với error message rõ ràng
```

---

### 4.3 Secret Rotation Support

**Vấn đề**: Hiện tại admin token là static. Không có cơ chế rotate mà không restart app.

**Giải pháp tối thiểu**: Hỗ trợ 2 admin token song song (current + new) trong giai đoạn chuyển tiếp.

```yaml
app:
  admin:
    token: ${ADMIN_TOKEN}
    token-secondary: ${ADMIN_TOKEN_SECONDARY:}  # optional
```

**Checklist**:
```
☐ AdminAccessGuard hỗ trợ cả primary và secondary token
☐ Log warning khi secondary token được dùng (nhắc nhở rotate xong)
☐ Docs: hướng dẫn quy trình rotate admin token
```

---

## 5. Feature Expansion

### 5.1 Multi-Repo per Bot

**Vấn đề**: MVP: 1 bot = 1 repo. Nhiều team muốn 1 bot theo dõi nhiều repo.

**Thay đổi domain**:
```
managed_bots.githubRepo (String)
→ managed_bots.githubRepos (List<String>)
```

**Thay đổi webhook URL**: `/github/webhook/{botUsername}/{repoAlias}` hoặc giữ nguyên URL, match theo payload.

**Checklist**:
```
☐ Đổi ManagedBot.githubRepo → githubRepos: List<String>
☐ Migration: script chuyển document cũ sang list 1 phần tử
☐ BotDocument: đổi field githubRepo → githubRepos
☐ Webhook verification: tìm secret theo repo
☐ Cập nhật API docs và validation
☐ Test: bot với nhiều repo nhận đúng event
```

---

### 5.2 Notification Filter per Group

**Vấn đề**: MVP gửi tất cả event type vào tất cả group. Team muốn chọn event: group A chỉ nhận `push`, group B nhận `pull_request` + `release`.

**Thay đổi domain**:
```
group_activations thêm field: allowedEvents: Set<String>  (null = all)
```

**Checklist**:
```
☐ Thêm allowedEvents vào GroupActivation record và GroupActivationDocument
☐ BroadcastUseCase: filter group theo allowedEvents trước khi gửi
☐ Telegram command: /filter push,release → set allowedEvents cho group hiện tại
☐ /status command: hiển thị danh sách event filter
☐ Admin API: hỗ trợ set allowedEvents qua REST
☐ Test: group có filter chỉ nhận đúng event được chỉ định
```

---

### 5.3 Web UI Quản Trị

**Vấn đề**: Admin phải dùng `curl` để quản lý bot — không thân thiện với người dùng không kỹ thuật.

**Giải pháp**: Single-page app đơn giản (Vanilla HTML/JS) phục vụ từ Spring Boot.

**Tính năng tối thiểu**:
- Danh sách bot (enabled/disabled toggle)
- Form tạo/sửa bot
- Danh sách group activations per bot
- Xem trạng thái health

**Checklist**:
```
☐ Tạo telegrambots-web/src/main/resources/static/admin/index.html
☐ JavaScript gọi Admin REST API với X-Admin-Token header
☐ Bảo vệ /admin/ui/** (redirect về login page nếu chưa auth)
☐ Simple token-based session trong localStorage (không cần backend session)
☐ Responsive, mobile-friendly
```

---

### 5.4 Telegram Command: /filter và /events

**Thêm commands** (liên quan đến 5.2):

| Command | Mô tả |
|---|---|
| `/filter push release` | Chỉ nhận push và release event trong group này |
| `/filter all` | Nhận tất cả event (mặc định) |
| `/events` | Hiển thị danh sách event type đang được filter |

**Checklist**:
```
☐ FilterCommand: parse event names từ args, validate, gọi GroupBotActivationService
☐ EventsCommand: hiển thị allowedEvents hiện tại của group
☐ GroupBotActivationService: thêm method updateEventFilter(botUsername, chatId, events)
☐ Test: /filter push → /events hiển thị đúng
```

---

### 5.5 Bot Status Dashboard Command

**Mở rộng `/status` command** (hiện tại chỉ hiển thị basic info):

```
📊 Status @mybot
━━━━━━━━━━━━━━━━━━
📦 Repo: owner/repo
👥 Groups active: 5
📨 Messages sent today: 142
⚡ Last event: push (2 min ago)
✅ Webhook: OK
```

**Checklist**:
```
☐ Thêm BotStatisticsRepository (đọc từ metrics hoặc collection riêng)
☐ Cập nhật StatusCommand để hiển thị thêm thông tin
☐ Cache stats 60 giây để tránh query nặng mỗi lần /status
```

---

## 6. Testing & Quality

### 6.1 Integration Tests với Testcontainers

**Vấn đề**: Unit tests mock MongoDB → không phát hiện lỗi mapping, index, query.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mongodb</artifactId>
    <scope>test</scope>
</dependency>
```

**Checklist**:
```
☐ Thêm testcontainers dependencies
☐ AbstractIntegrationTest base class với @Testcontainers + MongoDBContainer
☐ BotRepositoryIT: test CRUD thật với MongoDB
☐ ActivationRepositoryIT: test compound unique index
☐ AdminControllerIT: test full request → response với real MongoDB
☐ GitHub/TelegramWebhookIT: test pipeline end-to-end (mock Telegram API)
```

---

### 6.2 Contract Tests (Spring Cloud Contract)

**Vấn đề**: Không có contract test → thay đổi API response có thể break client.

**Giải pháp**: Spring Cloud Contract cho Admin REST API.

**Checklist**:
```
☐ Thêm spring-cloud-starter-contract-verifier
☐ Viết contract groovy/yaml cho mỗi Admin API endpoint
☐ Contract test chạy trong CI pipeline
```

---

### 6.3 Mutation Testing (PIT)

**Mục tiêu**: Đảm bảo test suite thực sự kiểm tra logic, không chỉ coverage.

```xml
<!-- PIT Mutation Testing -->
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
</plugin>
```

**Checklist**:
```
☐ Thêm PIT plugin vào parent pom.xml
☐ Chạy: ./mvnw org.pitest:pitest-maven:mutationCoverage
☐ Target: mutation score ≥ 70% cho domain và application modules
☐ Loại trừ: infrastructure adapters (khó test mutation cho I/O)
```

---

### 6.4 Architecture Tests (ArchUnit)

**Mục tiêu**: Enforce Clean Architecture dependency rules tự động trong CI.

```java
@ArchTest
static final ArchRule domainShouldNotDependOnSpring =
    noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("org.springframework..");
```

**Checklist**:
```
☐ Thêm ArchUnit dependency vào telegrambots-app/test scope
☐ Viết rules: domain không import Spring, application không import infrastructure
☐ Rules cho naming convention: *Controller, *Repository, *Formatter, *Command
☐ Chạy trong CI (fail build nếu vi phạm)
```

---

## 7. DevOps & Infra

### 7.1 CI/CD Pipeline (GitHub Actions)

**Checklist**:
```
☐ .github/workflows/ci.yml:
    - Trigger: push to main, PR to main
    - Jobs: build → test → docker build
    - Cache: ~/.m2 Maven cache
    - Secrets: không commit vào repo

☐ .github/workflows/cd.yml:
    - Trigger: push tag v*
    - Build Docker image
    - Push lên GitHub Container Registry (ghcr.io)
    - Deploy (tùy môi trường: SSH deploy, Railway, Fly.io...)
```

**Template CI job**:
```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - uses: actions/cache@v4
        with: { path: ~/.m2, key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }} }
      - run: ./mvnw clean verify
```

---

### 7.2 Docker Multi-instance với Health Check

**Cải tiến Dockerfile**:
```dockerfile
# Thêm HEALTHCHECK
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:${PORT:-8080}/actuator/health || exit 1
```

**Cải tiến docker-compose.yml**:
```yaml
services:
  app:
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    restart: unless-stopped
```

**Checklist**:
```
☐ Thêm HEALTHCHECK vào Dockerfile
☐ Cập nhật docker-compose.yml với health check và restart policy
☐ Thêm depends_on condition cho MongoDB health
☐ Test: kill MongoDB → app health = DOWN; restore → app health = UP
```

---

### 7.3 Environment-specific Configuration

**Profiles**: `dev`, `staging`, `prod`.

```yaml
# application-prod.yaml
spring:
  data:
    mongodb:
      auto-index-creation: false  # prod: không auto tạo index
logging:
  level:
    root: WARN
    com.lede.telegrambots: INFO
```

**Checklist**:
```
☐ Tạo application-dev.yaml (verbose logging, auto-index: true)
☐ Tạo application-prod.yaml (minimal logging, tắt auto-index)
☐ Tạo application-staging.yaml
☐ Document cách set profile: SPRING_PROFILES_ACTIVE=prod
```

---

### 7.4 MongoDB Index Management

**Vấn đề**: `spring.data.mongodb.auto-index-creation: true` không nên dùng trên production (blocking scan).

**Giải pháp**: Script tạo index tường minh.

```javascript
// scripts/mongo-init.js
db.managed_bots.createIndex({ username: 1 }, { unique: true });
db.managed_bots.createIndex({ enabled: 1 });
db.group_activations.createIndex({ botUsername: 1, chatId: 1 }, { unique: true });
db.group_activations.createIndex({ botId: 1 });
db.group_activations.createIndex({ active: 1 });
```

**Checklist**:
```
☐ Tạo scripts/mongo-init.js với tất cả indexes
☐ Mount vào MongoDB container trong docker-compose (init script)
☐ Tắt auto-index-creation trong application-prod.yaml
☐ Docs: hướng dẫn chạy index script trên MongoDB Atlas
```

---

## 8. Priority Matrix

Xếp theo **Impact × Effort** (Impact: 1-5, Effort: 1-5, Score = Impact/Effort).

| # | Task | Impact | Effort | Score | Sprint |
|---|---|:---:|:---:|:---:|:---:|
| 1 | **Async Broadcast** (1.1) | 5 | 2 | **2.50** | Sprint 1 |
| 2 | **Telegram 429 Handling** (1.3) | 5 | 2 | **2.50** | Sprint 1 |
| 3 | **Input Validation** (4.2) | 3 | 1 | **3.00** | Sprint 1 |
| 4 | **Health Check** (3.3) | 3 | 1 | **3.00** | Sprint 1 |
| 5 | **Docker Health Check** (7.2) | 3 | 1 | **3.00** | Sprint 1 |
| 6 | **Structured Logging + MDC** (3.2) | 4 | 2 | **2.00** | Sprint 1 |
| 7 | **Parallel Fan-out** (1.2) | 4 | 2 | **2.00** | Sprint 1 |
| 8 | **Spring Actuator + Metrics** (3.1) | 4 | 2 | **2.00** | Sprint 2 |
| 9 | **CI/CD GitHub Actions** (7.1) | 4 | 2 | **2.00** | Sprint 2 |
| 10 | **ArchUnit Tests** (6.4) | 2 | 1 | **2.00** | Sprint 2 |
| 11 | **Bot Registry Refresh** (1.4) | 2 | 1 | **2.00** | Sprint 2 |
| 12 | **Rate Limiting Admin API** (4.1) | 3 | 2 | **1.50** | Sprint 2 |
| 13 | **MongoDB Index Script** (7.4) | 3 | 2 | **1.50** | Sprint 2 |
| 14 | **Integration Tests Testcontainers** (6.1) | 4 | 3 | **1.33** | Sprint 2 |
| 15 | **Webhook Auto-Registration** (2.3) | 4 | 3 | **1.33** | Sprint 2 |
| 16 | **Notification Filter per Group** (5.2) | 4 | 3 | **1.33** | Sprint 3 |
| 17 | **Multi-Repo per Bot** (5.1) | 3 | 3 | **1.00** | Sprint 3 |
| 18 | **Secret Rotation** (4.3) | 2 | 2 | **1.00** | Sprint 3 |
| 19 | **Web UI Admin** (5.3) | 3 | 4 | **0.75** | Sprint 4 |
| 20 | **Mutation Testing PIT** (6.3) | 2 | 3 | **0.67** | Sprint 4 |

---

### Sprint Plan (đề xuất)

```
Sprint 1 (1-2 tuần) — Critical Fixes & Foundation
  → Async Broadcast, 429 Handling, Parallel Fan-out
  → Input Validation, Health Check, Structured Logging
  → Docker Health Check

Sprint 2 (2-3 tuần) — Observability & Reliability
  → Spring Actuator + Metrics
  → CI/CD GitHub Actions
  → Rate Limiting Admin API
  → Integration Tests Testcontainers
  → Webhook Auto-Registration
  → MongoDB Index Script, ArchUnit, Registry Refresh

Sprint 3 (2-3 tuần) — Feature Expansion
  → Notification Filter per Group (/filter, /events commands)
  → Multi-Repo per Bot
  → Secret Rotation Support

Sprint 4 (3-4 tuần) — UX & Quality
  → Web UI Admin
  → Mutation Testing (PIT)
  → Contract Tests
  → Bot Status Dashboard
```

---

> **Ghi chú**: Mỗi task nên có branch riêng, PR với checklist đã tick xong, và CI xanh trước khi merge vào `main`.  
> Xem thêm: [async-broadcast-proposal.md](../architecture/async-broadcast-proposal.md) · [mvp-summary.md](mvp-summary.md) · [architecture/overview.md](../architecture/overview.md)
