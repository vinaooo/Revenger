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

    // ========== performRename / performCopy / performMove / performDelete ==========
    //
    // These four private methods (see SaveStateGridFragment.saveStateManager, injected here via
    // reflection since the field is protected) share one if/else-toast/refresh shape. The tests
    // below mock saveStateManager to force each success/failure branch deterministically -
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

    private fun callPerformRename(slotNumber: Int, newName: String) {
        val method =
                ManageSavesFragment::class.java.getDeclaredMethod(
                        "performRename",
                        Int::class.javaPrimitiveType,
                        String::class.java
                )
        method.isAccessible = true
        method.invoke(fragment, slotNumber, newName)
    }

    private fun callPerformCopy(fromSlot: Int, toSlot: Int) {
        val method =
                ManageSavesFragment::class.java.getDeclaredMethod(
                        "performCopy",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType
                )
        method.isAccessible = true
        method.invoke(fragment, fromSlot, toSlot)
    }

    private fun callPerformMove(fromSlot: Int, toSlot: Int) {
        val method =
                ManageSavesFragment::class.java.getDeclaredMethod(
                        "performMove",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType
                )
        method.isAccessible = true
        method.invoke(fragment, fromSlot, toSlot)
    }

    private fun callPerformDelete(slotNumber: Int) {
        val method =
                ManageSavesFragment::class.java.getDeclaredMethod(
                        "performDelete",
                        Int::class.javaPrimitiveType
                )
        method.isAccessible = true
        method.invoke(fragment, slotNumber)
    }

    private fun expectedToast(resId: Int): String =
            FontUtils.getCapitalizedString(activity, resId)

    @Test
    fun `performRename com sucesso mostra toast de sucesso e atualiza grid`() {
        val manager = mockedSaveStateManager()
        every { manager.renameSlot(1, "New Name") } returns true
        injectSaveStateManager(manager)

        callPerformRename(1, "New Name")

        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.rename_success), ShadowToast.getTextOfLatestToast())
        verify(exactly = 1) { manager.getAllSlots() }
    }

    @Test
    fun `performRename com falha mostra toast de erro e nao atualiza grid`() {
        val manager = mockedSaveStateManager()
        every { manager.renameSlot(1, "New Name") } returns false
        injectSaveStateManager(manager)

        callPerformRename(1, "New Name")

        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.rename_error), ShadowToast.getTextOfLatestToast())
        verify(exactly = 0) { manager.getAllSlots() }
    }

    @Test
    fun `performDelete com sucesso mostra toast de sucesso e atualiza grid`() {
        val manager = mockedSaveStateManager()
        every { manager.deleteSlot(3) } returns true
        injectSaveStateManager(manager)

        callPerformDelete(3)

        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.delete_success), ShadowToast.getTextOfLatestToast())
        verify(exactly = 1) { manager.getAllSlots() }
    }

    @Test
    fun `performDelete com falha mostra toast de erro e nao atualiza grid`() {
        val manager = mockedSaveStateManager()
        every { manager.deleteSlot(3) } returns false
        injectSaveStateManager(manager)

        callPerformDelete(3)

        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.delete_error), ShadowToast.getTextOfLatestToast())
        verify(exactly = 0) { manager.getAllSlots() }
    }

    @Test
    fun `performCopy com sucesso mostra toast de sucesso e atualiza grid`() {
        val manager = mockedSaveStateManager()
        every { manager.copySlot(1, 2) } returns true
        injectSaveStateManager(manager)

        callPerformCopy(1, 2)

        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.copy_success), ShadowToast.getTextOfLatestToast())
        verify(exactly = 1) { manager.getAllSlots() }
    }

    @Test
    fun `performCopy com falha mostra toast de erro e nao atualiza grid`() {
        val manager = mockedSaveStateManager()
        every { manager.copySlot(1, 2) } returns false
        injectSaveStateManager(manager)

        callPerformCopy(1, 2)

        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.copy_error), ShadowToast.getTextOfLatestToast())
        verify(exactly = 0) { manager.getAllSlots() }
    }

    @Test
    fun `performMove com sucesso mostra toast de sucesso e atualiza grid`() {
        val manager = mockedSaveStateManager()
        every { manager.moveSlot(1, 2) } returns true
        injectSaveStateManager(manager)

        callPerformMove(1, 2)

        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.move_success), ShadowToast.getTextOfLatestToast())
        verify(exactly = 1) { manager.getAllSlots() }
    }

    @Test
    fun `performMove com falha mostra toast de erro e nao atualiza grid`() {
        val manager = mockedSaveStateManager()
        every { manager.moveSlot(1, 2) } returns false
        injectSaveStateManager(manager)

        callPerformMove(1, 2)

        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.move_error), ShadowToast.getTextOfLatestToast())
        verify(exactly = 0) { manager.getAllSlots() }
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
