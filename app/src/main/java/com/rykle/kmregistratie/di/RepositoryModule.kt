package com.rykle.kmregistratie.di

import com.rykle.kmregistratie.data.local.preferences.LocationHistoryPreferences
import com.rykle.kmregistratie.data.local.preferences.SuggestionPreferences
import com.rykle.kmregistratie.data.provider.FusedLocationProviderImpl
import com.rykle.kmregistratie.data.provider.GeocoderProviderImpl
import com.rykle.kmregistratie.data.remote.NextcloudBackupRepository
import com.rykle.kmregistratie.data.repository.DriveRepositoryImpl
import com.rykle.kmregistratie.data.repository.ProjectRepositoryImpl
import com.rykle.kmregistratie.data.repository.VolvoRepositoryImpl
import com.rykle.kmregistratie.domain.provider.GeocoderProvider
import com.rykle.kmregistratie.domain.provider.LocationProvider
import com.rykle.kmregistratie.domain.repository.CloudBackupRepository
import com.rykle.kmregistratie.domain.repository.DriveRepository
import com.rykle.kmregistratie.domain.repository.LocationHistoryRepository
import com.rykle.kmregistratie.domain.repository.ProjectRepository
import com.rykle.kmregistratie.domain.repository.SuggestionRepository
import com.rykle.kmregistratie.domain.repository.VolvoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDriveRepository(impl: DriveRepositoryImpl): DriveRepository

    @Binds
    @Singleton
    abstract fun bindVolvoRepository(impl: VolvoRepositoryImpl): VolvoRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindCloudBackupRepository(impl: NextcloudBackupRepository): CloudBackupRepository

    @Binds
    @Singleton
    abstract fun bindSuggestionRepository(impl: SuggestionPreferences): SuggestionRepository

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: FusedLocationProviderImpl): LocationProvider

    @Binds
    @Singleton
    abstract fun bindGeocoderProvider(impl: GeocoderProviderImpl): GeocoderProvider

    @Binds
    @Singleton
    abstract fun bindLocationHistoryRepository(impl: LocationHistoryPreferences): LocationHistoryRepository
}
