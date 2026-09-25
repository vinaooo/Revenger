package com.vinaooo.revenger.viewmodels.menu

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.event.Event
import com.swordfish.radialgamepad.library.event.GestureType
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.GamePadAssetsConfig
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
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
 * [GamePadInputController]'s [RetroView]/[ControllerInput]/gamePad-container dependencies are
 * read through provider lambdas backed by local vars here, standing in for
 * `GameActivityViewModel`'s own fields. `appConfig` needs every button/asset getter stubbed (not
 * just `relaxed = true`) for `GamePadConfig`/`RadialGamePad` to build real views without throwing,
 * following the same recipe as `GamePadConfig_test`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GamePadInputController_test {

    private var currentRetroView: RetroView? = null
    private var currentControllerInput: ControllerInput = mockk(relaxed = true)
    private var isMenuActive = false
    private var currentGamePadContainerView: LinearLayout? = null
    private lateinit var appConfig: AppConfig
    private lateinit var controller: GamePadInputController

    private fun stubbedAppConfig(): AppConfig {
        val config = mockk<AppConfig>(relaxed = true)
        every { config.gamePadConfigModel } returns GamePadAssetsConfig()
        every { config.getButtonA() } returns true
        every { config.getButtonB() } returns true
        every { config.getButtonX() } returns true
        every { config.getButtonY() } returns true
        every { config.getButtonStart() } returns true
        every { config.getButtonSelect() } returns true
        every { config.getButtonL1() } returns true
        every { config.getButtonR1() } returns true
        every { config.getButtonL2() } returns true
        every { config.getButtonR2() } returns true
        every { config.getLeftAnalog() } returns true
        every { config.getGpHaptic() } returns true
        every { config.getButtonAllowMultiplePressesAction() } returns false
        every { config.getFakeButton0() } returns false
        every { config.getFakeButton1() } returns false
        every { config.getFakeButton5() } returns false
        every { config.getFakeButton6() } returns false
        every { config.getFakeButton7() } returns false
        every { config.getMenuModeGamepad() } returns false
        every { config.getFakeButton9() } returns false
        every { config.getFakeButton10() } returns false
        every { config.getFakeButton11() } returns false
        every { config.getMenuModeFab() } returns "bottom-right"
        return config
    }

    @Before
    fun setUp() {
        currentRetroView = null
        currentControllerInput = mockk(relaxed = true)
        isMenuActive = false
        currentGamePadContainerView = null
        appConfig = stubbedAppConfig()
        val context = ApplicationProvider.getApplicationContext<Context>()
        controller =
                GamePadInputController(
                        applicationContext = context,
                        appConfig = appConfig,
                        retroView = { currentRetroView },
                        isAnyMenuActive = { isMenuActive },
                        controllerInput = { currentControllerInput },
                        gamePadContainerView = { currentGamePadContainerView }
                )
    }

    @After
    fun tearDown() {
        unmockkObject(GamePad)
    }

    // --- setupGamePads ---

    @Test
    fun `setupGamePads adiciona uma pad view em cada container`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val leftContainer = FrameLayout(activity)
        val rightContainer = FrameLayout(activity)

        controller.setupGamePads(activity, leftContainer, rightContainer)

        assertEquals(1, leftContainer.childCount)
        assertEquals(1, rightContainer.childCount)
    }

    // --- updateGamePadVisibility ---

    @Test
    fun `updateGamePadVisibility com shouldShow true mostra os containers e o gamePadContainerView`() {
        mockkObject(GamePad)
        every { GamePad.shouldShowGamePads(any(), any()) } returns true
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val leftContainer = FrameLayout(activity)
        val rightContainer = FrameLayout(activity)
        currentGamePadContainerView = LinearLayout(activity)

        controller.updateGamePadVisibility(activity, leftContainer, rightContainer)

        assertEquals(View.VISIBLE, leftContainer.visibility)
        assertEquals(View.VISIBLE, rightContainer.visibility)
        assertEquals(View.VISIBLE, currentGamePadContainerView?.visibility)
    }

    @Test
    fun `updateGamePadVisibility com shouldShow false esconde os containers`() {
        mockkObject(GamePad)
        every { GamePad.shouldShowGamePads(any(), any()) } returns false
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val leftContainer = FrameLayout(activity)
        val rightContainer = FrameLayout(activity)

        controller.updateGamePadVisibility(activity, leftContainer, rightContainer)

        assertEquals(View.GONE, leftContainer.visibility)
        assertEquals(View.GONE, rightContainer.visibility)
    }

    @Test
    fun `updateGamePadVisibility com FAB disabled deixa o floatingButton GONE mesmo com shouldShow false`() {
        mockkObject(GamePad)
        every { GamePad.shouldShowGamePads(any(), any()) } returns false
        every { appConfig.getMenuModeFab() } returns "disabled"
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val floatingButton = Button(activity)

        controller.updateGamePadVisibility(
                activity,
                FrameLayout(activity),
                FrameLayout(activity),
                floatingButton
        )

        assertEquals(View.GONE, floatingButton.visibility)
    }

    // --- clear ---

    @Test
    fun `clear nao lanca quando chamado antes ou depois de setupGamePads`() {
        controller.clear()

        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        controller.setupGamePads(activity, FrameLayout(activity), FrameLayout(activity))

        controller.clear()
    }

    // --- handleGamePadEvent (via setupGamePads' callback) ---

    private fun <T> invokePrivate(target: Any, methodName: String, vararg args: Any?): T {
        val method = target.javaClass.declaredMethods.first { it.name == methodName }
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST") return method.invoke(target, *args) as T
    }

    @Test
    fun `handleGamePadEvent com Button delega para controllerInput e retorna o resultado`() {
        every {
            currentControllerInput.processGamePadButtonEvent(99, KeyEvent.ACTION_DOWN)
        } returns true

        val result =
                invokePrivate<Boolean>(
                        controller,
                        "handleGamePadEvent",
                        Event.Button(99, KeyEvent.ACTION_DOWN, 0)
                )

        assertTrue(result)
        verify { currentControllerInput.processGamePadButtonEvent(99, KeyEvent.ACTION_DOWN) }
    }

    @Test
    fun `handleGamePadEvent com Direction e menu fechado nao intercepta`() {
        isMenuActive = false

        val result =
                invokePrivate<Boolean>(
                        controller,
                        "handleGamePadEvent",
                        Event.Direction(0, 1f, 0f, 0)
                )

        assertFalse(result)
        verify(exactly = 0) { currentControllerInput.processMotionEvent(any(), any()) }
    }

    @Test
    fun `handleGamePadEvent com Direction e menu aberto converte para MotionEvent e intercepta`() {
        isMenuActive = true
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        currentRetroView = retroView
        val motionEventSlot = slot<MotionEvent>()
        every {
            currentControllerInput.processMotionEvent(capture(motionEventSlot), any())
        } returns true

        val result =
                invokePrivate<Boolean>(
                        controller,
                        "handleGamePadEvent",
                        Event.Direction(0, 0.7f, -0.4f, 0)
                )

        assertTrue(result)
        val captured = motionEventSlot.captured
        assertEquals(0.7f, captured.getAxisValue(MotionEvent.AXIS_HAT_X))
        assertEquals(-0.4f, captured.getAxisValue(MotionEvent.AXIS_HAT_Y))
    }

    @Test
    fun `handleGamePadEvent com outro tipo de evento nao intercepta`() {
        val result =
                invokePrivate<Boolean>(
                        controller,
                        "handleGamePadEvent",
                        Event.Gesture(0, GestureType.SINGLE_TAP)
                )

        assertFalse(result)
    }
}
