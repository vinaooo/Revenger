package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.ui.retromenu3.callbacks.ManageSavesListener
import com.vinaooo.revenger.utils.FontUtils
import io.mockk.every
import io.mockk.mockk
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
 * Robolectric tests for ManageSavesFragment, following the pattern proven by
 * MenuIntegration_test. This is the most complex grid fragment (rename/copy/move/delete
 * dialogs), so - like the other grid fragments - onSlotConfirmed()'s dialog flow is
 * intentionally not exercised here; tests stick to structure and safe no-dialog navigation
 * against the default empty-slots state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ManageSavesFragment_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: ManageSavesFragment
    private lateinit var savesDir: File

    @Before
    fun setup() {
        SaveStateManager.clearInstance()
        val context: Context = ApplicationProvider.getApplicationContext()
        savesDir = File(context.filesDir, "saves")
        savesDir.deleteRecursively()

        activity =
                Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        fragment = ManageSavesFragment.newInstance()
        fragment.setListener(mockk<ManageSavesListener>(relaxed = true))
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "manage_saves")
                .commitNow()
    }

    @After
    fun cleanup() {
        savesDir.deleteRecursively()
        SaveStateManager.clearInstance()
    }

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
    fun `getTitleResId retorna manage_saves_title`() {
        assertEquals(com.vinaooo.revenger.R.string.manage_saves_title, fragment.getTitleResId())
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

    // ========== ManageSavesFragment wiring to SaveSlotOperationRunner ==========
    //
    // performRename/performCopy/performMove/performDelete were extracted into
    // SaveSlotOperationRunner (see SaveSlotOperationRunner_test.kt for the success/failure
    // outcome-reporting coverage). The tests below instead prove the three call sites in this
    // fragment (rename-dialog confirm, copy/move target selection, delete-dialog confirm) are
    // still wired to construct the runner correctly and pass it the right slot numbers -
    // deleteSlot() in particular can't be forced to fail through the real filesystem-backed
    // manager, since Kotlin's File.deleteRecursively() succeeds even on a missing directory.

    private fun mockedSaveStateManager(): SaveStateManager {
        val manager = mockk<SaveStateManager>(relaxed = true)
        val emptySlots = (1..SaveStateManager.TOTAL_SLOTS).map { SaveSlotData.empty(it) }
        every { manager.getAllSlots() } returns emptySlots
        every { manager.getSlot(any()) } answers { SaveSlotData.empty(firstArg()) }
        return manager
    }

    private fun injectSaveStateManager(manager: SaveStateManager) {
        val field = SaveStateGridFragment::class.java.getDeclaredField("saveStateManager")
        field.isAccessible = true
        field.set(fragment, manager)
    }

    private fun expectedToast(resId: Int): String =
            FontUtils.getCapitalizedString(activity, resId)

    private fun callStartTargetSlotSelection(slot: SaveSlotData, operation: ManageSavesFragment.Operation) {
        val method =
                ManageSavesFragment::class.java.getDeclaredMethod(
                        "startTargetSlotSelection",
                        SaveSlotData::class.java,
                        ManageSavesFragment.Operation::class.java
                )
        method.isAccessible = true
        method.invoke(fragment, slot, operation)
    }

    private fun callHandleTargetSlotSelected(targetSlot: SaveSlotData) {
        val method =
                ManageSavesFragment::class.java.getDeclaredMethod(
                        "handleTargetSlotSelected",
                        SaveSlotData::class.java
                )
        method.isAccessible = true
        method.invoke(fragment, targetSlot)
    }

    @Test
    fun `confirmar rename via retroKeyboard aciona SaveSlotOperationRunner e atualiza grid`() {
        val manager = mockedSaveStateManager()
        every { manager.renameSlot(1, "New Name") } returns true
        injectSaveStateManager(manager)
        callShowOperationsMenu(occupiedSlot(1))
        val renameButton = fragment.requireView().findViewById<RetroCardView>(R.id.operation_rename)
        renameButton.performClick()

        val retroKeyboardField = ManageSavesFragment::class.java.getDeclaredField("retroKeyboard")
        retroKeyboardField.isAccessible = true
        val retroKeyboard = retroKeyboardField.get(fragment)
        val onConfirmField = retroKeyboard.javaClass.getDeclaredField("onConfirm")
        onConfirmField.isAccessible = true
        @Suppress("UNCHECKED_CAST") val onConfirm = onConfirmField.get(retroKeyboard) as (String) -> Unit

        onConfirm("New Name")

        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.rename_success), ShadowToast.getTextOfLatestToast())
        verify(exactly = 1) { manager.getAllSlots() }
    }

    @Test
    fun `abrir rename preenche o campo de texto com o nome atual do slot`() {
        injectSaveStateManager(mockedSaveStateManager())
        callShowOperationsMenu(occupiedSlot(2))
        val renameButton = fragment.requireView().findViewById<RetroCardView>(R.id.operation_rename)
        renameButton.performClick()

        val editText = fragment.requireView().findViewById<RetroEditText>(R.id.rename_edit_text)

        assertEquals("Save 2", editText.getTextContent())
    }

    @Test
    fun `confirmar delete via dialog aciona SaveSlotOperationRunner e atualiza grid`() {
        val manager = mockedSaveStateManager()
        every { manager.deleteSlot(3) } returns true
        injectSaveStateManager(manager)
        callShowDeleteConfirmation(occupiedSlot(3))

        val confirmButton =
                fragment.requireView().findViewById<RetroCardView>(R.id.dialog_confirm_button)
        confirmButton.performClick()

        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.delete_success), ShadowToast.getTextOfLatestToast())
        verify(exactly = 1) { manager.getAllSlots() }
    }

    @Test
    fun `selecionar slot destino para copy aciona SaveSlotOperationRunner e atualiza grid`() {
        val manager = mockedSaveStateManager()
        every { manager.copySlot(1, 2) } returns true
        injectSaveStateManager(manager)

        callStartTargetSlotSelection(occupiedSlot(1), ManageSavesFragment.Operation.COPY)
        callHandleTargetSlotSelected(SaveSlotData.empty(2))

        // startTargetSlotSelection() itself shows a "select target slot" toast first, so only
        // the LATEST toast (from SaveSlotOperationRunner.copy()'s outcome) is asserted here.
        assertEquals(expectedToast(R.string.copy_success), ShadowToast.getTextOfLatestToast())
        verify(exactly = 1) { manager.getAllSlots() }
    }

    @Test
    fun `selecionar slot destino para move aciona SaveSlotOperationRunner e atualiza grid`() {
        val manager = mockedSaveStateManager()
        every { manager.moveSlot(1, 2) } returns true
        injectSaveStateManager(manager)

        callStartTargetSlotSelection(occupiedSlot(1), ManageSavesFragment.Operation.MOVE)
        callHandleTargetSlotSelected(SaveSlotData.empty(2))

        // startTargetSlotSelection() itself shows a "select target slot" toast first, so only
        // the LATEST toast (from SaveSlotOperationRunner.move()'s outcome) is asserted here.
        assertEquals(expectedToast(R.string.move_success), ShadowToast.getTextOfLatestToast())
        verify(exactly = 1) { manager.getAllSlots() }
    }

    // ========== updateDialogSelection ==========
    //
    // updateDialogSelection() maps each dialogButtons entry to its own arrow/text view id via a
    // single when(button.id) table (previously two verbatim tables). These tests drive the two
    // dialogs that populate dialogButtons (the operations menu and the delete confirmation) and
    // confirm every button id in the table still resolves to its own selected/unselected visuals.

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

    private fun callShowOperationsMenu(slot: SaveSlotData) {
        val method =
                ManageSavesFragment::class.java.getDeclaredMethod(
                        "showOperationsMenu",
                        SaveSlotData::class.java
                )
        method.isAccessible = true
        method.invoke(fragment, slot)
    }

    private fun callShowDeleteConfirmation(slot: SaveSlotData) {
        val method =
                ManageSavesFragment::class.java.getDeclaredMethod(
                        "showDeleteConfirmation",
                        SaveSlotData::class.java
                )
        method.isAccessible = true
        method.invoke(fragment, slot)
    }

    private fun setDialogSelectedIndex(index: Int) {
        val field = ManageSavesFragment::class.java.getDeclaredField("dialogSelectedIndex")
        field.isAccessible = true
        field.set(fragment, index)
    }

    private fun callUpdateDialogSelection() {
        val method = ManageSavesFragment::class.java.getDeclaredMethod("updateDialogSelection")
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    fun `updateDialogSelection marca o botao selecionado e sua seta no menu de operacoes`() {
        val manager = mockedSaveStateManager()
        injectSaveStateManager(manager)
        callShowOperationsMenu(occupiedSlot(1))

        val renameButton = fragment.requireView().findViewById<RetroCardView>(R.id.operation_rename)
        val renameArrow = fragment.requireView().findViewById<TextView>(R.id.rename_arrow)
        val deleteButton = fragment.requireView().findViewById<RetroCardView>(R.id.operation_delete)
        val deleteArrow = fragment.requireView().findViewById<TextView>(R.id.delete_arrow)

        // Default selection is index 0 (rename)
        assertEquals(RetroCardView.State.SELECTED, renameButton.getState())
        assertEquals(View.VISIBLE, renameArrow.visibility)
        assertEquals(RetroCardView.State.NORMAL, deleteButton.getState())
        assertEquals(View.GONE, deleteArrow.visibility)

        setDialogSelectedIndex(3) // delete
        callUpdateDialogSelection()

        assertEquals(RetroCardView.State.NORMAL, renameButton.getState())
        assertEquals(View.GONE, renameArrow.visibility)
        assertEquals(RetroCardView.State.SELECTED, deleteButton.getState())
        assertEquals(View.VISIBLE, deleteArrow.visibility)
    }

    @Test
    fun `updateDialogSelection marca o botao selecionado e sua seta no dialogo de confirmacao de delete`() {
        val manager = mockedSaveStateManager()
        injectSaveStateManager(manager)
        callShowDeleteConfirmation(occupiedSlot(1))

        val confirmButton =
                fragment.requireView().findViewById<RetroCardView>(R.id.dialog_confirm_button)
        val confirmArrow = fragment.requireView().findViewById<TextView>(R.id.confirm_button_arrow)
        val cancelButton =
                fragment.requireView().findViewById<RetroCardView>(R.id.dialog_cancel_button)
        val cancelArrow = fragment.requireView().findViewById<TextView>(R.id.cancel_button_arrow)

        // Delete confirmation defaults to cancel (index 1) selected
        assertEquals(RetroCardView.State.NORMAL, confirmButton.getState())
        assertEquals(View.GONE, confirmArrow.visibility)
        assertEquals(RetroCardView.State.SELECTED, cancelButton.getState())
        assertEquals(View.VISIBLE, cancelArrow.visibility)

        setDialogSelectedIndex(0) // confirm
        callUpdateDialogSelection()

        assertEquals(RetroCardView.State.SELECTED, confirmButton.getState())
        assertEquals(View.VISIBLE, confirmArrow.visibility)
        assertEquals(RetroCardView.State.NORMAL, cancelButton.getState())
        assertEquals(View.GONE, cancelArrow.visibility)
    }
}
