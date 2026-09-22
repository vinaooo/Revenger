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
 * Unit tests for [ComboKeyLogTracker], extracted from [ControllerInput] to keep that class's
 * function count within detekt's `TooManyFunctions` threshold. Exercises the tracker's own
 * public API directly; the dispatcher-level combo/keyLog scenarios (DIVERGENCE, grace period,
 * etc.) remain covered end-to-end by `ControllerInput_test`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ComboKeyLogTracker_test {

    private var callbacks = ControllerInputCallbacks()

    private fun newTracker() = ComboKeyLogTracker { callbacks }

    @Test
    fun `checkMenuKeyCombo fires selectStartComboCallback when both combo keys are held and eligible`() {
        var fired = false
        callbacks = ControllerInputCallbacks(selectStartComboCallback = { fired = true })
        val tracker = newTracker()
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_START)
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_SELECT)

        tracker.checkMenuKeyCombo()

        assertTrue(fired)
        assertTrue(tracker.getComboAlreadyTriggered())
    }

    @Test
    fun `checkMenuKeyCombo does not fire when shouldHandleSelectStartCombo returns false`() {
        var fired = false
        callbacks =
                ControllerInputCallbacks(
                        selectStartComboCallback = { fired = true },
                        shouldHandleSelectStartCombo = { false }
                )
        val tracker = newTracker()
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_START)
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_SELECT)

        tracker.checkMenuKeyCombo()

        assertFalse(fired)
        assertFalse(tracker.getComboAlreadyTriggered())
    }

    @Test
    fun `checkMenuKeyCombo does not fire again while comboAlreadyTriggered is true`() {
        var callCount = 0
        callbacks = ControllerInputCallbacks(selectStartComboCallback = { callCount++ })
        val tracker = newTracker()
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_START)
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_SELECT)

        tracker.checkMenuKeyCombo()
        tracker.checkMenuKeyCombo()

        assertEquals(1, callCount)
    }

    @Test
    fun `checkMenuKeyCombo does not fire when a third key is also held`() {
        var fired = false
        callbacks = ControllerInputCallbacks(selectStartComboCallback = { fired = true })
        val tracker = newTracker()
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_START)
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_SELECT)
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_A)

        tracker.checkMenuKeyCombo()

        assertFalse(fired)
    }

    @Test
    fun `clearKeyLog empties the keyLog and resets the combo latch`() {
        var fired = false
        callbacks = ControllerInputCallbacks(selectStartComboCallback = { fired = true })
        val tracker = newTracker()
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_START)
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_SELECT)
        tracker.checkMenuKeyCombo()
        assertTrue(fired)
        assertTrue(tracker.getComboAlreadyTriggered())

        tracker.clearKeyLog()

        assertTrue(tracker.keyLog.isEmpty())
        assertFalse(tracker.getComboAlreadyTriggered())
    }

    @Test
    fun `clearMenuActionButtons removes A B and DPAD but keeps START and SELECT`() {
        val tracker = newTracker()
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_START)
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_SELECT)
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_A)
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_B)
        tracker.keyLog.add(KeyEvent.KEYCODE_DPAD_UP)

        tracker.clearMenuActionButtons()

        assertEquals(
                setOf(KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT),
                tracker.keyLog
        )
    }

    @Test
    fun `resetComboAlreadyTriggered clears the latch without touching the keyLog`() {
        var fired = false
        callbacks = ControllerInputCallbacks(selectStartComboCallback = { fired = true })
        val tracker = newTracker()
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_START)
        tracker.keyLog.add(KeyEvent.KEYCODE_BUTTON_SELECT)
        tracker.checkMenuKeyCombo()
        assertTrue(fired)

        tracker.resetComboAlreadyTriggered()

        assertFalse(tracker.getComboAlreadyTriggered())
        assertEquals(2, tracker.keyLog.size)
    }

    @Test
    fun `trackKeyLogAndComboReset swallows a repeated ACTION_DOWN for the same key`() {
        val tracker = newTracker()

        val firstDown = tracker.trackKeyLogAndComboReset(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_DOWN)
        val repeatedDown = tracker.trackKeyLogAndComboReset(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_DOWN)

        assertFalse("first DOWN should not be swallowed", firstDown)
        assertTrue("repeated DOWN (hold) should be swallowed", repeatedDown)
        assertTrue(tracker.keyLog.contains(KeyEvent.KEYCODE_BUTTON_A))
    }

    @Test
    fun `trackKeyLogAndComboReset resets the combo latch only once BOTH combo keys are released`() {
        var fired = false
        callbacks = ControllerInputCallbacks(selectStartComboCallback = { fired = true })
        val tracker = newTracker()
        tracker.trackKeyLogAndComboReset(KeyEvent.KEYCODE_BUTTON_START, KeyEvent.ACTION_DOWN)
        tracker.trackKeyLogAndComboReset(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.ACTION_DOWN)
        tracker.checkMenuKeyCombo()
        assertTrue(fired)

        tracker.trackKeyLogAndComboReset(KeyEvent.KEYCODE_BUTTON_START, KeyEvent.ACTION_UP)
        assertTrue(
                "latch must stay set while SELECT is still held",
                tracker.getComboAlreadyTriggered()
        )

        tracker.trackKeyLogAndComboReset(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.ACTION_UP)
        assertFalse(
                "latch resets once both combo keys are released",
                tracker.getComboAlreadyTriggered()
        )
    }
}
