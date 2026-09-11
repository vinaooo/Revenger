package com.vinaooo.revenger.controllers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.utils.PreferencesConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SpeedController_test {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var retroView: GLRetroView
    private lateinit var controller: SpeedController

    private fun newController(fastForwardMultiplier: Int = 4): SpeedController {
        val appConfig = mockk<AppConfig>()
        every { appConfig.getFastForwardMultiplier() } returns fastForwardMultiplier
        val context = ApplicationProvider.getApplicationContext<Context>()
        return SpeedController(context, prefs, appConfig)
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        retroView = mockk(relaxed = true)
        controller = newController()
    }

    // --- toggleFastForward ---

    @Test
    fun `toggleFastForward liga o fast forward a partir da velocidade normal`() {
        every { retroView.frameSpeed } returns 1

        val active = controller.toggleFastForward(retroView)

        assertTrue(active)
        verify { retroView.frameSpeed = 4 }
        assertEquals(4, prefs.getInt(PreferencesConstants.PREF_FRAME_SPEED, 1))
    }

    @Test
    fun `toggleFastForward desliga o fast forward voltando para velocidade normal`() {
        every { retroView.frameSpeed } returns 4

        val active = controller.toggleFastForward(retroView)

        assertFalse(active)
        verify { retroView.frameSpeed = 1 }
    }

    // --- pause / resume ---

    @Test
    fun `pause zera o frameSpeed`() {
        controller.pause(retroView)

        verify { retroView.frameSpeed = 0 }
    }

    @Test
    fun `resume volta o frameSpeed para 1`() {
        controller.resume(retroView)

        verify { retroView.frameSpeed = 1 }
    }

    // --- isFastForwardActive ---

    @Test
    fun `isFastForwardActive e verdadeiro quando frameSpeed maior que 1`() {
        every { retroView.frameSpeed } returns 4

        assertTrue(controller.isFastForwardActive(retroView))
    }

    @Test
    fun `isFastForwardActive e falso na velocidade normal`() {
        every { retroView.frameSpeed } returns 1

        assertFalse(controller.isFastForwardActive(retroView))
    }

    // --- getCurrentSpeed / getFastForwardState (a partir das preferencias) ---

    @Test
    fun `getCurrentSpeed retorna 1 quando nao ha nada salvo`() {
        assertEquals(1, controller.getCurrentSpeed())
    }

    @Test
    fun `getCurrentSpeed trata velocidade salva 0 (pausado) como velocidade normal`() {
        prefs.edit().putInt(PreferencesConstants.PREF_FRAME_SPEED, 0).commit()

        assertEquals(1, controller.getCurrentSpeed())
    }

    @Test
    fun `getFastForwardState reflete a velocidade salva nas preferencias`() {
        prefs.edit().putInt(PreferencesConstants.PREF_FRAME_SPEED, 4).commit()

        assertTrue(controller.getFastForwardState())
    }

    // --- initializeSpeedState / restoreSpeedFromPreferences: nunca aplicam frameSpeed=0 ---

    @Test
    fun `initializeSpeedState nunca aplica frameSpeed 0 mesmo se o valor salvo for pausado`() {
        prefs.edit().putInt(PreferencesConstants.PREF_FRAME_SPEED, 0).commit()

        controller.initializeSpeedState(retroView)

        verify { retroView.frameSpeed = 1 }
    }

    @Test
    fun `initializeSpeedState aplica a velocidade salva quando nao esta pausada`() {
        prefs.edit().putInt(PreferencesConstants.PREF_FRAME_SPEED, 4).commit()

        controller.initializeSpeedState(retroView)

        verify { retroView.frameSpeed = 4 }
    }

    @Test
    fun `restoreSpeedFromPreferences nao lanca quando o retroView e nulo`() {
        controller.restoreSpeedFromPreferences(null)
    }

    @Test
    fun `restoreSpeedFromPreferences nunca aplica frameSpeed 0`() {
        prefs.edit().putInt(PreferencesConstants.PREF_FRAME_SPEED, 0).commit()

        controller.restoreSpeedFromPreferences(retroView)

        verify { retroView.frameSpeed = 1 }
    }

    // --- getSpeedStateDescription ---

    @Test
    fun `getSpeedStateDescription reflete o estado salvo`() {
        prefs.edit().putInt(PreferencesConstants.PREF_FRAME_SPEED, 4).commit()

        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(
            context.getString(com.vinaooo.revenger.R.string.fast_forward_active),
            controller.getSpeedStateDescription(),
        )
    }

    // --- getFastForwardSpeed ---

    @Test
    fun `getFastForwardSpeed retorna o multiplicador configurado no AppConfig`() {
        val custom = newController(fastForwardMultiplier = 8)

        assertEquals(8, custom.getFastForwardSpeed())
    }
}
