package com.rykle.kmregistratie.domain.model

data class DriveCorrection(
    val startKm: Int?,
    val endKm: Int?,
    val startLat: Double,
    val startLng: Double,
    val startAddress: String?,
    val endLat: Double?,
    val endLng: Double?,
    val endAddress: String?,
)
