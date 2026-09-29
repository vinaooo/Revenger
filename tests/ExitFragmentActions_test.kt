package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker
import com.vinaooo.revenger.models.SaveSlotPayload
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.retromenu3.navigation.MenuType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What each Exit menu entry does, with a mocked ViewModel ([MenuFragmentHost]). The host isn't a
 * GameActivity, so the shutdown animation (and the process kill after it) is never reached: the
 * tests stop at the calls the fragment makes before it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExitFragmentActions_test {

    private lateinit var host: MenuFragmentHost<ExitFragment>
    private lateinit var savesDir: File
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        SaveStateManager.clearInstance()
        SessionSlotTracker.clearInstance()
        savesDir = File(context.filesDir, "saves").apply { deleteRecursively() }
        host = MenuFragmentHost(ExitFragment.newInstance())
    }

    @After
    fun tearDown() {
        host.destroy()
        savesDir.deleteRecursively()
        SaveStateManager.clearInstance()
        SessionSlotTracker.clearInstance()
    }

    private val fragment get() = host.fragment

    private fun confirm(index: Int) {
        fragment.setSelectedIndex(index)
        fragment.onConfirm()
    }

    private fun arrow(id: Int) = fragment.requireView().findViewById<TextView>(id)

    @Test
    fun `registra o fragment no NavigationController com 3 itens`() {
        verify { host.navigationController.registerFragment(fragment, 3) }
    }

    @Test
    fun `toque seleciona o item na hora e ativa depois do atraso`() {
        fragment.requireView().findViewById<View>(R.id.exit_menu_option_c).performClick()

        verify { host.navigationController.selectItem(2) }
        verify(exactly = 0) { host.navigationController.activateItem() }

        host.advance(MenuFragmentBase.TOUCH_ACTIVATION_DELAY_MS)
        verify { host.navigationController.activateItem() }
    }

    @Test
    fun `navegacao circula pelos 3 itens e move a seta de selecao`() {
        fragment.onNavigateUp()
        assertEquals(2, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, arrow(R.id.selection_arrow_option_c).visibility)
        assertEquals(View.GONE, arrow(R.id.selection_arrow_option_a).visibility)

        fragment.onNavigateDown()
        assertEquals(0, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, arrow(R.id.selection_arrow_option_a).visibility)
        assertEquals(View.GONE, arrow(R.id.selection_arrow_option_c).visibility)
    }

    @Test
    fun `Save and Exit sem slot na sessao abre a grade de slots de saida`() {
        confirm(0)

        verify { host.navigationController.navigateToSubmenu(MenuType.EXIT_SAVE_SLOTS) }
        verify(exactly = 0) { host.viewModel.dismissRetroMenu3(any()) }
    }

    @Test
    fun `Save and Exit com slot na sessao salva nesse slot e fecha o menu`() {
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view.serializeState() } returns byteArrayOf(1, 2, 3)
        every { host.viewModel.retroView } returns retroView
        SessionSlotTracker.getInstance().recordLoad(4)

        confirm(0)

        val slot = SaveStateManager.getInstance(context).getSlot(4)
        assertFalse(slot.isEmpty)
        assertEquals("Slot 4", slot.name)
        assertEquals(4, SessionSlotTracker.getInstance().getLastUsedSlot())
        assertEquals(SessionSlotTracker.OperationType.SAVE, SessionSlotTracker.getInstance().getLastOperationType())
        verify { host.viewModel.dismissRetroMenu3(any()) }
    }

    private fun givenEmulatorState() {
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view.serializeState() } returns byteArrayOf(1, 2, 3)
        every { host.viewModel.retroView } returns retroView
    }

    @Test
    fun `Save and Exit mantem o nome que o slot ja tinha`() {
        givenEmulatorState()
        val manager = SaveStateManager.getInstance(context)
        manager.saveToSlot(4, SaveSlotPayload(stateBytes = byteArrayOf(9), name = "Before the boss", romName = "rom"))
        SessionSlotTracker.getInstance().recordSave(4)

        confirm(0)

        val slot = manager.getSlot(4)
        assertEquals("Before the boss", slot.name)
        assertEquals(context.getString(R.string.name), slot.romName)
        assertArrayEquals(byteArrayOf(1, 2, 3), File(savesDir, "slot_4/state.bin").readBytes())
    }

    @Test
    fun `Save and Exit fecha o menu mesmo quando a gravacao falha`() {
        givenEmulatorState()
        // A directory where the state file goes makes the write fail.
        File(savesDir, "slot_5/state.bin").mkdirs()
        SessionSlotTracker.getInstance().recordLoad(5)

        confirm(0)

        verify { host.viewModel.dismissRetroMenu3(any()) }
        assertTrue(File(savesDir, "slot_5/state.bin").isDirectory)
        assertEquals(SessionSlotTracker.OperationType.LOAD, SessionSlotTracker.getInstance().getLastOperationType())
    }

    @Test
    fun `Save and Exit com slot mas sem RetroView cai no salvamento legado`() {
        SessionSlotTracker.getInstance().recordSave(2)

        confirm(0)

        verify { host.viewModel.dismissRetroMenu3(any()) }
        verify { host.viewModel.saveStateCentralized(any(), any()) }
        assertTrue(SaveStateManager.getInstance(context).getSlot(2).isEmpty)
    }

    @Test
    fun `Save and Exit fecha o menu mesmo quando a serializacao falha`() {
        val retroView = mockk<RetroView>(relaxed = true)
        every { retroView.view.serializeState() } throws NullPointerException("native result")
        every { host.viewModel.retroView } returns retroView
        SessionSlotTracker.getInstance().recordSave(3)

        confirm(0)

        assertTrue(SaveStateManager.getInstance(context).getSlot(3).isEmpty)
        verify { host.viewModel.dismissRetroMenu3(any()) }
    }

    @Test
    fun `Exit without Save fecha o menu sem salvar`() {
        confirm(1)

        verify { host.viewModel.dismissRetroMenu3(any()) }
        verify(exactly = 0) { host.viewModel.saveStateCentralized(any(), any()) }
    }

    @Test
    fun `Back volta pelo NavigationController`() {
        confirm(2)

        verify { host.navigationController.navigateBack() }
    }

    @Test
    fun `onBack devolve false para o processor tratar a volta`() {
        assertFalse(fragment.onBack())
    }

    @Test
    fun `dismissMenuPublic remove o fragment`() {
        fragment.dismissMenuPublic()
        host.idle()

        assertFalse(fragment.isAdded)
    }
}
