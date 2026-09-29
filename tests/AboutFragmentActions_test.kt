package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.TextView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.ui.retromenu3.navigation.MenuType
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What each About menu entry does, with a mocked ViewModel ([MenuFragmentHost]). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AboutFragmentActions_test {

    private lateinit var host: MenuFragmentHost<AboutFragment>

    @After
    fun tearDown() {
        host.destroy()
    }

    @Before
    fun setUp() {
        host = MenuFragmentHost(AboutFragment.newInstance())
    }

    private val fragment get() = host.fragment

    private fun confirm(index: Int) {
        fragment.setSelectedIndex(index)
        fragment.onConfirm()
    }

    private fun visibility(id: Int) = fragment.requireView().findViewById<View>(id).visibility

    @Test
    fun `registra o fragment no NavigationController com 2 itens`() {
        verify { host.navigationController.registerFragment(fragment, 2) }
    }

    @Test
    fun `mostra o projeto, a ROM e o core configurados com a capitalizacao configurada`() {
        val context = fragment.requireContext()
        val appConfig = RevengerApplication.appConfig
        fun capitalized(text: String) =
                TextView(context).apply { this.text = text }.also { FontUtils.applyTextCapitalization(context, it) }.text.toString()
        fun shown(id: Int) = fragment.requireView().findViewById<TextView>(id).text.toString()

        assertEquals(capitalized("${context.getString(R.string.about_project_name)} Revenger"), shown(R.id.project_name_info))
        assertEquals(capitalized("${context.getString(R.string.about_rom_name)} ${appConfig.getRomName()}"), shown(R.id.rom_name_info))
        assertEquals(capitalized("${context.getString(R.string.about_core_name)} ${appConfig.getCore()}"), shown(R.id.core_name_info))
        assertEquals(FontUtils.getCapitalizedString(context, R.string.core_variables_menu_title), shown(R.id.core_variables_title))
        assertEquals(FontUtils.getCapitalizedString(context, R.string.about_back), shown(R.id.back_title))
    }

    @Test
    fun `Core Variables abre o submenu de variaveis do core`() {
        confirm(0)

        verify { host.navigationController.navigateToSubmenu(MenuType.CORE_VARIABLES) }
    }

    @Test
    fun `Back volta pelo NavigationController`() {
        confirm(1)

        verify { host.navigationController.navigateBack() }
        verify(exactly = 0) { host.navigationController.navigateToSubmenu(any()) }
    }

    @Test
    fun `toque seleciona o item na hora e ativa depois do atraso`() {
        fragment.requireView().findViewById<View>(R.id.about_back).performClick()

        verify { host.navigationController.selectItem(1) }
        verify(exactly = 0) { host.navigationController.activateItem() }

        host.advance(MenuFragmentBase.TOUCH_ACTIVATION_DELAY_MS)
        verify { host.navigationController.activateItem() }
    }

    @Test
    fun `navegacao circula pelos 2 itens e move a seta de selecao`() {
        fragment.onNavigateUp()
        assertEquals(1, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, visibility(R.id.selection_arrow_back))
        assertEquals(View.GONE, visibility(R.id.selection_arrow_core_variables))

        fragment.onNavigateDown()
        assertEquals(0, fragment.getCurrentSelectedIndex())
        assertEquals(View.VISIBLE, visibility(R.id.selection_arrow_core_variables))
        assertEquals(View.GONE, visibility(R.id.selection_arrow_back))
    }

    @Test
    fun `onBack devolve false para o processor tratar a volta`() {
        assertFalse(fragment.onBack())
    }

    @Test
    fun `remover o fragment destroi a view sem falhar`() {
        host.activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()

        assertFalse(fragment.isAdded)
    }
}
