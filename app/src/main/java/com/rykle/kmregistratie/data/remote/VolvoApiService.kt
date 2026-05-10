package com.rykle.kmregistratie.data.remote

import com.rykle.kmregistratie.data.remote.dto.OdometerResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface VolvoApiService {

    @GET("connected-vehicle/v2/vehicles/{vin}/odometer")
    suspend fun getOdometer(
        @Path("vin") vin: String,
        @Header("Authorization") authorization: String,
        @Header("VCC-Api-Key") vccApiKey: String
    ): OdometerResponse
}
