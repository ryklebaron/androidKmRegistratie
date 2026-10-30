package com.rykle.kmregistratie.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
    val createdAt: Long,
    val client: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val address: String? = null,
)
