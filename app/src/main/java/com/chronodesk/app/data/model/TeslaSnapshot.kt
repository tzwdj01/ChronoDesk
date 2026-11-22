package com.chronodesk.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tesla 车辆实体基础信息
 */
data class TeslaVehicle(
    val id: Int,
    val name: String?
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: "Tesla #$id"
}

/**
 * 车辆状态与会话快照
 */
data class VehicleState(
    val state: String? = null, // online, asleep, offline, charging, driving
    val startDate: String? = null,
    val sampledAt: Long? = null
) {
    val displayState: String
        get() = when (state?.lowercase()) {
            "online" -> "在线"
            "asleep" -> "休眠"
            "offline" -> "离线"
            "charging" -> "充电中"
            "driving" -> "行驶中"
            null -> "未知"
            else -> state
        }

    val isAsleep: Boolean
        get() = state?.lowercase() == "asleep"

    val isCharging: Boolean
        get() = state?.lowercase() == "charging"

    val isDriving: Boolean
        get() = state?.lowercase() == "driving"
}

/**
 * 车辆最新完整物理指标采样快照
 */
data class VehicleSnapshot(
    val sampledAt: String? = null,            // 采样 ISO 时间
    val batteryLevel: Int? = null,            // SOC % (0 ~ 100)
    val ratedBatteryRangeKm: Double? = null,  // 额定续航 (km)
    val estBatteryRangeKm: Double? = null,    // 预估续航 (km)
    val odometer: Double? = null,             // 累计总里程 (km)
    val insideTemp: Double? = null,           // 车内温度 (°C)
    val outsideTemp: Double? = null,          // 车外温度 (°C)
    val tpmsPressureFl: Double? = null,       // 左前胎压 (bar)
    val tpmsPressureFr: Double? = null,       // 右前胎压 (bar)
    val tpmsPressureRl: Double? = null,       // 左后胎压 (bar)
    val tpmsPressureRr: Double? = null,       // 右后胎压 (bar)
    val tpmsSampledAt: Map<String, String> = emptyMap(), // 各字段实际采样时间（历史回补时用）
    val speed: Double? = null                 // 速度 (km/h)
) {
    val formattedSampledTime: String?
        get() {
            if (sampledAt.isNullOrBlank()) return null
            return try {
                if (sampledAt.toLongOrNull() != null) {
                    val ms = sampledAt.toLong()
                    val actualMs = if (ms < 100000000000L) ms * 1000L else ms
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(actualMs))
                } else {
                    val isoFormats = listOf(
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.getDefault()),
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault()),
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    )
                    var parsedDate: Date? = null
                    for (format in isoFormats) {
                        try {
                            parsedDate = format.parse(sampledAt)
                            if (parsedDate != null) break
                        } catch (_: Exception) {}
                    }
                    if (parsedDate != null) {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(parsedDate)
                    } else if (sampledAt.contains("T")) {
                        sampledAt.substringAfter("T").take(5)
                    } else if (sampledAt.contains(" ")) {
                        sampledAt.substringAfter(" ").take(5)
                    } else {
                        sampledAt
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
}

/**
 * 充电状态与电量快照
 */
data class ChargingSnapshot(
    val isCharging: Boolean = false,
    val chargerPower: Double? = null,      // 最新充电功率 (kW)
    val chargeEnergyAdded: Double? = null, // 当前会话充入电量 (kWh)
    val todayCharge: Double? = null,       // 今日累计充入电量 (kWh)
    val monthCharge: Double? = null,       // 本月累计充入电量 (kWh)
    val lastChargeDate: String? = null
)

/**
 * 单日行程与能耗记录
 */
data class DailyDriveItem(
    val dateString: String,      // YYYY-MM-DD
    val distanceKm: Double,      // 该日完成里程
    val estimatedWhPerKm: Double? // 该日平均能耗估算
)

/**
 * 行程与能耗统计汇总
 */
data class DriveSummary(
    val todayDistanceKm: Double? = null,       // 今日已完成行程总里程 (km)
    val todayTrips: Int? = null,               // 今日已完成行程次数
    val todayEstimatedWhPerKm: Double? = null, // 今日平均能耗 (Wh/km)
    val todayEnergyKwh: Double? = null,        // 今日行驶净耗电估算 (kWh)
    val monthDistanceKm: Double? = null,       // 本月已完成行程总里程 (km)
    val monthTrips: Int? = null,               // 本月行程总数
    val recent7DaysDrives: List<DailyDriveItem> = emptyList() // 近7天能耗趋势数据
)

/**
 * 连接与同步状态
 */
data class TeslaConnectionState(
    val isConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val lastSuccessTime: Long? = null,
    val errorMessage: String? = null
) {
    val formattedLastSuccessTime: String
        get() = if (lastSuccessTime != null) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.format(Date(lastSuccessTime))
        } else {
            "--:--"
        }
}

/**
 * TeslaMate 监控完整数据模型
 */
data class TeslaFullSnapshot(
    val car: TeslaVehicle? = null,
    val state: VehicleState = VehicleState(),
    val snapshot: VehicleSnapshot = VehicleSnapshot(),
    val charging: ChargingSnapshot = ChargingSnapshot(),
    val drives: DriveSummary = DriveSummary(),
    val connectionState: TeslaConnectionState = TeslaConnectionState(),
    val queriedAt: Long = System.currentTimeMillis()
) {
    val isAvailable: Boolean
        get() = snapshot.batteryLevel != null || state.state != null
}

/**
 * Tesla 模块用户配置
 */
data class TeslaSettings(
    val enabled: Boolean = false,               // 默认未启用，启用后才显示第三页
    val baseUrl: String = "",                   // Grafana Base URL (如 https://grafana.example.com)
    val dataSourceUid: String = "",             // PostgreSQL 数据源 UID
    val dataSourceType: String = "postgres",    // 数据源类型 (postgres 或 grafana-postgresql-datasource)
    val carId: Int = 1,                         // 选定的车辆 ID
    val carName: String = "",                   // 选定的车辆名称
    val refreshIntervalSec: Int = 60,           // 默认刷新间隔 60s
    val timeZone: String = "Asia/Shanghai",     // 统计时区
    val allowHttp: Boolean = false              // 是否允许可信内网 HTTP
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && dataSourceUid.isNotBlank() && carId > 0
}
