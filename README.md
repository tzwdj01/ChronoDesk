# ChronoDesk - 极简科技风 Android 智能桌面时钟

一款专为开发者与极客打造的 Android 智能桌面时钟应用。集成了精致的机械翻牌时钟、公历农历天气气象、**水墨日间 / 赛博夜间双主题**，以及 **ChatGPT (Codex) / DeepSeek 多模型订阅额度实时聚合监控看板**。

本项目深度参考了桌面端优秀开源工具 [CC Switch](https://github.com/farion1231/cc-switch) 的 Codex OAuth 认证、Token 刷新、用量模型与 Keep-Last-Good 缓存降级机制，并针对 Android 移动端原生系统特性进行了完备的安全与工程适配。

---

## ✨ 核心特性

### 1. 水墨日间 / 赛博夜间双主题
- **水墨日间模式 (Ink Day Theme)**：
  - 采用米白柔和底色、温润宣纸质感与淡雅山水水墨底纹，呈现媲美电子墨水屏（E-ink）的宁静视觉体验。
  - 墨黑翻牌时钟、水墨渐变进度条，高对比度且长时间注视不刺眼。
- **暗黑赛博夜间模式 (Cyber Night Theme)**：
  - 极致深黑（AMOLED Pure Black）背景，搭配精致的荧光青（Cyan / Teal）科技线条与半透明磨砂玻璃卡片。
  - 纯黑黑阶深度省电，有效防止夜间强光刺眼。
- **自动/手动无缝切换**：
  - 支持顶部快捷键一键秒切，同时内置时间感应逻辑，夜间自动平滑过渡。

### 2. 精致翻页时钟与万年历
- **3D 机械折叠翻牌动画 (Split-Flap Animation)**：
  - 大尺寸时、分翻页数字，细腻的机械折页中缝阴影与反光细节，秒数精致紧凑悬浮。
- **完整日期与农历历法**：
  - 完整呈现公历日期（如 `2026年11月19日`）、星期（如 `星期四`）。
  - 内置精准中华传统农历算法，实时显示农历月日（如 `农历十月初十`）。
- **全屏沉浸与 AMOLED 防烧屏保护**：
  - 默认全屏沉浸模式与屏幕常亮（`FLAG_KEEP_SCREEN_ON`）。
  - 智能防烧屏位移（Burn-In Shift）：每隔 3 分钟自动微调像素位置，守护 OLED 屏幕寿命。

### 3. 多模型统一聚合额度看板 (Unified Provider Cards)
- **同提供商聚合展示 (Same Provider Grouping)**：
  - 相同订阅商（如 ChatGPT Plus 个人账号与工作账号）统一聚合在专属框体内，排版规整紧凑，消除无效留白。
- **多平台模型扩展**：
  - **ChatGPT / Codex**：深度适配官方 WHAM 额度接口，精准区分并同时展示 **5 小时限额窗口** 与 **7 天周额度窗口**。
  - **DeepSeek**：原生预留并接入 DeepSeek 模型平台额度、账户余额与调用状态看板。
- **真实额度精准解析与告警徽标**：
  - 彻底杜绝虚假静态占位，实时解析 `limit_reached` 状态与重置倒计时。
  - 状态胶囊徽标动态变色：
    - 🟢 **正常 (Normal)**：剩余额度充裕。
    - 🟠 **即将耗尽 (Warning)**：剩余额度 ≤ 15%，触发预警提示。
    - 🔴 **额度耗尽 (Exhausted)**：额度达到上限（0% 或 limit_reached），进度条变红并标注倒计时。

### 4. 横向滑屏双页面系统 (Two-Page Navigation Flow)
- **第一页（主监控页）**：
  - 占据视觉核心的大尺寸机械翻页时钟、公历农历天气气象、右侧 AI 额度监控看板。
- **第二页（扩展与用量遥测页）**：
  - **Token 用量实时遥测**：基于剩余额度动态估算 5 小时与周额度消耗的 Token 数量（如 `38.4k / 40.0k Tokens`）。
  - **API 接口延时与可用性状态**：实时监控各模型提供商网络往返延迟（RTT Latency）及连通健康度。
  - 预留未来模型接口管理与高级调试入口。
- **底部优雅双圆点指示器**：支持左右手势平滑横向滑动切换，指示清晰。

### 5. 实时天气与气象看板
- 集成 Open-Meteo 开放天气数据源，无需依赖商业 API Key。
- 支持实时气温、天气状况、空气湿度、风速。
- 30 分钟智能缓存与 Keep-Last-Good 策略，离线无感展示。

### 6. 智能网络与代理自适应
- **智能代理自动路由 (Smart Proxy Routing)**：针对国内网络环境，客户端优先尝试通过本地反向代理（如 `127.0.0.1:1082` / `7890`）直连 OpenAI 官方接口；遇到网络变化时无缝平滑回退，保障额度同步零阻塞。
- **硬件级安全加密**：敏感凭据（Token / RefreshToken）通过 `Android Keystore` 生成主密钥，使用 `EncryptedSharedPreferences` 进行 AES-256-GCM 硬件级密文持久化。
- **日志全脱敏 (Redaction)**：所有网络日志与凭据自动打码，杜绝泄露风险。

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
