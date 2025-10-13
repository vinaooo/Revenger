package com.vinaooo.revenger.repositories

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing application preferences. Provides a clean abstraction over
 * SharedPreferences with reactive state.
 */
interface PreferencesRepository {
    val audioEnabled: StateFlow<Boolean>
    val gameSpeed: StateFlow<Int>
    val shaderName: StateFlow<String>

    suspend fun setAudioEnabled(enabled: Boolean)
    suspend fun setGameSpeed(speed: Int)
    suspend fun setShaderName(name: String)

    // Synchronous getters for immediate access (when coroutines not available)
    fun getAudioEnabledSync(): Boolean
    fun getGameSpeedSync(): Int
    fun getShaderNameSync(): String
}
