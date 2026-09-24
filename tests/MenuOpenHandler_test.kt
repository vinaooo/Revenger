package com.vinaooo.revenger.viewmodels.menu

import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.RetroViewUtils
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MenuOpenHandler]'s dependencies are read through provider lambdas backed by local vars here,
 * standing in for `GameActivityViewModel`'s own fields.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuOpenHandler_test {

    private var currentRetroView: RetroView? = null
    private var currentRetroViewUtils: RetroViewUtils? = null
    private var currentSpeedController: SpeedController? = null
    private var captureScreenshotCalled = 0
    private lateinit var handler: MenuOpenHandler

    @Before
    fun setUp() {
        currentRetroView = null
        currentRetroViewUtils = null
        currentSpeedController = null
        captureScreenshotCalled = 0
        handler =
                MenuOpenHandler(
                        retroView = { currentRetroView },
                        retroViewUtils = { currentRetroViewUtils },
                        speedController = { currentSpeedController },
                        captureScreenshotForSaveState = { captureScreenshotCalled++ }
                )
    }

    @Test
    fun `captura o screenshot preserva o estado e pausa o emulador quando ha retroView`() {
        val retroView = mockk<RetroView>(relaxed = true)
        currentRetroView = retroView
        val retroViewUtils = mockk<RetroViewUtils>(relaxed = true)
        currentRetroViewUtils = retroViewUtils
        val speedController = mockk<SpeedController>(relaxed = true)
        currentSpeedController = speedController
        val activity = mockk<FragmentActivity>(relaxed = true)

        handler.handleMenuOpened(activity)

        assertEquals(1, captureScreenshotCalled)
        verify(exactly = 1) { retroViewUtils.preserveEmulatorState(retroView) }
        verify(exactly = 1) { speedController.pause(retroView.view) }
    }

    @Test
    fun `captura o screenshot mesmo sem retroView e nao lanca`() {
        val activity = mockk<FragmentActivity>(relaxed = true)

        handler.handleMenuOpened(activity)

        assertEquals(1, captureScreenshotCalled)
    }
}
