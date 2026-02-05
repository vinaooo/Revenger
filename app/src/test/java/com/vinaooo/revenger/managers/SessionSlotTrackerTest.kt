package com.vinaooo.revenger.managers

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SessionSlotTracker.
 * Pure JVM tests - no Android dependencies needed.
 */
class SessionSlotTrackerTest {

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
}
