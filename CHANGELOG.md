# ChronoDesk 更新日志 (CHANGELOG)

## [1.0.0] - 2026-11-19

### 新增功能
- **桌面智能时钟**：
  - 基于 Jetpack Compose 打造的极简科技风全屏数字时钟。
  - 实现小时与分钟物理质感 3D 机械翻页折叠动画 (`FlipDigit`)。
  - 支持 24/12 小时制切换、实时秒钟计数展示。
  - 全屏沉浸式体验与屏幕常亮支持 (`FLAG_KEEP_SCREEN_ON`)。
  - 自动夜间模式支持（默认 22:00 ~ 07:00）：自动降低应用窗口亮度，采用 OLED 纯黑背景防眩光。
  - AMOLED 屏幕防烧屏保护机制（每隔 3 分钟自动实施 1~2 像素无感漂移）。
  - 横屏与竖屏响应式布局自适应切换。
- **实时天气看板**：
  - 接入 Open-Meteo 免费开放气象接口，无须第三方 API Key。
  - 展示实时温度、体感温度、天气状况图标与描述、相对湿度与风速。
  - 支持主流城市预设选择与 30 分钟离线容错缓存。
- **双 ChatGPT / Codex 账号额度监控**：
  - 深度参考并复刻桌面端 [CC Switch](https://github.com/farion1231/cc-switch) 的 Codex OAuth 订阅额度查询模型。
  - 完整支持两个账号独立配置、独立刷新与隔离管理。
  - 展示 5 小时限额窗口与周限额窗口的剩余百分比、进度条与重置倒计时。
  - 支持官方设备码授权流程 (RFC 8628 Device Code Flow) + Chrome Custom Tabs。
  - 支持直接凭据导入 (Direct Token) 与高保真离线模拟账号 (Mock Provider)。
  - 实现 Keep-Last-Good 容错机制：网络抖动或上游 5xx 时自动保留上一次成功数据。
  - 实现单账号刷新与全局全部刷新支持。
- **安全与存储架构**：
  - 基于 Android Keystore + AndroidX Security Crypto (`EncryptedSharedPreferences`) 实现硬件级 AES-256-GCM 凭据加密。
  - 定制 OkHttp `AuthLoggingInterceptor`，对 Authorization 请求头与响应体敏感凭据实施强制脱敏。
  - 零自建服务器，全链路直连 OpenAI 官方服务端。
- **工程与文档交付**：
  - 交付 `docs/CC_SWITCH_RESEARCH.md` 深度研究报告。
  - 交付 `docs/AUTH_SECURITY.md` 安全规范报告。
  - 交付 `docs/ANDROID_BUILD.md` 构建与部署指南。
  - 交付 `docs/KNOWN_LIMITATIONS.md` 已知限制与技术风险说明。
  - 交付自动化单元测试套件。
