package com.healthsync.ai.data.remote.eightsleep

import com.healthsync.ai.data.remote.eightsleep.dto.SleepMetricsResponse
import com.healthsync.ai.data.remote.eightsleep.dto.UserResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface EightSleepApiService {
    @GET("v1/users/{userId}")
    suspend fun getUser(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): UserResponse

    @GET("v1/users/{userId}/sleeps")
    suspend fun getSleepMetrics(
        @Header("Authorization") token: String,
        @Path("userId") userId: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): SleepMetricsResponse
}
