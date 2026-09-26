package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.TextView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.navigation.MenuType
import io.mockk.clearMocks
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What each Progress menu entry does, with a mocked ViewModel ([MenuFragmentHost]). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProgressFragmentActions_test {

    private lateinit var host: MenuFragmentHost<ProgressFragment>

    @After
    fun tearDown() {
        host.destroy()
    }

    @Before
    fun setUp() {
        host = MenuFragmentHost(ProgressFragment.newInstance())
    }

    private val fragment get() = host.fragment

    private fun confirm(index: Int) {
        fragment.setSelectedIndex(index)
        fragment.onConfirm()
    }

    private fun arrow(id: Int) = fragment.requireView().findViewById<TextView>(id)

    @Test
    fun `cada item abre o submenu certo pelo NavigationController`() {
        confirm(0)
        verify { host.navigationController.navigateToSubmenu(MenuType.LOAD_SLOTS) }
        confirm(1)
        verify { host.navigationController.navigateToSubmenu(MenuType.SAVE_SLOTS) }
        confirm(2)
        verify { host.navigationController.navigateToSubmenu(MenuType.MANAGE_SAVES) }
    }

    @Test
    fun `Back volta pelo NavigationController`() {
        confirm(3)

        verify { host.navigationController.navigateBack() }
        verify(exactly = 0) { host.navigationController.navigateToSubmenu(any()) }
    }

    @Test
    fun `toque seleciona o item na hora e ativa depois do atraso`() {
        fragment.requireView().findViewById<View>(R.id.progress_manage_saves).performClick()

        verify { host.navigationController.selectItem(2) }
        verify(exactly = 0) { host.navigationController.activateItem() }

        host.advance(MenuFragmentBase.TOUCH_ACTIVATION_DELAY_MS)
        verify { host.navigationController.activateItem() }
    }

    @Test
    fun `navegacao circula pelos 4 itens e move a seta de selecao`() {
        fragment.onNavigateUp()
        assertEquals(3, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, arrow(R.id.selection_arrow_back).visibility)
        assertEquals(View.GONE, arrow(R.id.selection_arrow_load_state).visibility)

        fragment.onNavigateDown()
        fragment.onNavigateDown()
        assertEquals(1, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, arrow(R.id.selection_arrow_save_state).visibility)
        assertEquals(View.GONE, arrow(R.id.selection_arrow_back).visibility)
    }

    @Test
    fun `ao voltar de um submenu restaura a selecao de antes`() {
        confirm(2) // opens MANAGE_SAVES, remembering index 2
        fragment.setSelectedIndex(0)
        clearMocks(host.navigationController, answers = false)

        host.controller.pause().resume()

        assertEquals(2, fragment.getCurrentSelectedIndex())
        verify { host.navigationController.registerFragment(fragment, 4) }
        verify { host.navigationController.selectItem(2) }
    }

    @Test
    fun `onBack devolve false para o processor tratar a volta`() {
        assertFalse(fragment.onBack())
    }

    @Test
    fun `onMenuItemSelected roteia cada acao pelo clique do item`() {
        val items = fragment.getMenuItems()

        fragment.onMenuItemSelected(items[3]) // BACK -> click on the back card

        verify { host.navigationController.selectItem(3) }
    }

    @Test
    fun `dismissMenuPublic remove o fragment e o destroy limpa o log de teclas do combo`() {
        fragment.dismissMenuPublic()
        host.idle()

        assertFalse(fragment.isAdded)
        verify { host.viewModel.clearControllerKeyLog() }
    }
}
