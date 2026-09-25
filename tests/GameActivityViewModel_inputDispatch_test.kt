package com.vinaooo.revenger.viewmodels

import android.app.Application
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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
 * Characterization tests for the parts of the gamepad/keyboard input-dispatch cluster that
 * `GameActivityViewModel_test.kt`/`GameActivityViewModel_processKeyEvent_test.kt` don't already
 * cover: `updateGamePadVisibility`'s show/hide x FAB-mode combinations, `processMotionEvent`'s
 * null/non-null `retroView` branches, and the three `shouldHandle*` methods' `AppConfig`
 * pass-through. `setupGamePads` builds real `GamePad`/`RadialGamePad` views, so it's covered by
 * the new class's own test file instead. Written BEFORE extracting this cluster into dedicated
 * classes under `viewmodels/menu/`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameActivityViewModel_inputDispatch_test {

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

    @Before
    fun setUp() {
        setRevengerAppConfig(mockk(relaxed = true))
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GameActivityViewModel(app)
    }

    @After
    fun tearDown() {
        setRevengerAppConfig(null)
        unmockkObject(GamePad)
    }

    // --- updateGamePadVisibility ---

    /**
     * `appConfig` is a `private val` read once at [GameActivityViewModel] construction time, so
     * seeding `RevengerApplication.appConfig` only takes effect for a viewModel built afterwards.
     */
    private fun newViewModelWithMenuModeFab(menuModeFab: String): GameActivityViewModel {
        val appConfig = mockk<AppConfig>(relaxed = true)
        every { appConfig.getMenuModeFab() } returns menuModeFab
        setRevengerAppConfig(appConfig)
        return GameActivityViewModel(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `updateGamePadVisibility com shouldShow true mostra os containers`() {
        mockkObject(GamePad)
        every { GamePad.shouldShowGamePads(any(), any()) } returns true
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val leftContainer = FrameLayout(activity)
        val rightContainer = FrameLayout(activity)
        viewModel.setGamePadContainer(LinearLayout(activity))

        viewModel.updateGamePadVisibility(activity, leftContainer, rightContainer)

        assertEquals(View.VISIBLE, leftContainer.visibility)
        assertEquals(View.VISIBLE, rightContainer.visibility)
    }

    @Test
    fun `updateGamePadVisibility com shouldShow false esconde os containers`() {
        mockkObject(GamePad)
        every { GamePad.shouldShowGamePads(any(), any()) } returns false
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val leftContainer = FrameLayout(activity)
        val rightContainer = FrameLayout(activity)

        viewModel.updateGamePadVisibility(activity, leftContainer, rightContainer)

        assertEquals(View.GONE, leftContainer.visibility)
        assertEquals(View.GONE, rightContainer.visibility)
    }

    @Test
    fun `updateGamePadVisibility com FAB nao-disabled e shouldShow true deixa o floatingButton GONE`() {
        mockkObject(GamePad)
        every { GamePad.shouldShowGamePads(any(), any()) } returns true
        val vm = newViewModelWithMenuModeFab("bottom-right")
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val floatingButton = Button(activity)

        vm.updateGamePadVisibility(activity, FrameLayout(activity), FrameLayout(activity), floatingButton)

        assertEquals(View.GONE, floatingButton.visibility)
    }

    @Test
    fun `updateGamePadVisibility com FAB nao-disabled e shouldShow false deixa o floatingButton VISIBLE`() {
        mockkObject(GamePad)
        every { GamePad.shouldShowGamePads(any(), any()) } returns false
        val vm = newViewModelWithMenuModeFab("bottom-right")
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val floatingButton = Button(activity)

        vm.updateGamePadVisibility(activity, FrameLayout(activity), FrameLayout(activity), floatingButton)

        assertEquals(View.VISIBLE, floatingButton.visibility)
    }

    @Test
    fun `updateGamePadVisibility com FAB disabled deixa o floatingButton GONE mesmo com shouldShow false`() {
        mockkObject(GamePad)
        every { GamePad.shouldShowGamePads(any(), any()) } returns false
        val vm = newViewModelWithMenuModeFab("disabled")
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val floatingButton = Button(activity)

        vm.updateGamePadVisibility(activity, FrameLayout(activity), FrameLayout(activity), floatingButton)

        assertEquals(View.GONE, floatingButton.visibility)
    }

    @Test
    fun `updateGamePadVisibility sem floatingButton nao lanca`() {
        mockkObject(GamePad)
        every { GamePad.shouldShowGamePads(any(), any()) } returns true
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()

        viewModel.updateGamePadVisibility(activity, FrameLayout(activity), FrameLayout(activity))
    }

    // --- processMotionEvent ---

    @Test
    fun `processMotionEvent com retroView nulo retorna false`() {
        viewModel.retroView = null
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 0f, 0f, 0)

        val result = viewModel.processMotionEvent(event)

        assertEquals(false, result)
    }

    @Test
    fun `processMotionEvent com retroView delega para controllerInput e retorna o resultado`() {
        val controllerInput = mockk<ControllerInput>(relaxed = true)
        setPrivateField(viewModel, "controllerInput", controllerInput)
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        viewModel.retroView = retroView
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 0f, 0f, 0)
        every { controllerInput.processMotionEvent(event, retroView) } returns true

        val result = viewModel.processMotionEvent(event)

        assertEquals(true, result)
        verify(exactly = 1) { controllerInput.processMotionEvent(event, retroView) }
    }

    // --- shouldHandle*: AppConfig pass-through ---

    @Test
    fun `shouldHandleBackButton retorna appConfig getMenuModeBack`() {
        val appConfig = mockk<AppConfig>(relaxed = true)
        every { appConfig.getMenuModeBack() } returns true
        setRevengerAppConfig(appConfig)
        val vm = GameActivityViewModel(ApplicationProvider.getApplicationContext())

        assertTrue(vm.shouldHandleBackButton())
    }

    @Test
    fun `shouldHandleSelectStartCombo retorna appConfig getMenuModeCombo`() {
        val appConfig = mockk<AppConfig>(relaxed = true)
        every { appConfig.getMenuModeCombo() } returns false
        setRevengerAppConfig(appConfig)
        val vm = GameActivityViewModel(ApplicationProvider.getApplicationContext())

        assertFalse(vm.shouldHandleSelectStartCombo())
    }

    @Test
    fun `shouldHandleGamepadMenuButton retorna appConfig getMenuModeGamepad`() {
        val appConfig = mockk<AppConfig>(relaxed = true)
        every { appConfig.getMenuModeGamepad() } returns true
        setRevengerAppConfig(appConfig)
        val vm = GameActivityViewModel(ApplicationProvider.getApplicationContext())

        assertTrue(vm.shouldHandleGamepadMenuButton())
    }
}
