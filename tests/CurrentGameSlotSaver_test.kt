package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker
import com.vinaooo.revenger.models.SaveSlotPayload
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * [CurrentGameSlotSaver] writes the running game into a slot for the Save State and Save-and-Exit
 * grids: what goes into the slot, the toast, and that only a written save counts as this
 * session's last used slot.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class CurrentGameSlotSaver_test {

    private val stateBytes = byteArrayOf(4, 5, 6)
    private val screenshot = Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888)
    private val preview = Bitmap.createBitmap(8, 6, Bitmap.Config.ARGB_8888)

    private lateinit var context: Context
    private lateinit var viewModel: GameActivityViewModel
    private lateinit var manager: SaveStateManager
    private val payload = slot<SaveSlotPayload>()

    @Before
    fun setUp() {
        SessionSlotTracker.clearInstance()
        context = ApplicationProvider.getApplicationContext()
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view.serializeState() } returns stateBytes
        viewModel = mockk(relaxed = true)
        every { viewModel.retroView } returns retroView
        every { viewModel.getCachedScreenshot() } returns screenshot
        every { viewModel.getCachedFullScreenshot() } returns preview
        manager = mockk(relaxed = true)
        every { manager.saveToSlot(any(), capture(payload)) } returns true
    }

    @After
    fun tearDown() {
        SessionSlotTracker.clearInstance()
    }

    private fun saver() = CurrentGameSlotSaver(context, viewModel, manager)

    private fun toast(resId: Int, vararg args: Any) = FontUtils.getCapitalizedString(context, resId, *args)

    @Test
    fun `salva o estado, as capturas, o nome do slot e o nome do jogo`() {
        assertTrue(saver().save(4, "Before the boss"))

        verify(exactly = 1) { manager.saveToSlot(4, any()) }
        assertArrayEquals(stateBytes, payload.captured.stateBytes)
        assertSame(screenshot, payload.captured.screenshot)
        assertSame(preview, payload.captured.preview)
        assertEquals("Before the boss", payload.captured.name)
        assertEquals(context.getString(R.string.name), payload.captured.romName)
    }

    @Test
    fun `um save gravado vira o ultimo slot usado e mostra o toast de sucesso`() {
        saver().save(4, "Slot 4")

        assertEquals(4, SessionSlotTracker.getInstance().getLastUsedSlot())
        assertEquals(toast(R.string.save_success, 4), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `sem emulador nao grava e mostra erro`() {
        every { viewModel.retroView } returns null

        assertFalse(saver().save(4, "Slot 4"))

        verify(exactly = 0) { manager.saveToSlot(any(), any()) }
        assertEquals(toast(R.string.save_error), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `uma gravacao que falha nao vira o ultimo slot usado`() {
        every { manager.saveToSlot(any(), any()) } returns false

        assertFalse(saver().save(4, "Slot 4"))

        assertNull(SessionSlotTracker.getInstance().getLastUsedSlot())
        assertEquals(toast(R.string.save_error), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `uma falha ao serializar o estado nao grava e mostra erro`() {
        every { viewModel.retroView!!.view.serializeState() } throws NullPointerException("native result was null")

        assertFalse(saver().save(4, "Slot 4"))

        verify(exactly = 0) { manager.saveToSlot(any(), any()) }
        assertNull(SessionSlotTracker.getInstance().getLastUsedSlot())
        assertEquals(toast(R.string.save_error), ShadowToast.getTextOfLatestToast())
    }
}
