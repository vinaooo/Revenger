package com.vinaooo.revenger.ui.retromenu3.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [EventQueue]'s adaptive debounce thresholds. These lock the named constant extracted
 * from a former magic number (NAVIGATE_DEBOUNCE_WINDOW_MS = 30) that governs the shorter,
 * ultra-responsive debounce window used for [NavigationEvent.Navigate] events, distinct from the
 * longer `debounceWindowMs` (default 200) used for activation/selection events.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class EventQueue_test {

    private fun navigate(timestamp: Long) =
            NavigationEvent.Navigate(
                    direction = Direction.DOWN,
                    timestamp = timestamp,
                    inputSource = InputSource.PHYSICAL_GAMEPAD
            )

    private fun activate(timestamp: Long) =
            NavigationEvent.ActivateSelected(
                    timestamp = timestamp,
                    inputSource = InputSource.PHYSICAL_GAMEPAD
            )

    @Test
    fun `Navigate dentro de 30ms do anterior e debounced`() {
        // lastProcessedTimestamp starts at 0L, and shouldDebounce() treats 0L as "no previous
        // event yet" -- so the first event must use a non-zero timestamp for the debounce
        // window to actually apply to the second one.
        val queue = EventQueue()
        queue.enqueue(navigate(1000))
        queue.dequeue()

        val accepted = queue.enqueue(navigate(1029))

        assertFalse("event 29ms after the previous Navigate should be debounced", accepted)
    }

    @Test
    fun `Navigate exatamente 30ms depois do anterior nao e debounced`() {
        val queue = EventQueue()
        queue.enqueue(navigate(1000))
        queue.dequeue()

        val accepted = queue.enqueue(navigate(1030))

        assertTrue(
                "event exactly 30ms after the previous Navigate should be processed",
                accepted
        )
    }

    @Test
    fun `ActivateSelected usa a janela maior debounceWindowMs, nao a janela curta de Navigate`() {
        val queue = EventQueue(debounceWindowMs = 200)
        queue.enqueue(activate(1000))
        queue.dequeue()

        // 100ms is past the 30ms Navigate window but still inside the 200ms Activate window --
        // this specifically pins that ActivateSelected does NOT use NAVIGATE_DEBOUNCE_WINDOW_MS.
        val accepted = queue.enqueue(activate(1100))

        assertFalse(
                "ActivateSelected 100ms after the previous one should still be debounced" +
                        " under the 200ms window",
                accepted
        )
    }
}
