package com.vinaooo.revenger.utils

import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class StartupTimer_test {

    private var now = 1_000L
    private val timer = StartupTimer(startTimeMs = 1_000L, clock = { now })

    @Test
    fun `mark reports the elapsed time and the step`() {
        now = 1_250L

        assertEquals("⏱️ [T+250ms] setupGamePads() completed", timer.mark("setupGamePads() completed"))
    }

    @Test
    fun `mark at the start reports zero`() {
        assertEquals("⏱️ [T+0ms] start", timer.mark("start"))
    }

    @Test
    fun `mark logs at debug level, not error`() {
        ShadowLog.clear()
        now = 1_007L

        timer.mark("step")

        val entries = ShadowLog.getLogsForTag(StartupTimer.TAG)
        assertEquals(1, entries.size)
        assertEquals(Log.DEBUG, entries.single().type)
        assertTrue(entries.single().msg.endsWith("[T+7ms] step"))
    }
}
