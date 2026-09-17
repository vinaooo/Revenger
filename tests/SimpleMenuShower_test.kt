package com.vinaooo.revenger.ui.retromenu3.navigation

import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [SimpleMenuShower] was extracted out of [MenuFragmentPresenter] (the eight menus whose
 * transaction is a uniform replace + addToBackStack + commit), which had no direct coverage of
 * these individual show* paths before this file. Each covered menu keeps the same tag and
 * back-stack behavior it had before the split.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SimpleMenuShower_test {

    private lateinit var activity: FragmentActivity
    private lateinit var shower: SimpleMenuShower

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()
        val container = FrameLayout(activity).apply { id = R.id.menu_container }
        activity.setContentView(container)
        shower = SimpleMenuShower(activity.supportFragmentManager)
    }

    @Test
    fun `show SETTINGS adiciona SettingsMenuFragment e empilha`() {
        shower.show(MenuType.SETTINGS)
        activity.supportFragmentManager.executePendingTransactions()

        assertNotNull(activity.supportFragmentManager.findFragmentByTag("SettingsMenuFragment"))
        assertEquals(1, activity.supportFragmentManager.backStackEntryCount)
    }

    @Test
    fun `show ABOUT adiciona AboutFragment e empilha`() {
        shower.show(MenuType.ABOUT)
        activity.supportFragmentManager.executePendingTransactions()

        assertNotNull(activity.supportFragmentManager.findFragmentByTag("AboutFragment"))
        assertEquals(1, activity.supportFragmentManager.backStackEntryCount)
    }

    @Test
    fun `show EXIT adiciona ExitFragment e empilha`() {
        shower.show(MenuType.EXIT)
        activity.supportFragmentManager.executePendingTransactions()

        assertNotNull(activity.supportFragmentManager.findFragmentByTag("ExitFragment"))
        assertEquals(1, activity.supportFragmentManager.backStackEntryCount)
    }

    @Test
    fun `show MAIN e PROGRESS nao fazem nada (nao sao menus simples)`() {
        shower.show(MenuType.MAIN)
        shower.show(MenuType.PROGRESS)

        assertNull(activity.supportFragmentManager.findFragmentByTag("RetroMenu3Fragment"))
        assertNull(activity.supportFragmentManager.findFragmentByTag("ProgressFragment"))
        assertEquals(0, activity.supportFragmentManager.backStackEntryCount)
    }
}
