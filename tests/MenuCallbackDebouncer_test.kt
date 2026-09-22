package com.vinaooo.revenger.input

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [MenuCallbackDebouncer], extracted from [ControllerInput] to keep that class's
 * function count within detekt's `TooManyFunctions` threshold. Exercises the debouncer's own
 * public API directly; the dispatcher-level interception scenarios remain covered end-to-end by
 * `ControllerInput_test`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuCallbackDebouncer_test {

    private var callbacks = ControllerInputCallbacks()

    private fun newDebouncer() = MenuCallbackDebouncer { callbacks }

    @Test
    fun `executeMenuCallback runs the callback and updates the timestamp on first call`() {
        val debouncer = newDebouncer()
        var invoked = false
        var updated = -1L

        val result = debouncer.executeMenuCallback(callback = { invoked = true }, lastExecutionTime = 0L) { updated = it }

        assertTrue(result)
        assertTrue(invoked)
        assertTrue("updateTime should receive a real timestamp", updated > 0L)
    }

    @Test
    fun `executeMenuCallback is debounced when called again within the window`() {
        val debouncer = newDebouncer()
        var callCount = 0
        var lastTime = 0L

        debouncer.executeMenuCallback(callback = { callCount++ }, lastExecutionTime = lastTime) { lastTime = it }

        assertFalse(
                "a second call right after the first should be swallowed by the debounce window",
                debouncer.executeMenuCallback(callback = { callCount++ }, lastExecutionTime = lastTime) { lastTime = it }
        )
        assertEquals(1, callCount)
    }

    @Test
    fun `executeMenuCallback does nothing when isMenuOperationSafe returns false`() {
        callbacks = ControllerInputCallbacks(isMenuOperationSafe = { false })
        val debouncer = newDebouncer()
        var invoked = false

        val result = debouncer.executeMenuCallback(callback = { invoked = true }, lastExecutionTime = 0L) {}

        assertFalse(result)
        assertFalse(invoked)
    }

    @Test
    fun `interceptButtonAConfirm ignores keys other than BUTTON_A`() {
        callbacks = ControllerInputCallbacks(shouldInterceptDpadForMenu = { true })
        val debouncer = newDebouncer()

        assertFalse(debouncer.interceptButtonAConfirm(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.ACTION_DOWN))
    }

    @Test
    fun `interceptButtonAConfirm ignores BUTTON_A when shouldInterceptDpadForMenu is false`() {
        callbacks = ControllerInputCallbacks(shouldInterceptDpadForMenu = { false })
        val debouncer = newDebouncer()

        assertFalse(debouncer.interceptButtonAConfirm(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_DOWN))
    }

    @Test
    fun `interceptButtonAConfirm executes menuConfirmCallback on ACTION_DOWN and consumes ACTION_UP too`() {
        var confirmed = false
        callbacks =
                ControllerInputCallbacks(
                        shouldInterceptDpadForMenu = { true },
                        menuConfirmCallback = { confirmed = true }
                )
        val debouncer = newDebouncer()

        assertTrue(debouncer.interceptButtonAConfirm(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_DOWN))
        assertTrue(confirmed)

        confirmed = false
        assertTrue(
                "ACTION_UP must also be consumed so it doesn't leak to the core",
                debouncer.interceptButtonAConfirm(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_UP)
        )
        assertFalse("ACTION_UP must not re-invoke the confirm callback", confirmed)
    }

    @Test
    fun `keepInterceptingButtons and shouldInterceptSpecificButton block only the closing button during the grace period`() {
        callbacks = ControllerInputCallbacks(shouldInterceptDpadForMenu = { false })
        val debouncer = newDebouncer()

        debouncer.keepInterceptingButtons(durationMs = 500, closingButton = KeyEvent.KEYCODE_BUTTON_B)

        assertTrue(debouncer.shouldInterceptSpecificButton(KeyEvent.KEYCODE_BUTTON_B))
        assertFalse(debouncer.shouldInterceptSpecificButton(KeyEvent.KEYCODE_BUTTON_A))
    }

    @Test
    fun `shouldInterceptSpecificButton returns true for any button while the menu itself is open`() {
        callbacks = ControllerInputCallbacks(shouldInterceptDpadForMenu = { true })
        val debouncer = newDebouncer()

        assertTrue(debouncer.shouldInterceptSpecificButton(KeyEvent.KEYCODE_BUTTON_A))
    }

    @Test
    fun `reset clears the grace period and all debounce timestamps`() {
        val debouncer = newDebouncer()
        debouncer.keepInterceptingButtons(durationMs = 5000, closingButton = KeyEvent.KEYCODE_BUTTON_B)
        debouncer.lastMenuBackCallbackTime = System.currentTimeMillis()

        debouncer.reset()

        assertFalse(
                "grace period must be cleared",
                debouncer.shouldInterceptSpecificButton(KeyEvent.KEYCODE_BUTTON_B)
        )
        assertEquals(0L, debouncer.lastMenuBackCallbackTime)
    }
}
