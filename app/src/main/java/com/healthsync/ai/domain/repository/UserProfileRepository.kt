package com.healthsync.ai.domain.repository

import com.healthsync.ai.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun isOnboardingComplete(): Boolean
}
