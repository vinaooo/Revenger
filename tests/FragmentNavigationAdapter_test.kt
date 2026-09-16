package com.vinaooo.revenger.ui.retromenu3.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
