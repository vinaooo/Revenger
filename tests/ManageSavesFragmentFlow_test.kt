package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.models.SaveSlotPayload
import com.vinaooo.revenger.utils.FontUtils
import io.mockk.clearMocks
import io.mockk.verify
import java.io.File
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
 * The Manage Saves flow driven the way the user drives it: confirm a slot, move through the
 * operations dialog with the gamepad, pick a target slot, back out. Uses the real
 * SaveStateManager on Robolectric's files dir and a mocked ViewModel ([MenuFragmentHost]).
 * [ManageSavesFragment_test] covers the SaveSlotOperationRunner wiring.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ManageSavesFragmentFlow_test {

    private lateinit var host: MenuFragmentHost<ManageSavesFragment>
    private lateinit var savesDir: File
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val manager get() = SaveStateManager.getInstance(context)

    @Before
    fun setUp() {
        SaveStateManager.clearInstance()
        savesDir = File(context.filesDir, "saves").apply { deleteRecursively() }
        manager.saveToSlot(1, SaveSlotPayload(stateBytes = byteArrayOf(1, 2), name = "First", romName = "rom"))
        manager.saveToSlot(2, SaveSlotPayload(stateBytes = byteArrayOf(3, 4), name = "Second", romName = "rom"))
        host = MenuFragmentHost(ManageSavesFragment.newInstance())
    }

    @After
    fun tearDown() {
        host.destroy()
        savesDir.deleteRecursively()
        SaveStateManager.clearInstance()
    }

    private val fragment get() = host.fragment

    private fun slot(number: Int): SaveSlotData = manager.getSlot(number)

    private fun find(id: Int): View? = fragment.requireView().findViewById(id)

    private fun toast(resId: Int) = FontUtils.getCapitalizedString(context, resId)

    private fun isSelected(id: Int) = (find(id) as? RetroCardView)?.getState() == RetroCardView.State.SELECTED

    /** Opens the operations dialog for [number] and moves the gamepad cursor to [button]. */
    private fun openOperationsAndGoTo(number: Int, button: Int) {
        fragment.onSlotConfirmed(slot(number))
        repeat(button) { fragment.onNavigateDown() }
    }

    @Test
    fun `slot vazio so avisa e nao abre o dialogo`() {
        fragment.onSlotConfirmed(slot(5))

        assertEquals(toast(R.string.slot_is_empty), ShadowToast.getTextOfLatestToast())
        assertNull(find(R.id.operation_rename))
    }

    @Test
    fun `slot ocupado abre o dialogo de operacoes com Rename selecionado`() {
        fragment.onSlotConfirmed(slot(1))

        assertNotNull(find(R.id.operation_rename))
        assertTrue(isSelected(R.id.operation_rename))
    }

    @Test
    fun `gamepad percorre os botoes do dialogo e para nas pontas`() {
        fragment.onSlotConfirmed(slot(1))

        fragment.onNavigateUp() // already at the first button
        assertTrue(isSelected(R.id.operation_rename))

        repeat(6) { fragment.onNavigateDown() } // 5 buttons: stops at Cancel
        assertTrue(isSelected(R.id.operation_cancel))
        assertFalse(isSelected(R.id.operation_rename))

        fragment.onNavigateUp()
        assertTrue(isSelected(R.id.operation_delete))
    }

    @Test
    fun `confirmar Cancel fecha o dialogo`() {
        openOperationsAndGoTo(1, button = 4)

        fragment.onConfirm()

        assertNull(find(R.id.operation_rename))
    }

    @Test
    fun `back com dialogo aberto fecha so o dialogo`() {
        fragment.onSlotConfirmed(slot(1))

        assertTrue(fragment.onBack())

        assertNull(find(R.id.operation_rename))
        verify(exactly = 0) { host.navigationController.navigateBack() }
    }

    @Test
    fun `Copy pede o slot destino e copia para um slot vazio`() {
        openOperationsAndGoTo(1, button = 1)
        fragment.onConfirm()
        assertEquals(fragment.getString(R.string.select_slot_to_copy), ShadowToast.getTextOfLatestToast())

        fragment.onSlotConfirmed(slot(3))

        assertFalse(slot(3).isEmpty)
        assertFalse(slot(1).isEmpty)
    }

    @Test
    fun `Move para um slot vazio move o save`() {
        openOperationsAndGoTo(2, button = 2)
        fragment.onConfirm()
        assertEquals(fragment.getString(R.string.select_slot_to_move), ShadowToast.getTextOfLatestToast())

        fragment.onSlotConfirmed(slot(4))

        assertFalse(slot(4).isEmpty)
        assertTrue(slot(2).isEmpty)
    }

    @Test
    fun `destino igual a origem ou ocupado e recusado e a selecao continua`() {
        openOperationsAndGoTo(1, button = 1)
        fragment.onConfirm()

        fragment.onSlotConfirmed(slot(1))
        assertEquals(toast(R.string.same_slot_error), ShadowToast.getTextOfLatestToast())

        fragment.onSlotConfirmed(slot(2))
        assertEquals(toast(R.string.slot_occupied_error), ShadowToast.getTextOfLatestToast())
        assertEquals("Second", slot(2).name)

        fragment.onSlotConfirmed(slot(6)) // still selecting: an empty target works
        assertFalse(slot(6).isEmpty)
    }

    @Test
    fun `back durante a escolha do destino cancela a operacao`() {
        openOperationsAndGoTo(1, button = 1)
        fragment.onConfirm()

        assertTrue(fragment.onBack())
        assertEquals(toast(R.string.operation_cancelled), ShadowToast.getTextOfLatestToast())

        fragment.onSlotConfirmed(slot(7)) // no operation pending: empty slot just warns
        assertTrue(slot(7).isEmpty)
        assertEquals(toast(R.string.slot_is_empty), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `onBackConfirmed cancela a escolha do destino e depois volta pelo NavigationController`() {
        openOperationsAndGoTo(1, button = 2)
        fragment.onConfirm()

        fragment.onBackConfirmed()
        assertEquals(toast(R.string.operation_cancelled), ShadowToast.getTextOfLatestToast())
        verify(exactly = 0) { host.navigationController.navigateBack() }

        fragment.onBackConfirmed()
        verify { host.navigationController.navigateBack() }
    }

    @Test
    fun `onBackConfirmed com dialogo aberto fecha o dialogo`() {
        fragment.onSlotConfirmed(slot(1))

        fragment.onBackConfirmed()

        assertNull(find(R.id.operation_rename))
        verify(exactly = 0) { host.navigationController.navigateBack() }
    }

    @Test
    fun `back sem dialogo nem operacao devolve false`() {
        assertFalse(fragment.onBack())
    }

    @Test
    fun `Delete abre a confirmacao com Cancel selecionado e o gamepad alterna os botoes`() {
        openOperationsAndGoTo(1, button = 3)
        fragment.onConfirm()

        assertNotNull(find(R.id.dialog_confirm_button))
        assertTrue(isSelected(R.id.dialog_cancel_button))

        fragment.onNavigateUp()
        assertTrue(isSelected(R.id.dialog_confirm_button))
        fragment.onConfirm()

        assertTrue(slot(1).isEmpty)
    }

    @Test
    fun `Rename abre o teclado e o gamepad navega por ele`() {
        openOperationsAndGoTo(1, button = 0)
        fragment.onConfirm()
        assertNotNull(find(R.id.rename_edit_text))
        clearMocks(host.navigationController, answers = false)

        assertTrue(fragment.onNavigateRight())
        assertTrue(fragment.onNavigateLeft())
        fragment.onNavigateDown()
        fragment.onNavigateUp()
        fragment.onConfirm() // types the key under the cursor

        assertNotNull(find(R.id.rename_edit_text))
        assertEquals("First", slot(1).name)
    }

    @Test
    fun `cancelar o Rename fecha o teclado sem renomear`() {
        openOperationsAndGoTo(1, button = 0)
        fragment.onConfirm()
        assertNotNull(find(R.id.rename_edit_text))

        fragment.dialogs.keyboard?.let { keyboard ->
            repeat(4) { keyboard.navigateDown() } // row 4 starts with CANCEL
            keyboard.pressCurrentKey()
        }

        assertFalse(fragment.dialogs.isVisible)
        assertEquals("First", slot(1).name)
    }

    @Test
    fun `sem dialogo as setas movem a selecao do grid`() {
        val start = fragment.getCurrentSelectedIndex()

        assertTrue(fragment.onNavigateRight())
        val right = fragment.getCurrentSelectedIndex()
        assertTrue(fragment.onNavigateLeft())
        fragment.onNavigateDown()
        val down = fragment.getCurrentSelectedIndex()
        fragment.onNavigateUp()

        assertNotEquals(start, right)
        assertNotEquals(start, down)
        assertEquals(start, fragment.getCurrentSelectedIndex())
        assertFalse(fragment.dialogs.isVisible)
    }

    @Test
    fun `remover o fragment com um dialogo aberto nao falha`() {
        fragment.onSlotConfirmed(slot(1))

        host.activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()

        assertFalse(fragment.isAdded)
    }

    private fun title(): String = (find(R.id.grid_title) as android.widget.TextView).text.toString()

    /** Opens Copy for [number] (operations dialog, second button). */
    private fun startCopy(number: Int) {
        openOperationsAndGoTo(number, button = 1)
        fragment.onConfirm()
    }

    @Test
    fun `escolher o destino troca o titulo e cancelar pelo back o devolve`() {
        startCopy(1)
        assertEquals(fragment.getString(R.string.select_target_copy), title())

        fragment.onBack()

        assertEquals(fragment.getString(R.string.manage_saves_title), title())
    }

    @Test
    fun `cancelar a escolha do destino pelo onBackConfirmed devolve o titulo`() {
        startCopy(1)

        fragment.onBackConfirmed()

        assertEquals(fragment.getString(R.string.manage_saves_title), title())
    }

    @Test
    fun `depois de copiar o titulo volta e o proximo slot nao e mais um destino`() {
        startCopy(1)
        fragment.onSlotConfirmed(slot(3))
        assertEquals(fragment.getString(R.string.manage_saves_title), title())

        fragment.onSlotConfirmed(slot(4))

        assertTrue(slot(4).isEmpty)
        assertEquals(toast(R.string.slot_is_empty), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `os botoes de operacao usam a fonte do menu e nao tem fundo colorido`() {
        fragment.onSlotConfirmed(slot(1))

        assertSame(FontUtils.getSelectedTypeface(context), (find(R.id.rename_text) as android.widget.TextView).typeface)
        for (id in listOf(R.id.operation_rename, R.id.operation_copy, R.id.operation_move, R.id.operation_delete, R.id.operation_cancel)) {
            assertFalse((find(id) as RetroCardView).getUseBackgroundColor())
        }
    }

    @Test
    fun `no Rename as setas andam no teclado na direcao certa e nao no grid`() {
        fragment.setSelectedIndex(1)
        openOperationsAndGoTo(1, button = 0)
        fragment.onConfirm()

        fragment.onNavigateRight()
        assertTrue(find(R.id.key_2)!!.isSelected)

        fragment.onNavigateLeft()
        assertTrue(find(R.id.key_1)!!.isSelected)
        assertFalse(find(R.id.key_2)!!.isSelected)
        assertEquals(1, fragment.getCurrentSelectedIndex())
    }

    @Test
    fun `remover o fragment fecha o dialogo aberto`() {
        fragment.onSlotConfirmed(slot(1))

        host.activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()

        assertFalse(fragment.dialogs.isVisible)
    }
}
