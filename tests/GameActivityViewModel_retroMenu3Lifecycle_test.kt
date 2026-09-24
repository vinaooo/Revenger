package com.vinaooo.revenger.viewmodels

import android.app.Application
import android.os.Looper
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.R
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Characterization tests for the RetroMenu3 fragment/container/toggle cluster of
 * [GameActivityViewModel] -- written BEFORE extracting these methods into dedicated classes, to
 * pin down current behavior none of these had a ViewModel-level test for yet.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameActivityViewModel_retroMenu3Lifecycle_test {

    private lateinit var viewModel: GameActivityViewModel

    private fun setRevengerAppConfig(appConfig: AppConfig?) {
        val field = RevengerApplication::class.java.getDeclaredField("appConfig")
        field.isAccessible = true
        field.set(null, appConfig)
    }

    private fun <T> setPrivateField(target: Any, fieldName: String, value: T) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun <T> getPrivateField(target: Any, fieldName: String): T {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST") return field.get(target) as T
    }

    private fun mockMenuManager(): MenuManager {
        val mock = mockk<MenuManager>(relaxed = true)
        setPrivateField(viewModel, "menuManager", mock)
        return mock
    }

    private fun mockMenuViewModel(): MenuViewModel {
        val mock = mockk<MenuViewModel>(relaxed = true)
        setPrivateField(viewModel, "menuViewModel", mock)
        return mock
    }

    private fun retroMenu3Fragment(): RetroMenu3Fragment? =
            getPrivateField(viewModel, "retroMenu3Fragment")

    @Before
    fun setUp() {
        setRevengerAppConfig(mockk(relaxed = true))
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GameActivityViewModel(app)
    }

    @After
    fun tearDown() {
        setRevengerAppConfig(null)
    }

    // ---------------------------------------------------------------------------------------
    // prepareRetroMenu3 / recreateRetroMenu3
    // ---------------------------------------------------------------------------------------

    @Test
    fun `prepareRetroMenu3 cria e registra o fragmento quando nao existe nenhum`() {
        val menuManagerMock = mockMenuManager()

        viewModel.prepareRetroMenu3()

        assertTrue(retroMenu3Fragment() != null)
        verify(exactly = 1) {
            menuManagerMock.registerFragment(MenuState.MAIN_MENU, retroMenu3Fragment()!!)
        }
    }

    @Test
    fun `prepareRetroMenu3 nao recria nem reregistra quando ja existe um fragmento`() {
        val menuManagerMock = mockMenuManager()
        val existing = mockk<RetroMenu3Fragment>(relaxed = true)
        setPrivateField(viewModel, "retroMenu3Fragment", existing)

        viewModel.prepareRetroMenu3()

        assertSame(existing, retroMenu3Fragment())
        verify(exactly = 0) { menuManagerMock.registerFragment(any(), any()) }
    }

    @Test
    fun `recreateRetroMenu3 substitui o fragmento existente por uma nova instancia`() {
        mockMenuManager()
        val existing = mockk<RetroMenu3Fragment>(relaxed = true)
        setPrivateField(viewModel, "retroMenu3Fragment", existing)

        viewModel.recreateRetroMenu3()

        assertNotSame(existing, retroMenu3Fragment())
        assertTrue(retroMenu3Fragment() != null)
    }

    // ---------------------------------------------------------------------------------------
    // updateRetroMenu3FragmentReference / onRetroMenu3FragmentDestroyed
    // ---------------------------------------------------------------------------------------

    @Test
    fun `updateRetroMenu3FragmentReference substitui a referencia e reregistra no menuManager`() {
        val menuManagerMock = mockMenuManager()
        val fragment = mockk<RetroMenu3Fragment>(relaxed = true)

        viewModel.updateRetroMenu3FragmentReference(fragment)

        assertSame(fragment, retroMenu3Fragment())
        verify(exactly = 1) { menuManagerMock.registerFragment(MenuState.MAIN_MENU, fragment) }
    }

    @Test
    fun `onRetroMenu3FragmentDestroyed limpa a referencia do fragmento`() {
        setPrivateField(viewModel, "retroMenu3Fragment", mockk<RetroMenu3Fragment>(relaxed = true))

        viewModel.onRetroMenu3FragmentDestroyed()

        assertNull(retroMenu3Fragment())
    }

    // ---------------------------------------------------------------------------------------
    // isRetroMenu3Open
    // ---------------------------------------------------------------------------------------

    @Test
    fun `isRetroMenu3Open retorna false quando nao ha fragmento`() {
        assertFalse(viewModel.isRetroMenu3Open())
    }

    @Test
    fun `isRetroMenu3Open reflete o isAdded do fragmento registrado`() {
        val fragment = mockk<RetroMenu3Fragment>(relaxed = true)
        every { fragment.isAdded } returns true
        setPrivateField(viewModel, "retroMenu3Fragment", fragment)

        assertTrue(viewModel.isRetroMenu3Open())
    }

    // ---------------------------------------------------------------------------------------
    // setMenuContainer / getMenuContainerId / setGamePadContainer
    // ---------------------------------------------------------------------------------------

    @Test
    fun `setMenuContainer guarda o container e notifica o menuViewModel`() {
        val menuViewModelMock = mockMenuViewModel()
        val container = FrameLayout(ApplicationProvider.getApplicationContext())

        viewModel.setMenuContainer(container)

        assertSame(container, getPrivateField<FrameLayout?>(viewModel, "menuContainerView"))
        verify(exactly = 1) { menuViewModelMock.setMenuContainer(container) }
    }

    @Test
    fun `getMenuContainerId cai no fallback quando nenhum container foi configurado`() {
        assertEquals(R.id.menu_container, viewModel.getMenuContainerId())
    }

    @Test
    fun `getMenuContainerId retorna o id do container configurado`() {
        mockMenuViewModel()
        val container = FrameLayout(ApplicationProvider.getApplicationContext())
        container.id = 12345
        viewModel.setMenuContainer(container)

        assertEquals(12345, viewModel.getMenuContainerId())
    }

    @Test
    fun `setGamePadContainer guarda a referencia do container`() {
        val container = LinearLayout(ApplicationProvider.getApplicationContext())

        viewModel.setGamePadContainer(container)

        assertSame(
                container,
                getPrivateField<LinearLayout?>(viewModel, "gamePadContainerView")
        )
    }

    // ---------------------------------------------------------------------------------------
    // toggleMainMenu
    // ---------------------------------------------------------------------------------------

    @Test
    fun `toggleMainMenu fecha todos os menus quando ja ha um menu ativo`() {
        val navController = mockk<NavigationController>(relaxed = true)
        every { navController.isMenuActive() } returns true
        viewModel.navigationController = navController

        viewModel.toggleMainMenu()

        verify(exactly = 1) {
            navController.handleNavigationEvent(match { it is NavigationEvent.CloseAllMenus })
        }
    }

    @Test
    fun `toggleMainMenu abre o menu quando nenhum menu esta ativo`() {
        val navController = mockk<NavigationController>(relaxed = true)
        every { navController.isMenuActive() } returns false
        viewModel.navigationController = navController

        viewModel.toggleMainMenu()

        verify(exactly = 1) {
            navController.handleNavigationEvent(match { it is NavigationEvent.OpenMenu })
        }
    }

    // ---------------------------------------------------------------------------------------
    // dismissRetroMenu3
    // ---------------------------------------------------------------------------------------

    @Test
    fun `dismissRetroMenu3 sincroniza o NavigationController e o menuStateManager ao completar a animacao`() {
        val navController = mockk<NavigationController>(relaxed = true)
        viewModel.navigationController = navController

        val fragment = mockk<RetroMenu3Fragment>(relaxed = true)
        every { fragment.dismissMenuPublic(any()) } answers
                {
                    @Suppress("UNCHECKED_CAST")
                    (it.invocation.args[0] as? (() -> Unit))?.invoke()
                }
        setPrivateField(viewModel, "retroMenu3Fragment", fragment)

        val menuStateManager = getPrivateField<MenuStateManager>(viewModel, "menuStateManager")
        menuStateManager.setRetroMenu3Open(true)

        var animationEndCalled = false
        viewModel.dismissRetroMenu3 { animationEndCalled = true }

        verify(exactly = 1) { navController.closeMenuExternal() }
        assertFalse(menuStateManager.isRetroMenu3Open())
        assertTrue(animationEndCalled)
    }

    // ---------------------------------------------------------------------------------------
    // clearControllerInputState
    // ---------------------------------------------------------------------------------------

    @Test
    fun `clearControllerInputState emite o reset do combo no InputViewModel apos o delay`() {
        viewModel.clearControllerInputState()

        val inputViewModel = getPrivateField<InputViewModel>(viewModel, "inputViewModel")
        assertTrue(inputViewModel.eventFlow.value is InputViewModel.InputEvent.Idle)

        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(250))

        assertTrue(
                inputViewModel.eventFlow.value is
                        InputViewModel.InputEvent.ResetComboAlreadyTriggered
        )
    }
}
