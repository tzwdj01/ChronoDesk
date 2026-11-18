# ChronoDesk 认证与凭据安全设计规范 (AUTH_SECURITY.md)

## 一、核心安全原则

ChronoDesk 面向极简、高安全、自用场景设计。在处理敏感凭据（如 OpenAI / ChatGPT OAuth Access Token、Refresh Token、ID Token）时，坚决遵守以下安全红线：

1. **严禁明文存储**：严禁将 Token、RefreshToken 或 ID Token 以明文形式存储在普通 SharedPreferences、数据库、SD 卡或应用普通文本文件中。
2. **严禁在日志中输出凭据**：所有网络拦截器、调试日志、崩溃堆栈必须实施严格的脱敏（Token Redaction）。日志中仅能出现 Account ID 或邮箱前缀，敏感字符串一律替换为 `***`。
3. **严禁上传第三方中转服务器**：所有 OAuth 交互与额度查询均直接由 Android 客户端连接 OpenAI / ChatGPT 官方端点（`auth.openai.com` 与 `chatgpt.com`），绝不部署、也不经过任何未经认证的第三方后端服务器。
4. **严禁硬编码敏感凭据**：APK 源码或打包二进制中绝不嵌入任何真实用户 Token 或凭证。
5. **严禁多账号凭据串号**：两个账号使用完全隔离的独立物理键存储与锁机制，一个账号失效或被删除绝不连累另一个账号。

---

## 二、Android 端安全存储方案 (Android Keystore + EncryptedSharedPreferences)

ChronoDesk 采用 Google 官方 `androidx.security:security-crypto` 库，基于硬件级 **Android Keystore System** 进行凭据持久化：

```mermaid
flowchart TD
    App[ChronoDesk 认证层] --> MasterKey[MasterKey (Android Keystore)]
    MasterKey --> AES[AES-256-GCM 硬件加密]
    AES --> EncryptedPrefs[EncryptedSharedPreferences]
    EncryptedPrefs --> Storage[(私有存储目录: /data/data/com.chronodesk.app/shared_prefs)]
```

- **主密钥 (MasterKey)**: 使用 `MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()` 在安全硬件芯片 (TEE / StrongBox) 中生成与保护。
- **凭据文件加密**:
  - Key 键名加密：`PrefKeyEncryptionScheme.AES256_SIV`
  - Value 凭价值加密：`PrefValueEncryptionScheme.AES256_GCM`
- **账号删除彻底销毁**:
  当用户主动执行“退出登录”或“删除账号”时，立即从 `EncryptedSharedPreferences` 中物理移除其 `account_{id}_refresh_token`、`account_{id}_access_token` 等全部条目，并在内存 StateFlow 中同步重置为未登录状态。

---

## 三、网络通信安全与防泄漏

1. **网络层 OkHttpClient 统一拦截**:
   - 定制 `AuthLoggingInterceptor`：自动匹配 `Authorization: Bearer <token>` 请求头，在日志输出时截断脱敏为 `Bearer eyJ***...`。
   - 响应体中的 `access_token`、`refresh_token` 字段在进入日志前统一打码。
2. **TLS 传输安全**:
   - 强制使用 HTTPS (TLS 1.2 / 1.3)，禁止明文 HTTP 传输认证与额度数据。
   - 遵循系统代理配置，支持用户在需要科学上网的环境下配置系统 VPN 或局域网代理，但 App 本身不内置任何未经审计的代理逻辑。

---

## 四、双账号隔离机制

1. **唯一账号标识符 (`accountId`)**:
   每个添加的账号由客户端生成唯一的 `UUID` 字符串作为主键。
2. **并发锁与生命周期隔离**:
   在 Kotlin 协程层，为每个账号分配独立的 `Mutex` 刷新锁。
   - 账号 A 在刷新 Token 时，不会阻塞账号 B。
   - 账号 A 遇到 `invalid_grant`（永久失效）时，仅标记账号 A 的状态为 `CredentialStatus.Expired`，账号 B 仍然持续正常同步额度。
3. **数据缓存隔离**:
   - `CodexUsageSnapshot` 按照 `accountId` 作为主键分别存放在内存 `MutableStateFlow<Map<String, CodexUsageSnapshot>>`。
   - UI 组件按照自身的 `slotIndex`（Slot A / Slot B）严格绑定对应的 `accountId`，彻底杜绝数据交叉混淆。

---

## 五、认证方式兼容性与安全退避

根据 CC Switch 的研究，OpenAI 的 Codex CLI Client ID (`app_EMoamEEZ73f0CkXaXp7hrann`) 并非通用第三方公用 Client ID。为保证用户自用的合法性与灵活性，ChronoDesk 设计了三大 Provider 适配层：

1. **Device Code OAuth Provider (`DeviceCodeOAuthProvider`)**:
   - 遵循 RFC 8628 OAuth 2.0 Device Authorization Grant。
   - 使用系统 Chrome Custom Tabs 或外部浏览器打开 `https://auth.openai.com/codex/device`，由用户在官方网页输入或确认 `user_code`。
   - 绝对不使用 WebView 劫持页面、注入 JS 或盗取 Cookie。
2. **Direct Token Provider (`DirectTokenProvider`)**:
   - 允许高级用户直接将已在合规环境下获取的 Access Token / Refresh Token 粘贴输入。
   - 适合对自动化授权流程有顾虑、或处于特殊网络限制环境下的用户。
3. **Mock Provider (`MockCodexProvider`)**:
   - 供无账号、离线演示、自动化单元测试使用，输出结构与真实 `wham/usage` 100% 吻合的高保真模拟数据，保障项目在任何环境下均可独立验证。

---

## 六、Token 刷新与错误退避机制

- **提前刷新缓冲**: 在 `expires_at` 到期前 60 秒触发自动刷新，避免请求发出时 Token 刚好过期。
- **永久性失败判定**:
  当刷新接口返回 HTTP 401 或错误码包含 `invalid_grant`、`refresh_token_expired` 时，停止重试，状态置为 `EXPIRED`，UI 提示“登录已过期，请重新登录”。
- **瞬时性失败判定**:
  遇到网络抖动、HTTP 429 (Rate Limit)、HTTP 5xx 时，保留上一次成功的额度快照（Keep-Last-Good 策略），并在下次定时周期或手动触发时再重试，防止由于网络波动引起界面频繁报错或闪烁。
