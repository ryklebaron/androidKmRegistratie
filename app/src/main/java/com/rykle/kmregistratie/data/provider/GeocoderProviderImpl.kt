package com.rykle.kmregistratie.data.provider

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.rykle.kmregistratie.domain.model.LocationPoint
import com.rykle.kmregistratie.domain.provider.GeocoderProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

class GeocoderProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GeocoderProvider {

    override suspend fun getAddress(lat: Double, lng: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                cont.resume(addresses.firstOrNull())
                            }
                            override fun onError(errorMessage: String?) {
                                cont.resume(null)
                            }
                        })
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
                }
                address?.let { formatAddress(it) }
            } catch (e: Exception) {
                null
            }
        }

    override suspend fun getLocationFromAddress(address: String): LocationPoint? =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocationName(address, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                cont.resume(addresses.firstOrNull())
                            }
                            override fun onError(errorMessage: String?) {
                                cont.resume(null)
                            }
                        })
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(address, 1)?.firstOrNull()
                }
                result?.let { LocationPoint(it.latitude, it.longitude) }
            } catch (e: Exception) {
                null
            }
        }

    private fun formatAddress(address: Address): String? {
        val street = buildString {
            address.thoroughfare?.let { append(it) }
            address.subThoroughfare?.let { append(" $it") }
        }.takeIf { it.isNotBlank() }
        val city = address.locality ?: address.subAdminArea
        val province = address.adminArea
        return when {
            street != null && city != null && province != null -> "$street, $city, $province"
            street != null && city != null -> "$street, $city"
            street != null -> street
            city != null -> city
            else -> null
        }
    }
}
