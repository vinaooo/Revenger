package com.vinaooo.revenger.controllers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.utils.PreferencesConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [FrameSpeedPreferencesStore] was split out of [SpeedController] purely to keep that class under
 * the project's function-count threshold; these tests replicate the frame-speed preference
 * coverage [SpeedController_test] already had for [getCurrentSpeed]/[getFastForwardState] before
 * the extraction.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class FrameSpeedPreferencesStore_test {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var store: FrameSpeedPreferencesStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = context.getSharedPreferences("test_prefs_frame_speed_store", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        store = FrameSpeedPreferencesStore(prefs)
    }

    @Test
    fun `getCurrentSpeed retorna 1 quando nao ha nada salvo`() {
        assertEquals(1, store.getCurrentSpeed())
    }

    @Test
    fun `getCurrentSpeed trata velocidade salva 0 (pausado) como velocidade normal`() {
        prefs.edit().putInt(PreferencesConstants.PREF_FRAME_SPEED, 0).commit()

        assertEquals(1, store.getCurrentSpeed())
    }

    @Test
    fun `getCurrentSpeed retorna a velocidade salva quando maior que 0`() {
        prefs.edit().putInt(PreferencesConstants.PREF_FRAME_SPEED, 4).commit()

        assertEquals(4, store.getCurrentSpeed())
    }

    @Test
    fun `getFastForwardState e falso quando nao ha nada salvo`() {
        assertFalse(store.getFastForwardState())
    }

    @Test
    fun `getFastForwardState reflete a velocidade salva nas preferencias`() {
        prefs.edit().putInt(PreferencesConstants.PREF_FRAME_SPEED, 4).commit()

        assertTrue(store.getFastForwardState())
    }

    @Test
    fun `saveSpeedState grava a velocidade nas preferencias`() {
        store.saveSpeedState(4)

        assertEquals(4, prefs.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1))
    }
}
