package com.vinaooo.revenger.viewmodels.menu

import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.viewmodels.AudioViewModel
import com.vinaooo.revenger.viewmodels.ShaderViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MenuToggleActions]'s [RetroView]/[SpeedController] dependencies are read through provider
 * lambdas backed by local vars here, standing in for `GameActivityViewModel`'s own fields.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuToggleActions_test {

    private var currentRetroView: RetroView? = null
    private var currentSpeedController: SpeedController? = null
    private lateinit var audioViewModel: AudioViewModel
    private lateinit var shaderViewModel: ShaderViewModel
    private lateinit var actions: MenuToggleActions

    @Before
    fun setUp() {
        currentRetroView = null
        currentSpeedController = null
        audioViewModel = mockk(relaxed = true)
        shaderViewModel = mockk(relaxed = true)
        actions =
                MenuToggleActions(
                        retroView = { currentRetroView },
                        audioViewModel = audioViewModel,
                        speedController = { currentSpeedController },
                        shaderViewModel = shaderViewModel
                )
    }

    @Test
    fun `toggleAudio com retroView chama audioViewModel toggleAudio com a view`() {
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        currentRetroView = retroView

        actions.toggleAudio()

        verify(exactly = 1) { audioViewModel.toggleAudio(glRetroView) }
    }

    @Test
    fun `toggleAudio sem retroView nao chama audioViewModel`() {
        currentRetroView = null

        actions.toggleAudio()

        verify(exactly = 0) { audioViewModel.toggleAudio(any()) }
    }

    @Test
    fun `toggleSpeed com retroView chama toggleFastForward no speedController`() {
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        currentRetroView = retroView
        val speedController = mockk<SpeedController>(relaxed = true)
        currentSpeedController = speedController

        actions.toggleSpeed()

        verify(exactly = 1) { speedController.toggleFastForward(glRetroView) }
    }

    @Test
    fun `toggleSpeed sem retroView nao chama speedController`() {
        currentRetroView = null
        val speedController = mockk<SpeedController>(relaxed = true)
        currentSpeedController = speedController

        actions.toggleSpeed()

        verify(exactly = 0) { speedController.toggleFastForward(any()) }
    }

    @Test
    fun `toggleSpeed com retroView mas sem speedController nao lanca`() {
        currentRetroView = mockk(relaxed = true)
        currentSpeedController = null

        actions.toggleSpeed()
    }

    @Test
    fun `toggleShader chama shaderViewModel toggleShader independente do retroView`() {
        currentRetroView = null

        actions.toggleShader()

        verify(exactly = 1) { shaderViewModel.toggleShader() }
    }
}
