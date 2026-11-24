package com.chronodesk.app.data

import com.chronodesk.app.data.model.DailyDriveItem
import com.chronodesk.app.data.model.DriveSummary
import com.chronodesk.app.data.model.TeslaSettings
import com.chronodesk.app.data.model.TeslaVehicle
import com.chronodesk.app.data.model.VehicleSnapshot
import com.chronodesk.app.data.model.VehicleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeslaSnapshotTest {

    @Test
    fun testVehicleStateDisplayNames() {
        assertEquals("在线", VehicleState("online").displayState)
        assertEquals("休眠", VehicleState("asleep").displayState)
        assertEquals("离线", VehicleState("offline").displayState)
        assertEquals("充电中", VehicleState("charging").displayState)
        assertEquals("行驶中", VehicleState("driving").displayState)
        assertEquals("未知", VehicleState(null).displayState)
    }

    @Test
    fun testVehicleStateFlags() {
        val stateAsleep = VehicleState("asleep")
        assertTrue(stateAsleep.isAsleep)
        assertFalse(stateAsleep.isCharging)
        assertFalse(stateAsleep.isDriving)

        val stateCharging = VehicleState("charging")
        assertTrue(stateCharging.isCharging)
        assertFalse(stateCharging.isAsleep)

        val stateDriving = VehicleState("driving")
        assertTrue(stateDriving.isDriving)
    }

    @Test
    fun testTeslaVehicleDisplayName() {
        val carNamed = TeslaVehicle(1, "星舰白 Model Y")
        assertEquals("星舰白 Model Y", carNamed.displayName)

        val carBlank = TeslaVehicle(2, "")
        assertEquals("Tesla #2", carBlank.displayName)

        val carNull = TeslaVehicle(3, null)
        assertEquals("Tesla #3", carNull.displayName)
    }

    @Test
    fun testTeslaSettingsConfiguration() {
        val unconfigured = TeslaSettings()
        assertFalse(unconfigured.isConfigured)
        assertFalse(unconfigured.enabled)
        assertEquals(60, unconfigured.refreshIntervalSec)
        assertEquals("Asia/Shanghai", unconfigured.timeZone)

        val configured = TeslaSettings(
            enabled = true,
            baseUrl = "https://grafana.example.com",
            dataSourceUid = "teslamate_uid",
            carId = 1
        )
        assertTrue(configured.isConfigured)
        assertTrue(configured.enabled)
    }

    @Test
    fun testDriveSummaryAggregation() {
        val summary = DriveSummary(
            todayDistanceKm = 42.5,
            todayDurationMin = 75,
            todayTrips = 3,
            todayEstimatedWhPerKm = 145.2,
            todayEnergyKwh = 6.17,
            monthDistanceKm = 380.0,
            recent7DaysDrives = listOf(
                DailyDriveItem("2026-11-20", 25.0, 142.0),
                DailyDriveItem("2026-11-21", 15.0, 148.0)
            )
        )

        assertEquals(42.5, summary.todayDistanceKm ?: 0.0, 0.01)
        assertEquals(75, summary.todayDurationMin)
        assertEquals("1小时15分", summary.formattedTodayDuration)
        assertEquals(3, summary.todayTrips)
        assertEquals(6.17, summary.todayEnergyKwh ?: 0.0, 0.01)
        assertEquals(2, summary.recent7DaysDrives.size)
    }
}
