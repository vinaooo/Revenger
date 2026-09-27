package com.vinaooo.revenger.ui.retromenu3.navigation

import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression tests for [FragmentNavigationAdapter.navigateBack]: it used to unconditionally
 * return `true` whenever the back stack was non-empty, discarding
 * `FragmentManager.popBackStackImmediate()`'s own success/failure result. That let
 * [NavigationEventProcessor] believe a pop succeeded (and advance its logical menu state)
 * even on a call that silently did nothing, desyncing the logical nav state from what's
 * actually on screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class FragmentNavigationAdapter_test {

    private lateinit var activity: FragmentActivity
    private lateinit var adapter: FragmentNavigationAdapter

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        adapter = FragmentNavigationAdapter(activity)
    }

    @Test
    fun `navigateBack retorna false quando a back stack esta vazia`() {
        assertEquals(0, activity.supportFragmentManager.backStackEntryCount)

        assertFalse(adapter.navigateBack())
    }

    @Test
    fun `navigateBack retorna o resultado real de popBackStackImmediate quando ha um pop valido`() {
        activity.supportFragmentManager
                .beginTransaction()
                .add(Fragment(), "submenu")
                .addToBackStack("submenu")
                .commit()
        activity.supportFragmentManager.executePendingTransactions()
        assertEquals(1, activity.supportFragmentManager.backStackEntryCount)

        val result = adapter.navigateBack()

        assertTrue(result)
        assertEquals(0, activity.supportFragmentManager.backStackEntryCount)
    }

    // --- hideMenu ---

    private fun withMenuContainer() {
        activity.setContentView(FrameLayout(activity).apply { id = R.id.menu_container })
    }

    /** The app's shape: main menu with no backstack entry, each submenu replacing it with one. */
    private fun openMenus(depth: Int): Fragment {
        val fm = activity.supportFragmentManager
        var top: Fragment = Fragment()
        fm.beginTransaction().replace(R.id.menu_container, top, "main").commitNow()
        repeat(depth) { level ->
            top = Fragment()
            fm.beginTransaction()
                    .replace(R.id.menu_container, top, "submenu$level")
                    .addToBackStack("submenu$level")
                    .commit()
            fm.executePendingTransactions()
        }
        assertEquals(depth, fm.backStackEntryCount)
        return top
    }

    private fun assertMenuContainerEmpty() {
        val fm = activity.supportFragmentManager
        fm.executePendingTransactions()
        assertNull(
                "no menu fragment may stay in the container after hideMenu",
                fm.findFragmentById(R.id.menu_container)
        )
        assertEquals(0, fm.backStackEntryCount)
        assertTrue("no fragment is left added", fm.fragments.none { it.isAdded })
    }

    @Test
    fun `hideMenu from the main menu empties the container`() {
        withMenuContainer()
        openMenus(depth = 0)

        adapter.hideMenu()

        assertMenuContainerEmpty()
    }

    /**
     * Regression: hideMenu removed the current fragment and then popped the backstack, which
     * reversed the transaction that had replaced the main menu and put it back in the container.
     * The next open found it "already visible" and reused a menu the navigation state considered
     * closed.
     */
    @Test
    fun `hideMenu from a submenu does not bring the main menu back`() {
        withMenuContainer()
        openMenus(depth = 1)

        adapter.hideMenu()

        assertMenuContainerEmpty()
    }

    @Test
    fun `hideMenu from a nested submenu does not bring any menu back`() {
        withMenuContainer()
        openMenus(depth = 2)

        adapter.hideMenu()

        assertMenuContainerEmpty()
    }

    @Test
    fun `hideMenu with no menu open does nothing`() {
        withMenuContainer()

        adapter.hideMenu()

        assertMenuContainerEmpty()
    }
}
