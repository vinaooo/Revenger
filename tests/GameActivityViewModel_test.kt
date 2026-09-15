package com.vinaooo.revenger.viewmodels

import android.app.Application
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.input.ControllerInputCallbacks
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Characterization tests for [GameActivityViewModel.setupMenuCallback]. Written BEFORE the
 * method-decomposition refactor to pin down current behavior: menu-open ordering, menu-close
 * ordering (including the `closingButton` threading into the grace-period intercept), and that
 * every real `ControllerInputCallbacks` field gets wired (nothing silently dropped).
 *
 * `RevengerApplication.appConfig` is a `lateinit var` (companion object, private setter) that is
 * only populated by `RevengerApplication.onCreate()`, which never runs against Robolectric's
 * default test Application. `GameActivityViewModel` reads it directly in its constructor, so it
 * has to be seeded via reflection before each test and cleared afterwards, exactly like
 * `InputViewModel_test.kt` does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameActivityViewModel_test {

    private lateinit var viewModel: GameActivityViewModel

    private fun setRevengerAppConfig(appConfig: AppConfig?) {
        val field = RevengerApplication::class.java.getDeclaredField("appConfig")
        field.isAccessible = true
        field.set(null, appConfig)
    }

    private fun <T> setPrivateField(target: Any, fieldName: String, value: T) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun <T> getPrivateField(target: Any, fieldName: String): T {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST") return field.get(target) as T
    }

    @Before
    fun setUp() {
        setRevengerAppConfig(mockk(relaxed = true))
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GameActivityViewModel(app)
    }

    @After
    fun tearDown() {
        setRevengerAppConfig(null)
        unmockkObject(ScreenshotCaptureUtil)
    }

    /**
     * Test 1: opening the menu must capture the save-state screenshot BEFORE pausing emulation.
     * `ScreenshotCaptureUtil` is a singleton object, mocked via `mockkObject` so its call can
     * take part in the same `verifyOrder` chain as the mocked `speedController`.
     */
    @Test
    fun `abrir o menu captura screenshot antes de pausar o emulador`() {
        mockkObject(ScreenshotCaptureUtil)
        every { ScreenshotCaptureUtil.captureAndCacheScreenshot(any(), any()) } just Runs
        every { ScreenshotCaptureUtil.capturePipFrame(any(), any()) } just Runs

        val speedController = mockk<SpeedController>(relaxed = true)
        setPrivateField(viewModel, "speedController", speedController)
        viewModel.retroView = mockk<RetroView>(relaxed = true)

        val activity = mockk<FragmentActivity>(relaxed = true)
        viewModel.setupMenuCallback(activity)

        viewModel.navigationController?.onMenuOpenedCallback?.invoke()

        verifyOrder {
            ScreenshotCaptureUtil.captureAndCacheScreenshot(any(), any())
            speedController.pause(any())
        }
    }

    /**
     * Test 2: closing the menu must, in order, clear the menu action buttons, reset the
     * already-triggered combo flag, clear the key log, update the debounce time, and only then
     * start the post-close grace period -- with the exact `closingButton` value passed through so
     * that only the button that actually closed the menu gets blocked during the grace period.
     *
     * `controllerInput` is swapped for a `spyk()` wrapping the SAME real instance (not a bare
     * mock): every call still runs its real implementation, so ordering is verified without
     * changing behavior.
     */
    @Test
    fun `fechar o menu limpa o estado do combo em ordem e bloqueia so o botao que fechou`() {
        val realControllerInput = getPrivateField<ControllerInput>(viewModel, "controllerInput")
        val spyControllerInput = spyk(realControllerInput)
        setPrivateField(viewModel, "controllerInput", spyControllerInput)

        val activity = mockk<FragmentActivity>(relaxed = true)
        viewModel.setupMenuCallback(activity)

        val closingButton = KeyEvent.KEYCODE_BUTTON_B

        viewModel.navigationController?.onMenuClosedCallback?.invoke(closingButton)

        verifyOrder {
            spyControllerInput.clearMenuActionButtons()
            spyControllerInput.resetComboAlreadyTriggered()
            spyControllerInput.clearKeyLog()
            spyControllerInput.updateMenuCloseDebounceTime()
            spyControllerInput.keepInterceptingButtons(200, closingButton = closingButton)
        }

        // Prove the exact closingButton value really reached keepInterceptingButtons: during
        // the grace period, only the button that closed the menu is intercepted on ACTION_UP.
        // ACTION_UP is used (rather than ACTION_DOWN) so this check doesn't also invoke
        // menuBackCallback / navigationController as a side effect.
        assertTrue(
                "the button that closed the menu should be intercepted during the grace period",
                spyControllerInput.processGamePadButtonEvent(closingButton, KeyEvent.ACTION_UP)
        )
        assertFalse(
                "a different button should NOT be intercepted during the grace period",
                spyControllerInput.processGamePadButtonEvent(
                        KeyEvent.KEYCODE_BUTTON_A,
                        KeyEvent.ACTION_UP
                )
        )
    }

    /**
     * Test 3 (regression guard for the bundle-copy consolidation): after `setupMenuCallback` runs
     * once, all 16 real `ControllerInputCallbacks` fields (the 4 set in `init {}` plus the 12 set
     * by `setupMenuCallback`) must be non-default. Only the dead `menuCallback` field (never
     * assigned anywhere) is expected to stay at its default. Comparing by identity, not behavior,
     * because function references from distinct lambda literals are never `===`/`==` to each
     * other -- so this reliably catches "a callback got dropped during the copy() consolidation".
     */
    @Test
    fun `setupMenuCallback substitui todos os 16 callbacks reais e deixa o campo morto no default`() {
        val activity = mockk<FragmentActivity>(relaxed = true)
        viewModel.setupMenuCallback(activity)

        val controllerInput = getPrivateField<ControllerInput>(viewModel, "controllerInput")
        val callbacks = controllerInput.callbacks
        val defaults = ControllerInputCallbacks()

        // Set in init {} (untouched by this task, but part of the 16 "real" fields).
        assertNotSame(defaults.selectStartComboCallback, callbacks.selectStartComboCallback)
        assertNotSame(defaults.startButtonCallback, callbacks.startButtonCallback)
        assertNotSame(
                defaults.shouldHandleSelectStartCombo,
                callbacks.shouldHandleSelectStartCombo
        )
        assertNotSame(
                defaults.shouldHandleGamepadMenuButton,
                callbacks.shouldHandleGamepadMenuButton
        )

        // Set by setupMenuCallback() (the 12 under test).
        assertNotSame(defaults.gamepadMenuButtonCallback, callbacks.gamepadMenuButtonCallback)
        assertNotSame(defaults.menuNavigateUpCallback, callbacks.menuNavigateUpCallback)
        assertNotSame(defaults.menuNavigateDownCallback, callbacks.menuNavigateDownCallback)
        assertNotSame(defaults.menuNavigateLeftCallback, callbacks.menuNavigateLeftCallback)
        assertNotSame(defaults.menuNavigateRightCallback, callbacks.menuNavigateRightCallback)
        assertNotSame(defaults.menuConfirmCallback, callbacks.menuConfirmCallback)
        assertNotSame(defaults.menuBackCallback, callbacks.menuBackCallback)
        assertNotSame(defaults.shouldInterceptDpadForMenu, callbacks.shouldInterceptDpadForMenu)
        assertNotSame(defaults.shouldHandleStartButton, callbacks.shouldHandleStartButton)
        assertNotSame(defaults.shouldBlockAllGamepadInput, callbacks.shouldBlockAllGamepadInput)
        assertNotSame(defaults.isRetroMenu3Open, callbacks.isRetroMenu3Open)
        assertNotSame(defaults.isMenuOperationSafe, callbacks.isMenuOperationSafe)

        // Dead field, never assigned anywhere -- must stay at the class's own default.
        assertSame(defaults.menuCallback, callbacks.menuCallback)
    }
}
