package com.rykle.kmregistratie.data.remote

import com.rykle.kmregistratie.data.remote.dto.TokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface VolvoAuthService {

    @FormUrlEncoded
    @POST("realms/volvocars/protocol/openid-connect/token")
    suspend fun refreshToken(
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String,
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String,
    ): TokenResponse

    @FormUrlEncoded
    @POST("realms/volvocars/protocol/openid-connect/token")
    suspend fun exchangeCode(
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("client_id") clientId: String,
        @Field("code_verifier") codeVerifier: String,
    ): TokenResponse
}
