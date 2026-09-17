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
 * [LevelLogger] was split out of [MenuLogger] purely to keep that object under the project's
 * function-count threshold, taking over its always-on info/warn/error logging.
 */
class LevelLogger_test {

    private val logger = LevelLogger("TestTag")

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `i registra com a tag informada`() {
        logger.i("hello")

        verify { Log.i("TestTag", "hello") }
    }

    @Test
    fun `i com throwable registra a excecao`() {
        val error = RuntimeException("boom")

        logger.i("hello", error)

        verify { Log.i("TestTag", "hello", error) }
    }

    @Test
    fun `w registra com a tag informada`() {
        logger.w("careful")

        verify { Log.w("TestTag", "careful") }
    }

    @Test
    fun `w com throwable registra a excecao`() {
        val error = RuntimeException("boom")

        logger.w("careful", error)

        verify { Log.w("TestTag", "careful", error) }
    }

    @Test
    fun `e registra com a tag informada`() {
        logger.e("failure")

        verify { Log.e("TestTag", "failure") }
    }

    @Test
    fun `e com throwable registra a excecao`() {
        val error = RuntimeException("boom")

        logger.e("failure", error)

        verify { Log.e("TestTag", "failure", error) }
    }
}
