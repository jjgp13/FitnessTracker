package com.healthsync.ai.data.remote.eightsleep

import com.healthsync.ai.data.remote.eightsleep.dto.EightSleepTokenRequest
import com.healthsync.ai.data.remote.eightsleep.dto.EightSleepTokenResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface EightSleepAuthService {
    @POST("v1/tokens")
    suspend fun authenticate(@Body request: EightSleepTokenRequest): EightSleepTokenResponse
}
