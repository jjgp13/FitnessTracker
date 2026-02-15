package com.healthsync.ai.data.remote.eightsleep

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class EightSleepTokenManager @Inject constructor(
    @Named("encrypted") private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_ACCESS_TOKEN = "eight_sleep_access_token"
        private const val KEY_USER_ID = "eight_sleep_user_id"
        private const val KEY_EXPIRY = "eight_sleep_token_expiry"
        private const val KEY_EMAIL = "eight_sleep_email"
        private const val KEY_PASSWORD = "eight_sleep_password"
        private const val BUFFER_SECONDS = 300L
    }

    fun saveToken(token: String, userId: String, expiresIn: Long) {
        val expiryTime = System.currentTimeMillis() / 1000 + expiresIn
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putLong(KEY_EXPIRY, expiryTime)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun isTokenValid(): Boolean {
        val expiry = prefs.getLong(KEY_EXPIRY, 0)
        val now = System.currentTimeMillis() / 1000
        return getAccessToken() != null && now < (expiry - BUFFER_SECONDS)
    }

    fun saveCredentials(email: String, password: String) {
        prefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getPassword(): String? = prefs.getString(KEY_PASSWORD, null)

    fun hasCredentials(): Boolean = getEmail() != null && getPassword() != null

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
