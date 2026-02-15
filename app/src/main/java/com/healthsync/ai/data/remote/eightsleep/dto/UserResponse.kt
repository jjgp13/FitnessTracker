package com.healthsync.ai.data.remote.eightsleep.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val userId: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)
