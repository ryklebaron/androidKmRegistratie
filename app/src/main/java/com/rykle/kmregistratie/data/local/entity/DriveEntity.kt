package com.rykle.kmregistratie.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drives")
data class DriveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val startLat: Double,
    val startLng: Double,
    val startAddress: String?,
    val endLat: Double?,
    val endLng: Double?,
    val endAddress: String?,
    val driveType: String,
    val startKm: Int?,
    val endKm: Int?,
    val hasKmMismatch: Boolean,
    @ColumnInfo(defaultValue = "0") val kmPending: Boolean = false,
)
