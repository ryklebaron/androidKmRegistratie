package com.rykle.kmregistratie.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rykle.kmregistratie.data.local.dao.DriveDao
import com.rykle.kmregistratie.data.local.dao.ProjectDao
import com.rykle.kmregistratie.data.local.entity.DriveEntity
import com.rykle.kmregistratie.data.local.entity.ProjectEntity

@Database(entities = [DriveEntity::class, ProjectEntity::class], version = 5, exportSchema = false)
abstract class KmDatabase : RoomDatabase() {
    abstract fun driveDao(): DriveDao
    abstract fun projectDao(): ProjectDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE drives ADD COLUMN kmPending INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Projecten + per rit een project en notitie. Bestaande ritten blijven ongemoeid. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS projects (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "archived INTEGER NOT NULL DEFAULT 0, " +
                        "createdAt INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE drives ADD COLUMN projectId INTEGER")
                db.execSQL("ALTER TABLE drives ADD COLUMN note TEXT")
            }
        }

        /** Klant en locatie per project. Raakt de ritten niet aan. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN client TEXT")
                db.execSQL("ALTER TABLE projects ADD COLUMN lat REAL")
                db.execSQL("ALTER TABLE projects ADD COLUMN lng REAL")
                db.execSQL("ALTER TABLE projects ADD COLUMN address TEXT")
            }
        }
    }
}
