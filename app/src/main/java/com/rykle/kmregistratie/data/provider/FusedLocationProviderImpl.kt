package com.rykle.kmregistratie.data.provider

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.rykle.kmregistratie.domain.model.LocationPoint
import com.rykle.kmregistratie.domain.provider.LocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FusedLocationProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationProvider {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationPoint? {
        return try {
            val cts = CancellationTokenSource()
            val location = fusedClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .await()
            location?.let { LocationPoint(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }
}
