package com.vinaooo.revenger.ui.retromenu3.navigation

import com.vinaooo.revenger.ui.retromenu3.MenuFragment
import com.vinaooo.revenger.ui.retromenu3.MenuIndices
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * NavigationEventProcessor concentra a máquina de estados de navegação do menu. Estes testes
 * fixam, em particular, dois contratos que já quebraram em produção no histórico do projeto:
 *
 * - Quando um fragment consome o evento de "voltar" ([MenuFragment.onBack] retorna true), o
 *   processor NÃO deve prosseguir navegando a pilha — regressão de um loop infinito causado por
 *   um fragment que chamava de volta o próprio navigateBack() dentro do seu tratamento de back.
 * - O callback de fechamento de menu ([onMenuClosed]) precisa disparar em todo caminho que
 *   efetivamente fecha o menu (voltar no menu principal, fechar tudo, ou estourar uma pilha
 *   vazia sem estado anterior) — é esse callback que reseta o estado de combo de abrir/fechar o
 *   menu a jusante; quando deixou de disparar em um desses caminhos, o combo passou a exigir
 *   dois pressionamentos para funcionar de novo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class NavigationEventProcessor_test {

    private lateinit var stateManager: NavigationStateManager
    private lateinit var fragmentAdapter: FragmentNavigationAdapter
    private lateinit var eventQueue: EventQueue
    private lateinit var processor: NavigationEventProcessor

    private var menuOpenedCalls = 0
    private val menuClosedCalls = mutableListOf<Int?>()

    @Before
    fun setUp() {
        stateManager = NavigationStateManager()
        fragmentAdapter = mockk(relaxed = true)
        eventQueue = EventQueue()
        menuOpenedCalls = 0
        menuClosedCalls.clear()

        processor =
            NavigationEventProcessor(
                stateManager = stateManager,
                fragmentAdapter = fragmentAdapter,
                eventQueue = eventQueue,
                onMenuOpened = { menuOpenedCalls++ },
                onMenuClosed = { button -> menuClosedCalls.add(button) },
            )
    }

    private fun fakeFragment(
        onBack: Boolean = false,
        selectedIndex: Int = 0,
    ): MenuFragment {
        val fragment = mockk<MenuFragment>(relaxed = true)
        every { fragment.onBack() } returns onBack
        every { fragment.getCurrentSelectedIndex() } returns selectedIndex
        return fragment
    }

    // --- Regressão: loop infinito por back consumido pelo fragment (CoreVariablesFragment) ---

    @Test
    fun `navigateBack nao mexe na pilha quando o fragment consome o evento de voltar`() {
        stateManager.pushCurrentState()
        stateManager.updateCurrentMenu(MenuType.SETTINGS)
        stateManager.registerFragment(fakeFragment(onBack = true), itemCount = 3)

        val handled = processor.navigateBack()

        assertTrue(handled)
        verify(exactly = 0) { fragmentAdapter.navigateBack() }
        assertEquals(MenuType.SETTINGS, stateManager.currentMenu)
        assertEquals(1, stateManager.getStackSize())
    }

    @Test
    fun `navigateBack navega normalmente quando o fragment nao consome o evento`() {
        stateManager.pushCurrentState()
        stateManager.updateCurrentMenu(MenuType.SETTINGS)
        stateManager.registerFragment(fakeFragment(onBack = false), itemCount = 3)
        every { fragmentAdapter.navigateBack() } returns true

        val handled = processor.navigateBack()

        assertTrue(handled)
        verify { fragmentAdapter.navigateBack() }
        assertEquals(MenuType.MAIN, stateManager.currentMenu)
        assertTrue(stateManager.isStackEmpty())
    }

    // --- Regressão: onMenuClosed precisa disparar em todo caminho que fecha o menu ---

    @Test
    fun `navigateBack no menu principal com pilha vazia fecha o menu e notifica onMenuClosed`() {
        stateManager.registerFragment(fakeFragment(onBack = false), itemCount = MenuIndices.TOTAL_ITEMS)

        processor.processEvent(
            NavigationEvent.NavigateBack(keyCode = 99, inputSource = InputSource.PHYSICAL_GAMEPAD)
        )

        assertEquals(listOf(99), menuClosedCalls)
        verify { fragmentAdapter.hideMenu() }
        assertNull(stateManager.currentFragment)
    }

    @Test
    fun `closeAllMenus fecha o menu e notifica onMenuClosed`() {
        stateManager.updateCurrentMenu(MenuType.SETTINGS)
        stateManager.registerFragment(fakeFragment(), itemCount = 3)

        processor.processEvent(
            NavigationEvent.CloseAllMenus(keyCode = 42, inputSource = InputSource.PHYSICAL_GAMEPAD)
        )

        assertEquals(listOf(42), menuClosedCalls)
        assertEquals(MenuType.MAIN, stateManager.currentMenu)
        verify { fragmentAdapter.hideMenu() }
    }

    @Test
    fun `navigateBack fecha o menu quando a pilha estoura sem estado anterior (submenu aberto como raiz)`() {
        // Simula um submenu aberto diretamente como raiz (ex: grade de save do fluxo de PiP),
        // sem nenhum estado empilhado para restaurar.
        stateManager.updateCurrentMenu(MenuType.EXIT_SAVE_SLOTS)
        every { fragmentAdapter.navigateBack() } returns false
        every { fragmentAdapter.getBackStackCount() } returns 0

        val handled = processor.navigateBack()

        assertTrue(handled)
        assertEquals(listOf(null), menuClosedCalls)
        assertEquals(MenuType.MAIN, stateManager.currentMenu)
    }

    @Test
    fun `navigateBack nao fecha o menu quando ainda ha fragments na back stack apos estourar a pilha`() {
        stateManager.updateCurrentMenu(MenuType.EXIT_SAVE_SLOTS)
        every { fragmentAdapter.navigateBack() } returns true
        every { fragmentAdapter.getBackStackCount() } returns 1

        val handled = processor.navigateBack()

        assertTrue(handled)
        assertTrue(menuClosedCalls.isEmpty())
    }

    // --- selectItem: validação de bounds ---

    @Test
    fun `selectItem ignora indices fora dos limites`() {
        val fragment = fakeFragment()
        stateManager.registerFragment(fragment, itemCount = 3)
        stateManager.updateSelectedIndex(1)

        processor.selectItem(5)
        processor.selectItem(-1)

        assertEquals(1, stateManager.selectedItemIndex)
        verify(exactly = 0) { fragment.setSelectedIndex(any()) }
    }

    @Test
    fun `selectItem dentro dos limites atualiza indice e sincroniza visual`() {
        val fragment = fakeFragment()
        stateManager.registerFragment(fragment, itemCount = 3)

        processor.selectItem(2)

        assertEquals(2, stateManager.selectedItemIndex)
        verify { fragment.setSelectedIndex(2) }
    }

    // --- activateItem no menu principal ---

    @Test
    fun `activateItem em CONTINUE fecha o menu`() {
        stateManager.updateSelectedIndex(MenuIndices.CONTINUE)
        stateManager.registerFragment(fakeFragment(), itemCount = MenuIndices.TOTAL_ITEMS)

        processor.activateItem()

        assertEquals(listOf(null), menuClosedCalls)
        verify { fragmentAdapter.hideMenu() }
        assertNull(stateManager.currentFragment)
    }

    @Test
    fun `activateItem em SETTINGS empilha o estado atual e navega para o submenu`() {
        stateManager.updateSelectedIndex(MenuIndices.SETTINGS)

        processor.activateItem()

        assertEquals(MenuType.SETTINGS, stateManager.currentMenu)
        assertEquals(1, stateManager.getStackSize())
        verify { fragmentAdapter.showMenu(MenuType.SETTINGS) }
    }

    @Test
    fun `activateItem em submenu delega a confirmacao ao fragment atual`() {
        val fragment = fakeFragment()
        stateManager.updateCurrentMenu(MenuType.SETTINGS)
        stateManager.registerFragment(fragment, itemCount = 3)

        processor.activateItem()

        verify { fragment.onConfirm() }
    }

    // --- Navegação direcional delega ao fragment e sincroniza o índice selecionado ---

    @Test
    fun `navigateDown delega ao fragment e sincroniza o indice selecionado`() {
        val fragment = fakeFragment(selectedIndex = 2)
        stateManager.registerFragment(fragment, itemCount = 5)

        processor.navigateDown()

        verify { fragment.onNavigateDown() }
        assertEquals(2, stateManager.selectedItemIndex)
    }

    // --- openMainMenu via processEvent(OpenMenu) ---

    @Test
    fun `OpenMenu pausa o jogo e mostra o menu principal`() {
        processor.processEvent(NavigationEvent.OpenMenu(inputSource = InputSource.PHYSICAL_GAMEPAD))

        assertEquals(1, menuOpenedCalls)
        assertEquals(MenuType.MAIN, stateManager.currentMenu)
        verify { fragmentAdapter.showMenu(MenuType.MAIN) }
    }
}
