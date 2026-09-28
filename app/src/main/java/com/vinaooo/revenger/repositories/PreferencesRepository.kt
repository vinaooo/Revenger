package com.vinaooo.revenger.repositories

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing application preferences. Provides a clean abstraction over
 * SharedPreferences with reactive state.
 */
interface PreferencesRepository {
    val audioEnabled: StateFlow<Boolean>
    val shaderName: StateFlow<String>
    val fastForwardEnabled: StateFlow<Boolean>

    suspend fun setAudioEnabled(enabled: Boolean)
    suspend fun setShaderName(name: String)
    suspend fun setFastForwardEnabled(enabled: Boolean)

    // Synchronous getters for immediate access (when coroutines not available)
    fun getAudioEnabledSync(): Boolean
    fun getShaderNameSync(): String
    fun getFastForwardEnabledSync(): Boolean
}
