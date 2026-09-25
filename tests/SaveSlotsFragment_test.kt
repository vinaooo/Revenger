package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.models.SaveSlotPayload
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.retromenu3.callbacks.SaveSlotsListener
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
 * Robolectric tests for SaveSlotsFragment, following the pattern proven by MenuIntegration_test
 * and ManageSavesFragment_test.
 *
 * Beyond structure, these cover both dialogs that onSlotConfirmed() opens (the naming keyboard
 * for an empty slot, the overwrite confirmation for an occupied one), their navigation, and every
 * performSave() outcome. A mocked [SaveStateManager] is injected by reflection into
 * `SaveStateGridFragment.saveStateManager` after the view is created, and the view model's
 * `retroView` is a mock with a stubbed `serializeState()`. The keyboard's confirm/cancel
 * callbacks are read from the private `RetroKeyboard.onConfirm`/`onCancel` fields.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SaveSlotsFragment_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: SaveSlotsFragment
    private lateinit var savesDir: File
    private lateinit var listener: SaveSlotsListener

    private val stateBytes = byteArrayOf(1, 2, 3)

    @Before
    fun setup() {
        SaveStateManager.clearInstance()
        SessionSlotTracker.clearInstance()
        val context: Context = ApplicationProvider.getApplicationContext()
        savesDir = File(context.filesDir, "saves")
        savesDir.deleteRecursively()

        activity =
                Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        listener = mockk(relaxed = true)
        fragment = SaveSlotsFragment.newInstance()
        fragment.setListener(listener)
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "save_slots")
                .commitNow()
    }

    @After
    fun cleanup() {
        savesDir.deleteRecursively()
        SaveStateManager.clearInstance()
        SessionSlotTracker.clearInstance()
    }

    // ========== Helpers ==========

    private fun mockedSaveStateManager(saveResult: Boolean = true): SaveStateManager {
        val manager = mockk<SaveStateManager>(relaxed = true)
        every { manager.getAllSlots() } returns
                (1..SaveStateManager.TOTAL_SLOTS).map { SaveSlotData.empty(it) }
        every { manager.getSlot(any()) } answers { SaveSlotData.empty(firstArg()) }
        every { manager.saveToSlot(any(), any()) } returns saveResult
        val field = SaveStateGridFragment::class.java.getDeclaredField("saveStateManager")
        field.isAccessible = true
        field.set(fragment, manager)
        return manager
    }

    private fun viewModel(): GameActivityViewModel =
            ViewModelProvider(activity)[GameActivityViewModel::class.java]

    private fun installRetroView(serialize: () -> ByteArray = { stateBytes }) {
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view.serializeState() } answers { serialize() }
        viewModel().retroView = retroView
    }

    private fun occupiedSlot(slotNumber: Int) =
            SaveSlotData(
                    slotNumber = slotNumber,
                    name = "Save $slotNumber",
                    timestamp = null,
                    romName = "rom",
                    stateFile = null,
                    screenshotFile = null,
                    isEmpty = false
            )

    private fun rootView(): View = fragment.requireView()

    private fun dialogVisible(): Boolean =
            SaveSlotsFragment::class.java.getDeclaredField("isDialogVisible").let {
                it.isAccessible = true
                it.getBoolean(fragment)
            }

    private fun retroKeyboard(): RetroKeyboard {
        val field = SaveSlotsFragment::class.java.getDeclaredField("retroKeyboard")
        field.isAccessible = true
        return field.get(fragment) as RetroKeyboard
    }

    @Suppress("UNCHECKED_CAST")
    private fun keyboardOnConfirm(): (String) -> Unit =
            RetroKeyboard::class.java.getDeclaredField("onConfirm").let {
                it.isAccessible = true
                it.get(retroKeyboard()) as (String) -> Unit
            }

    @Suppress("UNCHECKED_CAST")
    private fun keyboardOnCancel(): () -> Unit =
            RetroKeyboard::class.java.getDeclaredField("onCancel").let {
                it.isAccessible = true
                it.get(retroKeyboard()) as () -> Unit
            }

    private fun expectedToast(resId: Int, vararg args: Any): String =
            FontUtils.getCapitalizedString(activity, resId, *args)

    private fun confirmButton(): RetroCardView = rootView().findViewById(R.id.dialog_confirm_button)

    private fun cancelButton(): RetroCardView = rootView().findViewById(R.id.dialog_cancel_button)

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
    fun `getTitleResId retorna menu_save_state`() {
        assertEquals(R.string.menu_save_state, fragment.getTitleResId())
    }

    @Test
    fun `getMenuItems retorna item unico representando o grid`() {
        val menuItems = fragment.getMenuItems()
        assertEquals(1, menuItems.size)
        assertEquals("grid", menuItems[0].id)
    }

    @Test
    fun `onBackConfirmed sem dialog visivel nao lanca excecao`() {
        try {
            fragment.onBackConfirmed()
            assertTrue(true)
        } catch (e: Exception) {
            fail("onBackConfirmed should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `onBack sem dialog retorna false`() {
        assertFalse(fragment.onBack())
    }

    // ========== Naming dialog (empty slot) ==========

    @Test
    fun `slot vazio abre o dialogo de nome com o nome padrao preenchido`() {
        mockedSaveStateManager()

        fragment.onSlotConfirmed(SaveSlotData.empty(4))

        assertTrue(dialogVisible())
        val editText = rootView().findViewById<RetroEditText>(R.id.rename_edit_text)
        assertNotNull(editText)
        assertEquals("Slot 4", editText.getTextContent())
        assertEquals(
                FontUtils.getCapitalizedString(activity, R.string.name_save_dialog_title),
                rootView().findViewById<TextView>(R.id.dialog_title).text.toString()
        )
    }

    @Test
    fun `confirmar o nome salva no slot com o nome digitado`() {
        val manager = mockedSaveStateManager()
        installRetroView()
        fragment.onSlotConfirmed(SaveSlotData.empty(2))

        keyboardOnConfirm()("My Save")

        val payload = slot<SaveSlotPayload>()
        verify(exactly = 1) { manager.saveToSlot(2, capture(payload)) }
        assertEquals("My Save", payload.captured.name)
        assertArrayEquals(stateBytes, payload.captured.stateBytes)
        assertEquals(activity.getString(R.string.name), payload.captured.romName)
        assertFalse(dialogVisible())
        assertNull(rootView().findViewById(R.id.rename_edit_text))
    }

    @Test
    fun `confirmar nome em branco usa o nome padrao do slot`() {
        val manager = mockedSaveStateManager()
        installRetroView()
        fragment.onSlotConfirmed(SaveSlotData.empty(5))

        keyboardOnConfirm()("   ")

        val payload = slot<SaveSlotPayload>()
        verify(exactly = 1) { manager.saveToSlot(5, capture(payload)) }
        assertEquals("Slot 5", payload.captured.name)
    }

    @Test
    fun `cancelar o dialogo de nome ainda salva com o nome padrao`() {
        val manager = mockedSaveStateManager()
        installRetroView()
        fragment.onSlotConfirmed(SaveSlotData.empty(6))

        keyboardOnCancel()()

        val payload = slot<SaveSlotPayload>()
        verify(exactly = 1) { manager.saveToSlot(6, capture(payload)) }
        assertEquals("Slot 6", payload.captured.name)
        assertFalse(dialogVisible())
    }

    @Test
    fun `com o teclado ativo a navegacao nao move a selecao do grid`() {
        mockedSaveStateManager()
        fragment.onSlotConfirmed(SaveSlotData.empty(1))
        val indexBefore = fragment.getCurrentSelectedIndex()

        fragment.onNavigateDown()
        fragment.onNavigateUp()
        assertTrue(fragment.onNavigateRight())
        assertTrue(fragment.onNavigateLeft())
        fragment.onNavigateDown()

        assertEquals(indexBefore, fragment.getCurrentSelectedIndex())
        assertTrue(dialogVisible())
    }

    @Test
    fun `onBack com o dialogo de nome fecha o dialogo sem salvar`() {
        val manager = mockedSaveStateManager()
        fragment.onSlotConfirmed(SaveSlotData.empty(1))

        assertTrue(fragment.onBack())

        assertFalse(dialogVisible())
        verify(exactly = 0) { manager.saveToSlot(any(), any()) }
    }

    // ========== Overwrite dialog (occupied slot) ==========

    @Test
    fun `slot ocupado abre a confirmacao de sobrescrita com confirmar selecionado`() {
        mockedSaveStateManager()

        fragment.onSlotConfirmed(occupiedSlot(3))

        assertTrue(dialogVisible())
        assertEquals(
                FontUtils.getCapitalizedString(activity, R.string.overwrite_dialog_title),
                rootView().findViewById<TextView>(R.id.dialog_title).text.toString()
        )
        assertEquals(RetroCardView.State.SELECTED, confirmButton().getState())
        assertEquals(View.VISIBLE, rootView().findViewById<TextView>(R.id.confirm_button_arrow).visibility)
        assertEquals(RetroCardView.State.NORMAL, cancelButton().getState())
        assertEquals(View.GONE, rootView().findViewById<TextView>(R.id.cancel_button_arrow).visibility)
    }

    @Test
    fun `navegacao no dialogo de sobrescrita alterna entre os botoes sem sair dos limites`() {
        mockedSaveStateManager()
        fragment.onSlotConfirmed(occupiedSlot(3))
        val gridIndexBefore = fragment.getCurrentSelectedIndex()

        fragment.onNavigateDown()
        fragment.onNavigateDown() // already on the last button: stays there
        assertEquals(RetroCardView.State.NORMAL, confirmButton().getState())
        assertEquals(RetroCardView.State.SELECTED, cancelButton().getState())

        fragment.onNavigateUp()
        fragment.onNavigateUp() // already on the first button: stays there
        assertEquals(RetroCardView.State.SELECTED, confirmButton().getState())
        assertEquals(RetroCardView.State.NORMAL, cancelButton().getState())

        assertEquals(gridIndexBefore, fragment.getCurrentSelectedIndex())
    }

    @Test
    fun `confirmar sobrescrita salva no slot e notifica o listener`() {
        val manager = mockedSaveStateManager()
        installRetroView()
        fragment.onSlotConfirmed(occupiedSlot(7))

        fragment.onConfirm() // confirm is selected by default

        val payload = slot<SaveSlotPayload>()
        verify(exactly = 1) { manager.saveToSlot(7, capture(payload)) }
        assertEquals("Slot 7", payload.captured.name)
        verify(exactly = 1) { listener.onSaveCompleted(7) }
        verify(exactly = 1) { manager.getAllSlots() } // grid refreshed
        assertEquals(7, SessionSlotTracker.getInstance().getLastUsedSlot())
        assertEquals(expectedToast(R.string.save_success, 7), ShadowToast.getTextOfLatestToast())
        assertFalse(dialogVisible())
    }

    @Test
    fun `cancelar sobrescrita fecha o dialogo sem salvar`() {
        val manager = mockedSaveStateManager()
        installRetroView()
        fragment.onSlotConfirmed(occupiedSlot(7))

        fragment.onNavigateDown()
        fragment.onConfirm() // cancel

        assertFalse(dialogVisible())
        assertNull(rootView().findViewById(R.id.dialog_confirm_button))
        verify(exactly = 0) { manager.saveToSlot(any(), any()) }
        verify(exactly = 0) { listener.onSaveCompleted(any()) }
    }

    @Test
    fun `onBackConfirmed com dialogo visivel apenas fecha o dialogo`() {
        val manager = mockedSaveStateManager()
        fragment.onSlotConfirmed(occupiedSlot(2))

        fragment.onBackConfirmed()

        assertFalse(dialogVisible())
        verify(exactly = 0) { manager.saveToSlot(any(), any()) }
    }

    // ========== performSave failure paths ==========

    @Test
    fun `sem retroView o save mostra erro e nao grava`() {
        val manager = mockedSaveStateManager()
        viewModel().retroView = null
        fragment.onSlotConfirmed(occupiedSlot(1))

        confirmButton().performClick()

        verify(exactly = 0) { manager.saveToSlot(any(), any()) }
        verify(exactly = 0) { listener.onSaveCompleted(any()) }
        assertEquals(expectedToast(R.string.save_error), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `falha do SaveStateManager mostra erro e nao notifica o listener`() {
        val manager = mockedSaveStateManager(saveResult = false)
        installRetroView()
        fragment.onSlotConfirmed(occupiedSlot(8))

        confirmButton().performClick()

        verify(exactly = 1) { manager.saveToSlot(8, any()) }
        verify(exactly = 0) { listener.onSaveCompleted(any()) }
        assertNull(SessionSlotTracker.getInstance().getLastUsedSlot())
        assertEquals(expectedToast(R.string.save_error), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `excecao ao serializar o estado mostra erro e nao grava`() {
        val manager = mockedSaveStateManager()
        installRetroView { throw NullPointerException("native state unavailable") }
        fragment.onSlotConfirmed(occupiedSlot(9))

        confirmButton().performClick()

        verify(exactly = 0) { manager.saveToSlot(any(), any()) }
        verify(exactly = 0) { listener.onSaveCompleted(any()) }
        assertEquals(expectedToast(R.string.save_error), ShadowToast.getTextOfLatestToast())
    }

    // ========== Lifecycle ==========

    @Test
    fun `remover o fragment com dialogo aberto limpa o dialogo sem lancar excecao`() {
        mockedSaveStateManager()
        fragment.onSlotConfirmed(occupiedSlot(1))
        val dialog = rootView().findViewById<View>(R.id.dialog_confirm_button)
        assertNotNull(dialog)

        activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()

        assertFalse(dialogVisible())
        assertFalse(fragment.isAdded)
    }
}
