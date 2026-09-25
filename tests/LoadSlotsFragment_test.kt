package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.retromenu3.callbacks.LoadSlotsListener
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.File
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * Robolectric tests for LoadSlotsFragment, following the pattern proven by MenuIntegration_test
 * and ManageSavesFragment_test.
 *
 * Structure tests run against the real (empty) SaveStateManager. The load-flow tests inject a
 * mocked [SaveStateManager] and a relaxed mocked [GameActivityViewModel] by reflection into
 * `SaveStateGridFragment`'s fields after the view is created. They cover the empty-slot toast,
 * the full-screen preview selection (preview first, screenshot fallback, none), and every
 * performLoad() outcome.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LoadSlotsFragment_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: LoadSlotsFragment
    private lateinit var savesDir: File
    private lateinit var imagesDir: File
    private lateinit var listener: LoadSlotsListener
    private lateinit var viewModel: GameActivityViewModel
    private lateinit var navigationController: NavigationController
    private lateinit var retroView: RetroView

    private val stateBytes = byteArrayOf(7, 8, 9)

    @Before
    fun setup() {
        SaveStateManager.clearInstance()
        SessionSlotTracker.clearInstance()
        val context: Context = ApplicationProvider.getApplicationContext()
        savesDir = File(context.filesDir, "saves")
        savesDir.deleteRecursively()
        imagesDir = File(context.cacheDir, "load_slots_test").apply { mkdirs() }

        activity =
                Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        listener = mockk(relaxed = true)
        fragment = LoadSlotsFragment.newInstance()
        fragment.setListener(listener)
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "load_slots")
                .commitNow()
    }

    @After
    fun cleanup() {
        savesDir.deleteRecursively()
        imagesDir.deleteRecursively()
        SaveStateManager.clearInstance()
        SessionSlotTracker.clearInstance()
    }

    // ========== Helpers ==========

    private fun setGridField(name: String, value: Any) {
        val field = SaveStateGridFragment::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(fragment, value)
    }

    private fun mockedSaveStateManager(loadResult: ByteArray? = stateBytes): SaveStateManager {
        val manager = mockk<SaveStateManager>(relaxed = true)
        every { manager.getAllSlots() } returns
                (1..SaveStateManager.TOTAL_SLOTS).map { SaveSlotData.empty(it) }
        every { manager.getSlot(any()) } answers { occupiedSlot(firstArg()) }
        every { manager.loadFromSlot(any()) } returns loadResult
        setGridField("saveStateManager", manager)
        return manager
    }

    /** Injects a relaxed view model mock; [unserialize] backs `retroView.view.unserializeState()`. */
    private fun mockedViewModel(
            withRetroView: Boolean = true,
            unserialize: (ByteArray) -> Boolean = { true }
    ) {
        viewModel = mockk(relaxed = true)
        navigationController = mockk(relaxed = true)
        every { viewModel.navigationController } returns navigationController
        if (withRetroView) {
            retroView = mockk(relaxed = true)
            every { retroView.view.unserializeState(any()) } answers { unserialize(firstArg()) }
            every { viewModel.retroView } returns retroView
        } else {
            every { viewModel.retroView } returns null
        }
        setGridField("viewModel", viewModel)
    }

    private fun occupiedSlot(
            slotNumber: Int,
            screenshotFile: File? = null,
            previewFile: File? = null
    ) =
            SaveSlotData(
                    slotNumber = slotNumber,
                    name = "Save $slotNumber",
                    timestamp = null,
                    romName = "rom",
                    stateFile = null,
                    screenshotFile = screenshotFile,
                    previewFile = previewFile,
                    isEmpty = false
            )

    /** Writes a real PNG of the given size, so the decoded bitmap's width identifies the file. */
    private fun pngFile(name: String, width: Int, height: Int): File {
        val file = File(imagesDir, name)
        file.outputStream().use { out ->
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    .compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    private fun expectedToast(resId: Int, vararg args: Any): String =
            FontUtils.getCapitalizedString(activity, resId, *args)

    // ========== Structure ==========

    @Test
    fun `fragment e criado corretamente`() {
        assertNotNull(fragment)
        assertTrue(fragment.isAdded)
    }

    @Test
    fun `fragment inherits from SaveStateGridFragment and MenuFragmentBase`() {
        assertTrue(fragment is SaveStateGridFragment)
        assertTrue(fragment is MenuFragmentBase)
    }

    @Test
    fun `getTitleResId retorna menu_load_state`() {
        assertEquals(R.string.menu_load_state, fragment.getTitleResId())
    }

    @Test
    fun `getMenuItems retorna item unico representando o grid`() {
        val menuItems = fragment.getMenuItems()
        assertEquals(1, menuItems.size)
        assertEquals("grid", menuItems[0].id)
    }

    @Test
    fun `onBackConfirmed can be called without error`() {
        try {
            fragment.onBackConfirmed()
            assertTrue(true)
        } catch (e: Exception) {
            fail("onBackConfirmed should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `onBackConfirmed volta pelo NavigationController`() {
        mockedSaveStateManager()
        mockedViewModel()

        fragment.onBackConfirmed()

        verify(exactly = 1) { navigationController.navigateBack() }
    }

    // ========== Empty slot ==========

    @Test
    fun `onSlotConfirmed em slot vazio nao lanca excecao`() {
        val emptySlot = SaveStateManager.getInstance(activity).getSlot(1)
        try {
            fragment.onSlotConfirmed(emptySlot)
            assertTrue(true)
        } catch (e: Exception) {
            fail("onSlotConfirmed should not throw exception on empty slot: ${e.message}")
        }
    }

    @Test
    fun `slot vazio mostra aviso e nao carrega nem mostra preview`() {
        val manager = mockedSaveStateManager()
        mockedViewModel()

        fragment.onSlotConfirmed(SaveSlotData.empty(3))

        assertEquals(expectedToast(R.string.slot_is_empty), ShadowToast.getTextOfLatestToast())
        verify(exactly = 0) { manager.loadFromSlot(any()) }
        verify(exactly = 0) { viewModel.showLoadPreview(any()) }
        verify(exactly = 0) { listener.onLoadCompleted(any()) }
    }

    // ========== Load outcomes ==========

    @Test
    fun `load bem sucedido restaura o estado, registra o slot e notifica o listener`() {
        val manager = mockedSaveStateManager()
        mockedViewModel()

        fragment.onSlotConfirmed(occupiedSlot(4))

        verify(exactly = 1) { manager.loadFromSlot(4) }
        verify(exactly = 1) { retroView.view.unserializeState(stateBytes) }
        verify(exactly = 1) { listener.onLoadCompleted(4) }
        assertEquals(4, SessionSlotTracker.getInstance().getLastUsedSlot())
        assertEquals(
                SessionSlotTracker.OperationType.LOAD,
                SessionSlotTracker.getInstance().getLastOperationType()
        )
        assertEquals(expectedToast(R.string.load_success, 4), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `confirmar pelo gamepad carrega o slot selecionado`() {
        val manager = mockedSaveStateManager()
        mockedViewModel()

        fragment.onNavigateRight() // (0,1) -> slot 2
        fragment.onConfirm()

        verify(exactly = 1) { manager.loadFromSlot(2) }
        verify(exactly = 1) { listener.onLoadCompleted(2) }
    }

    @Test
    fun `sem retroView o load mostra erro e nao le o slot`() {
        val manager = mockedSaveStateManager()
        mockedViewModel(withRetroView = false)

        fragment.onSlotConfirmed(occupiedSlot(1))

        verify(exactly = 0) { manager.loadFromSlot(any()) }
        verify(exactly = 0) { listener.onLoadCompleted(any()) }
        assertEquals(expectedToast(R.string.load_error), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `slot sem bytes de estado mostra erro e nao restaura`() {
        mockedSaveStateManager(loadResult = null)
        mockedViewModel()

        fragment.onSlotConfirmed(occupiedSlot(5))

        verify(exactly = 0) { retroView.view.unserializeState(any()) }
        verify(exactly = 0) { listener.onLoadCompleted(any()) }
        assertNull(SessionSlotTracker.getInstance().getLastUsedSlot())
        assertEquals(expectedToast(R.string.load_error), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `falha ao restaurar o estado mostra erro e nao notifica o listener`() {
        mockedSaveStateManager()
        mockedViewModel { false }

        fragment.onSlotConfirmed(occupiedSlot(6))

        verify(exactly = 0) { listener.onLoadCompleted(any()) }
        assertNull(SessionSlotTracker.getInstance().getLastUsedSlot())
        assertEquals(expectedToast(R.string.load_error), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `excecao ao restaurar o estado mostra erro e nao notifica o listener`() {
        mockedSaveStateManager()
        mockedViewModel { throw NullPointerException("native state unavailable") }

        fragment.onSlotConfirmed(occupiedSlot(7))

        verify(exactly = 0) { listener.onLoadCompleted(any()) }
        assertEquals(expectedToast(R.string.load_error), ShadowToast.getTextOfLatestToast())
    }

    // ========== Load preview ==========

    @Test
    fun `preview do slot tem prioridade sobre o screenshot`() {
        mockedSaveStateManager()
        mockedViewModel()
        val slot =
                occupiedSlot(
                        8,
                        screenshotFile = pngFile("screenshot.png", 3, 3),
                        previewFile = pngFile("preview.png", 12, 6)
                )

        fragment.onSlotConfirmed(slot)

        val shown = slot<Bitmap>()
        verify(exactly = 1) { viewModel.showLoadPreview(capture(shown)) }
        assertEquals(12, shown.captured.width)
    }

    @Test
    fun `sem preview o screenshot do slot e usado`() {
        mockedSaveStateManager()
        mockedViewModel()
        val slot = occupiedSlot(8, screenshotFile = pngFile("screenshot.png", 3, 3))

        fragment.onSlotConfirmed(slot)

        val shown = slot<Bitmap>()
        verify(exactly = 1) { viewModel.showLoadPreview(capture(shown)) }
        assertEquals(3, shown.captured.width)
    }

    @Test
    fun `arquivos de imagem ausentes nao mostram preview mas o load continua`() {
        val manager = mockedSaveStateManager()
        mockedViewModel()
        val slot =
                occupiedSlot(
                        9,
                        screenshotFile = File(imagesDir, "missing_screenshot.png"),
                        previewFile = File(imagesDir, "missing_preview.png")
                )

        fragment.onSlotConfirmed(slot)

        verify(exactly = 0) { viewModel.showLoadPreview(any()) }
        verify(exactly = 1) { manager.loadFromSlot(9) }
        verify(exactly = 1) { listener.onLoadCompleted(9) }
    }
}
