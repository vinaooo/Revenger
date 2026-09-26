package com.vinaooo.revenger.ui.retromenu3.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.CoreVariablesFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.ExitSaveGridFragment
import com.vinaooo.revenger.ui.retromenu3.LoadSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.ManageSavesFragment
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SaveSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Fragment transactions the real-activity tests ([SimpleMenuShower_test],
 * [MenuFragmentPresenter_test]) can't reach: the menus whose fragments need the game's ViewModel
 * to attach (save/load grids, manage saves, core variables), PROGRESS clearing grid submenus off
 * the back stack, and [FragmentNavigationAdapter.hideMenu]'s error handling.
 *
 * The FragmentManager is a mock recording the transactions, so the menu fragments are only
 * instantiated, never attached: these tests pin which Fragment class and tag each [MenuType] is
 * committed with, independent of the fragments' views or ViewModels.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuTransactions_test {

    private lateinit var fragmentManager: FragmentManager
    private lateinit var transaction: FragmentTransaction
    private val shown = slot<Fragment>()

    @Before
    fun setUp() {
        fragmentManager = mockk(relaxed = true)
        transaction = mockk(relaxed = true)
        every { fragmentManager.beginTransaction() } returns transaction
        every { transaction.replace(any<Int>(), capture(shown), any<String>()) } returns transaction
        every { transaction.add(any<Int>(), capture(shown), any<String>()) } returns transaction
        every { transaction.addToBackStack(any()) } returns transaction
        every { transaction.remove(any()) } returns transaction
    }

    // --- SimpleMenuShower ---

    private val simpleMenus =
            mapOf(
                    MenuType.SETTINGS to (SettingsMenuFragment::class.java to "SettingsMenuFragment"),
                    MenuType.ABOUT to (AboutFragment::class.java to "AboutFragment"),
                    MenuType.EXIT to (ExitFragment::class.java to "ExitFragment"),
                    MenuType.CORE_VARIABLES to (CoreVariablesFragment::class.java to "CoreVariablesFragment"),
                    MenuType.SAVE_SLOTS to (SaveSlotsFragment::class.java to "SaveSlotsFragment"),
                    MenuType.LOAD_SLOTS to (LoadSlotsFragment::class.java to "LoadSlotsFragment"),
                    MenuType.MANAGE_SAVES to (ManageSavesFragment::class.java to "ManageSavesFragment"),
                    MenuType.EXIT_SAVE_SLOTS to (ExitSaveGridFragment::class.java to "ExitSaveGridFragment"),
            )

    @Test
    fun `cada menu simples substitui o container pelo seu fragment e entra na back stack`() {
        val shower = SimpleMenuShower(fragmentManager)

        for ((menu, expected) in simpleMenus) {
            val (fragmentClass, tag) = expected

            shower.show(menu)

            assertEquals("fragment for $menu", fragmentClass, shown.captured.javaClass)
            verify { transaction.replace(R.id.menu_container, shown.captured, tag) }
            verify { transaction.addToBackStack(tag) }
        }
        verify(exactly = simpleMenus.size) { transaction.commitAllowingStateLoss() }
    }

    @Test
    fun `todo MenuType tem um caminho de exibicao`() {
        val handledByPresenter = setOf(MenuType.MAIN, MenuType.PROGRESS)

        assertEquals(MenuType.values().toSet() - handledByPresenter, simpleMenus.keys)
    }

    // --- MenuFragmentPresenter ---

    @Test
    fun `MAIN adiciona de novo quando o fragment com a tag existe mas nao esta anexado`() {
        val detached = mockk<Fragment> { every { isAdded } returns false }
        every { fragmentManager.findFragmentByTag("RetroMenu3Fragment") } returns detached

        MenuFragmentPresenter(fragmentManager).show(MenuType.MAIN)

        assertTrue(shown.captured is RetroMenu3Fragment)
        verify { transaction.commitNow() }
    }

    @Test
    fun `PROGRESS limpa os submenus de grade da back stack antes de abrir`() {
        var backStack = 3
        every { fragmentManager.backStackEntryCount } answers { backStack }
        every { fragmentManager.popBackStackImmediate() } answers { backStack--; true }

        MenuFragmentPresenter(fragmentManager).show(MenuType.PROGRESS)

        assertEquals(1, backStack)
        verify(exactly = 2) { fragmentManager.popBackStackImmediate() }
        assertTrue(shown.captured is ProgressFragment)
        verify { transaction.replace(R.id.menu_container, shown.captured, "ProgressFragment") }
        verify { transaction.addToBackStack("ProgressFragment") }
        verify { transaction.commitAllowingStateLoss() }
    }

    // --- FragmentNavigationAdapter ---

    private fun adapter(): FragmentNavigationAdapter {
        val manager = fragmentManager
        val activity = mockk<FragmentActivity> { every { supportFragmentManager } returns manager }
        return FragmentNavigationAdapter(activity)
    }

    @Test
    fun `showMenu delega ao presenter`() {
        adapter().showMenu(MenuType.SETTINGS)

        assertTrue(shown.captured is SettingsMenuFragment)
    }

    @Test
    fun `hideMenu remove o fragment visivel e esvazia a back stack`() {
        val visible = mockk<Fragment>(relaxed = true) { every { isAdded } returns true }
        every { fragmentManager.findFragmentById(R.id.menu_container) } returns visible
        every { fragmentManager.backStackEntryCount } returns 2

        adapter().hideMenu()

        verify { transaction.remove(visible) }
        verify { transaction.commitAllowingStateLoss() }
        verify { fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE) }
    }

    @Test
    fun `hideMenu sem menu visivel e sem back stack nao faz transacao`() {
        every { fragmentManager.findFragmentById(R.id.menu_container) } returns null
        every { fragmentManager.backStackEntryCount } returns 0

        adapter().hideMenu()

        verify(exactly = 0) { fragmentManager.beginTransaction() }
        verify(exactly = 0) { fragmentManager.popBackStackImmediate(any<String>(), any()) }
    }

    @Test
    fun `hideMenu ignora um fragment que nao esta anexado`() {
        val detached = mockk<Fragment> { every { isAdded } returns false }
        every { fragmentManager.findFragmentById(R.id.menu_container) } returns detached
        every { fragmentManager.backStackEntryCount } returns 0

        adapter().hideMenu()

        verify(exactly = 0) { transaction.remove(any()) }
    }

    @Test
    fun `hideMenu sobrevive a um FragmentManager ja destruido`() {
        val visible = mockk<Fragment>(relaxed = true) { every { isAdded } returns true }
        every { fragmentManager.findFragmentById(R.id.menu_container) } returns visible
        every { transaction.commitAllowingStateLoss() } throws IllegalStateException("destroyed")
        every { fragmentManager.backStackEntryCount } returns 1
        every { fragmentManager.popBackStackImmediate(any<String>(), any()) } throws
                IllegalStateException("state saved")

        adapter().hideMenu() // must not throw

        verify { fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE) }
    }
}
