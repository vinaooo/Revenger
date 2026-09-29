package com.vinaooo.revenger.controllers

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.GameLifecycleObserver
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.models.SaveSlotPayload
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import java.io.File
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [PipQuickSaveExecutor] runs the PiP window's "Quick Save" action: it waits for the emulator to
 * render a frame after leaving PiP, then writes the state on a background thread to the slot last
 * used this session, else the first empty slot, else the oldest save. The
 * background runner, the slot store and the PiP frame are injected, so these tests run the whole
 * save synchronously and check what gets written, to which slot, and that the PiP task is always
 * finished exactly once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PipQuickSaveExecutor_test {

    private class FakePipHost(override val activity: FragmentActivity) : PipHost {
        var finishPipTaskCalls = 0
        override fun isCurrentlyInPip() = false
        override fun enterPip(params: PictureInPictureParams) = true
        override fun updatePipParams(params: PictureInPictureParams) = Unit
        override fun registerPipReceiver(receiver: BroadcastReceiver, filter: IntentFilter) = Unit
        override fun unregisterPipReceiver(receiver: BroadcastReceiver) = Unit
        override fun bringTaskToFront() = Unit
        override fun finishPipTask() {
            finishPipTaskCalls++
        }
        override fun postToUiThread(action: () -> Unit) = action()
        override fun restoreFloatingButtonVisibility() = Unit
        override fun gameLifecycleObserverOrNull(): GameLifecycleObserver? = null
    }

    private val stateBytes = byteArrayOf(1, 2, 3)

    private lateinit var activity: FragmentActivity
    private lateinit var host: FakePipHost
    private lateinit var viewModel: GameActivityViewModel
    private lateinit var glRetroView: GLRetroView
    private lateinit var saveManager: SaveStateManager
    private lateinit var tracker: SessionSlotTracker
    private val payload = slot<SaveSlotPayload>()
    private val savedSlot = slot<Int>()
    private var pipFrame: Bitmap? = null

    @Before
    fun setUp() {
        SessionSlotTracker.clearInstance()
        tracker = SessionSlotTracker.getInstance()

        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        host = FakePipHost(activity)

        glRetroView = mockk(relaxed = true)
        every { glRetroView.getGLRetroEvents() } returns flowOf(GLRetroView.GLRetroEvents.FrameRendered)
        every { glRetroView.serializeState() } returns stateBytes
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view } returns glRetroView
        viewModel = mockk(relaxed = true)
        every { viewModel.retroView } returns retroView

        saveManager = mockk(relaxed = true)
        every { saveManager.getSlot(any()) } answers { SaveSlotData.empty(firstArg()) }
        every { saveManager.getAllSlots() } returns (1..9).map { SaveSlotData.empty(it) }
        every { saveManager.saveToSlot(capture(savedSlot), capture(payload)) } returns true
    }

    @After
    fun tearDown() {
        SessionSlotTracker.clearInstance()
    }

    private fun executor(frameTimeoutMs: Long = 2_000L) =
            PipQuickSaveExecutor(
                    host = host,
                    viewModel = viewModel,
                    runInBackground = { it.run() },
                    saveManager = { saveManager },
                    pipFrame = { pipFrame },
                    frameTimeoutMs = frameTimeoutMs
            )

    private fun occupiedSlot(number: Int, name: String, savedAt: String = "2026-01-01T00:00:00Z") =
            SaveSlotData(
                    slotNumber = number,
                    name = name,
                    timestamp = Instant.parse(savedAt),
                    romName = "",
                    stateFile = File("slot$number.state"),
                    screenshotFile = null,
                    isEmpty = false
            )

    @Test
    fun `execute sem retroView finaliza a task imediatamente`() {
        every { viewModel.retroView } returns null

        executor().execute()

        assertEquals(1, host.finishPipTaskCalls)
        verify(exactly = 0) { saveManager.saveToSlot(any(), any()) }
    }

    @Test
    fun `com todos os slots vazios salva no slot 1 com o nome padrao`() {
        executor().execute()

        assertEquals(1, savedSlot.captured)
        assertEquals("Slot 1", payload.captured.name)
        assertArrayEquals(stateBytes, payload.captured.stateBytes)
        assertEquals(activity.getString(R.string.name), payload.captured.romName)
        assertEquals(1, host.finishPipTaskCalls)
    }

    @Test
    fun `sem slot usado na sessao salva no primeiro slot vazio sem sobrescrever o slot 1`() {
        every { saveManager.getAllSlots() } returns
                listOf(occupiedSlot(1, "Older save"), occupiedSlot(2, "Slot 2")) + (3..9).map { SaveSlotData.empty(it) }

        executor().execute()

        assertEquals(3, savedSlot.captured)
        assertEquals("Slot 3", payload.captured.name)
    }

    @Test
    fun `com todos os slots cheios sobrescreve o save mais antigo e mantem o nome dele`() {
        val slots = (1..9).map { occupiedSlot(it, "Save $it", savedAt = "2026-02-0${it}T00:00:00Z") }
                .toMutableList()
        slots[4] = occupiedSlot(5, "Oldest", savedAt = "2025-12-31T00:00:00Z")
        every { saveManager.getAllSlots() } returns slots
        every { saveManager.getSlot(5) } returns slots[4]

        executor().execute()

        assertEquals(5, savedSlot.captured)
        assertEquals("Oldest", payload.captured.name)
    }

    @Test
    fun `as datas que faltam sao preenchidas antes de escolher o slot`() {
        executor().execute()

        verifyOrder {
            saveManager.backfillMissingTimestamps()
            saveManager.getAllSlots()
            saveManager.saveToSlot(any(), any())
        }
    }

    @Test
    fun `um save que falha ao gravar nao vira o ultimo slot usado`() {
        every { saveManager.saveToSlot(any(), any()) } returns false

        executor().execute()

        assertNull(tracker.getLastUsedSlot())
        assertEquals(1, host.finishPipTaskCalls)
    }

    @Test
    fun `salva no ultimo slot usado na sessao e mantem o nome dele`() {
        tracker.recordLoad(4)
        every { saveManager.getSlot(4) } returns occupiedSlot(4, "Before the boss")

        executor().execute()

        assertEquals(4, savedSlot.captured)
        assertEquals("Before the boss", payload.captured.name)
    }

    @Test
    fun `o frame do PiP vira screenshot e preview do slot`() {
        val frame = Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888)
        pipFrame = frame

        executor().execute()

        assertSame(frame, payload.captured.screenshot)
        assertSame(frame, payload.captured.preview)
    }

    @Test
    fun `sem frame do PiP o slot e salvo sem screenshot`() {
        executor().execute()

        assertNull(payload.captured.screenshot)
        assertNull(payload.captured.preview)
    }

    @Test
    fun `um save bem sucedido registra o slot como o ultimo usado`() {
        // A load, so only the executor's own recordSave can turn it into a SAVE.
        tracker.recordLoad(6)

        executor().execute()

        assertEquals(6, tracker.getLastUsedSlot())
        assertEquals(SessionSlotTracker.OperationType.SAVE, tracker.getLastOperationType())
    }

    @Test
    fun `se o emulador nao renderiza a tempo o save e abortado e a task finalizada`() {
        every { glRetroView.getGLRetroEvents() } returns flow { awaitCancellation() }

        executor(frameTimeoutMs = 50L).execute()
        assertEquals(0, host.finishPipTaskCalls)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))

        assertEquals(1, host.finishPipTaskCalls)
        verify(exactly = 0) { glRetroView.serializeState() }
        verify(exactly = 0) { saveManager.saveToSlot(any(), any()) }
    }

    @Test
    fun `se serializeState lanca o save e descartado e a task ainda e finalizada`() {
        every { glRetroView.serializeState() } throws NullPointerException("native result was null")

        executor().execute()

        verify(exactly = 0) { saveManager.saveToSlot(any(), any()) }
        assertNull(tracker.getLastUsedSlot())
        assertEquals(1, host.finishPipTaskCalls)
    }
}
