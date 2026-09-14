package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for CoreVariablesFragment. Unlike most submenus, it has no
 * companion `newInstance()` and no dedicated listener interface.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CoreVariablesFragment_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: CoreVariablesFragment

    @Before
    fun setup() {
        activity =
                Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        fragment = CoreVariablesFragment()
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "core_variables")
                .commitNow()
    }

    @Test
    fun `fragment e criado corretamente`() {
        assertNotNull(fragment)
        assertTrue(fragment.isAdded)
    }

    @Test
    fun `fragment inherits from MenuFragmentBase`() {
        assertTrue(fragment is MenuFragmentBase)
    }

    @Test
    fun `getMenuItems inclui pelo menos o item de voltar`() {
        val menuItems = fragment.getMenuItems()
        // Back item is always appended, even with zero configured core variables.
        assertTrue(menuItems.isNotEmpty())
    }

    @Test
    fun `getMenuItems tem titulos nao vazios`() {
        fragment.getMenuItems().forEach { item -> assertFalse(item.title.isEmpty()) }
    }
}
