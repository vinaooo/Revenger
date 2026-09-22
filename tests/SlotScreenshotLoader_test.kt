package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.File
import java.util.Base64
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Unit tests for [SlotScreenshotLoader], extracted from [SaveStateGridFragment]'s
 * `createSlotView` to keep that fragment's function count within detekt's `TooManyFunctions`
 * threshold. Mirrors the branch coverage the fragment-level tests already had (no file, decode
 * returns null, valid file, decode throws) but exercised directly against the loader.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SlotScreenshotLoader_test {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var tempDir: File

    @Before
    fun setup() {
        tempDir = File(context.filesDir, "slot_screenshot_loader_test")
        tempDir.mkdirs()
    }

    @After
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    private fun writeValidPng(): File {
        // Minimal 1x1 transparent PNG - valid enough for BitmapFactory to decode under Robolectric.
        val pngBytes =
                Base64.getDecoder()
                        .decode(
                                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
                        )
        val file = File(tempDir, "valid_screenshot.png")
        file.writeBytes(pngBytes)
        return file
    }

    private fun assertShowsDrawableResource(view: ImageView, resId: Int) {
        val actual = Shadows.shadowOf(view.drawable!!)
        assertEquals(resId, actual.createdFromResId)
    }

    @Test
    fun `null screenshot file shows the no-screenshot icon`() {
        val screenshot = ImageView(context)

        SlotScreenshotLoader.load(screenshot, screenshotFile = null)

        assertShowsDrawableResource(screenshot, R.drawable.ic_no_screenshot)
    }

    @Test
    fun `decode returning null shows the no-screenshot icon`() {
        mockkStatic(BitmapFactory::class)
        try {
            every { BitmapFactory.decodeFile(any<String>()) } returns null
            val screenshot = ImageView(context)

            SlotScreenshotLoader.load(screenshot, writeValidPng())

            assertShowsDrawableResource(screenshot, R.drawable.ic_no_screenshot)
        } finally {
            unmockkStatic(BitmapFactory::class)
        }
    }

    @Test
    fun `valid screenshot file shows the decoded bitmap`() {
        val screenshot = ImageView(context)

        SlotScreenshotLoader.load(screenshot, writeValidPng())

        assertTrue(screenshot.drawable is android.graphics.drawable.BitmapDrawable)
    }

    @Test
    fun `decode throwing shows the no-screenshot icon without propagating the exception`() {
        mockkStatic(BitmapFactory::class)
        try {
            every { BitmapFactory.decodeFile(any<String>()) } throws
                    RuntimeException("corrupt native decode")
            val screenshot = ImageView(context)

            SlotScreenshotLoader.load(screenshot, writeValidPng())

            assertShowsDrawableResource(screenshot, R.drawable.ic_no_screenshot)
        } finally {
            unmockkStatic(BitmapFactory::class)
        }
    }
}
