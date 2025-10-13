package com.vinaooo.revenger.repositories

import android.content.SharedPreferences
import com.vinaooo.revenger.utils.PreferencesConstants
import com.vinaooo.revenger.utils.ShaderType
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class PreferencesRepositoryTest {

    @MockK private lateinit var mockSharedPreferences: SharedPreferences

    @MockK private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var repository: SharedPreferencesRepository
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        // Setup SharedPreferences mock
        every {
            mockSharedPreferences.getBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, true)
        } returns true
        every { mockSharedPreferences.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1) } returns 1
        every {
            mockSharedPreferences.getString(
                    PreferencesConstants.PREF_SHADER_NAME,
                    ShaderType.SHARP.configName
            )
        } returns ShaderType.SHARP.configName

        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs

        repository = SharedPreferencesRepository(mockSharedPreferences, testScope)
    }

    @Test
    fun testInitialValuesLoaded() =
            testScope.runTest {
                // Wait for initial values to be loaded
                testScheduler.advanceUntilIdle()

                assertEquals(true, repository.audioEnabled.value)
                assertEquals(1, repository.gameSpeed.value)
                assertEquals(ShaderType.SHARP.configName, repository.shaderName.value)
            }

    @Test
    fun testSetAudioEnabled() =
            testScope.runTest {
                repository.setAudioEnabled(false)

                verify { mockEditor.putBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, false) }
                verify { mockEditor.apply() }

                assertEquals(false, repository.audioEnabled.value)
            }

    @Test
    fun testSetGameSpeed() =
            testScope.runTest {
                repository.setGameSpeed(2)

                verify { mockEditor.putInt(PreferencesConstants.PREF_FRAME_SPEED, 2) }
                verify { mockEditor.apply() }

                assertEquals(2, repository.gameSpeed.value)
            }

    @Test
    fun testSetShaderName() =
            testScope.runTest {
                val newShader = ShaderType.CRT.configName
                repository.setShaderName(newShader)

                verify { mockEditor.putString(PreferencesConstants.PREF_SHADER_NAME, newShader) }
                verify { mockEditor.apply() }

                assertEquals(newShader, repository.shaderName.value)
            }

    @Test
    fun testGetAudioEnabledSync() {
        val result = repository.getAudioEnabledSync()
        assertEquals(true, result)
        verify { mockSharedPreferences.getBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, true) }
    }

    @Test
    fun testGetGameSpeedSync() {
        val result = repository.getGameSpeedSync()
        assertEquals(1, result)
        verify { mockSharedPreferences.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1) }
    }

    @Test
    fun testGetShaderNameSync() {
        val result = repository.getShaderNameSync()
        assertEquals(ShaderType.SHARP.configName, result)
        verify {
            mockSharedPreferences.getString(
                    PreferencesConstants.PREF_SHADER_NAME,
                    ShaderType.SHARP.configName
            )
        }
    }
}
