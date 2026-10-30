package com.rykle.kmregistratie.domain.model

import java.time.Instant
import java.time.ZoneId

/** Totalen over een groep ritten. Ritten zonder bekende km tellen wel mee als rit en in uren. */
data class DriveTotals(
    val businessKm: Int = 0,
    val privateKm: Int = 0,
    val drives: Int = 0,
    val drivingMillis: Long = 0,
    val unknownKmDrives: Int = 0,
) {
    val totalKm: Int get() = businessKm + privateKm
    val drivingHours: Double get() = drivingMillis / 3_600_000.0
}

object DriveStats {

    /** Grens van de Belastingdienst: tot 500 privé-km per kalenderjaar geen bijtelling. */
    const val PRIVATE_KM_LIMIT = 500

    fun totals(drives: List<Drive>): DriveTotals = drives.filter { !it.isActive }.fold(DriveTotals()) { t, d ->
        val km = d.kmDriven
        t.copy(
            businessKm = t.businessKm + if (d.driveType == DriveType.BUSINESS) km ?: 0 else 0,
            privateKm = t.privateKm + if (d.driveType == DriveType.PRIVATE) km ?: 0 else 0,
            drives = t.drives + 1,
            drivingMillis = t.drivingMillis + (d.endTime!! - d.startTime),
            unknownKmDrives = t.unknownKmDrives + if (km == null) 1 else 0,
        )
    }

    fun year(d: Drive, zone: ZoneId = ZoneId.systemDefault()): Int =
        Instant.ofEpochMilli(d.startTime).atZone(zone).year

    /** Maand 1..12 → totalen, voor alle twaalf maanden (lege maanden met nullen). */
    fun perMonth(drives: List<Drive>, year: Int, zone: ZoneId = ZoneId.systemDefault()): Map<Int, DriveTotals> {
        val byMonth = drives
            .filter { year(it, zone) == year }
            .groupBy { Instant.ofEpochMilli(it.startTime).atZone(zone).monthValue }
        return (1..12).associateWith { totals(byMonth[it].orEmpty()) }
    }

    /** Project-id (null = geen project) → totalen, grootste eerst. */
    fun perProject(drives: List<Drive>): List<Pair<Long?, DriveTotals>> =
        drives.groupBy { it.projectId }
            .map { (id, list) -> id to totals(list) }
            .sortedByDescending { it.second.totalKm }
}
