package com.vinaooo.revenger.utils

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * [MenuLogger]'s category helpers: each one logs through the debug gate under the menu's tag,
 * with its own prefix.
 */
class MenuLogger_test {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0
        MenuLogger.setDebugEnabled(true)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `lifecycle, action e state registram com o prefixo da categoria`() {
        MenuLogger.lifecycle("created")
        MenuLogger.action("select")
        MenuLogger.state("main")

        verify(exactly = 1) { Log.d("RetroMenu3", "[LIFECYCLE] created") }
        verify(exactly = 1) { Log.d("RetroMenu3", "[ACTION] select") }
        verify(exactly = 1) { Log.d("RetroMenu3", "[STATE] main") }
    }
}
