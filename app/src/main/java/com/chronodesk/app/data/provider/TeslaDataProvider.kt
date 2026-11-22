package com.chronodesk.app.data.provider

import android.util.Log
import com.chronodesk.app.data.model.ChargingSnapshot
import com.chronodesk.app.data.model.DailyDriveItem
import com.chronodesk.app.data.model.DriveSummary
import com.chronodesk.app.data.model.TeslaConnectionState
import com.chronodesk.app.data.model.TeslaFullSnapshot
import com.chronodesk.app.data.model.TeslaSettings
import com.chronodesk.app.data.model.TeslaVehicle
import com.chronodesk.app.data.model.VehicleSnapshot
import com.chronodesk.app.data.model.VehicleState
import com.chronodesk.app.data.network.GrafanaApiClient
import com.chronodesk.app.data.network.GrafanaDatasourceItem
import com.chronodesk.app.data.network.GrafanaDatasourceRef
import com.chronodesk.app.data.network.GrafanaFrameParser
import com.chronodesk.app.data.network.GrafanaFrameParser.getDouble
import com.chronodesk.app.data.network.GrafanaFrameParser.getInt
import com.chronodesk.app.data.network.GrafanaFrameParser.getString
import com.chronodesk.app.data.network.GrafanaQueryBody
import com.chronodesk.app.data.network.GrafanaQueryEntry
import retrofit2.HttpException
import java.io.IOException
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TeslaDataProvider(
    private val apiClient: GrafanaApiClient = GrafanaApiClient.instance
) {

    private val tag = "TeslaDataProvider"

    /**
     * 发现 PostgreSQL 数据源列表
     */
    suspend fun discoverDatasources(
        baseUrl: String,
        token: String,
        allowHttp: Boolean = false
    ): Result<List<GrafanaDatasourceItem>> {
        return try {
            val service = apiClient.getService(baseUrl, allowHttp)
            val auth = "Bearer ${token.trim()}"
            val allSources = service.getDatasources(auth)
            val pgSources = allSources.filter {
                it.type.equals("postgres", ignoreCase = true) ||
                it.type.equals("grafana-postgresql-datasource", ignoreCase = true)
            }
            Result.success(pgSources)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    /**
     * 验证数据库连接与车辆列表发现
     */
    suspend fun testConnectionAndDiscoverCars(
        settings: TeslaSettings,
        token: String
    ): Result<List<TeslaVehicle>> {
        return try {
            val service = apiClient.getService(settings.baseUrl, settings.allowHttp)
            val auth = "Bearer ${token.trim()}"

            // 1. SELECT 1 AS ok 验证数据源连通性
            val testQuery = GrafanaQueryBody(
                queries = listOf(
                    GrafanaQueryEntry(
                        refId = "A",
                        datasource = GrafanaDatasourceRef(settings.dataSourceUid, settings.dataSourceType),
                        rawSql = "SELECT 1 AS ok"
                    )
                )
            )
            val testResp = service.queryDataSource(auth, testQuery)
            GrafanaFrameParser.parseResultToRows(testResp, "A")

            // 2. 查询车辆列表 SELECT id, name FROM cars
            val carsQuery = GrafanaQueryBody(
                queries = listOf(
                    GrafanaQueryEntry(
                        refId = "A",
                        datasource = GrafanaDatasourceRef(settings.dataSourceUid, settings.dataSourceType),
                        rawSql = "SELECT id, name FROM cars ORDER BY id LIMIT 50"
                    )
                )
            )
            val carsResp = service.queryDataSource(auth, carsQuery)
            val carRows = GrafanaFrameParser.parseResultToRows(carsResp, "A")
            val cars = carRows.mapNotNull { row ->
                val id = row.getInt("id") ?: return@mapNotNull null
                val name = row.getString("name")
                TeslaVehicle(id, name)
            }

            if (cars.isEmpty()) {
                Result.failure(IllegalArgumentException("数据库连接成功，但 cars 表中未找到任何车辆记录"))
            } else {
                Result.success(cars)
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    /**
     * 读取车辆最新状态与采样快照 (A/B/C 查询契约)
     */
    suspend fun fetchVehicleSnapshot(
        settings: TeslaSettings,
        token: String,
        lastSnapshot: TeslaFullSnapshot? = null
    ): TeslaFullSnapshot {
        val carId = settings.carId
        if (carId <= 0) {
            return TeslaFullSnapshot(
                connectionState = TeslaConnectionState(
                    isConfigured = false,
                    errorMessage = "未指定有效车辆 ID"
                )
            )
        }

        val service = apiClient.getService(settings.baseUrl, settings.allowHttp)
        val auth = "Bearer ${token.trim()}"
        val dsRef = GrafanaDatasourceRef(settings.dataSourceUid, settings.dataSourceType)

        // 1. 查询 A：车辆最新完整采样 (positions)
        var vehicleSnap = fetchLatestPositions(service, auth, dsRef, carId, lastSnapshot?.snapshot)

        // 2. 查询 B：车辆状态与会话 (states, drives, charging_processes)
        val (vehicleState, activeCharging) = fetchVehicleStateAndActiveSession(service, auth, dsRef, carId)

        // 3. 查询 C：行程与能耗统计 (drives, charging_processes)
        val (drivesSummary, totalCharges) = fetchDriveAndChargeSummaries(service, auth, dsRef, carId, settings.timeZone)

        // 合并充电快照
        val finalCharging = activeCharging.copy(
            todayCharge = totalCharges.todayCharge,
            monthCharge = totalCharges.monthCharge
        )

        val carObj = TeslaVehicle(
            id = carId,
            name = settings.carName.takeIf { it.isNotBlank() } ?: "Tesla"
        )

        return TeslaFullSnapshot(
            car = carObj,
            state = vehicleState,
            snapshot = vehicleSnap,
            charging = finalCharging,
            drives = drivesSummary,
            connectionState = TeslaConnectionState(
                isConfigured = true,
                isLoading = false,
                lastSuccessTime = System.currentTimeMillis(),
                errorMessage = null
            ),
            queriedAt = System.currentTimeMillis()
        )
    }

    private suspend fun fetchLatestPositions(
        service: com.chronodesk.app.data.network.GrafanaRetrofitService,
        auth: String,
        dsRef: GrafanaDatasourceRef,
        carId: Int,
        previousSnapshot: VehicleSnapshot?
    ): VehicleSnapshot {
        val sqlA = """
            SELECT date, battery_level, rated_battery_range_km, est_battery_range_km, odometer,
                   inside_temp, outside_temp, tpms_pressure_fl, tpms_pressure_fr,
                   tpms_pressure_rl, tpms_pressure_rr, speed
            FROM positions
            WHERE car_id = $carId AND ideal_battery_range_km IS NOT NULL
            ORDER BY date DESC LIMIT 1
        """.trimIndent()

        val respA = service.queryDataSource(auth, GrafanaQueryBody(queries = listOf(GrafanaQueryEntry("A", dsRef, sqlA))))
        val rowsA = GrafanaFrameParser.parseResultToRows(respA, "A")
        val mainRow = rowsA.firstOrNull()

        if (mainRow == null) {
            return previousSnapshot ?: VehicleSnapshot()
        }

        var sampledAt = mainRow.getString("date")
        var batteryLevel = mainRow.getInt("battery_level")
        var ratedRange = mainRow.getDouble("rated_battery_range_km")
        var estRange = mainRow.getDouble("est_battery_range_km")
        var odometer = mainRow.getDouble("odometer")
        var insideTemp = mainRow.getDouble("inside_temp")
        var outsideTemp = mainRow.getDouble("outside_temp")
        var tpmsFl = mainRow.getDouble("tpms_pressure_fl")
        var tpmsFr = mainRow.getDouble("tpms_pressure_fr")
        var tpmsRl = mainRow.getDouble("tpms_pressure_rl")
        var tpmsRr = mainRow.getDouble("tpms_pressure_rr")
        var speed = mainRow.getDouble("speed")

        val tpmsSampledAt = mutableMapOf<String, String>()
        if (sampledAt != null) {
            listOf("fl", "fr", "rl", "rr").forEach { tpmsSampledAt[it] = sampledAt }
        }

        // 如果胎压存在空缺，回补最近非空值
        val missingTpms = mutableListOf<String>()
        if (tpmsFl == null) missingTpms.add("tpms_pressure_fl")
        if (tpmsFr == null) missingTpms.add("tpms_pressure_fr")
        if (tpmsRl == null) missingTpms.add("tpms_pressure_rl")
        if (tpmsRr == null) missingTpms.add("tpms_pressure_rr")

        if (missingTpms.isNotEmpty()) {
            for (field in missingTpms) {
                try {
                    val fallbackSql = "SELECT date, $field FROM positions WHERE car_id = $carId AND $field IS NOT NULL ORDER BY date DESC LIMIT 1"
                    val fbResp = service.queryDataSource(auth, GrafanaQueryBody(queries = listOf(GrafanaQueryEntry("A", dsRef, fallbackSql))))
                    val fbRow = GrafanaFrameParser.parseResultToRows(fbResp, "A").firstOrNull()
                    if (fbRow != null) {
                        val fbVal = fbRow.getDouble(field)
                        val fbDate = fbRow.getString("date")
                        when (field) {
                            "tpms_pressure_fl" -> { tpmsFl = fbVal; if (fbDate != null) tpmsSampledAt["fl"] = fbDate }
                            "tpms_pressure_fr" -> { tpmsFr = fbVal; if (fbDate != null) tpmsSampledAt["fr"] = fbDate }
                            "tpms_pressure_rl" -> { tpmsRl = fbVal; if (fbDate != null) tpmsSampledAt["rl"] = fbDate }
                            "tpms_pressure_rr" -> { tpmsRr = fbVal; if (fbDate != null) tpmsSampledAt["rr"] = fbDate }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Fallback query for $field failed: ${e.message}")
                }
            }
        }

        // 如果仍有字段为空，使用上次成功快照回退
        if (batteryLevel == null) batteryLevel = previousSnapshot?.batteryLevel
        if (ratedRange == null) ratedRange = previousSnapshot?.ratedBatteryRangeKm
        if (odometer == null) odometer = previousSnapshot?.odometer
        if (insideTemp == null) insideTemp = previousSnapshot?.insideTemp
        if (outsideTemp == null) outsideTemp = previousSnapshot?.outsideTemp
        if (tpmsFl == null) tpmsFl = previousSnapshot?.tpmsPressureFl
        if (tpmsFr == null) tpmsFr = previousSnapshot?.tpmsPressureFr
        if (tpmsRl == null) tpmsRl = previousSnapshot?.tpmsPressureRl
        if (tpmsRr == null) tpmsRr = previousSnapshot?.tpmsPressureRr

        return VehicleSnapshot(
            sampledAt = sampledAt,
            batteryLevel = batteryLevel,
            ratedBatteryRangeKm = ratedRange,
            estBatteryRangeKm = estRange,
            odometer = odometer,
            insideTemp = insideTemp,
            outsideTemp = outsideTemp,
            tpmsPressureFl = tpmsFl,
            tpmsPressureFr = tpmsFr,
            tpmsPressureRl = tpmsRl,
            tpmsPressureRr = tpmsRr,
            tpmsSampledAt = tpmsSampledAt,
            speed = speed
        )
    }

    private suspend fun fetchVehicleStateAndActiveSession(
        service: com.chronodesk.app.data.network.GrafanaRetrofitService,
        auth: String,
        dsRef: GrafanaDatasourceRef,
        carId: Int
    ): Pair<VehicleState, ChargingSnapshot> {
        val stateSql = "SELECT state::text AS state, start_date FROM states WHERE car_id = $carId ORDER BY start_date DESC LIMIT 1"
        var rawState = "online"
        var startDate: String? = null

        try {
            val resp = service.queryDataSource(auth, GrafanaQueryBody(queries = listOf(GrafanaQueryEntry("A", dsRef, stateSql))))
            val row = GrafanaFrameParser.parseResultToRows(resp, "A").firstOrNull()
            if (row != null) {
                rawState = row.getString("state") ?: "online"
                startDate = row.getString("start_date")
            }
        } catch (e: Exception) {
            Log.w(tag, "Query states failed: ${e.message}")
        }

        var isCharging = false
        var chargerPower: Double? = null
        var chargeEnergyAdded: Double? = null
        var lastChargeDate: String? = null

        // 检查是否有未结束的活动充电会话
        try {
            val chargeSql = """
                SELECT cp.id, cp.charge_energy_added, c.charger_power, c.date AS last_charge_date
                FROM charging_processes cp
                LEFT JOIN charges c ON c.charging_process_id = cp.id
                WHERE cp.car_id = $carId AND cp.end_date IS NULL
                ORDER BY c.date DESC LIMIT 1
            """.trimIndent()
            val cResp = service.queryDataSource(auth, GrafanaQueryBody(queries = listOf(GrafanaQueryEntry("A", dsRef, chargeSql))))
            val cRow = GrafanaFrameParser.parseResultToRows(cResp, "A").firstOrNull()
            if (cRow != null) {
                isCharging = true
                chargerPower = cRow.getDouble("charger_power")
                chargeEnergyAdded = cRow.getDouble("charge_energy_added")
                lastChargeDate = cRow.getString("last_charge_date")
            }
        } catch (e: Exception) {
            Log.w(tag, "Query active charging session failed: ${e.message}")
        }

        // 检查是否有未结束的活动行驶会话
        var isDriving = false
        if (!isCharging && rawState.equals("online", ignoreCase = true)) {
            try {
                val driveSql = "SELECT id FROM drives WHERE car_id = $carId AND end_date IS NULL ORDER BY start_date DESC LIMIT 1"
                val dResp = service.queryDataSource(auth, GrafanaQueryBody(queries = listOf(GrafanaQueryEntry("A", dsRef, driveSql))))
                val dRow = GrafanaFrameParser.parseResultToRows(dResp, "A").firstOrNull()
                if (dRow != null) {
                    isDriving = true
                }
            } catch (e: Exception) {
                Log.w(tag, "Query active drive session failed: ${e.message}")
            }
        }

        val derivedState = when {
            isCharging -> "charging"
            isDriving -> "driving"
            else -> rawState
        }

        val stateObj = VehicleState(
            state = derivedState,
            startDate = startDate,
            sampledAt = System.currentTimeMillis()
        )

        val chargingObj = ChargingSnapshot(
            isCharging = isCharging,
            chargerPower = chargerPower,
            chargeEnergyAdded = chargeEnergyAdded,
            lastChargeDate = lastChargeDate
        )

        return stateObj to chargingObj
    }

    private suspend fun fetchDriveAndChargeSummaries(
        service: com.chronodesk.app.data.network.GrafanaRetrofitService,
        auth: String,
        dsRef: GrafanaDatasourceRef,
        carId: Int,
        timeZoneString: String
    ): Pair<DriveSummary, ChargingSnapshot> {
        val zoneId = try { ZoneId.of(timeZoneString) } catch (_: Exception) { ZoneId.of("Asia/Shanghai") }
        val nowInZone = ZonedDateTime.now(zoneId)
        val todayStart = nowInZone.toLocalDate().atStartOfDay(zoneId).withZoneSameInstant(ZoneOffset.UTC)
        val todayEnd = todayStart.plusDays(1)
        val monthStart = nowInZone.toLocalDate().withDayOfMonth(1).atStartOfDay(zoneId).withZoneSameInstant(ZoneOffset.UTC)
        val sevenDaysStart = todayStart.minusDays(6)

        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val todayStartUtc = todayStart.format(fmt)
        val todayEndUtc = todayEnd.format(fmt)
        val monthStartUtc = monthStart.format(fmt)
        val sevenDaysStartUtc = sevenDaysStart.format(fmt)

        // 1. 今日已完成行程与平均能耗
        val todayDrivesSql = """
            SELECT COUNT(*) AS trips,
                   COALESCE(SUM(d.distance), 0) AS distance_km,
                   SUM((d.start_rated_range_km - d.end_rated_range_km) * c.efficiency * 1000)
                     FILTER (WHERE d.distance > 0 AND d.start_rated_range_km IS NOT NULL AND d.end_rated_range_km IS NOT NULL AND c.efficiency > 0)
                   / NULLIF(SUM(d.distance) FILTER (WHERE d.distance > 0 AND d.start_rated_range_km IS NOT NULL AND d.end_rated_range_km IS NOT NULL AND c.efficiency > 0), 0) AS estimated_wh_per_km,
                   SUM((d.start_rated_range_km - d.end_rated_range_km) * c.efficiency)
                     FILTER (WHERE d.distance > 0 AND d.start_rated_range_km IS NOT NULL AND d.end_rated_range_km IS NOT NULL AND c.efficiency > 0) AS energy_kwh
            FROM drives d JOIN cars c ON c.id = d.car_id
            WHERE d.car_id = $carId AND d.end_date IS NOT NULL
              AND d.start_date >= TIMESTAMP '$todayStartUtc'
              AND d.start_date < TIMESTAMP '$todayEndUtc'
        """.trimIndent()

        var todayDist: Double? = 0.0
        var todayTrips: Int? = 0
        var todayWhKm: Double? = null
        var todayEnergy: Double? = null

        try {
            val tdResp = service.queryDataSource(auth, GrafanaQueryBody(queries = listOf(GrafanaQueryEntry("A", dsRef, todayDrivesSql))))
            val tdRow = GrafanaFrameParser.parseResultToRows(tdResp, "A").firstOrNull()
            if (tdRow != null) {
                todayDist = tdRow.getDouble("distance_km")
                todayTrips = tdRow.getInt("trips")
                todayWhKm = tdRow.getDouble("estimated_wh_per_km")
                todayEnergy = tdRow.getDouble("energy_kwh")
            }
        } catch (e: Exception) {
            Log.w(tag, "Query today drives failed: ${e.message}")
        }

        // 2. 本月已完成行程里程
        var monthDist: Double? = 0.0
        var monthTrips: Int? = 0
        try {
            val mSql = "SELECT COALESCE(SUM(distance), 0) AS distance_km, COUNT(*) AS trips FROM drives WHERE car_id = $carId AND end_date IS NOT NULL AND start_date >= TIMESTAMP '$monthStartUtc'"
            val mResp = service.queryDataSource(auth, GrafanaQueryBody(queries = listOf(GrafanaQueryEntry("A", dsRef, mSql))))
            val mRow = GrafanaFrameParser.parseResultToRows(mResp, "A").firstOrNull()
            if (mRow != null) {
                monthDist = mRow.getDouble("distance_km")
                monthTrips = mRow.getInt("trips")
            }
        } catch (e: Exception) {
            Log.w(tag, "Query month drives failed: ${e.message}")
        }

        // 3. 今日充入与本月充入电量
        var todayCharge: Double? = 0.0
        var monthCharge: Double? = 0.0
        try {
            val tChargeSql = "SELECT COALESCE(SUM(charge_energy_added), 0) AS charge_kwh FROM charging_processes WHERE car_id = $carId AND start_date >= TIMESTAMP '$todayStartUtc' AND start_date < TIMESTAMP '$todayEndUtc'"
            val tcResp = service.queryDataSource(auth, GrafanaQueryBody(queries = listOf(GrafanaQueryEntry("A", dsRef, tChargeSql))))
            val tcRow = GrafanaFrameParser.parseResultToRows(tcResp, "A").firstOrNull()
            if (tcRow != null) todayCharge = tcRow.getDouble("charge_kwh")

            val mChargeSql = "SELECT COALESCE(SUM(charge_energy_added), 0) AS charge_kwh FROM charging_processes WHERE car_id = $carId AND start_date >= TIMESTAMP '$monthStartUtc'"
            val mcResp = service.queryDataSource(auth, GrafanaQueryBody(queries = listOf(GrafanaQueryEntry("A", dsRef, mChargeSql))))
            val mcRow = GrafanaFrameParser.parseResultToRows(mcResp, "A").firstOrNull()
            if (mcRow != null) monthCharge = mcRow.getDouble("charge_kwh")
        } catch (e: Exception) {
            Log.w(tag, "Query charging summary failed: ${e.message}")
        }

        // 4. 近 7 天能耗趋势 (按天分组)
        val recent7List = mutableListOf<DailyDriveItem>()
        try {
            val sevenSql = """
                SELECT to_char(d.start_date, 'YYYY-MM-DD') AS day_date,
                       COALESCE(SUM(d.distance), 0) AS distance_km,
                       SUM((d.start_rated_range_km - d.end_rated_range_km) * c.efficiency * 1000)
                         FILTER (WHERE d.distance > 0 AND d.start_rated_range_km IS NOT NULL AND d.end_rated_range_km IS NOT NULL AND c.efficiency > 0)
                       / NULLIF(SUM(d.distance) FILTER (WHERE d.distance > 0 AND d.start_rated_range_km IS NOT NULL AND d.end_rated_range_km IS NOT NULL AND c.efficiency > 0), 0) AS estimated_wh_per_km
                FROM drives d JOIN cars c ON c.id = d.car_id
                WHERE d.car_id = $carId AND d.end_date IS NOT NULL
                  AND d.start_date >= TIMESTAMP '$sevenDaysStartUtc'
                GROUP BY 1 ORDER BY 1 ASC
            """.trimIndent()
            val sevenResp = service.queryDataSource(auth, GrafanaQueryBody(queries = listOf(GrafanaQueryEntry("A", dsRef, sevenSql))))
            val rows = GrafanaFrameParser.parseResultToRows(sevenResp, "A")
            for (r in rows) {
                val dDate = r.getString("day_date") ?: continue
                val dist = r.getDouble("distance_km") ?: 0.0
                val wh = r.getDouble("estimated_wh_per_km")
                recent7List.add(DailyDriveItem(dDate, dist, wh))
            }
        } catch (e: Exception) {
            Log.w(tag, "Query 7 days drives failed: ${e.message}")
        }

        val driveSummary = DriveSummary(
            todayDistanceKm = todayDist,
            todayTrips = todayTrips,
            todayEstimatedWhPerKm = todayWhKm,
            todayEnergyKwh = todayEnergy,
            monthDistanceKm = monthDist,
            monthTrips = monthTrips,
            recent7DaysDrives = recent7List
        )

        val chargingSnapshot = ChargingSnapshot(
            todayCharge = todayCharge,
            monthCharge = monthCharge
        )

        return driveSummary to chargingSnapshot
    }

    private fun mapException(e: Exception): Exception {
        return when (e) {
            is HttpException -> {
                val code = e.code()
                val msg = when (code) {
                    401 -> "Grafana 鉴权失败 (HTTP 401)：Token 无效或已过期"
                    403 -> "Grafana 访问拒绝 (HTTP 403)：Token 缺少数据源或 API 查询权限"
                    404 -> "Grafana 端点不存在 (HTTP 404)：请检查 Base URL 是否正确"
                    429 -> "Grafana 请求过于频繁 (HTTP 429)：已触发速率限制"
                    502, 503 -> "Grafana 服务暂时不可用 (HTTP $code)：网关或反向代理异常"
                    else -> "Grafana 错误 (HTTP $code)"
                }
                IllegalArgumentException(msg, e)
            }
            is IOException -> {
                IllegalArgumentException("网络连接异常：${e.message}", e)
            }
            else -> e
        }
    }
}
