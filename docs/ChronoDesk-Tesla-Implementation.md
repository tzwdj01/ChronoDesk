# ChronoDesk · Tesla 车辆监控页实施方案

版本：V1.0 / 设计与开发任务书

## 1. 目标和范围

在 tzwdj01/ChronoDesk 中新增可配置的第三页 Tesla 车辆监控页，保持原时钟页和 AI 遥测页不变；沿用 Kotlin、Jetpack Compose、Material 3、MVVM、StateFlow、Retrofit/OkHttp、现有日夜主题与防烧屏机制。V1 直接读取已有 TeslaMate Grafana API，不新增后端、不调用 Tesla 车辆接口、不唤醒车辆，不加入车辆控制。默认未启用；启用后才显示第三页。

## 2. 界面规范

横屏 16:9，三列布局，上方为车辆名称、记录状态和小尺寸时钟；主区占可视高度约 60%，左侧大字 SOC 与额定续航，中间车辆俯视矢量图与四轮胎压（bar），右侧总里程、今日已完成行程里程与今日平均能耗（估算，Wh/km）。下方三块为近七日能耗趋势、本月已完成行程与充入电量、最近车内外温度；底部共用页面指示器、最近车辆数据时间及连接状态。充电时增加最新采样充电功率、充入电量；无法获取充电剩余时间时隐藏，不猜测。日间米白/墨黑，夜间 AMOLED 纯黑/青绿；复用主题 token。小屏优先保留主区，缩减辅助指标，保留无障碍语义。车辆矢量图应使用自绘 Compose Canvas/VectorDrawable，不引入第三方商标图片。

## 3. Grafana 设置与连接验证

设置页增加：启用开关、Base URL、Grafana 服务账号 Token（密码输入）、数据源 UID（自动发现/手动）、车辆选择（按 ID）、刷新间隔（默认 60s）、统计时区（默认 Asia/Shanghai）、测试连接、清除连接和缓存。使用 HTTPS 并严格验证证书；凭据复用 CredentialStorage，禁止明文日志、提交、备份和硬编码。专用低权限服务账号；Grafana Viewer 不是数据库写保护，PostgreSQL 数据源账号必须只有所需表/视图的 SELECT 权限。若以后公开分发或跨公网提供服务，使用后端白名单代理，不能将 Grafana 管理 Token 嵌入 APK。不要要求用户把 Token 发到对话中。

验证顺序：GET /api/datasources（如果无权限，可手动 UID）；POST /api/ds/query 先执行 SELECT 1 AS ok；再尝试 SELECT id, name FROM cars ORDER BY id；列出车辆供选择。401 提示 Token 无效，403 提示缺少查询权限，404 检查 Base URL 或反代路径，429/5xx 退避。注意 HTTP 200 也可能包含单个 query result error。不得假设 UID 一定是 TeslaMate；数据源类型按服务器返回配置（postgres 或 grafana-postgresql-datasource）。

## 4. 数据查询契约

先检查实际部署版本的列存在性和单位，再采用下列查询。TeslaMate 内部表结构不是稳定外部 API，需封装适配器/兼容检查。

### A：车辆最新完整采样

```sql
SELECT date, battery_level, rated_battery_range_km, odometer,
       inside_temp, outside_temp, tpms_pressure_fl, tpms_pressure_fr,
       tpms_pressure_rl, tpms_pressure_rr
FROM positions
WHERE car_id = 1 AND ideal_battery_range_km IS NOT NULL
ORDER BY date DESC LIMIT 1;
```

示例 car_id=1 必须替换为验证后的车辆 ID。过滤稀疏 streaming 采样。各胎压、温度字段若为空，按单字段最近非空值回补并各自保留采样时间；旧值标记缓存，不把 null 转成 0。充电期间电量优先使用活动充电会话最新有效采样（比较时间）。单位：km、bar、摄氏度、整数 SOC %。

### B：状态与充电

```sql
SELECT state, start_date FROM states
WHERE car_id = 1 ORDER BY start_date DESC LIMIT 1;
```

states 通常只表示 online/asleep/offline。在线时检查 drives 和 charging_processes 中对应车辆 end_date IS NULL 的会话，并读取对应最新采样；仅在会话一致且可信时派生 driving/charging，遇到过时会话或冲突显示未知/最后记录。充电功率来自 charges.charger_power（kW），通过 charging_process_id 关联 charging_processes，按 date DESC LIMIT 1。已休眠不主动唤醒。

### C：行程与能耗

选择已完成 drives，按 start_date 归属本地日期，跨午夜整段归于开始日（UI 文案明确），不将未结束行程计入。月里程按该月窗口求 SUM(distance)。能耗参考安装版本 TeslaMate Drives 看板，使用 rated 续航差乘 cars.efficiency（kWh/km），求有效样本的距离加权平均：

```sql
SELECT SUM(d.distance) AS distance_km,
       SUM((d.start_rated_range_km-d.end_rated_range_km)*c.efficiency*1000)
         FILTER (WHERE d.distance > 0 AND d.start_rated_range_km IS NOT NULL
          AND d.end_rated_range_km IS NOT NULL AND c.efficiency > 0)
       / NULLIF(SUM(d.distance) FILTER (WHERE d.distance > 0
          AND d.start_rated_range_km IS NOT NULL AND d.end_rated_range_km IS NOT NULL
          AND c.efficiency > 0),0) AS estimated_wh_per_km
FROM drives d JOIN cars c ON c.id=d.car_id
WHERE d.car_id=1 AND d.end_date IS NOT NULL
  AND d.start_date >= TIMESTAMP '2026-11-21 16:00:00'
  AND d.start_date < TIMESTAMP '2026-11-22 16:00:00';
```

上例时间为 Asia/Shanghai 2026-11-22 全天对应 UTC，适用于 date 列为 UTC-naive timestamp 的部署；若实际为 timestamptz 则采用明确带时区字面量。生产实现动态计算窗口。近七日逐日分组或使用七日边界；缺失显示空缺而非零。能耗为估算净行驶耗电，不包括停车耗电，非 BMS 实测，若缺少效率参数显示暂无。有效能耗样本少于总行程时标为部分数据。月充电量按充电会话定义汇总 charge_energy_added，明确是充入量，不是电网用电量；排除未完成会话。

## 5. Kotlin 推荐改造结构

```
data/model/TeslaSnapshot.kt
data/network/GrafanaApi.kt
data/network/GrafanaFrameParser.kt
data/provider/TeslaDataProvider.kt
data/repository/TeslaRepository.kt
ui/tesla/TeslaScreen.kt
ui/tesla/TeslaViewModel.kt
ui/tesla/TirePressureDiagram.kt
ui/settings/TeslaSettingsDialog.kt
```

## 6. 刷新、缓存和异常策略

可见车辆页每 60 秒请求快照，充电时可选 30 秒；状态为 asleep/offline 时可放宽至 180 秒。历史聚合每 5 分钟或首次进入/用户手动刷新；同类请求合并、超时 10 秒，指数退避最高 5 分钟。应用离开前台或关闭模块时停止轮询，非车辆页按需暂停高频查询。读取 Grafana 不会直接唤醒车。保持最后成功快照并单独显示查询成功时间和每个字段实际采样时间。睡眠是正常车辆状态，不等同于 Grafana 断连；Grafana 连接正常但历史数据旧时仍显示历史记录提示。未录入凭据时显示设置入口，不使用模拟数据填充生产 UI。
