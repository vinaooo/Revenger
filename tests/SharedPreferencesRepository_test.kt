package com.vinaooo.revenger.repositories

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.utils.PreferencesConstants
import com.vinaooo.revenger.utils.ShaderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SharedPreferencesRepository_test {

    private lateinit var prefs: android.content.SharedPreferences
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    private fun newRepository() = SharedPreferencesRepository(prefs, CoroutineScope(dispatcher))

    // --- Carga inicial ---

    @Test
    fun `carrega valores padrao quando as preferencias estao vazias`() {
        val repository = newRepository()

        assertTrue(repository.getAudioEnabledSync())
        assertEquals(1, repository.getGameSpeedSync())
        assertEquals(ShaderType.SHARP.configName, repository.getShaderNameSync())
        assertFalse(repository.getFastForwardEnabledSync())

        assertTrue(repository.audioEnabled.value)
        assertEquals(1, repository.gameSpeed.value)
        assertEquals(ShaderType.SHARP.configName, repository.shaderName.value)
        assertFalse(repository.fastForwardEnabled.value)
    }

    @Test
    fun `carrega valores ja existentes nas preferencias`() {
        prefs.edit()
            .putBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, false)
            .putInt(PreferencesConstants.PREF_FRAME_SPEED, 4)
            .putString(PreferencesConstants.PREF_SHADER_NAME, ShaderType.CRT.configName)
            .putBoolean(PreferencesConstants.PREF_FAST_FORWARD_ENABLED, true)
            .commit()

        val repository = newRepository()

        assertFalse(repository.audioEnabled.value)
        assertEquals(4, repository.gameSpeed.value)
        assertEquals(ShaderType.CRT.configName, repository.shaderName.value)
        assertTrue(repository.fastForwardEnabled.value)
    }

    // --- Setters: persistem e atualizam o StateFlow ---

    @Test
    fun `setAudioEnabled persiste e atualiza o StateFlow`() = runTest(dispatcher) {
        val repository = newRepository()

        repository.setAudioEnabled(false)

        assertFalse(repository.audioEnabled.value)
        assertFalse(prefs.getBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, true))
    }

    @Test
    fun `setGameSpeed persiste e atualiza o StateFlow`() = runTest(dispatcher) {
        val repository = newRepository()

        repository.setGameSpeed(4)

        assertEquals(4, repository.gameSpeed.value)
        assertEquals(4, prefs.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1))
    }

    @Test
    fun `setShaderName persiste e atualiza o StateFlow`() = runTest(dispatcher) {
        val repository = newRepository()

        repository.setShaderName(ShaderType.LCD.configName)

        assertEquals(ShaderType.LCD.configName, repository.shaderName.value)
        assertEquals(
            ShaderType.LCD.configName,
            prefs.getString(PreferencesConstants.PREF_SHADER_NAME, null),
        )
    }

    @Test
    fun `setFastForwardEnabled persiste e atualiza o StateFlow`() = runTest(dispatcher) {
        val repository = newRepository()

        repository.setFastForwardEnabled(true)

        assertTrue(repository.fastForwardEnabled.value)
        assertTrue(prefs.getBoolean(PreferencesConstants.PREF_FAST_FORWARD_ENABLED, false))
    }

    // --- Getters síncronos refletem o SharedPreferences diretamente ---

    @Test
    fun `getters sincronos refletem alteracoes feitas fora do repositorio`() {
        val repository = newRepository()

        prefs.edit().putInt(PreferencesConstants.PREF_FRAME_SPEED, 8).commit()

        assertEquals(8, repository.getGameSpeedSync())
    }
}
