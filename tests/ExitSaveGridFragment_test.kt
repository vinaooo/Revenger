package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.models.SaveSlotPayload
import com.vinaooo.revenger.retroview.RetroView
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
 * Robolectric tests for ExitSaveGridFragment, following the pattern proven by
 * ManageSavesFragment_test and SaveSlotsFragment_test. It has no dedicated listener interface.
 *
 * After the view is created, a mocked [SaveStateManager] and a relaxed mocked
 * [GameActivityViewModel] are injected by reflection into `SaveStateGridFragment`'s
 * `saveStateManager`/`viewModel` fields. That lets these tests drive the naming and overwrite
 * dialogs, every performSave() outcome, and the EXIT button's disabled/enabled states.
 *
 * The shutdown path is safe to reach here: performExitWithShutdown() only kills the process
 * inside `GameActivity.startShutdownAnimation`'s callback, and the host here is a plain
 * [FragmentActivity], so only the (mocked) `dismissRetroMenu3()` call runs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExitSaveGridFragment_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: ExitSaveGridFragment
    private lateinit var savesDir: File
    private lateinit var viewModel: GameActivityViewModel
    private lateinit var navigationController: NavigationController

    private val stateBytes = byteArrayOf(4, 5, 6)

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

        fragment = ExitSaveGridFragment.newInstance()
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "exit_save_grid")
                .commitNow()
    }

    @After
    fun cleanup() {
        savesDir.deleteRecursively()
        SaveStateManager.clearInstance()
        SessionSlotTracker.clearInstance()
    }

    // ========== Helpers ==========

    private fun setGridField(name: String, value: Any) {
        val field = SaveStateGridFragment::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(fragment, value)
    }

    private fun mockedSaveStateManager(saveResult: Boolean = true): SaveStateManager {
        val manager = mockk<SaveStateManager>(relaxed = true)
        every { manager.getAllSlots() } returns
                (1..SaveStateManager.TOTAL_SLOTS).map { SaveSlotData.empty(it) }
        every { manager.getSlot(any()) } answers { SaveSlotData.empty(firstArg()) }
        every { manager.saveToSlot(any(), any()) } returns saveResult
        setGridField("saveStateManager", manager)
        return manager
    }

    /** Injects a relaxed view model mock; [serialize] backs `retroView.view.serializeState()`. */
    private fun mockedViewModel(
            withRetroView: Boolean = true,
            serialize: () -> ByteArray = { stateBytes }
    ) {
        viewModel = mockk(relaxed = true)
        navigationController = mockk(relaxed = true)
        every { viewModel.navigationController } returns navigationController
        if (withRetroView) {
            val retroView = mockk<RetroView>(relaxed = true)
            every { retroView.view.serializeState() } answers { serialize() }
            every { viewModel.retroView } returns retroView
        } else {
            every { viewModel.retroView } returns null
        }
        every { viewModel.getCachedScreenshot() } returns null
        every { viewModel.getCachedFullScreenshot() } returns null
        setGridField("viewModel", viewModel)
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

    private fun exitButton(): Button = rootView().findViewById(R.id.grid_back_button)

    private fun privateField(name: String): Any? =
            ExitSaveGridFragment::class.java.getDeclaredField(name).let {
                it.isAccessible = true
                it.get(fragment)
            }

    private fun dialogVisible(): Boolean = privateField("isDialogVisible") as Boolean

    private fun exitEnabled(): Boolean = privateField("exitEnabled") as Boolean

    private fun retroKeyboard(): RetroKeyboard = privateField("retroKeyboard") as RetroKeyboard

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

    /** Moves the grid selection from the first row down onto the EXIT button. */
    private fun selectExitButton() {
        repeat(SaveStateGridFragment.GRID_ROWS) { fragment.onNavigateDown() }
    }

    /** Saves successfully through the overwrite dialog, which enables the EXIT button. */
    private fun completeSave(slotNumber: Int = 1) {
        fragment.onSlotConfirmed(occupiedSlot(slotNumber))
        confirmButton().performClick()
    }

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
    fun `getTitleResId retorna exit_save_grid_title`() {
        assertEquals(R.string.exit_save_grid_title, fragment.getTitleResId())
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

    // ========== EXIT button (disabled until a save) ==========

    @Test
    fun `botao EXIT comeca desabilitado com o rotulo de saida`() {
        assertEquals(expectedToast(R.string.exit_button_label), exitButton().text.toString())
        assertFalse(exitEnabled())
        assertEquals(0.5f, exitButton().alpha, 0.001f)
        assertEquals(activity.getColor(R.color.rm_disabled_color), exitButton().currentTextColor)
    }

    @Test
    fun `EXIT desabilitado selecionado continua com visual desabilitado`() {
        mockedSaveStateManager()
        mockedViewModel()

        selectExitButton()

        assertEquals(activity.getColor(R.color.rm_disabled_color), exitButton().currentTextColor)
        assertEquals(0.5f, exitButton().alpha, 0.001f)
    }

    @Test
    fun `confirmar EXIT desabilitado mostra a dica e nao sai`() {
        mockedSaveStateManager()
        mockedViewModel()
        selectExitButton()

        fragment.onConfirm()

        assertEquals(activity.getString(R.string.exit_button_disabled_hint), ShadowToast.getTextOfLatestToast())
        verify(exactly = 0) { viewModel.dismissRetroMenu3(any()) }
    }

    @Test
    fun `onBackConfirmed com EXIT desabilitado volta ao menu de saida`() {
        mockedSaveStateManager()
        mockedViewModel()

        fragment.onBackConfirmed()

        verify(exactly = 1) { navigationController.navigateBack() }
        verify(exactly = 0) { viewModel.dismissRetroMenu3(any()) }
    }

    @Test
    fun `save bem sucedido habilita o botao EXIT`() {
        mockedSaveStateManager()
        mockedViewModel()

        completeSave()

        assertTrue(exitEnabled())
        assertEquals(1.0f, exitButton().alpha, 0.001f)
        assertEquals(activity.getColor(R.color.rm_text_color), exitButton().currentTextColor)
    }

    @Test
    fun `EXIT habilitado selecionado usa a cor de selecao`() {
        mockedSaveStateManager()
        mockedViewModel()
        completeSave()

        selectExitButton()

        assertEquals(activity.getColor(R.color.rm_selected_color), exitButton().currentTextColor)
        assertEquals(1.0f, exitButton().alpha, 0.001f)
    }

    @Test
    fun `confirmar EXIT habilitado fecha o menu para o desligamento`() {
        mockedSaveStateManager()
        mockedViewModel()
        completeSave()
        selectExitButton()

        fragment.onConfirm()

        verify(exactly = 1) { viewModel.dismissRetroMenu3(any()) }
    }

    @Test
    fun `onBackConfirmed com EXIT habilitado fecha o menu em vez de voltar`() {
        mockedSaveStateManager()
        mockedViewModel()
        completeSave()

        fragment.onBackConfirmed()

        verify(exactly = 1) { viewModel.dismissRetroMenu3(any()) }
        verify(exactly = 0) { navigationController.navigateBack() }
    }

    // ========== Naming dialog (empty slot) ==========

    @Test
    fun `slot vazio abre o dialogo de nome com o nome padrao preenchido`() {
        mockedSaveStateManager()
        mockedViewModel()

        fragment.onSlotConfirmed(SaveSlotData.empty(4))

        assertTrue(dialogVisible())
        assertEquals("Slot 4", rootView().findViewById<RetroEditText>(R.id.rename_edit_text).getTextContent())
    }

    @Test
    fun `confirmar o nome salva com o nome digitado e habilita o EXIT`() {
        val manager = mockedSaveStateManager()
        mockedViewModel()
        fragment.onSlotConfirmed(SaveSlotData.empty(2))

        keyboardOnConfirm()("My Save")

        val payload = slot<SaveSlotPayload>()
        verify(exactly = 1) { manager.saveToSlot(2, capture(payload)) }
        assertEquals("My Save", payload.captured.name)
        assertArrayEquals(stateBytes, payload.captured.stateBytes)
        assertEquals(activity.getString(R.string.name), payload.captured.romName)
        assertEquals(2, SessionSlotTracker.getInstance().getLastUsedSlot())
        assertEquals(expectedToast(R.string.save_success, 2), ShadowToast.getTextOfLatestToast())
        assertTrue(exitEnabled())
        assertFalse(dialogVisible())
    }

    @Test
    fun `confirmar nome em branco usa o nome padrao do slot`() {
        val manager = mockedSaveStateManager()
        mockedViewModel()
        fragment.onSlotConfirmed(SaveSlotData.empty(5))

        keyboardOnConfirm()("  ")

        val payload = slot<SaveSlotPayload>()
        verify(exactly = 1) { manager.saveToSlot(5, capture(payload)) }
        assertEquals("Slot 5", payload.captured.name)
    }

    @Test
    fun `cancelar o dialogo de nome ainda salva com o nome padrao`() {
        val manager = mockedSaveStateManager()
        mockedViewModel()
        fragment.onSlotConfirmed(SaveSlotData.empty(6))

        keyboardOnCancel()()

        val payload = slot<SaveSlotPayload>()
        verify(exactly = 1) { manager.saveToSlot(6, capture(payload)) }
        assertEquals("Slot 6", payload.captured.name)
        assertFalse(dialogVisible())
    }

    @Test
    fun `com o teclado ativo confirmar pressiona a tecla e a navegacao nao move o grid`() {
        val manager = mockedSaveStateManager()
        mockedViewModel()
        fragment.onSlotConfirmed(SaveSlotData.empty(1))
        val editText = rootView().findViewById<RetroEditText>(R.id.rename_edit_text)
        val gridIndexBefore = fragment.getCurrentSelectedIndex()

        fragment.onNavigateDown()
        fragment.onNavigateUp()
        assertTrue(fragment.onNavigateRight())
        assertTrue(fragment.onNavigateLeft())
        fragment.onConfirm() // presses the keyboard's selected (character) key

        assertEquals(gridIndexBefore, fragment.getCurrentSelectedIndex())
        assertNotEquals("Slot 1", editText.getTextContent())
        assertTrue(dialogVisible())
        verify(exactly = 0) { manager.saveToSlot(any(), any()) }
    }

    // ========== Overwrite dialog (occupied slot) ==========

    @Test
    fun `slot ocupado abre a confirmacao de sobrescrita com confirmar selecionado`() {
        mockedSaveStateManager()
        mockedViewModel()

        fragment.onSlotConfirmed(occupiedSlot(3))

        assertTrue(dialogVisible())
        assertEquals(
                FontUtils.getCapitalizedString(activity, R.string.overwrite_dialog_title),
                rootView().findViewById<TextView>(R.id.dialog_title).text.toString()
        )
        assertEquals(RetroCardView.State.SELECTED, confirmButton().getState())
        assertEquals(RetroCardView.State.NORMAL, cancelButton().getState())
    }

    @Test
    fun `navegacao no dialogo de sobrescrita alterna entre os botoes sem sair dos limites`() {
        mockedSaveStateManager()
        mockedViewModel()
        fragment.onSlotConfirmed(occupiedSlot(3))
        val gridIndexBefore = fragment.getCurrentSelectedIndex()

        fragment.onNavigateDown()
        fragment.onNavigateDown() // already on the last button: stays there
        assertEquals(RetroCardView.State.NORMAL, confirmButton().getState())
        assertEquals(RetroCardView.State.SELECTED, cancelButton().getState())
        assertEquals(View.VISIBLE, rootView().findViewById<TextView>(R.id.cancel_button_arrow).visibility)

        fragment.onNavigateUp()
        fragment.onNavigateUp() // already on the first button: stays there
        assertEquals(RetroCardView.State.SELECTED, confirmButton().getState())
        assertEquals(RetroCardView.State.NORMAL, cancelButton().getState())

        assertEquals(gridIndexBefore, fragment.getCurrentSelectedIndex())
    }

    @Test
    fun `confirmar sobrescrita pelo gamepad salva no slot`() {
        val manager = mockedSaveStateManager()
        mockedViewModel()
        fragment.onSlotConfirmed(occupiedSlot(7))

        fragment.onConfirm() // confirm is selected by default

        val payload = slot<SaveSlotPayload>()
        verify(exactly = 1) { manager.saveToSlot(7, capture(payload)) }
        assertEquals("Slot 7", payload.captured.name)
        verify(exactly = 1) { manager.getAllSlots() } // grid refreshed
        assertTrue(exitEnabled())
    }

    @Test
    fun `cancelar sobrescrita fecha o dialogo sem salvar`() {
        val manager = mockedSaveStateManager()
        mockedViewModel()
        fragment.onSlotConfirmed(occupiedSlot(7))

        fragment.onNavigateDown()
        fragment.onConfirm() // cancel

        assertFalse(dialogVisible())
        assertNull(rootView().findViewById(R.id.dialog_confirm_button))
        verify(exactly = 0) { manager.saveToSlot(any(), any()) }
        assertFalse(exitEnabled())
    }

    @Test
    fun `onBack e onBackConfirmed com dialogo visivel apenas fecham o dialogo`() {
        val manager = mockedSaveStateManager()
        mockedViewModel()

        fragment.onSlotConfirmed(occupiedSlot(2))
        assertTrue(fragment.onBack())
        assertFalse(dialogVisible())

        fragment.onSlotConfirmed(occupiedSlot(2))
        fragment.onBackConfirmed()
        assertFalse(dialogVisible())

        verify(exactly = 0) { navigationController.navigateBack() }
        verify(exactly = 0) { manager.saveToSlot(any(), any()) }
    }

    // ========== performSave failure paths ==========

    @Test
    fun `sem retroView o save mostra erro e o EXIT continua desabilitado`() {
        val manager = mockedSaveStateManager()
        mockedViewModel(withRetroView = false)

        completeSave(1)

        verify(exactly = 0) { manager.saveToSlot(any(), any()) }
        assertEquals(expectedToast(R.string.save_error), ShadowToast.getTextOfLatestToast())
        assertFalse(exitEnabled())
    }

    @Test
    fun `falha do SaveStateManager mostra erro e o EXIT continua desabilitado`() {
        val manager = mockedSaveStateManager(saveResult = false)
        mockedViewModel()

        completeSave(8)

        verify(exactly = 1) { manager.saveToSlot(8, any()) }
        assertNull(SessionSlotTracker.getInstance().getLastUsedSlot())
        assertEquals(expectedToast(R.string.save_error), ShadowToast.getTextOfLatestToast())
        assertFalse(exitEnabled())
    }

    @Test
    fun `excecao ao serializar o estado mostra erro e o EXIT continua desabilitado`() {
        val manager = mockedSaveStateManager()
        mockedViewModel { throw NullPointerException("native state unavailable") }

        completeSave(9)

        verify(exactly = 0) { manager.saveToSlot(any(), any()) }
        assertEquals(expectedToast(R.string.save_error), ShadowToast.getTextOfLatestToast())
        assertFalse(exitEnabled())
    }

    // ========== Lifecycle ==========

    @Test
    fun `remover o fragment com dialogo aberto limpa o dialogo sem lancar excecao`() {
        mockedSaveStateManager()
        mockedViewModel()
        fragment.onSlotConfirmed(SaveSlotData.empty(1))

        activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()

        assertFalse(dialogVisible())
        assertNull(privateField("retroKeyboard"))
        assertFalse(fragment.isAdded)
    }
}
