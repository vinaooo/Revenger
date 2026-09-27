package com.vinaooo.revenger.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker
import java.io.File
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [PipLastSlotScreenshotResolver] picks the thumbnail shown as a last-resort PiP still: the slot
 * used last this session, otherwise the newest save. It must never fall back to a fixed slot,
 * which would show an unrelated (possibly very old) save.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PipLastSlotScreenshotResolver_test {

    private lateinit var context: Context
    private lateinit var savesDir: File

    @Before
    fun setUp() {
        SaveStateManager.clearInstance()
        SessionSlotTracker.clearInstance()
        context = ApplicationProvider.getApplicationContext()
        savesDir = File(context.filesDir, "saves")
        savesDir.deleteRecursively()
    }

    @After
    fun tearDown() {
        savesDir.deleteRecursively()
        SaveStateManager.clearInstance()
        SessionSlotTracker.clearInstance()
    }

    // An occupied slot saved at [savedAt], with a thumbnail unless [withScreenshot] is false.
    private fun writeSlot(number: Int, savedAt: String, withScreenshot: Boolean = true): File {
        val dir = File(savesDir, "slot_$number").apply { mkdirs() }
        File(dir, "state.bin").writeBytes(byteArrayOf(1))
        File(dir, "metadata.json").writeText(JSONObject().put("name", "Slot $number").put("timestamp", savedAt).toString())
        val screenshot = File(dir, "screenshot.webp")
        if (withScreenshot) screenshot.writeBytes(byteArrayOf(1))
        return screenshot
    }

    private fun decodedPath() =
            PipLastSlotScreenshotResolver.resolve(context)?.let { shadowOf(it).createdFromPath }

    @Test
    fun `usa a miniatura do slot usado por ultimo nesta sessao`() {
        writeSlot(1, "2026-05-01T00:00:00Z")
        val used = writeSlot(4, "2026-01-01T00:00:00Z")
        SessionSlotTracker.getInstance().recordLoad(4)

        assertEquals(used.absolutePath, decodedPath())
    }

    @Test
    fun `sem slot usado na sessao usa a miniatura do save mais novo`() {
        writeSlot(1, "2026-01-01T00:00:00Z")
        val newest = writeSlot(6, "2026-04-01T00:00:00Z")
        writeSlot(8, "2026-02-01T00:00:00Z")

        assertEquals(newest.absolutePath, decodedPath())
    }

    @Test
    fun `sem nenhum save nao mostra nada`() {
        assertNull(PipLastSlotScreenshotResolver.resolve(context))
    }

    @Test
    fun `slot escolhido sem miniatura nao mostra nada`() {
        writeSlot(2, "2026-01-01T00:00:00Z", withScreenshot = false)

        assertNull(PipLastSlotScreenshotResolver.resolve(context))
    }
}
