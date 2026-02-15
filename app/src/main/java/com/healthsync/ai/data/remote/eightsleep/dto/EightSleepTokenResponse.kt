package com.healthsync.ai.data.remote.eightsleep.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EightSleepTokenRequest(
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
    val email: String,
    val password: String
)

@Serializable
data class EightSleepTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("userId") val userId: String,
    @SerialName("token_type") val tokenType: String
)
