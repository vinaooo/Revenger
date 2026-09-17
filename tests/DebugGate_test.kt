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
 * [DebugGate] was split out of [MenuLogger] purely to keep that object under the project's
 * function-count threshold, taking over the debug-flag-gated conditional logging ([d]) and its
 * BuildConfig.DEBUG-via-reflection default.
 */
class DebugGate_test {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `construcao nao lanca e resolve um valor inicial para isDebugEnabled`() {
        // Just exercises the BuildConfig.DEBUG-via-reflection init path without asserting which
        // way it resolves (debug unit tests run against the debug BuildConfig, whose DEBUG value
        // isn't this class's concern).
        DebugGate("TestTag")
    }

    @Test
    fun `d registra quando o debug esta habilitado`() {
        val gate = DebugGate("TestTag")
        gate.setDebugEnabled(true)

        gate.d("hello")

        verify { Log.d("TestTag", "hello") }
    }

    @Test
    fun `d nao registra quando o debug esta desabilitado`() {
        val gate = DebugGate("TestTag")
        gate.setDebugEnabled(false)

        gate.d("hello")

        verify(exactly = 0) { Log.d("TestTag", "hello") }
    }

    @Test
    fun `d com throwable so registra quando o debug esta habilitado`() {
        val gate = DebugGate("TestTag")
        val error = RuntimeException("boom")

        gate.setDebugEnabled(false)
        gate.d("hello", error)
        verify(exactly = 0) { Log.d("TestTag", "hello", error) }

        gate.setDebugEnabled(true)
        gate.d("hello", error)
        verify { Log.d("TestTag", "hello", error) }
    }

    @Test
    fun `setDebugEnabled sempre registra um log informativo da mudanca`() {
        val gate = DebugGate("TestTag")

        gate.setDebugEnabled(true)
        verify { Log.i("TestTag", "[LOGGER] Debug logging enabled") }

        gate.setDebugEnabled(false)
        verify { Log.i("TestTag", "[LOGGER] Debug logging disabled") }
    }
}
