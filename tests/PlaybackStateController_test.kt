package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.viewmodels.AudioViewModel
import com.vinaooo.revenger.viewmodels.ShaderViewModel
import com.vinaooo.revenger.viewmodels.SpeedViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [PlaybackStateController] is thin delegation to the specialized audio/speed/shader ViewModels
 * (each with their own test suite covering the actual logic) -- these tests pin only which
 * dependency each method forwards to.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PlaybackStateController_test {

    private lateinit var audioViewModel: AudioViewModel
    private lateinit var speedViewModel: SpeedViewModel
    private lateinit var shaderViewModel: ShaderViewModel
    private lateinit var controller: PlaybackStateController

    @Before
    fun setUp() {
        audioViewModel = mockk(relaxed = true)
        speedViewModel = mockk(relaxed = true)
        shaderViewModel = mockk(relaxed = true)
        controller =
                PlaybackStateController(
                        audioViewModel = audioViewModel,
                        speedViewModel = speedViewModel,
                        shaderViewModel = shaderViewModel
                )
    }

    @Test
    fun `getAudioState delega para audioViewModel`() {
        every { audioViewModel.getAudioState() } returns true

        assertTrue(controller.getAudioState())
    }

    @Test
    fun `getFastForwardState delega para speedViewModel`() {
        every { speedViewModel.getFastForwardState() } returns true

        assertTrue(controller.getFastForwardState())
    }

    @Test
    fun `onToggleShader delega para shaderViewModel`() {
        every { shaderViewModel.toggleShader() } returns "crt"

        assertEquals("crt", controller.onToggleShader())
    }

    @Test
    fun `getShaderState delega para shaderViewModel`() {
        every { shaderViewModel.getShaderState() } returns "sharp"

        assertEquals("sharp", controller.getShaderState())
    }

    @Test
    fun `getShaderDisplayName delega para shaderViewModel`() {
        every { shaderViewModel.getCurrentShaderDisplayName() } returns "Sharp"

        assertEquals("Sharp", controller.getShaderDisplayName())
    }
}
