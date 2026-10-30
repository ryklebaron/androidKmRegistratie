package com.rykle.kmregistratie.di

import com.rykle.kmregistratie.data.remote.VolvoApiService
import com.rykle.kmregistratie.data.remote.VolvoAuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                    // Volvo-token en Nextcloud-wachtwoord niet in logcat
                    redactHeader("Authorization")
                }
            )
            .build()

    @Provides
    @Singleton
    fun provideVolvoApiService(client: OkHttpClient): VolvoApiService =
        Retrofit.Builder()
            .baseUrl("https://api.volvocars.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VolvoApiService::class.java)

    @Provides
    @Singleton
    fun provideVolvoAuthService(client: OkHttpClient): VolvoAuthService =
        Retrofit.Builder()
            .baseUrl("https://volvoid.eu.volvocars.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VolvoAuthService::class.java)
}
