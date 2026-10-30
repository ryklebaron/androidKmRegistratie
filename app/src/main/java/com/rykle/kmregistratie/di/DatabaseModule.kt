package com.rykle.kmregistratie.di

import android.content.Context
import androidx.room.Room
import com.rykle.kmregistratie.data.local.dao.DriveDao
import com.rykle.kmregistratie.data.local.dao.ProjectDao
import com.rykle.kmregistratie.data.local.database.KmDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KmDatabase =
        Room.databaseBuilder(context, KmDatabase::class.java, "km_registratie.db")
            .addMigrations(KmDatabase.MIGRATION_2_3, KmDatabase.MIGRATION_3_4, KmDatabase.MIGRATION_4_5)
            // Bewust géén fallbackToDestructiveMigration(): een ontbrekende migratie moet crashen,
            // niet stilletjes alle ritten wissen.
            .build()

    @Provides
    fun provideDriveDao(db: KmDatabase): DriveDao = db.driveDao()

    @Provides
    fun provideProjectDao(db: KmDatabase): ProjectDao = db.projectDao()
}
