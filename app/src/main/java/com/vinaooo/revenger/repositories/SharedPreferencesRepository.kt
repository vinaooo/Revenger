package com.vinaooo.revenger.repositories

import android.content.SharedPreferences
import com.vinaooo.revenger.utils.PreferencesConstants
import com.vinaooo.revenger.utils.ShaderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * SharedPreferences-based implementation of PreferencesRepository. Manages application preferences
 * with reactive state updates.
 */
class SharedPreferencesRepository(
        private val sharedPreferences: SharedPreferences,
        private val coroutineScope: CoroutineScope
) : PreferencesRepository {

    // Reactive state flows
    private val _audioEnabled = MutableStateFlow(false)
    override val audioEnabled: StateFlow<Boolean> = _audioEnabled.asStateFlow()

    private val _gameSpeed = MutableStateFlow(1)
    override val gameSpeed: StateFlow<Int> = _gameSpeed.asStateFlow()

    private val _shaderName = MutableStateFlow(ShaderType.SHARP.configName)
    override val shaderName: StateFlow<String> = _shaderName.asStateFlow()

    private val _fastForwardEnabled = MutableStateFlow(false)
    override val fastForwardEnabled: StateFlow<Boolean> = _fastForwardEnabled.asStateFlow()

    init {
        // Load initial values
        loadInitialValues()
    }

    private fun loadInitialValues() {
        coroutineScope.launch {
            _audioEnabled.value =
                    sharedPreferences.getBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, true)
            _gameSpeed.value = sharedPreferences.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1)
            _shaderName.value =
                    sharedPreferences.getString(
                            PreferencesConstants.PREF_SHADER_NAME,
                            ShaderType.SHARP.configName
                    )
                            ?: ShaderType.SHARP.configName
            _fastForwardEnabled.value =
                    sharedPreferences.getBoolean(
                            PreferencesConstants.PREF_FAST_FORWARD_ENABLED,
                            false
                    )
        }
    }

    override suspend fun setAudioEnabled(enabled: Boolean) {
        sharedPreferences
                .edit()
                .putBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, enabled)
                .apply()
        _audioEnabled.value = enabled
    }

    override suspend fun setGameSpeed(speed: Int) {
        sharedPreferences.edit().putInt(PreferencesConstants.PREF_FRAME_SPEED, speed).apply()
        _gameSpeed.value = speed
    }

    override suspend fun setShaderName(name: String) {
        sharedPreferences.edit().putString(PreferencesConstants.PREF_SHADER_NAME, name).apply()
        _shaderName.value = name
    }

    override suspend fun setFastForwardEnabled(enabled: Boolean) {
        sharedPreferences
                .edit()
                .putBoolean(PreferencesConstants.PREF_FAST_FORWARD_ENABLED, enabled)
                .apply()
        _fastForwardEnabled.value = enabled
    }

    override fun getAudioEnabledSync(): Boolean {
        return sharedPreferences.getBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, true)
    }

    override fun getGameSpeedSync(): Int {
        return sharedPreferences.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1)
    }

    override fun getShaderNameSync(): String {
        return sharedPreferences.getString(
                PreferencesConstants.PREF_SHADER_NAME,
                ShaderType.SHARP.configName
        )
                ?: ShaderType.SHARP.configName
    }

    override fun getFastForwardEnabledSync(): Boolean {
        return sharedPreferences.getBoolean(PreferencesConstants.PREF_FAST_FORWARD_ENABLED, false)
    }
}
