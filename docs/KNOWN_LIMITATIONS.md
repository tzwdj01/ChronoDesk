# ChronoDesk 已知限制与技术风险说明 (KNOWN_LIMITATIONS.md)

## 一、上游协议性质与变更风险

1. **Codex 用量端点是非官方公开 SLA 接口**:
   - 端点 `https://chatgpt.com/backend-api/wham/usage` 及 `https://chatgpt.com/backend-api/wham/rate-limit-reset-credits` 均属于 OpenAI 为 Codex CLI 原生客户端提供的内部微服务，并未纳入官方公开的第三方开发者 REST API 规范。
   - 上游有随时重构字段格式、更动验证逻辑甚至下线端点的可能性。
   - **应对措施**: ChronoDesk 采用宽松的容错解析器（宽松的反序列化、字段均为可选、空值兜底映射），并提供 Mock Provider 确保在接口发生异动时不引起应用闪退。

2. **客户端凭证 (Client ID) 策略风险**:
   - `CODEX_CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"` 原为 OpenAI 官方 Codex CLI 所使用的公开 Client ID。
   - 移动端虽采用标准 Device Code 流程（用户在官方验证页面 `https://auth.openai.com/codex/device` 登录），但若 OpenAI 调整策略限制第三方 IP 轮询或收紧设备码授权，可能导致授权失败。
   - **应对措施**: ChronoDesk 支持“直接凭据导入 (Direct Token Import)”，用户可将自己的 Token / Session 直接填入，绕过浏览器授权环节。

---

## 二、网络与风控限制

1. **Cloudflare WAF 与人机验证**:
   - 当手机处于数据中心 IP、共享公共代理或高风险节点时，上游服务可能触发 Cloudflare Turnstile 或验证码拦截，返回 HTTP 403。
   - 根据安全准则，ChronoDesk 坚决不使用任何破解或自动化脚本绕过人机挑战。
   - **应对措施**: 当遇到 HTTP 403 / 429 时，状态标记为“网络风控或重试中”，保留上一次成功抓取的数据（Keep-Last-Good），提示用户检查当前网络代理或切换 Wi-Fi。

2. **区域网络访问性**:
   - OpenAI 官方域名在中国大陆直连通常受限。用户需要在路由器端或手机系统级开启合规代理。
   - ChronoDesk 原生网络层支持读取系统代理配置，但不内置任何第三方中转服务以确保凭证绝对安全。

---

## 三、Android 硬件与系统限制

1. **AMOLED 屏幕防烧屏 (Burn-In)**:
   - 智能桌面时钟通常需要 24 小时长时间点亮。固定位置的超大高对比度数字时钟在 OLED / AMOLED 屏幕上存在烧屏隐患。
   - **应对措施**:
     - 内置微幅像素漂移机制（Pixel Shifter）：每隔数分钟使主时钟数字产生 1~3 像素的随机微偏移。
     - 夜间模式（默认 22:00 ~ 07:00）自动切换为纯黑深空背景，仅保留微弱暗光灰度字符，并降低窗口亮度（Window Attributes Brightness）。

2. **后台执行限制 (Doze Mode)**:
   - 当应用退到后台或息屏时，Android 系统的 Doze 机制和后台电源管理会冻结网络请求和短间隔定时器。
   - ChronoDesk 定位为**桌面时钟状态展示面板**，通过 `FLAG_KEEP_SCREEN_ON` 保持前台运行。在前台时按用户配置精准刷新（默认 10 分钟），退到后台时自动挂起网络请求以节省电量。

---

## 四、天气数据源限制

1. **Open-Meteo 接口限制**:
   - 天气采用公开可访问且无需 API Key 的 Open-Meteo 开放数据。
   - Open-Meteo 免费端点对高频并发有每分钟/每天请求上限。
   - **应对措施**: ChronoDesk 将天气更新周期设为 30 分钟一次，并在无网络或请求失败时保留本地天气缓存。支持通过经纬度/城市名预设配置，无须强制索取高精度地理位置权限。
