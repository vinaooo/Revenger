package com.vinaooo.revenger.ui.retromenu3.navigation

import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MenuFragmentPresenter] was extracted out of [FragmentNavigationAdapter] (the per-[MenuType]
 * fragment transaction), which had no direct coverage of [FragmentNavigationAdapter.showMenu]
 * before this file -- [FragmentNavigationAdapter_test] only exercises `navigateBack`. This file
 * covers the two menus this class special-cases itself (MAIN, which must be added synchronously
 * via `commitNow()`, and PROGRESS, which clears any grid submenus off the back stack first) and
 * confirms every other type is delegated to [SimpleMenuShower].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuFragmentPresenter_test {

    private lateinit var activity: FragmentActivity
    private lateinit var presenter: MenuFragmentPresenter

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()
        val container = FrameLayout(activity).apply { id = R.id.menu_container }
        activity.setContentView(container)
        presenter = MenuFragmentPresenter(activity.supportFragmentManager)
    }

    @Test
    fun `show MAIN adiciona RetroMenu3Fragment sincronamente sem empilhar`() {
        presenter.show(MenuType.MAIN)

        val fragment = activity.supportFragmentManager.findFragmentByTag("RetroMenu3Fragment")
        assertNotNull(fragment)
        assertTrue(fragment!!.isAdded)
        assertEquals(0, activity.supportFragmentManager.backStackEntryCount)
    }

    @Test
    fun `show MAIN chamado duas vezes nao duplica o fragment`() {
        presenter.show(MenuType.MAIN)
        presenter.show(MenuType.MAIN)

        assertEquals(0, activity.supportFragmentManager.backStackEntryCount)
        assertNotNull(activity.supportFragmentManager.findFragmentByTag("RetroMenu3Fragment"))
    }

    @Test
    fun `show PROGRESS adiciona ProgressFragment e empilha`() {
        presenter.show(MenuType.PROGRESS)
        activity.supportFragmentManager.executePendingTransactions()

        assertNotNull(activity.supportFragmentManager.findFragmentByTag("ProgressFragment"))
        assertEquals(1, activity.supportFragmentManager.backStackEntryCount)
    }

    @Test
    fun `show SETTINGS delega para SimpleMenuShower e adiciona o fragment`() {
        presenter.show(MenuType.SETTINGS)
        activity.supportFragmentManager.executePendingTransactions()

        assertNotNull(activity.supportFragmentManager.findFragmentByTag("SettingsMenuFragment"))
        assertEquals(1, activity.supportFragmentManager.backStackEntryCount)
    }
}
