package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.FrameLayout
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
 * Regression coverage for [RetroMenu3Fragment.performBack] (reached via the public
 * [MenuFragmentBase.onBack]), rewritten to an early-return + try-as-expression shape to satisfy
 * detekt's ReturnCount rule without changing behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RetroMenu3Fragment_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: RetroMenu3Fragment

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        fragment = RetroMenu3Fragment.newInstance()
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "retro_menu")
                .commitNow()
    }

    @Test
    fun `onBack retorna false quando nao ha submenu na back stack`() {
        assertEquals(0, fragment.parentFragmentManager.backStackEntryCount)

        assertFalse(fragment.onBack())
    }

    @Test
    fun `onBack fecha o submenu e retorna true quando a back stack tem um submenu ativo`() {
        activity.supportFragmentManager
                .beginTransaction()
                .add(Fragment(), "dummy-submenu")
                .addToBackStack("dummy-submenu")
                .commit()
        activity.supportFragmentManager.executePendingTransactions()
        assertEquals(1, fragment.parentFragmentManager.backStackEntryCount)

        assertTrue(fragment.onBack())
        activity.supportFragmentManager.executePendingTransactions()

        assertEquals(0, fragment.parentFragmentManager.backStackEntryCount)
    }
}
