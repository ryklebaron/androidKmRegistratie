package com.rykle.kmregistratie.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rykle.kmregistratie.data.local.dao.DriveDao
import com.rykle.kmregistratie.data.local.entity.DriveEntity

@Database(entities = [DriveEntity::class], version = 3, exportSchema = false)
abstract class KmDatabase : RoomDatabase() {
    abstract fun driveDao(): DriveDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE drives ADD COLUMN kmPending INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
