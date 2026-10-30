package com.rykle.kmregistratie.domain.model

data class Drive(
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val startLat: Double,
    val startLng: Double,
    val startAddress: String? = null,
    val endLat: Double? = null,
    val endLng: Double? = null,
    val endAddress: String? = null,
    val driveType: DriveType,
    val startKm: Int? = null,
    val endKm: Int? = null,
    val hasKmMismatch: Boolean = false,
    // Eindstand nog niet betrouwbaar: Volvo had nog geen nieuwe stand toen de rit eindigde.
    val kmPending: Boolean = false,
    val projectId: Long? = null,
    val note: String? = null,
) {
    val isActive: Boolean get() = endTime == null
    val kmDriven: Int? get() = if (!kmPending && startKm != null && endKm != null) endKm - startKm else null
}
