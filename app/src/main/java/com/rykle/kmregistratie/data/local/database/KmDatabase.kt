package com.rykle.kmregistratie.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rykle.kmregistratie.data.local.dao.DriveDao
import com.rykle.kmregistratie.data.local.entity.DriveEntity

@Database(entities = [DriveEntity::class], version = 2, exportSchema = false)
abstract class KmDatabase : RoomDatabase() {
    abstract fun driveDao(): DriveDao
}
