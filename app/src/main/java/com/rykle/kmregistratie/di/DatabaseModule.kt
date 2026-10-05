package com.rykle.kmregistratie.di

import android.content.Context
import androidx.room.Room
import com.rykle.kmregistratie.data.local.dao.DriveDao
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
            .addMigrations(KmDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDriveDao(db: KmDatabase): DriveDao = db.driveDao()
}
