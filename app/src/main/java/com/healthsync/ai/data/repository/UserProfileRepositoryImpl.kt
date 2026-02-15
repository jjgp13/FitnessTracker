package com.healthsync.ai.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.healthsync.ai.data.local.db.dao.UserProfileDao
import com.healthsync.ai.data.local.mapper.toDomain
import com.healthsync.ai.data.local.mapper.toEntity
import com.healthsync.ai.domain.model.UserProfile
import com.healthsync.ai.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val firestore: FirebaseFirestore
) : UserProfileRepository {

    override fun getUserProfile(): Flow<UserProfile?> {
        return userProfileDao.getProfile().map { it?.toDomain() }
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        userProfileDao.insert(profile.toEntity())
        syncToFirestore(profile)
    }

    override suspend fun isOnboardingComplete(): Boolean {
        return userProfileDao.count() > 0
    }

    private suspend fun syncToFirestore(profile: UserProfile) {
        try {
            val data = mapOf(
                "displayName" to profile.displayName,
                "age" to profile.age,
                "primarySports" to profile.primarySports,
                "strengthFocus" to profile.strengthFocus,
                "dietaryPreferences" to profile.dietaryPreferences,
                "carbsPercent" to profile.macroSplit.carbsPercent,
                "proteinPercent" to profile.macroSplit.proteinPercent,
                "fatPercent" to profile.macroSplit.fatPercent
            )
            firestore.collection("users").document(profile.id).set(data).await()
        } catch (_: Exception) {
            // Firestore sync is best-effort; local Room is source of truth
        }
    }
}
