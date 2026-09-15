package com.vinaooo.revenger.input

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Characterization tests for [ControllerInput]'s callback-wiring surface (menu combo
 * detection, START handling, menu-navigation debouncing, and the three "clear state"
 * methods). These pin down CURRENT behavior before the callback fields are bundled into
 * [ControllerInputCallbacks] -- they must pass unmodified both before and after that
 * refactor.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ControllerInput_test {

    // ControllerInput reads System.currentTimeMillis() directly (not a mockable clock),
    // so proving cooldown-independent behavior requires waiting out the real windows.
    private val comboCooldownMs = 500L

    private fun newControllerInput(): ControllerInput =
            ControllerInput(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun `SELECT+START combo fires selectStartComboCallback exactly once per hold via the latch, not the cooldown`() {
        val controllerInput = newControllerInput()
        var fireCount = 0
        controllerInput.selectStartComboCallback = { fireCount++ }
        controllerInput.shouldHandleSelectStartCombo = { true }

        // Press SELECT then START: both are now in the internal key-tracking set
        // simultaneously, which is what checkMenuKeyCombo() requires to detect the combo.
        controllerInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent.ACTION_DOWN
        )
        controllerInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent.ACTION_DOWN
        )
        assertEquals(1, fireCount)
        assertTrue(controllerInput.getComboAlreadyTriggered())

        // Simulate repeated polls while both keys remain held, with no key-up or clear
        // call in between. Wait out the 500ms cooldown window entirely first -- if the
        // callback still doesn't fire again, that proves it's the comboAlreadyTriggered
        // latch (not merely the cooldown) blocking repeat firing.
        Thread.sleep(comboCooldownMs + 100)
        repeat(3) {
            controllerInput.processGamePadButtonEvent(
                    KeyEvent.KEYCODE_BUTTON_B,
                    KeyEvent.ACTION_UP
            )
        }

        assertEquals(1, fireCount)
        assertTrue(controllerInput.getComboAlreadyTriggered())
    }

    @Test
    fun `START alone triggers startButtonCallback only when shouldHandleStartButton returns true`() {
        val controllerInput = newControllerInput()
        var fired = false
        controllerInput.startButtonCallback = { fired = true }
        controllerInput.shouldHandleStartButton = { false }

        controllerInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent.ACTION_DOWN
        )
        assertFalse(fired)

        controllerInput.shouldHandleStartButton = { true }
        controllerInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent.ACTION_DOWN
        )
        assertTrue(fired)
    }

    @Test
    fun `menuConfirmCallback is debounced at 150ms and gated by isMenuOperationSafe`() {
        val controllerInput = newControllerInput()
        var fireCount = 0
        controllerInput.menuConfirmCallback = { fireCount++ }
        controllerInput.shouldInterceptDpadForMenu = { true }

        // Back-to-back calls with no time advance between them (Robolectric's clock
        // doesn't auto-advance) land well under the 150ms debounce window.
        controllerInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.ACTION_DOWN
        )
        controllerInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.ACTION_DOWN
        )
        assertEquals(1, fireCount)

        // isMenuOperationSafe gates the callback even on a fresh instance's very first call.
        val gatedInput = newControllerInput()
        var gatedFireCount = 0
        gatedInput.menuConfirmCallback = { gatedFireCount++ }
        gatedInput.shouldInterceptDpadForMenu = { true }
        gatedInput.isMenuOperationSafe = { false }
        gatedInput.processGamePadButtonEvent(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_DOWN)
        assertEquals(0, gatedFireCount)
    }

    @Test
    fun `clearKeyLog resets the combo latch but leaves menu-callback debounce timers untouched`() {
        val controllerInput = newControllerInput()
        var fireCount = 0
        controllerInput.menuConfirmCallback = { fireCount++ }
        controllerInput.shouldInterceptDpadForMenu = { true }
        controllerInput.shouldHandleSelectStartCombo = { true }

        // Set the combo latch.
        controllerInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent.ACTION_DOWN
        )
        controllerInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent.ACTION_DOWN
        )
        assertTrue(controllerInput.getComboAlreadyTriggered())

        // Set a debounce timestamp for menuConfirmCallback.
        controllerInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.ACTION_DOWN
        )
        assertEquals(1, fireCount)

        controllerInput.clearKeyLog()

        // Latch is reset.
        assertFalse(controllerInput.getComboAlreadyTriggered())

        // Debounce timer is NOT reset: an immediate repeat call is still blocked.
        controllerInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.ACTION_DOWN
        )
        assertEquals(1, fireCount)
    }

    @Test
    fun `clearPendingInputs resets menu-callback debounce timers in addition to what clearKeyLog resets`() {
        val controllerInput = newControllerInput()
        var fireCount = 0
        controllerInput.menuConfirmCallback = { fireCount++ }
        controllerInput.shouldInterceptDpadForMenu = { true }

        controllerInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.ACTION_DOWN
        )
        assertEquals(1, fireCount)

        controllerInput.clearPendingInputs()

        // Debounce timer IS reset this time: the very next call fires again immediately,
        // unlike the clearKeyLog() case above.
        controllerInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.ACTION_DOWN
        )
        assertEquals(2, fireCount)
    }

    @Test
    fun `clearPendingInputsPreserveHeld resets debounce timers and the combo latch but keeps the key log intact`() {
        val controllerInput = newControllerInput()

        // Debounce-reset half: same proof shape as clearPendingInputs() above.
        var fireCount = 0
        controllerInput.menuConfirmCallback = { fireCount++ }
        controllerInput.shouldInterceptDpadForMenu = { true }
        controllerInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.ACTION_DOWN
        )
        assertEquals(1, fireCount)

        controllerInput.clearPendingInputsPreserveHeld()

        controllerInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.ACTION_DOWN
        )
        assertEquals(2, fireCount)

        // "Preserve held keys" half: hold SELECT+START so the combo fires, then call
        // clearPendingInputsPreserveHeld() and wait out the combo cooldown. If the key
        // log had been cleared (as clearPendingInputs() does), hasSelectAndStart would
        // be false and the combo could never fire again. It firing a second time after
        // the cooldown proves the key log still reports both keys held.
        val comboInput = newControllerInput()
        var comboFireCount = 0
        comboInput.selectStartComboCallback = { comboFireCount++ }
        comboInput.shouldHandleSelectStartCombo = { true }
        comboInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent.ACTION_DOWN
        )
        comboInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent.ACTION_DOWN
        )
        assertEquals(1, comboFireCount)

        comboInput.clearPendingInputsPreserveHeld()

        Thread.sleep(comboCooldownMs + 100)
        comboInput.processGamePadButtonEvent(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.ACTION_UP)
        assertEquals(2, comboFireCount)
    }
}
