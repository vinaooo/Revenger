package com.vinaooo.revenger.input

import android.content.Context
import android.view.KeyEvent
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.retroview.RetroView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
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

    /**
     * Builds a mocked [RetroView] whose [RetroView.frameRendered] is pinned to [frameRendered]
     * (default `true`, so `processKeyEvent`'s readiness gate doesn't short-circuit before
     * reaching the logic under test), plus the mocked [GLRetroView] backing `view`, so tests
     * can verify `sendKeyEvent` calls against one stable instance.
     */
    private fun mockRetroView(frameRendered: Boolean = true): Pair<RetroView, GLRetroView> {
        val glRetroView = mockk<GLRetroView>(relaxed = true)
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        every { retroView.frameRendered } returns MutableLiveData(frameRendered)
        return retroView to glRetroView
    }

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

    @Test
    fun `assigning a ControllerInputCallbacks bundle updates all 16 delegate properties without cross-contamination`() {
        val controllerInput = newControllerInput()

        val selectStartComboCallbackFake: () -> Unit = {}
        val startButtonCallbackFake: () -> Unit = {}
        val shouldHandleSelectStartComboFake: () -> Boolean = { true }
        val shouldHandleStartButtonFake: () -> Boolean = { true }
        val shouldHandleGamepadMenuButtonFake: () -> Boolean = { true }
        val gamepadMenuButtonCallbackFake: () -> Unit = {}
        val shouldBlockAllGamepadInputFake: () -> Boolean = { true }
        val isRetroMenu3OpenFake: () -> Boolean = { true }
        val menuNavigateUpCallbackFake: () -> Unit = {}
        val menuNavigateDownCallbackFake: () -> Unit = {}
        val menuNavigateLeftCallbackFake: () -> Unit = {}
        val menuNavigateRightCallbackFake: () -> Unit = {}
        val menuConfirmCallbackFake: () -> Unit = {}
        val menuBackCallbackFake: () -> Unit = {}
        val shouldInterceptDpadForMenuFake: () -> Boolean = { true }
        val isMenuOperationSafeFake: () -> Boolean = { false }

        controllerInput.callbacks =
                ControllerInputCallbacks(
                        selectStartComboCallback = selectStartComboCallbackFake,
                        startButtonCallback = startButtonCallbackFake,
                        shouldHandleSelectStartCombo = shouldHandleSelectStartComboFake,
                        shouldHandleStartButton = shouldHandleStartButtonFake,
                        shouldHandleGamepadMenuButton = shouldHandleGamepadMenuButtonFake,
                        gamepadMenuButtonCallback = gamepadMenuButtonCallbackFake,
                        shouldBlockAllGamepadInput = shouldBlockAllGamepadInputFake,
                        isRetroMenu3Open = isRetroMenu3OpenFake,
                        menuNavigateUpCallback = menuNavigateUpCallbackFake,
                        menuNavigateDownCallback = menuNavigateDownCallbackFake,
                        menuNavigateLeftCallback = menuNavigateLeftCallbackFake,
                        menuNavigateRightCallback = menuNavigateRightCallbackFake,
                        menuConfirmCallback = menuConfirmCallbackFake,
                        menuBackCallback = menuBackCallbackFake,
                        shouldInterceptDpadForMenu = shouldInterceptDpadForMenuFake,
                        isMenuOperationSafe = isMenuOperationSafeFake
                )

        assertSame(selectStartComboCallbackFake, controllerInput.selectStartComboCallback)
        assertSame(startButtonCallbackFake, controllerInput.startButtonCallback)
        assertSame(
                shouldHandleSelectStartComboFake,
                controllerInput.shouldHandleSelectStartCombo
        )
        assertSame(shouldHandleStartButtonFake, controllerInput.shouldHandleStartButton)
        assertSame(
                shouldHandleGamepadMenuButtonFake,
                controllerInput.shouldHandleGamepadMenuButton
        )
        assertSame(gamepadMenuButtonCallbackFake, controllerInput.gamepadMenuButtonCallback)
        assertSame(shouldBlockAllGamepadInputFake, controllerInput.shouldBlockAllGamepadInput)
        assertSame(isRetroMenu3OpenFake, controllerInput.isRetroMenu3Open)
        assertSame(menuNavigateUpCallbackFake, controllerInput.menuNavigateUpCallback)
        assertSame(menuNavigateDownCallbackFake, controllerInput.menuNavigateDownCallback)
        assertSame(menuNavigateLeftCallbackFake, controllerInput.menuNavigateLeftCallback)
        assertSame(menuNavigateRightCallbackFake, controllerInput.menuNavigateRightCallback)
        assertSame(menuConfirmCallbackFake, controllerInput.menuConfirmCallback)
        assertSame(menuBackCallbackFake, controllerInput.menuBackCallback)
        assertSame(shouldInterceptDpadForMenuFake, controllerInput.shouldInterceptDpadForMenu)
        assertSame(isMenuOperationSafeFake, controllerInput.isMenuOperationSafe)
    }

    @Test
    fun `assigning each of the 16 delegate properties individually updates the callbacks bundle without cross-contamination`() {
        val controllerInput = newControllerInput()

        val selectStartComboCallbackFake: () -> Unit = {}
        val startButtonCallbackFake: () -> Unit = {}
        val shouldHandleSelectStartComboFake: () -> Boolean = { true }
        val shouldHandleStartButtonFake: () -> Boolean = { true }
        val shouldHandleGamepadMenuButtonFake: () -> Boolean = { true }
        val gamepadMenuButtonCallbackFake: () -> Unit = {}
        val shouldBlockAllGamepadInputFake: () -> Boolean = { true }
        val isRetroMenu3OpenFake: () -> Boolean = { true }
        val menuNavigateUpCallbackFake: () -> Unit = {}
        val menuNavigateDownCallbackFake: () -> Unit = {}
        val menuNavigateLeftCallbackFake: () -> Unit = {}
        val menuNavigateRightCallbackFake: () -> Unit = {}
        val menuConfirmCallbackFake: () -> Unit = {}
        val menuBackCallbackFake: () -> Unit = {}
        val shouldInterceptDpadForMenuFake: () -> Boolean = { true }
        val isMenuOperationSafeFake: () -> Boolean = { false }

        // Assign every property individually (as GameActivityViewModel.setupMenuCallback()
        // and InputViewModel do), rather than replacing the whole bundle at once. Each
        // setter is a hand-written `callbacks = callbacks.copy(field = value)` -- this
        // proves every one of the 16 names its own field (no copy-paste mismatch) and
        // that earlier assignments survive later ones (each copy() only touches its own
        // field, it doesn't reconstruct the bundle from defaults).
        controllerInput.selectStartComboCallback = selectStartComboCallbackFake
        controllerInput.startButtonCallback = startButtonCallbackFake
        controllerInput.shouldHandleSelectStartCombo = shouldHandleSelectStartComboFake
        controllerInput.shouldHandleStartButton = shouldHandleStartButtonFake
        controllerInput.shouldHandleGamepadMenuButton = shouldHandleGamepadMenuButtonFake
        controllerInput.gamepadMenuButtonCallback = gamepadMenuButtonCallbackFake
        controllerInput.shouldBlockAllGamepadInput = shouldBlockAllGamepadInputFake
        controllerInput.isRetroMenu3Open = isRetroMenu3OpenFake
        controllerInput.menuNavigateUpCallback = menuNavigateUpCallbackFake
        controllerInput.menuNavigateDownCallback = menuNavigateDownCallbackFake
        controllerInput.menuNavigateLeftCallback = menuNavigateLeftCallbackFake
        controllerInput.menuNavigateRightCallback = menuNavigateRightCallbackFake
        controllerInput.menuConfirmCallback = menuConfirmCallbackFake
        controllerInput.menuBackCallback = menuBackCallbackFake
        controllerInput.shouldInterceptDpadForMenu = shouldInterceptDpadForMenuFake
        controllerInput.isMenuOperationSafe = isMenuOperationSafeFake

        val callbacks = controllerInput.callbacks

        assertSame(selectStartComboCallbackFake, callbacks.selectStartComboCallback)
        assertSame(startButtonCallbackFake, callbacks.startButtonCallback)
        assertSame(shouldHandleSelectStartComboFake, callbacks.shouldHandleSelectStartCombo)
        assertSame(shouldHandleStartButtonFake, callbacks.shouldHandleStartButton)
        assertSame(shouldHandleGamepadMenuButtonFake, callbacks.shouldHandleGamepadMenuButton)
        assertSame(gamepadMenuButtonCallbackFake, callbacks.gamepadMenuButtonCallback)
        assertSame(shouldBlockAllGamepadInputFake, callbacks.shouldBlockAllGamepadInput)
        assertSame(isRetroMenu3OpenFake, callbacks.isRetroMenu3Open)
        assertSame(menuNavigateUpCallbackFake, callbacks.menuNavigateUpCallback)
        assertSame(menuNavigateDownCallbackFake, callbacks.menuNavigateDownCallback)
        assertSame(menuNavigateLeftCallbackFake, callbacks.menuNavigateLeftCallback)
        assertSame(menuNavigateRightCallbackFake, callbacks.menuNavigateRightCallback)
        assertSame(menuConfirmCallbackFake, callbacks.menuConfirmCallback)
        assertSame(menuBackCallbackFake, callbacks.menuBackCallback)
        assertSame(shouldInterceptDpadForMenuFake, callbacks.shouldInterceptDpadForMenu)
        assertSame(isMenuOperationSafeFake, callbacks.isMenuOperationSafe)
    }

    // --- processKeyEvent coverage (added for Task 11: de-duplicating the safe overlap
    // between processGamePadButtonEvent and processKeyEvent). None of these existed before --
    // processKeyEvent had zero test coverage. They mirror the equivalent
    // processGamePadButtonEvent tests above so both paths' shared behavior is pinned down
    // before extracting the two safe helper methods. ---

    @Test
    fun `BUTTON_A confirm via processKeyEvent is debounced at 150ms and gated by isMenuOperationSafe`() {
        val controllerInput = newControllerInput()
        var fireCount = 0
        controllerInput.menuConfirmCallback = { fireCount++ }
        controllerInput.shouldInterceptDpadForMenu = { true }
        val (retroView, _) = mockRetroView()
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A)

        // Back-to-back calls with no time advance land well under the 150ms debounce window.
        assertEquals(
                true,
                controllerInput.processKeyEvent(KeyEvent.KEYCODE_BUTTON_A, downEvent, retroView)
        )
        assertEquals(
                true,
                controllerInput.processKeyEvent(KeyEvent.KEYCODE_BUTTON_A, downEvent, retroView)
        )
        assertEquals(1, fireCount)

        // Both ACTION_DOWN and ACTION_UP are consumed unconditionally (so ACTION_UP never
        // leaks to the core once the menu has intercepted the confirm button).
        assertEquals(
                true,
                controllerInput.processKeyEvent(KeyEvent.KEYCODE_BUTTON_A, upEvent, retroView)
        )

        // isMenuOperationSafe gates the callback even on a fresh instance's very first call.
        val gatedInput = newControllerInput()
        var gatedFireCount = 0
        gatedInput.menuConfirmCallback = { gatedFireCount++ }
        gatedInput.shouldInterceptDpadForMenu = { true }
        gatedInput.isMenuOperationSafe = { false }
        gatedInput.processKeyEvent(KeyEvent.KEYCODE_BUTTON_A, downEvent, retroView)
        assertEquals(0, gatedFireCount)
    }

    @Test
    fun `processKeyEvent swallows a repeated ACTION_DOWN for the same key without re-triggering the core`() {
        val controllerInput = newControllerInput()
        val (retroView, glRetroView) = mockRetroView()
        val thirdButton = KeyEvent.KEYCODE_BUTTON_L1
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, thirdButton)

        controllerInput.processKeyEvent(thirdButton, downEvent, retroView)
        controllerInput.processKeyEvent(thirdButton, downEvent, retroView)
        controllerInput.processKeyEvent(thirdButton, downEvent, retroView)

        // Only the first DOWN actually reaches the core; the repeats are swallowed by the
        // keyLog "wasAlreadyPressed" check before ever reaching sendKeyEvent.
        verify(exactly = 1) { glRetroView.sendKeyEvent(KeyEvent.ACTION_DOWN, thirdButton, any()) }
    }

    @Test
    fun `processKeyEvent resets the combo latch only once BOTH SELECT and START are released`() {
        val controllerInput = newControllerInput()
        val (retroView, _) = mockRetroView()
        controllerInput.shouldHandleSelectStartCombo = { true }
        var comboFireCount = 0
        controllerInput.selectStartComboCallback = { comboFireCount++ }

        controllerInput.processKeyEvent(
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT),
                retroView
        )
        controllerInput.processKeyEvent(
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START),
                retroView
        )
        assertEquals(1, comboFireCount)
        assertTrue(controllerInput.getComboAlreadyTriggered())

        // Releasing only SELECT: START is still held, so the latch must NOT reset yet.
        controllerInput.processKeyEvent(
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT),
                retroView
        )
        assertTrue(controllerInput.getComboAlreadyTriggered())

        // Releasing START too: now NEITHER combo button remains held, so the latch resets.
        controllerInput.processKeyEvent(
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_START),
                retroView
        )
        assertFalse(controllerInput.getComboAlreadyTriggered())
    }

    // --- Divergence-pinning tests: these two spots LOOK like duplicates between
    // processGamePadButtonEvent and processKeyEvent but have real, deliberate behavioral
    // differences. They must keep failing (i.e. keep detecting the divergence) after the
    // Task 11 helper extraction and any future change to this file -- if either ever starts
    // passing because the two paths were made to agree, that is itself a regression to catch. ---

    @Test
    fun `DIVERGENCE - START-closes-menu resets comboAlreadyTriggered immediately via processKeyEvent but NOT via processGamePadButtonEvent`() {
        // --- GamePad path: relies entirely on the generic keyLog bookkeeping to eventually
        // clear the latch once both combo buttons are released -- there is no explicit reset
        // in the START-close block itself, so the latch is still `true` right after this call.
        val gamePadInput = newControllerInput()
        gamePadInput.shouldHandleSelectStartCombo = { true }
        gamePadInput.shouldHandleStartButton = { false }
        gamePadInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent.ACTION_DOWN
        )
        gamePadInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent.ACTION_DOWN
        )
        assertTrue(gamePadInput.getComboAlreadyTriggered())

        // Simulate the menu now being open, and START being pressed again to close it.
        gamePadInput.shouldHandleStartButton = { true }
        gamePadInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent.ACTION_DOWN
        )
        assertTrue(
                "GamePad path must NOT reset comboAlreadyTriggered immediately on START-close",
                gamePadInput.getComboAlreadyTriggered()
        )

        // --- KeyEvent path: explicitly resets comboAlreadyTriggered right after firing
        // startButtonCallback, with a comment explaining it's safe because we KNOW the user
        // is closing the menu -- so the latch is already `false` right after this call.
        val keyEventInput = newControllerInput()
        val (retroView, _) = mockRetroView()
        keyEventInput.shouldHandleSelectStartCombo = { true }
        keyEventInput.shouldHandleStartButton = { false }
        keyEventInput.processKeyEvent(
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT),
                retroView
        )
        keyEventInput.processKeyEvent(
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START),
                retroView
        )
        assertTrue(keyEventInput.getComboAlreadyTriggered())

        keyEventInput.shouldHandleStartButton = { true }
        keyEventInput.processKeyEvent(
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START),
                retroView
        )
        assertFalse(
                "KeyEvent path MUST reset comboAlreadyTriggered immediately on START-close",
                keyEventInput.getComboAlreadyTriggered()
        )
    }

    @Test
    fun `DIVERGENCE - holding a third button with the SELECT+START combo blocks the event on the GamePad path but lets it leak to the core on the KeyEvent path`() {
        val thirdButton = KeyEvent.KEYCODE_BUTTON_L1

        // --- GamePad path: the leak-block's condition is just contains(START) &&
        // contains(SELECT) -- it still blocks even with a third button held. ---
        val gamePadInput = newControllerInput()
        gamePadInput.processGamePadButtonEvent(thirdButton, KeyEvent.ACTION_DOWN)
        gamePadInput.processGamePadButtonEvent(
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent.ACTION_DOWN
        )
        val gamePadResult =
                gamePadInput.processGamePadButtonEvent(
                        KeyEvent.KEYCODE_BUTTON_SELECT,
                        KeyEvent.ACTION_DOWN
                )
        // `true` here means "event intercepted - don't send to core" per the leak-block's
        // own comment/return, i.e. the event was blocked despite the third button held.
        assertTrue(
                "GamePad path must still block START/SELECT with a third button held",
                gamePadResult
        )

        // --- KeyEvent path: the leak-block ADDITIONALLY requires keyLog.size == 2, so with a
        // third button held the block does NOT trigger and the event reaches sendKeyEvent. ---
        val keyEventInput = newControllerInput()
        val (retroView, glRetroView) = mockRetroView()
        keyEventInput.processKeyEvent(
                thirdButton,
                KeyEvent(KeyEvent.ACTION_DOWN, thirdButton),
                retroView
        )
        keyEventInput.processKeyEvent(
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START),
                retroView
        )
        keyEventInput.processKeyEvent(
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT),
                retroView
        )

        // The SELECT event reached sendKeyEvent -- it leaked to the core, unlike the
        // GamePad path above with the same three-button-held state.
        verify(exactly = 1) {
            glRetroView.sendKeyEvent(
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_BUTTON_SELECT,
                    any()
            )
        }
    }
}
