package com.rykle.kmregistratie.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OdometerResponse(
    @SerializedName("data") val data: OdometerData?
)

data class OdometerData(
    @SerializedName("odometer") val odometer: OdometerValue?
)

data class OdometerValue(
    @SerializedName("value") val value: Int?,
    @SerializedName("unit") val unit: String?
)
