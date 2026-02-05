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
 * Testes de integração para o sistema RetroMenu3.
 * Testa fluxos completos de navegação entre menus.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MenuIntegrationTest {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: RetroMenu3Fragment

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java)
            .create()
            .start()
            .resume()
            .get()

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        fragment = RetroMenu3Fragment.newInstance()
        activity.supportFragmentManager
            .beginTransaction()
            .add(container.id, fragment, "retro_menu")
            .commitNow()
    }

    @Test
    fun `fragment e criado corretamente`() {
        assertNotNull(fragment)
        assertTrue(fragment.isAdded)
    }

    @Test
    fun `fragment implementa todas as interfaces de listener`() {
        assertTrue(fragment is com.vinaooo.revenger.ui.retromenu3.callbacks.ProgressListener)
        assertTrue(fragment is com.vinaooo.revenger.ui.retromenu3.callbacks.SettingsMenuListener)
        assertTrue(fragment is com.vinaooo.revenger.ui.retromenu3.callbacks.ExitListener)
        assertTrue(fragment is com.vinaooo.revenger.ui.retromenu3.callbacks.AboutListener)
    }

    @Test
    fun `menu principal tem 6 items`() {
        val menuItems = fragment.getMenuItems()
        assertEquals(6, menuItems.size)
    }

    @Test
    fun `menu items tem IDs corretos`() {
        val menuItems = fragment.getMenuItems()
        val expectedIds = listOf("continue", "reset", "progress", "settings", "about", "exit")
        
        menuItems.forEachIndexed { index, item ->
            assertEquals(expectedIds[index], item.id)
        }
    }

    @Test
    fun `menu items tem titulos nao vazios`() {
        val menuItems = fragment.getMenuItems()
        
        menuItems.forEach { item ->
            assertFalse(item.title.isEmpty())
        }
    }

    @Test
    fun `menu items tem acoes definidas`() {
        val menuItems = fragment.getMenuItems()
        
        menuItems.forEach { item ->
            assertNotNull(item.action)
        }
    }

    @Test
    fun `configuracao do menu e valida`() {
        val config = MenuConfigurationBuilder.createMainMenu().build()
        
        assertTrue(config.isValid())
        assertEquals("main_menu", config.menuId)
        assertEquals(6, config.items.size)
    }

    @Test
    fun `fragment herda de MenuFragmentBase`() {
        assertTrue(fragment is MenuFragmentBase)
    }

    @Test
    fun `fragment implementa MenuFragment`() {
        assertTrue(fragment is MenuFragment)
    }

    @Test
    fun `onBackToMainMenu pode ser chamado sem erro`() {
        try {
            fragment.onBackToMainMenu()
            // Sucesso se não lançar exceção
            assertTrue(true)
        } catch (e: Exception) {
            fail("onBackToMainMenu não deveria lançar exceção: ${e.message}")
        }
    }

    @Test
    fun `onAboutBackToMainMenu pode ser chamado sem erro`() {
        try {
            fragment.onAboutBackToMainMenu()
            assertTrue(true)
        } catch (e: Exception) {
            fail("onAboutBackToMainMenu não deveria lançar exceção: ${e.message}")
        }
    }

    @Test
    fun `fragment activity e FragmentActivity`() {
        assertNotNull(fragment.activity)
        assertTrue(fragment.activity is FragmentActivity)
    }

    @Test
    fun `fragment manager e acessivel`() {
        assertNotNull(fragment.parentFragmentManager)
    }
}
