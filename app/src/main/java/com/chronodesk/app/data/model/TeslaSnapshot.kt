package com.chronodesk.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tesla 车辆实体基础信息
 */
data class TeslaVehicle(
    val id: Int,
    val name: String?,
    val model: String? = null,
    val trimBadging: String? = null,
    val exteriorColor: String? = null
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: "Tesla #$id"

    val normalizedModel: String
        get() = when (model?.uppercase()?.trim()) {
            "3", "MODEL 3", "MODEL3" -> "Model 3"
            "Y", "MODEL Y", "MODELY" -> "Model Y"
            "S", "MODEL S", "MODELS" -> "Model S"
            "X", "MODEL X", "MODELX" -> "Model X"
            null -> "Model 3"
            else -> if (model.startsWith("Model", ignoreCase = true)) model else "Model $model"
        }

    val normalizedExteriorColor: String
        get() {
            val raw = exteriorColor?.lowercase()?.trim() ?: return "RED"
            return when {
                raw.contains("ultra") || raw.contains("pr01") -> "RED"
                raw.contains("multicoat") || raw.contains("ppmr") -> "RED_CLASSIC"
                raw.contains("red") -> "RED"
                raw.contains("white") || raw.contains("ppsw") -> "WHITE"
                raw.contains("black") || raw.contains("pbsb") -> "BLACK"
                raw.contains("silver") || raw.contains("grey") || raw.contains("gray") || raw.contains("pmng") || raw.contains("pn00") -> "SILVER"
                raw.contains("blue") || raw.contains("ppsb") -> "BLUE"
                else -> "RED"
            }
        }
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
        get() = formatSampledTime(sampledAt)
}

/**
 * 统一格式化车辆与位置采样时间，兼容整型/科学计数法纪元时间戳与 ISO 字符串
 */
private fun formatSampledTime(sampledAt: String?): String? {
    val trimmed = sampledAt?.trim()
    if (trimmed.isNullOrBlank()) return null
    return try {
        val numMs = trimmed.toLongOrNull() ?: trimmed.toDoubleOrNull()?.takeIf { it.isFinite() }?.toLong()
        if (numMs != null) {
            val actualMs = if (numMs < 100000000000L) numMs * 1000L else numMs
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
                    parsedDate = format.parse(trimmed)
                    if (parsedDate != null) break
                } catch (_: Exception) {}
            }
            when {
                parsedDate != null -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(parsedDate)
                trimmed.contains("T") -> trimmed.substringAfter("T").take(5)
                trimmed.contains(" ") -> trimmed.substringAfter(" ").take(5)
                else -> trimmed
            }
        }
    } catch (_: Exception) {
        null
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
 * 车辆最后有效位置与围栏信息
 */
data class VehicleLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geofenceName: String? = null,
    val resolvedAddress: String? = null,
    val sampledAt: String? = null
) {
    val isValid: Boolean
        get() = latitude != null && longitude != null && (latitude != 0.0 || longitude != 0.0)

    val coordinateGridKey: String?
        get() = if (isValid) String.format(Locale.US, "%.3f_%.3f", latitude, longitude) else null

    val primaryPlaceName: String
        get() {
            val cleanGeofence = geofenceName?.takeIf { it.isNotBlank() }
            val cleanAddress = resolvedAddress?.takeIf { it.isNotBlank() }
            return cleanGeofence ?: cleanAddress ?: if (isValid) "已记录停车点" else "位置未记录"
        }

    val secondaryPlaceDetail: String
        get() {
            val cleanGeofence = geofenceName?.takeIf { it.isNotBlank() }
            val cleanAddress = resolvedAddress?.takeIf { it.isNotBlank() }
            return when {
                cleanGeofence != null && cleanAddress != null -> cleanAddress
                isValid -> String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
                else -> "—"
            }
        }

    val formattedSampledTime: String?
        get() = formatSampledTime(sampledAt)
}

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
    val location: VehicleLocation = VehicleLocation(),
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
    val allowHttp: Boolean = false,             // 是否允许可信内网 HTTP
    val vehicleColor: String = "AUTO"           // 车辆外观配色模型: AUTO, RED, RED_CLASSIC, WHITE, BLACK, SILVER, BLUE
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && dataSourceUid.isNotBlank() && carId > 0
}
