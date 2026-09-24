package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [RetroMenu3FragmentLifecycle]'s dependencies are read/written through provider lambdas backed
 * by a local var here, standing in for `GameActivityViewModel`'s own `retroMenu3Fragment` field.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RetroMenu3FragmentLifecycle_test {

    private lateinit var menuManager: MenuManager
    private var currentFragment: RetroMenu3Fragment? = null
    private lateinit var controller: RetroMenu3FragmentLifecycle

    @Before
    fun setUp() {
        menuManager = mockk(relaxed = true)
        currentFragment = null
        controller =
                RetroMenu3FragmentLifecycle(
                        retroMenu3Fragment = { currentFragment },
                        setRetroMenu3Fragment = { currentFragment = it },
                        menuManager = { menuManager }
                )
    }

    @Test
    fun `prepareRetroMenu3 cria e registra quando nao ha fragmento`() {
        controller.prepareRetroMenu3()

        assertNotNull(currentFragment)
        verify(exactly = 1) {
            menuManager.registerFragment(MenuState.MAIN_MENU, currentFragment!!)
        }
    }

    @Test
    fun `prepareRetroMenu3 nao faz nada quando ja existe um fragmento`() {
        val existing = mockk<RetroMenu3Fragment>(relaxed = true)
        currentFragment = existing

        controller.prepareRetroMenu3()

        assertSame(existing, currentFragment)
        verify(exactly = 0) { menuManager.registerFragment(any(), any()) }
    }

    @Test
    fun `recreateRetroMenu3 limpa a referencia antes de criar uma nova`() {
        val existing = mockk<RetroMenu3Fragment>(relaxed = true)
        currentFragment = existing

        controller.recreateRetroMenu3()

        assertNotNull(currentFragment)
        assertNotSame(existing, currentFragment)
    }

    @Test
    fun `updateRetroMenu3FragmentReference substitui a referencia e reregistra`() {
        val fragment = mockk<RetroMenu3Fragment>(relaxed = true)

        controller.updateRetroMenu3FragmentReference(fragment)

        assertSame(fragment, currentFragment)
        verify(exactly = 1) { menuManager.registerFragment(MenuState.MAIN_MENU, fragment) }
    }

    @Test
    fun `onRetroMenu3FragmentDestroyed limpa a referencia`() {
        currentFragment = mockk(relaxed = true)

        controller.onRetroMenu3FragmentDestroyed()

        assertNull(currentFragment)
    }

    @Test
    fun `isRetroMenu3Open retorna false quando nao ha fragmento`() {
        assertFalse(controller.isRetroMenu3Open())
    }

    @Test
    fun `isRetroMenu3Open retorna true quando o fragmento esta adicionado`() {
        val fragment = mockk<RetroMenu3Fragment>(relaxed = true)
        every { fragment.isAdded } returns true
        currentFragment = fragment

        assertTrue(controller.isRetroMenu3Open())
    }

    @Test
    fun `isRetroMenu3Open retorna false quando o fragmento nao esta adicionado`() {
        val fragment = mockk<RetroMenu3Fragment>(relaxed = true)
        every { fragment.isAdded } returns false
        currentFragment = fragment

        assertFalse(controller.isRetroMenu3Open())
    }
}
