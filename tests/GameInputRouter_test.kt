package com.vinaooo.revenger.controllers

import android.view.KeyEvent
import android.view.MotionEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [GameInputRouter] holds the key/motion routing that used to live in `GameActivity`'s
 * `onKeyDown` / `onKeyUp` / `onGenericMotionEvent` / `dispatchTouchEvent`. These tests pin the call
 * order per event type, that the ViewModel's result wins over `super`, and that key-up skips the
 * frame time, fade and PiP capture.
 */
class GameInputRouter_test {

    private val host = mockk<GameInputHost>(relaxed = true)
    private val router = GameInputRouter(host)
    private val keyEvent = mockk<KeyEvent>()
    private val motionEvent = mockk<MotionEvent>()
    private var superCalls = 0

    private fun superReturning(value: Boolean): () -> Boolean = {
        superCalls++
        value
    }

    @Test
    fun `key down records, fades and captures before asking the view model`() {
        every { host.processKey(any(), any()) } returns true

        router.onKeyDown(KeyEvent.KEYCODE_BUTTON_A, keyEvent, superReturning(false))

        verifySequence {
            host.recordFrame()
            host.triggerButtonFade()
            host.capturePipFrame()
            host.processKey(KeyEvent.KEYCODE_BUTTON_A, keyEvent)
        }
    }

    @Test
    fun `key down returns the view model result without calling super`() {
        every { host.processKey(any(), any()) } returns false

        val handled = router.onKeyDown(KeyEvent.KEYCODE_BUTTON_A, keyEvent, superReturning(true))

        assertFalse(handled)
        assertEquals(0, superCalls)
    }

    @Test
    fun `key down falls back to super when the view model returns null`() {
        every { host.processKey(any(), any()) } returns null

        val handled = router.onKeyDown(KeyEvent.KEYCODE_BUTTON_A, keyEvent, superReturning(true))

        assertTrue(handled)
        assertEquals(1, superCalls)
    }

    @Test
    fun `key up only asks the view model`() {
        every { host.processKey(any(), any()) } returns true

        router.onKeyUp(KeyEvent.KEYCODE_BUTTON_A, keyEvent, superReturning(false))

        verifySequence { host.processKey(KeyEvent.KEYCODE_BUTTON_A, keyEvent) }
    }

    @Test
    fun `key up returns the view model result without calling super`() {
        every { host.processKey(any(), any()) } returns true

        assertTrue(router.onKeyUp(KeyEvent.KEYCODE_BUTTON_A, keyEvent, superReturning(false)))
        assertEquals(0, superCalls)
    }

    @Test
    fun `key up falls back to super when the view model returns null`() {
        every { host.processKey(any(), any()) } returns null

        assertFalse(router.onKeyUp(KeyEvent.KEYCODE_BUTTON_A, keyEvent, superReturning(false)))
        assertEquals(1, superCalls)
    }

    @Test
    fun `generic motion records, fades and captures before asking the view model`() {
        every { host.processMotion(any()) } returns true

        router.onGenericMotionEvent(motionEvent, superReturning(false))

        verifySequence {
            host.recordFrame()
            host.triggerButtonFade()
            host.capturePipFrame()
            host.processMotion(motionEvent)
        }
    }

    @Test
    fun `generic motion returns the view model result without calling super`() {
        every { host.processMotion(any()) } returns true

        assertTrue(router.onGenericMotionEvent(motionEvent, superReturning(false)))
        assertEquals(0, superCalls)
    }

    @Test
    fun `generic motion falls back to super when the view model returns null`() {
        every { host.processMotion(any()) } returns null

        assertTrue(router.onGenericMotionEvent(motionEvent, superReturning(true)))
        assertEquals(1, superCalls)
    }

    @Test
    fun `touch only captures the PiP frame, then returns super's result`() {
        val handled = router.dispatchTouchEvent(superReturning(true))

        assertTrue(handled)
        assertEquals(1, superCalls)
        verifySequence { host.capturePipFrame() }
    }

    @Test
    fun `touch never reaches the view model`() {
        router.dispatchTouchEvent(superReturning(false))

        verify(exactly = 0) { host.processKey(any(), any()) }
        verify(exactly = 0) { host.processMotion(any()) }
    }
}
