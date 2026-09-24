package com.vinaooo.revenger.viewmodels.menu

import android.os.Looper
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [MenuCloseHandler]'s dependencies are read through provider lambdas backed by local vars here,
 * standing in for `GameActivityViewModel`'s own fields. [ScreenshotCaptureUtil] is a singleton
 * object, mocked via `mockkObject` since this class calls its static `promoteCachedFullToPipFrame`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuCloseHandler_test {

    private var currentRetroView: RetroView? = null
    private var currentSpeedController: SpeedController? = null
    private lateinit var controllerInput: ControllerInput
    private var clearCachedScreenshotCalled = 0
    private var hideLoadPreviewCalled = 0
    private lateinit var handler: MenuCloseHandler

    @Before
    fun setUp() {
        mockkObject(ScreenshotCaptureUtil)
        every { ScreenshotCaptureUtil.promoteCachedFullToPipFrame() } just Runs
        currentRetroView = null
        currentSpeedController = null
        controllerInput = ControllerInput()
        clearCachedScreenshotCalled = 0
        hideLoadPreviewCalled = 0
        handler =
                MenuCloseHandler(
                        retroView = { currentRetroView },
                        speedController = { currentSpeedController },
                        controllerInput = { controllerInput },
                        clearCachedScreenshot = { clearCachedScreenshotCalled++ },
                        hideLoadPreview = { hideLoadPreviewCalled++ },
                        viewModelScope = CoroutineScope(Dispatchers.Main)
                )
    }

    @After
    fun tearDown() {
        unmockkObject(ScreenshotCaptureUtil)
    }

    @Test
    fun `esconde o preview imediatamente e limpa o cache quando nao ha retroView`() {
        val activity = mockk<FragmentActivity>(relaxed = true)

        handler.handleMenuClosed(activity, null)

        assertEquals(1, hideLoadPreviewCalled)
        assertEquals(1, clearCachedScreenshotCalled)
    }

    @Test
    fun `so esconde o preview depois do FrameRendered quando ha retroView`() {
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        val eventsFlow = MutableSharedFlow<GLRetroView.GLRetroEvents>(extraBufferCapacity = 1)
        every { glRetroView.getGLRetroEvents() } returns eventsFlow
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        currentRetroView = retroView
        val activity = mockk<FragmentActivity>(relaxed = true)

        handler.handleMenuClosed(activity, null)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(0, hideLoadPreviewCalled)

        eventsFlow.tryEmit(GLRetroView.GLRetroEvents.FrameRendered)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, hideLoadPreviewCalled)
    }

    @Test
    fun `bloqueia apenas o botao que fechou o menu durante o periodo de graca`() {
        val activity = mockk<FragmentActivity>(relaxed = true)
        val closingButton = KeyEvent.KEYCODE_BUTTON_B

        handler.handleMenuClosed(activity, closingButton)

        assertTrue(controllerInput.processGamePadButtonEvent(closingButton, KeyEvent.ACTION_UP))
        assertFalse(
                controllerInput.processGamePadButtonEvent(
                        KeyEvent.KEYCODE_BUTTON_A,
                        KeyEvent.ACTION_UP
                )
        )
    }
}
