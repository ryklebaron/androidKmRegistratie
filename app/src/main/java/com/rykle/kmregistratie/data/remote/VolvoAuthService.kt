package com.rykle.kmregistratie.data.remote

import com.rykle.kmregistratie.data.remote.dto.TokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface VolvoAuthService {

    @FormUrlEncoded
    @POST("as/token.oauth2")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String,
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
    ): TokenResponse

    @FormUrlEncoded
    @POST("as/token.oauth2")
    suspend fun exchangeCode(
        @Field("grant_type") grantType: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code_verifier") codeVerifier: String,
    ): TokenResponse
}
