package com.vinaooo.revenger.viewmodels.menu

import android.view.KeyEvent
import android.view.MotionEvent
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.retromenu3.navigation.KeyboardInputAdapter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [KeyMotionInputRouter]'s [ControllerInput]/[RetroView]/[KeyboardInputAdapter] dependencies are
 * read through provider lambdas backed by local vars here, standing in for
 * `GameActivityViewModel`'s own fields.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class KeyMotionInputRouter_test {

    private var currentControllerInput: ControllerInput = mockk(relaxed = true)
    private var currentRetroView: RetroView? = null
    private var currentKeyboardInputAdapter: KeyboardInputAdapter? = null
    private var isMenuActive = false
    private lateinit var appConfig: AppConfig
    private lateinit var router: KeyMotionInputRouter

    private fun keyEvent(action: Int, keyCode: Int) = KeyEvent(action, keyCode)

    @Before
    fun setUp() {
        currentControllerInput = mockk(relaxed = true)
        currentRetroView = null
        currentKeyboardInputAdapter = null
        isMenuActive = false
        appConfig = mockk(relaxed = true)
        router =
                KeyMotionInputRouter(
                        controllerInput = { currentControllerInput },
                        retroView = { currentRetroView },
                        keyboardInputAdapter = { currentKeyboardInputAdapter },
                        isAnyMenuActive = { isMenuActive },
                        appConfig = appConfig
                )
    }

    // --- processKeyEvent ---

    @Test
    fun `sem keyboardInputAdapter e sem retroView cai no fallback e retorna false`() {
        val result =
                router.processKeyEvent(
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN)
                )

        assertEquals(false, result)
    }

    @Test
    fun `tecla de navegacao consumida pelo adapter retorna true sem tocar no retroView`() {
        val adapter = mockk<KeyboardInputAdapter>(relaxed = true)
        every { adapter.isNavigationKey(KeyEvent.KEYCODE_F12) } returns true
        every { adapter.onKeyDown(KeyEvent.KEYCODE_F12, any()) } returns true
        currentKeyboardInputAdapter = adapter

        val result =
                router.processKeyEvent(
                        KeyEvent.KEYCODE_F12,
                        keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F12)
                )

        assertEquals(true, result)
    }

    @Test
    fun `com retroView presente e sem consumo do teclado delega ao ControllerInput`() {
        val retroView = mockk<RetroView>(relaxed = true)
        currentRetroView = retroView
        every { currentControllerInput.processKeyEvent(any(), any(), any()) } returns true

        val result =
                router.processKeyEvent(
                        KeyEvent.KEYCODE_A,
                        keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A)
                )

        assertEquals(true, result)
        verify { currentControllerInput.processKeyEvent(KeyEvent.KEYCODE_A, any(), retroView) }
    }

    // --- tryConsumeKeyboardNavigation (private, direct branches) ---

    private fun <T> invokePrivate(target: Any, methodName: String, vararg args: Any?): T {
        val method = target.javaClass.declaredMethods.first { it.name == methodName }
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST") return method.invoke(target, *args) as T
    }

    @Test
    fun `retorna null quando nao ha keyboardInputAdapter`() {
        val result =
                invokePrivate<Boolean?>(
                        router,
                        "tryConsumeKeyboardNavigation",
                        KeyEvent.KEYCODE_A,
                        keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A)
                )

        assertNull(result)
    }

    @Test
    fun `retorna null quando a tecla nao e de navegacao`() {
        val adapter = mockk<KeyboardInputAdapter>(relaxed = true)
        every { adapter.isNavigationKey(KeyEvent.KEYCODE_A) } returns false
        currentKeyboardInputAdapter = adapter

        val result =
                invokePrivate<Boolean?>(
                        router,
                        "tryConsumeKeyboardNavigation",
                        KeyEvent.KEYCODE_A,
                        keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A)
                )

        assertNull(result)
    }

    @Test
    fun `retorna null para tecla de navegacao diferente de F12 com menu fechado`() {
        val adapter = mockk<KeyboardInputAdapter>(relaxed = true)
        every { adapter.isNavigationKey(KeyEvent.KEYCODE_DEL) } returns true
        currentKeyboardInputAdapter = adapter
        isMenuActive = false

        val result =
                invokePrivate<Boolean?>(
                        router,
                        "tryConsumeKeyboardNavigation",
                        KeyEvent.KEYCODE_DEL,
                        keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                )

        assertNull(result)
    }

    @Test
    fun `processa F12 mesmo com menu fechado`() {
        val adapter = mockk<KeyboardInputAdapter>(relaxed = true)
        every { adapter.isNavigationKey(KeyEvent.KEYCODE_F12) } returns true
        every { adapter.onKeyDown(KeyEvent.KEYCODE_F12, any()) } returns true
        currentKeyboardInputAdapter = adapter
        isMenuActive = false

        val result =
                invokePrivate<Boolean?>(
                        router,
                        "tryConsumeKeyboardNavigation",
                        KeyEvent.KEYCODE_F12,
                        keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F12)
                )

        assertTrue(result == true)
    }

    @Test
    fun `roteia ACTION_UP para onKeyUp`() {
        val adapter = mockk<KeyboardInputAdapter>(relaxed = true)
        every { adapter.isNavigationKey(KeyEvent.KEYCODE_DEL) } returns true
        every { adapter.onKeyUp(KeyEvent.KEYCODE_DEL, any()) } returns true
        currentKeyboardInputAdapter = adapter
        isMenuActive = true

        val result =
                invokePrivate<Boolean?>(
                        router,
                        "tryConsumeKeyboardNavigation",
                        KeyEvent.KEYCODE_DEL,
                        keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL)
                )

        assertTrue(result == true)
        verify { adapter.onKeyUp(KeyEvent.KEYCODE_DEL, any()) }
    }

    // --- processMotionEvent ---

    @Test
    fun `processMotionEvent com retroView nulo retorna false`() {
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 0f, 0f, 0)

        val result = router.processMotionEvent(event)

        assertEquals(false, result)
    }

    @Test
    fun `processMotionEvent com retroView delega para controllerInput e retorna o resultado`() {
        val retroView = mockk<RetroView>(relaxed = true)
        currentRetroView = retroView
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 0f, 0f, 0)
        every { currentControllerInput.processMotionEvent(event, retroView) } returns true

        val result = router.processMotionEvent(event)

        assertEquals(true, result)
        verify(exactly = 1) { currentControllerInput.processMotionEvent(event, retroView) }
    }

    // --- appConfig pass-through ---

    @Test
    fun `shouldHandleBackButton retorna appConfig getMenuModeBack`() {
        every { appConfig.getMenuModeBack() } returns true

        assertTrue(router.shouldHandleBackButton())
    }

    @Test
    fun `shouldHandleSelectStartCombo retorna appConfig getMenuModeCombo`() {
        every { appConfig.getMenuModeCombo() } returns false

        assertFalse(router.shouldHandleSelectStartCombo())
    }

    @Test
    fun `shouldHandleGamepadMenuButton retorna appConfig getMenuModeGamepad`() {
        every { appConfig.getMenuModeGamepad() } returns true

        assertTrue(router.shouldHandleGamepadMenuButton())
    }
}
