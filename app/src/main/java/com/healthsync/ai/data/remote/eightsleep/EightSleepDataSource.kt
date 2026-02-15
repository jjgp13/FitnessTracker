package com.healthsync.ai.data.remote.eightsleep

import com.healthsync.ai.data.remote.eightsleep.dto.EightSleepTokenRequest
import com.healthsync.ai.data.remote.eightsleep.dto.SleepMetricsResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EightSleepDataSource @Inject constructor(
    private val authService: EightSleepAuthService,
    private val apiService: EightSleepApiService,
    private val tokenManager: EightSleepTokenManager
) {
    companion object {
        private const val CLIENT_ID = "0894c7f33bb94800a03f1f4df13a4f38"
        private const val CLIENT_SECRET = "f0954a3ed5763ba3d06834c73731a32f15f168f47d4f164751275def86db0c76"
    }

    suspend fun authenticate(email: String, password: String): Result<Unit> {
        return try {
            val response = authService.authenticate(
                EightSleepTokenRequest(
                    clientId = CLIENT_ID,
                    clientSecret = CLIENT_SECRET,
                    email = email,
                    password = password
                )
            )
            tokenManager.saveToken(response.accessToken, response.userId, response.expiresIn)
            tokenManager.saveCredentials(email, password)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun ensureAuthenticated() {
        if (!tokenManager.isTokenValid() && tokenManager.hasCredentials()) {
            val email = tokenManager.getEmail()!!
            val password = tokenManager.getPassword()!!
            authenticate(email, password)
        }
    }

    suspend fun getSleepMetrics(startDate: String? = null, endDate: String? = null): Result<SleepMetricsResponse> {
        return try {
            ensureAuthenticated()
            val token = tokenManager.getAccessToken()
                ?: return Result.failure(IllegalStateException("Not authenticated with Eight Sleep"))
            val userId = tokenManager.getUserId()
                ?: return Result.failure(IllegalStateException("No Eight Sleep user ID"))

            val response = apiService.getSleepMetrics(
                token = "Bearer $token",
                userId = userId,
                startDate = startDate,
                endDate = endDate
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isAuthenticated(): Boolean = tokenManager.isTokenValid()
}
