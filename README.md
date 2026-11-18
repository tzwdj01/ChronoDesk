# ChronoDesk - 极简科技风 Android 智能桌面时钟

一款专为开发者与极客打造的 Android 智能桌面时钟应用。集成了精致的机械翻页时钟、实时天气气象，以及**双 ChatGPT / Codex 账号订阅额度实时监控看板**。

本项目深度参考了桌面端优秀开源工具 [CC Switch](https://github.com/farion1231/cc-switch) 的 Codex OAuth 认证、Token 刷新、用量模型与 Keep-Last-Good 缓存降级机制，并针对 Android 移动端原生系统特性进行了完备的安全与工程适配。

---

## ✨ 核心特性

### 1. 极简科技桌面时钟
- **精致翻页动画 (Split-Flap Animation)**：小时与分钟切换时呈现顺滑的 3D 机械折叠翻牌动画，质感细腻。
- **全屏沉浸与常亮**：支持全屏无干扰展示，默认开启 `FLAG_KEEP_SCREEN_ON`，适合放置在桌面支架长期运行。
- **夜间自动调暗 (Night Dim)**：默认在 22:00 至次日 07:00 自动切换为超低对比度暗光黑阶，并通过 Window 属性直接降低本应用窗口亮度（不修改系统全局亮度），伴有纯黑背景护眼。
- **AMOLED 防烧屏保护 (Burn-In Shift)**：每隔 3 分钟自动微调主时钟字符偏移（1~2 像素），杜绝 OLED 屏幕因长时间固定像素点亮导致的残影烧屏。
- **横竖屏自适应**：
  - **竖屏模式**：大翻牌时钟居顶，日期与天气居中，双账号额度卡片排列在下方。
  - **横屏模式**：左侧超大翻页时钟与日期星期，右侧并列天气与双账号监控面板。

### 2. 实时天气与气象看板
- 集成 Open-Meteo 开放天气数据源，无需依赖商业 API Key。
- 支持实时气温、体感温度、天气状况图标与文字、空气湿度、风速。
- 支持预设热门城市（北京、上海、广州、深圳、成都、杭州、东京、纽约等）。
- 30 分钟智能缓存，网络离线时自动保留上次成功天气数据。

### 3. 双 ChatGPT / Codex 账号额度监控
- **双账号独立共存**：支持同时添加并维护两个独立的 ChatGPT / Codex 账号（如个人账号与工作账号）。
- **细粒度用量监控**：
  - **5 小时限额窗口 (Primary Window)**：显示剩余使用百分比（%）、进度条与重置倒计时（如 `3小时15分`）。
  - **周额度窗口 (Secondary Window)**：显示 7 天周用量剩余比例与重置倒计时。
  - **套餐与余额显示**：自动识别 `Plus`、`Pro 200`、`Business` 等套餐标识。
- **多途径安全接入**：
  - **官方设备码授权 (Device Code Flow)**：利用 Chrome Custom Tabs 打开 OpenAI 官方设备验证页 `https://auth.openai.com/codex/device`，用户在官方网页确认即可自动换取凭据，无需暴露密码或 Cookie。
  - **直接导入 Token (Direct Token)**：允许高级用户直接粘贴已有 Access Token / Refresh Token。
  - **高保真模拟账号 (Mock Provider)**：内置 100% 吻合上游结构的用量模拟器，零门槛进行功能验证与 UI 交互体验。
- **刷新调度与 Keep-Last-Good 容错**：
  - 支持单账号刷新、全部统一刷新，以及 5/10/15/30/60 分钟自动定时轮询。
  - 深度复刻 CC Switch 的 **Keep-Last-Good** 策略：遇到网络抖动、超时或上游 5xx 时，保留最后一次成功获取的额度快照，避免卡片闪烁或界面空白。
  - 凭据永久失效（如 HTTP 401 / `invalid_grant`）时精准提示“需重新登录”，且一个账号失效绝对不影响另一个账号。

### 4. 极致安全性
- **硬件级安全加密**：敏感凭据（Token / RefreshToken）通过 `Android Keystore` 生成主密钥，使用 `EncryptedSharedPreferences` 进行 AES-256-GCM 硬件级密文持久化。
- **日志全脱敏 (Redaction)**：所有网络日志中的 Authorization 头与 Token 字符串强制自动打码（如 `eyJ***`），严禁敏感信息打印至 Logcat。
- **零第三方中转**：所有 OAuth 与额度查询直接与 OpenAI 官方服务端（`auth.openai.com` 与 `chatgpt.com`）通信，杜绝中间人风险。

---

## 🛠️ 技术架构

- **语言**: 100% Kotlin
- **UI 框架**: Jetpack Compose + Material 3 (响应式声明式 UI)
- **架构模式**: MVVM (Model-View-ViewModel) + 单向数据流 (UDF)
- **异步与状态管理**: Kotlin Coroutines + StateFlow / SharedFlow
- **网络层**: Retrofit 2 + OkHttp 3 + Gson + 自定义脱敏拦截器
- **安全与存储**: Android Keystore + AndroidX Security Crypto (`EncryptedSharedPreferences`) + SharedPreferences
- **系统集成**: AndroidX Browser (Chrome Custom Tabs) + WindowManager (Keep-Screen-On & Brightness Override)

```
app/src/main/java/com/chronodesk/app/
├── MainActivity.kt                 # 入口 Activity、全屏设置、屏幕常亮与夜间亮度控制
├── ChronoDeskApp.kt                # Application 单例
├── data/
│   ├── model/                      # 核心数据模型 (CodexAccount, UsageSnapshot, WeatherInfo...)
│   ├── network/                    # Retrofit 接口、OkHttp 客户端、AuthLoggingInterceptor
│   ├── provider/                   # Codex 数据源抽象 (Real Provider, Mock Provider)
│   ├── repository/                 # 数据仓库 (AccountRepo, WeatherRepo, SettingsRepo)
│   └── security/                   # 安全存储 (CredentialStorage, TokenRedactor)
└── ui/
    ├── clock/                      # 主界面、FlipDigit 翻页动画、WeatherCard、CodexQuotaCard
    ├── settings/                   # 账号管理 Dialog、设备码授权 Dialog、系统设置 Dialog
    └── theme/                      # 极简暗黑科技风配色与排版
```

---

## 📦 构建与安装

### 运行环境要求
- OpenJDK 17 LTS
- Android SDK 34 (Android 14)
- Gradle 8.7

### 1. 运行自动化单元测试
```bash
./gradlew testDebugUnitTest
```
测试套件覆盖：
- 5 小时与周额度窗口解析、已用转剩余百分比换算、0%已用抑制虚假时间戳。
- Token 与敏感 JSON 日志脱敏验证。
- 跨午夜夜间模式区间计算。
- 双账号物理隔离与 Keep-Last-Good 降级策略。

### 2. 构建 Debug APK
```bash
./gradlew assembleDebug
```
产物输出路径：
```
app/build/outputs/apk/debug/app-debug.apk
```

### 3. 安装到 Android 设备
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.chronodesk.app/.MainActivity
```

---

## 📚 详细设计文档

- [CC Switch 源码研究技术报告](docs/CC_SWITCH_RESEARCH.md)
- [凭据与网络安全设计规范](docs/AUTH_SECURITY.md)
- [Android 构建与部署指南](docs/ANDROID_BUILD.md)
- [已知限制与技术风险说明](docs/KNOWN_LIMITATIONS.md)

---

## 📄 开源许可证

本项目基于 MIT License 开源。
涉及 CC Switch 代码设计与逻辑部分，版权归属原作者 (Copyright (c) 2025 Jason Young)。
