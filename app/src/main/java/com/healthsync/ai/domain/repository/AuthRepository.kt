package com.healthsync.ai.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isAuthenticated: Flow<Boolean>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun signOut()
    fun getCurrentUserId(): String?
}
