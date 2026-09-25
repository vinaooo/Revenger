package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.After
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
    private var originalAppConfig: AppConfig? = null

    private val appConfigField =
            RevengerApplication::class.java.getDeclaredField("appConfig").apply {
                isAccessible = true
            }

    @Before
    fun setup() {
        originalAppConfig = appConfigField.get(null) as AppConfig?
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

    @After
    fun tearDown() {
        appConfigField.set(null, originalAppConfig)
    }

    /** Adds a fresh fragment whose `loadVariables()` reads [variables] from a mocked AppConfig. */
    private fun fragmentWithVariables(variables: String): CoreVariablesFragment {
        val appConfig = mockk<AppConfig>(relaxed = true)
        every { appConfig.getVariables() } returns variables
        appConfigField.set(null, appConfig)

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)
        val newFragment = CoreVariablesFragment()
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, newFragment, "core_variables_mocked")
                .commitNow()
        return newFragment
    }

    @Test
    fun `variaveis vazias geram apenas o item de voltar`() {
        val menuItems = fragmentWithVariables("").getMenuItems()

        assertEquals(1, menuItems.size)
    }

    @Test
    fun `variaveis separadas por virgula viram itens aparados antes do item de voltar`() {
        val menuItems = fragmentWithVariables("opt_a=1, opt_b=2 ,,  ").getMenuItems()

        assertEquals(3, menuItems.size)
        assertEquals("opt_a=1", menuItems[0].title)
        assertEquals("opt_b=2", menuItems[1].title)
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
