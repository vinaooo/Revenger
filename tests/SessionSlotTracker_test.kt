package com.vinaooo.revenger.managers

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SessionSlotTracker.
 * Pure JVM tests - no Android dependencies needed.
 */
class SessionSlotTracker_test {

    private lateinit var tracker: SessionSlotTracker

    @Before
    fun setup() {
        SessionSlotTracker.clearInstance()
        tracker = SessionSlotTracker.getInstance()
    }

    @After
    fun tearDown() {
        SessionSlotTracker.clearInstance()
    }

    // ========== INITIAL STATE ==========

    @Test
    fun `initial state has no slot context`() {
        assertFalse(tracker.hasSlotContext())
        assertNull(tracker.getLastUsedSlot())
        assertNull(tracker.getLastOperationType())
    }

    // ========== RECORD SAVE ==========

    @Test
    fun `recordSave sets last used slot`() {
        tracker.recordSave(3)
        assertTrue(tracker.hasSlotContext())
        assertEquals(3, tracker.getLastUsedSlot())
        assertEquals(SessionSlotTracker.OperationType.SAVE, tracker.getLastOperationType())
    }

    @Test
    fun `recordSave with slot 1 is valid`() {
        tracker.recordSave(1)
        assertEquals(1, tracker.getLastUsedSlot())
    }

    @Test
    fun `recordSave with slot 9 is valid`() {
        tracker.recordSave(9)
        assertEquals(9, tracker.getLastUsedSlot())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recordSave with slot 0 throws exception`() {
        tracker.recordSave(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recordSave with slot 10 throws exception`() {
        tracker.recordSave(10)
    }

    // ========== RECORD LOAD ==========

    @Test
    fun `recordLoad sets last used slot`() {
        tracker.recordLoad(5)
        assertTrue(tracker.hasSlotContext())
        assertEquals(5, tracker.getLastUsedSlot())
        assertEquals(SessionSlotTracker.OperationType.LOAD, tracker.getLastOperationType())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recordLoad with slot 0 throws exception`() {
        tracker.recordLoad(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recordLoad with negative slot throws exception`() {
        tracker.recordLoad(-1)
    }

    // ========== OVERWRITE BEHAVIOR ==========

    @Test
    fun `recordSave overwrites previous load`() {
        tracker.recordLoad(2)
        tracker.recordSave(7)
        assertEquals(7, tracker.getLastUsedSlot())
        assertEquals(SessionSlotTracker.OperationType.SAVE, tracker.getLastOperationType())
    }

    @Test
    fun `recordLoad overwrites previous save`() {
        tracker.recordSave(4)
        tracker.recordLoad(8)
        assertEquals(8, tracker.getLastUsedSlot())
        assertEquals(SessionSlotTracker.OperationType.LOAD, tracker.getLastOperationType())
    }

    @Test
    fun `multiple saves track only the last one`() {
        tracker.recordSave(1)
        tracker.recordSave(5)
        tracker.recordSave(9)
        assertEquals(9, tracker.getLastUsedSlot())
    }

    // ========== CLEAR ==========

    @Test
    fun `clear resets all state`() {
        tracker.recordSave(3)
        assertTrue(tracker.hasSlotContext())

        tracker.clear()
        assertFalse(tracker.hasSlotContext())
        assertNull(tracker.getLastUsedSlot())
        assertNull(tracker.getLastOperationType())
    }

    @Test
    fun `clear on empty tracker does not throw`() {
        tracker.clear()
        assertFalse(tracker.hasSlotContext())
    }

    // ========== SINGLETON ==========

    @Test
    fun `getInstance returns same instance`() {
        val instance1 = SessionSlotTracker.getInstance()
        val instance2 = SessionSlotTracker.getInstance()
        assertSame(instance1, instance2)
    }

    @Test
    fun `clearInstance creates new instance`() {
        val instance1 = SessionSlotTracker.getInstance()
        instance1.recordSave(5)

        SessionSlotTracker.clearInstance()
        val instance2 = SessionSlotTracker.getInstance()

        assertNotSame(instance1, instance2)
        assertFalse(instance2.hasSlotContext())
    }

    // ========== CONCURRENCY ==========

    // Regression test for the PiP quick-save flow (a background Thread) racing main-thread
    // save/load operations. lastUsedSlotNumber and lastOperationType must always be updated
    // together: without @Synchronized, one thread's recordSave(1) and another's recordLoad(2)
    // can interleave their field writes, leaving e.g. slot=2 paired with type=SAVE -- a
    // combination neither call ever asked for. With @Synchronized each call is atomic, so the
    // final pair must always match one of the two calls exactly.
    @Test
    fun `recordSave e recordLoad concorrentes nunca deixam um par slot-tipo inconsistente`() {
        val threadCount = 20
        val iterationsPerThread = 500
        val ready = java.util.concurrent.CountDownLatch(threadCount)
        val go = java.util.concurrent.CountDownLatch(1)
        val done = java.util.concurrent.CountDownLatch(threadCount)

        val threads = (0 until threadCount).map { i ->
            Thread {
                ready.countDown()
                go.await()
                repeat(iterationsPerThread) {
                    if (i % 2 == 0) tracker.recordSave(1) else tracker.recordLoad(2)
                }
                done.countDown()
            }
        }
        threads.forEach { it.start() }
        ready.await()
        go.countDown()
        done.await()

        val slot = tracker.getLastUsedSlot()
        val type = tracker.getLastOperationType()
        val consistent =
                (slot == 1 && type == SessionSlotTracker.OperationType.SAVE) ||
                        (slot == 2 && type == SessionSlotTracker.OperationType.LOAD)
        assertTrue("Inconsistent slot/type pair after concurrent access: slot=$slot type=$type", consistent)
    }
}
