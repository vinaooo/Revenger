package com.vinaooo.revenger.utils

import android.app.Activity
import android.content.Context
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.repositories.Storage
import com.vinaooo.revenger.retroview.RetroView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [RetroViewUtils.preserveEmulatorState] runs when the game is paused or closed: it writes the
 * SRAM and temp state, and remembers the speed and audio. A speed of 0 means the menu had paused
 * the game, so it must never be remembered: the next launch would start on a black screen.
 * [RetroViewUtils.hasSaveState] decides whether the legacy single save can be loaded.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RetroViewUtils_test {

    private lateinit var activity: Activity
    private lateinit var stateFiles: StateFileOperations
    private lateinit var utils: RetroViewUtils
    private lateinit var glRetroView: GLRetroView
    private lateinit var retroView: RetroView

    private val prefs get() = activity.getPreferences(Context.MODE_PRIVATE)
    private val legacyState get() = Storage.getInstance(activity).state

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        prefs.edit().clear().commit()
        legacyState.delete()
        stateFiles = mockk(relaxed = true)
        utils = RetroViewUtils(activity, stateFiles)
        glRetroView = mockk(relaxed = true)
        retroView = mockk(relaxed = true)
        every { retroView.view } returns glRetroView
    }

    @After
    fun tearDown() {
        legacyState.delete()
    }

    @Test
    fun `preserveEmulatorState grava a SRAM e o estado temporario`() {
        utils.preserveEmulatorState(retroView)

        verifyOrder {
            stateFiles.saveSRAM(retroView)
            stateFiles.saveTempState(retroView)
        }
    }

    @Test
    fun `preserveEmulatorState lembra a velocidade e o audio atuais`() {
        every { glRetroView.frameSpeed } returns 2
        every { glRetroView.audioEnabled } returns false

        utils.preserveEmulatorState(retroView)

        assertEquals(2, prefs.getInt(PreferencesConstants.PREF_FRAME_SPEED, -1))
        assertFalse(prefs.getBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, true))
    }

    @Test
    fun `preserveEmulatorState com o jogo pausado pelo menu mantem a ultima velocidade valida`() {
        prefs.edit().putInt(PreferencesConstants.PREF_FRAME_SPEED, 3).commit()
        every { glRetroView.frameSpeed } returns 0
        every { glRetroView.audioEnabled } returns true

        utils.preserveEmulatorState(retroView)

        assertEquals(3, prefs.getInt(PreferencesConstants.PREF_FRAME_SPEED, -1))
        assertTrue(prefs.getBoolean(PreferencesConstants.PREF_AUDIO_ENABLED, false))
    }

    @Test
    fun `preserveEmulatorState pausado sem velocidade salva nao grava velocidade nenhuma`() {
        every { glRetroView.frameSpeed } returns 0

        utils.preserveEmulatorState(retroView)

        assertFalse(prefs.contains(PreferencesConstants.PREF_FRAME_SPEED))
    }

    @Test
    fun `hasSaveState sem arquivo e false`() {
        assertFalse(utils.hasSaveState())
    }

    @Test
    fun `hasSaveState com arquivo vazio e false`() {
        legacyState.writeBytes(byteArrayOf())

        assertFalse(utils.hasSaveState())
    }

    @Test
    fun `hasSaveState com arquivo gravado e true`() {
        legacyState.writeBytes(byteArrayOf(1, 2))

        assertTrue(utils.hasSaveState())
    }

    @Test
    fun `as operacoes de arquivo sao delegadas ao StateFileOperations`() {
        utils.loadState(retroView)
        utils.saveState(retroView)

        verify(exactly = 1) { stateFiles.loadState(retroView) }
        verify(exactly = 1) { stateFiles.saveState(retroView) }
    }
}
