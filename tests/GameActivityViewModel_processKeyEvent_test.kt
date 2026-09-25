package com.vinaooo.revenger.viewmodels

import android.app.Application
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.retromenu3.navigation.KeyboardInputAdapter
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Characterization tests for [GameActivityViewModel.processKeyEvent] and the
 * `KeyMotionInputRouter.tryConsumeKeyboardNavigation` helper it delegates to. Written to pin down
 * the routing decision (keyboard-navigation adapter vs. `ControllerInput` fallback) across a
 * detekt-driven restructuring (flattening nested `if`s into guard clauses, and reshaping the
 * return statements to stay within `NestedBlockDepth`/`ReturnCount` thresholds) that was meant to
 * be behavior-preserving.
 *
 * "Menu closed" is a bare `GameActivityViewModel`'s natural state (`navigationController` and
 * every fragment reference default to `null`, so `isAnyMenuActive()` returns `false` without any
 * stubbing). "Menu open" is driven by stubbing `navigationController.isMenuActive()` -- the same
 * seam `isAnyMenuActive()` itself checks first -- rather than by spying the ViewModel under test,
 * to avoid coupling these tests to `isAnyMenuActive()`'s own internal fragment-tracking logic
 * (out of scope here, and separately flagged for its own dedicated refactor).
 *
 * `RevengerApplication.appConfig` is seeded via reflection before each test, same as
 * `GameActivityViewModel_test`'s header comment explains.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameActivityViewModel_processKeyEvent_test {

    private lateinit var viewModel: GameActivityViewModel

    private fun setRevengerAppConfig(appConfig: AppConfig?) {
        val field = RevengerApplication::class.java.getDeclaredField("appConfig")
        field.isAccessible = true
        field.set(null, appConfig)
    }

    private fun <T> invokePrivate(target: Any, methodName: String, vararg args: Any?): T {
        val method = target.javaClass.declaredMethods.first { it.name == methodName }
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST") return method.invoke(target, *args) as T
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

    private fun tryConsumeKeyboardNavigation(keyCode: Int, event: KeyEvent): Boolean? =
            invokePrivate(
                    getPrivateField(viewModel, "keyMotionInputRouter"),
                    "tryConsumeKeyboardNavigation",
                    keyCode,
                    event
            )

    private fun keyEvent(action: Int, keyCode: Int) = KeyEvent(action, keyCode)

    /** Makes `isAnyMenuActive()` return `true` by stubbing the NavigationController seam it
     * checks first, without touching any of its own fragment-tracking fields. */
    private fun makeMenuActive() {
        val navController = mockk<NavigationController>(relaxed = true)
        every { navController.isMenuActive() } returns true
        viewModel.navigationController = navController
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
    }

    // ---------------------------------------------------------------------------------------
    // processKeyEvent(): overall routing (keyboard adapter vs. ControllerInput fallback)
    // ---------------------------------------------------------------------------------------

    @Test
    fun `sem keyboardInputAdapter e sem retroView cai no fallback e retorna false`() {
        viewModel.keyboardInputAdapter = null
        viewModel.retroView = null

        val result =
                viewModel.processKeyEvent(
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN)
                )

        assertEquals(false, result)
    }

    @Test
    fun `tecla nao consumida pelo teclado cai no fallback e retorna false sem retroView`() {
        val adapter = mockk<KeyboardInputAdapter>(relaxed = true)
        every { adapter.isNavigationKey(any()) } returns false
        viewModel.keyboardInputAdapter = adapter
        viewModel.retroView = null

        val result =
                viewModel.processKeyEvent(KeyEvent.KEYCODE_A, keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A))

        assertEquals(false, result)
    }

    @Test
    fun `tecla de navegacao consumida pelo adapter retorna true sem tocar no retroView`() {
        val adapter = mockk<KeyboardInputAdapter>(relaxed = true)
        every { adapter.isNavigationKey(KeyEvent.KEYCODE_F12) } returns true
        every { adapter.onKeyDown(KeyEvent.KEYCODE_F12, any()) } returns true
        viewModel.keyboardInputAdapter = adapter
        viewModel.retroView = null // proves the ControllerInput fallback is never reached

        val result =
                viewModel.processKeyEvent(
                        KeyEvent.KEYCODE_F12,
                        keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F12)
                )

        assertEquals(true, result)
    }

    @Test
    fun `tecla de navegacao nao consumida pelo adapter cai no fallback`() {
        val adapter = mockk<KeyboardInputAdapter>(relaxed = true)
        every { adapter.isNavigationKey(KeyEvent.KEYCODE_F12) } returns true
        every { adapter.onKeyDown(KeyEvent.KEYCODE_F12, any()) } returns false
        viewModel.keyboardInputAdapter = adapter
        viewModel.retroView = null

        val result =
                viewModel.processKeyEvent(
                        KeyEvent.KEYCODE_F12,
                        keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F12)
                )

        assertEquals(false, result)
    }

    @Test
    fun `com retroView presente e sem consumo do teclado delega ao ControllerInput`() {
        viewModel.keyboardInputAdapter = null
        val retroView = mockk<RetroView>(relaxed = true)
        viewModel.retroView = retroView

        // ControllerInput's own routing/return-value logic is covered by ControllerInput_test.kt;
        // this just pins that processKeyEvent reaches it (rather than short-circuiting to
        // `false`) once nothing upstream consumed the event -- mocked out here with a sentinel
        // return value so this test doesn't depend on ControllerInput's real internal decision.
        val controllerInputMock = mockk<ControllerInput>(relaxed = true)
        every { controllerInputMock.processKeyEvent(any(), any(), any()) } returns true
        setPrivateField(viewModel, "controllerInput", controllerInputMock)

        val result =
                viewModel.processKeyEvent(KeyEvent.KEYCODE_A, keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A))

        assertEquals(true, result)
        verify { controllerInputMock.processKeyEvent(KeyEvent.KEYCODE_A, any(), retroView) }
    }

    // ---------------------------------------------------------------------------------------
    // tryConsumeKeyboardNavigation(): the extracted routing decision, exercised directly
    // ---------------------------------------------------------------------------------------

    @Test
    fun `retorna null quando nao ha keyboardInputAdapter`() {
        viewModel.keyboardInputAdapter = null

        val result =
                tryConsumeKeyboardNavigation(
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN)
                )

        assertNull(result)
    }

    @Test
    fun `retorna null quando a tecla nao e de navegacao`() {
        val adapter = mockk<KeyboardInputAdapter>(relaxed = true)
        every { adapter.isNavigationKey(KeyEvent.KEYCODE_A) } returns false
        viewModel.keyboardInputAdapter = adapter

        val result =
                tryConsumeKeyboardNavigation(KeyEvent.KEYCODE_A, keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A))

        assertNull(result)
    }

    @Test
    fun `retorna null quando o menu esta fechado e a tecla nao e F12`() {
        val adapter = mockk<KeyboardInputAdapter>(relaxed = true)
        every { adapter.isNavigationKey(KeyEvent.KEYCODE_DPAD_DOWN) } returns true
        viewModel.keyboardInputAdapter = adapter
        // navigationController is null by default -> isAnyMenuActive() is false.

        val result =
                tryConsumeKeyboardNavigation(
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN)
                )

        assertNull(result)
    }

    @Test
    fun `F12 e roteado ao adapter mesmo com o menu fechado`() {
        val adapter = mockk<KeyboardInputAdapter>(relaxed = true)
        every { adapter.isNavigationKey(KeyEvent.KEYCODE_F12) } returns true
        every { adapter.onKeyDown(KeyEvent.KEYCODE_F12, any()) } returns true
        viewModel.keyboardInputAdapter = adapter

        val result =
                tryConsumeKeyboardNavigation(
                        KeyEvent.KEYCODE_F12,
                        keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F12)
                )

        assertEquals(true, result)
    }

    @Test
    fun `tecla de navegacao e roteada ao adapter quando o menu esta aberto`() {
        val adapter = mockk<KeyboardInputAdapter>(relaxed = true)
        every { adapter.isNavigationKey(KeyEvent.KEYCODE_DPAD_UP) } returns true
        every { adapter.onKeyUp(KeyEvent.KEYCODE_DPAD_UP, any()) } returns true
        viewModel.keyboardInputAdapter = adapter
        makeMenuActive()

        val result =
                tryConsumeKeyboardNavigation(
                        KeyEvent.KEYCODE_DPAD_UP,
                        keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP)
                )

        assertEquals(true, result)
    }

    @Test
    fun `acao diferente de DOWN ou UP nunca e consumida pelo adapter`() {
        val adapter = mockk<KeyboardInputAdapter>(relaxed = true)
        every { adapter.isNavigationKey(KeyEvent.KEYCODE_F12) } returns true
        viewModel.keyboardInputAdapter = adapter
        val unhandledAction = 2 // neither ACTION_DOWN (0) nor ACTION_UP (1)

        val result =
                tryConsumeKeyboardNavigation(
                        KeyEvent.KEYCODE_F12,
                        keyEvent(unhandledAction, KeyEvent.KEYCODE_F12)
                )

        assertEquals(false, result)
    }
}
