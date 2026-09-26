package com.vinaooo.revenger.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrameTimeRecorder_test {

    private val recorder = FrameTimeRecorder()

    @Test
    fun `first record has no delta`() {
        assertNull(recorder.record(1_000L))
    }

    @Test
    fun `later records return the time since the previous one`() {
        recorder.record(1_000L)

        assertEquals(500L, recorder.record(1_500L))
        assertEquals(250L, recorder.record(1_750L))
    }

    @Test
    fun `reset restarts the measurement from the given time`() {
        recorder.record(1_000L)

        recorder.reset(5_000L)

        assertEquals(100L, recorder.record(5_100L))
    }

    @Test
    fun `record right after reset returns a delta, not null`() {
        recorder.reset(2_000L)

        assertEquals(1L, recorder.record(2_001L))
    }
}
