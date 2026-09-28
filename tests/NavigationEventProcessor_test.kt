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
        stateManager.registerFragment(fakeFragment(), itemCount = 9)
        stateManager.updateSelectedIndex(4)
        every { fragmentAdapter.navigateBack() } returns false
        every { fragmentAdapter.getBackStackCount() } returns 0

        val handled = processor.navigateBack()

        assertTrue(handled)
        assertEquals(listOf(null), menuClosedCalls)
        assertEquals(MenuType.MAIN, stateManager.currentMenu)
        // Mutation testing: hiding the menu, dropping the fragment and resetting the selection
        // on this path went unnoticed by every test.
        assertEquals(0, stateManager.selectedItemIndex)
        assertNull(stateManager.currentFragment)
        verify { fragmentAdapter.hideMenu() }
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

    // --- processEvent: cada tipo de evento chega à ação certa ---

    @Test
    fun `Navigate UP e DOWN delegam ao fragment e sincronizam o indice`() {
        val fragment = fakeFragment(selectedIndex = 4)
        stateManager.registerFragment(fragment, itemCount = 6)

        processor.processEvent(NavigationEvent.Navigate(Direction.UP, inputSource = InputSource.KEYBOARD))
        processor.processEvent(NavigationEvent.Navigate(Direction.DOWN, inputSource = InputSource.KEYBOARD))

        verify { fragment.onNavigateUp() }
        verify { fragment.onNavigateDown() }
        assertEquals(4, stateManager.selectedItemIndex)
    }

    @Test
    fun `navigateUp sem fragment registrado volta o indice para zero`() {
        stateManager.updateSelectedIndex(3)

        processor.navigateUp()

        assertEquals(0, stateManager.selectedItemIndex)
    }

    @Test
    fun `Navigate LEFT e RIGHT so sincronizam o indice quando o fragment trata o evento`() {
        val fragment = fakeFragment(selectedIndex = 5)
        every { fragment.onNavigateLeft() } returns true
        every { fragment.onNavigateRight() } returns false
        stateManager.registerFragment(fragment, itemCount = 9)

        processor.processEvent(NavigationEvent.Navigate(Direction.RIGHT, inputSource = InputSource.TOUCH))
        assertEquals(0, stateManager.selectedItemIndex)

        processor.processEvent(NavigationEvent.Navigate(Direction.LEFT, inputSource = InputSource.TOUCH))
        assertEquals(5, stateManager.selectedItemIndex)
    }

    @Test
    fun `Navigate LEFT e RIGHT sem fragment nao alteram nada`() {
        stateManager.updateSelectedIndex(2)

        processor.navigateLeft()
        processor.navigateRight()

        assertEquals(2, stateManager.selectedItemIndex)
    }

    @Test
    fun `SelectItem seleciona o indice e atualiza o visual`() {
        val fragment = fakeFragment()
        stateManager.registerFragment(fragment, itemCount = 4)

        processor.processEvent(NavigationEvent.SelectItem(3, inputSource = InputSource.TOUCH))

        assertEquals(3, stateManager.selectedItemIndex)
        verify { fragment.setSelectedIndex(3) }
    }

    @Test
    fun `ActivateSelected em CONTINUE repassa o botao que fechou o menu`() {
        stateManager.updateSelectedIndex(MenuIndices.CONTINUE)

        processor.processEvent(NavigationEvent.ActivateSelected(keyCode = 96, inputSource = InputSource.PHYSICAL_GAMEPAD))

        assertEquals(listOf<Int?>(96), menuClosedCalls)
        assertNull(eventQueue.dequeue())
    }

    @Test
    fun `NavigateBack no menu principal repassa o botao de voltar ao fechar`() {
        processor.processEvent(NavigationEvent.NavigateBack(keyCode = 97, inputSource = InputSource.PHYSICAL_GAMEPAD))

        assertEquals(listOf<Int?>(97), menuClosedCalls)
        verify { fragmentAdapter.hideMenu() }
    }

    @Test
    fun `CloseAllMenus volta ao principal, limpa a pilha e esquece o botao depois de fechar`() {
        stateManager.pushCurrentState()
        stateManager.updateCurrentMenu(MenuType.SETTINGS)
        stateManager.updateSelectedIndex(2)
        stateManager.registerFragment(fakeFragment(), itemCount = 3)

        processor.processEvent(NavigationEvent.CloseAllMenus(keyCode = 108, inputSource = InputSource.PHYSICAL_GAMEPAD))

        assertEquals(listOf<Int?>(108), menuClosedCalls)
        assertEquals(MenuType.MAIN, stateManager.currentMenu)
        assertEquals(0, stateManager.selectedItemIndex)
        assertTrue(stateManager.isStackEmpty())
        assertNull(stateManager.currentFragment)

        processor.navigateBack() // closes again at MAIN: the old button must not be reused
        assertEquals(listOf<Int?>(108, null), menuClosedCalls)
    }

    @Test
    fun `OpenMenu com destino abre direto no submenu e limpa a pilha antiga`() {
        stateManager.pushCurrentState()
        stateManager.updateSelectedIndex(4)

        processor.processEvent(
                NavigationEvent.OpenMenu(targetMenu = MenuType.PROGRESS, inputSource = InputSource.EMULATED_GAMEPAD)
        )

        assertEquals(MenuType.PROGRESS, stateManager.currentMenu)
        assertEquals(0, stateManager.selectedItemIndex)
        assertTrue(stateManager.isStackEmpty())
        verify { fragmentAdapter.showMenu(MenuType.PROGRESS) }
    }

    // --- activateItem: itens do menu principal ---

    @Test
    fun `activateItem nos itens de submenu navega para o submenu correspondente`() {
        val targets =
                mapOf(
                        MenuIndices.PROGRESS to MenuType.PROGRESS,
                        MenuIndices.SETTINGS to MenuType.SETTINGS,
                        MenuIndices.ABOUT to MenuType.ABOUT,
                        MenuIndices.EXIT to MenuType.EXIT,
                )
        for ((index, menu) in targets) {
            stateManager.clearStack()
            stateManager.updateCurrentMenu(MenuType.MAIN)
            stateManager.updateSelectedIndex(index)

            processor.activateItem()

            assertEquals(menu, stateManager.currentMenu)
            assertEquals(0, stateManager.selectedItemIndex)
            assertEquals(MenuState(MenuType.MAIN, index), stateManager.popState())
            verify { fragmentAdapter.showMenu(menu) }
        }
    }

    @Test
    fun `activateItem em RESET delega ao fragment e continua no menu principal`() {
        for (handled in listOf(true, false)) {
            val fragment = fakeFragment()
            every { fragment.onConfirm() } returns handled
            stateManager.registerFragment(fragment, itemCount = MenuIndices.TOTAL_ITEMS)
            stateManager.updateSelectedIndex(MenuIndices.RESET)

            processor.activateItem()

            verify { fragment.onConfirm() }
            assertEquals(MenuType.MAIN, stateManager.currentMenu)
            assertTrue(stateManager.isStackEmpty())
        }
        verify(exactly = 0) { fragmentAdapter.showMenu(any()) }
    }

    @Test
    fun `activateItem em RESET sem fragment nao faz nada`() {
        stateManager.updateSelectedIndex(MenuIndices.RESET)

        processor.activateItem()

        assertEquals(MenuType.MAIN, stateManager.currentMenu)
        assertTrue(menuClosedCalls.isEmpty())
    }

    @Test
    fun `activateItem com indice desconhecido nao navega nem fecha`() {
        stateManager.updateSelectedIndex(MenuIndices.TOTAL_ITEMS + 3)

        processor.activateItem()

        assertEquals(MenuType.MAIN, stateManager.currentMenu)
        assertTrue(stateManager.isStackEmpty())
        assertTrue(menuClosedCalls.isEmpty())
        verify(exactly = 0) { fragmentAdapter.showMenu(any()) }
        verify(exactly = 0) { fragmentAdapter.hideMenu() }
    }

    @Test
    fun `activateItem em submenu sem fragment nao falha`() {
        stateManager.updateCurrentMenu(MenuType.ABOUT)

        processor.activateItem()

        assertEquals(MenuType.ABOUT, stateManager.currentMenu)
    }

    // --- navigateBack restaurando da pilha ---

    @Test
    fun `navigateBack de volta ao principal esquece o botao de acao anterior`() {
        stateManager.updateSelectedIndex(MenuIndices.SETTINGS)
        processor.processEvent(NavigationEvent.ActivateSelected(keyCode = 96, inputSource = InputSource.PHYSICAL_GAMEPAD))
        every { fragmentAdapter.navigateBack() } returns true

        assertTrue(processor.navigateBack()) // SETTINGS -> MAIN, stack now empty
        assertEquals(MenuType.MAIN, stateManager.currentMenu)
        assertEquals(MenuIndices.SETTINGS, stateManager.selectedItemIndex)

        processor.navigateBack() // closes: the button from the earlier activation is gone
        assertEquals(listOf<Int?>(null), menuClosedCalls)
    }

    @Test
    fun `navigateBack devolve o resultado do adapter ao restaurar um submenu intermediario`() {
        stateManager.pushCurrentState() // MAIN
        stateManager.updateCurrentMenu(MenuType.PROGRESS)
        stateManager.pushCurrentState() // PROGRESS
        stateManager.updateCurrentMenu(MenuType.SAVE_SLOTS)
        every { fragmentAdapter.navigateBack() } returns false

        assertEquals(false, processor.navigateBack())
        assertEquals(MenuType.PROGRESS, stateManager.currentMenu)
        assertTrue(menuClosedCalls.isEmpty())
    }

    @Test
    fun `navigateBack no principal com pilha cheia restaura o estado anterior sem fechar`() {
        stateManager.pushCurrentState() // MAIN, index 0
        stateManager.updateSelectedIndex(2)
        stateManager.pushCurrentState() // MAIN, index 2
        every { fragmentAdapter.navigateBack() } returns true

        assertEquals(true, processor.navigateBack())

        assertEquals(MenuType.MAIN, stateManager.currentMenu)
        assertEquals(2, stateManager.selectedItemIndex)
        assertEquals(1, stateManager.getStackSize())
        assertTrue(menuClosedCalls.isEmpty())
    }

    @Test
    fun `navigateDown sem fragment registrado volta o indice para zero`() {
        stateManager.updateSelectedIndex(3)

        processor.navigateDown()

        assertEquals(0, stateManager.selectedItemIndex)
    }

    // --- updateSelectionVisual ---

    @Test
    fun `updateSelectionVisual sem fragment nao falha`() {
        processor.updateSelectionVisual()

        assertNull(stateManager.currentFragment)
    }

    @Test
    fun `updateSelectionVisual com fragment desanexado limpa a referencia`() {
        val fragment = fakeFragment()
        every { fragment.setSelectedIndex(any()) } throws IllegalStateException("not attached")
        stateManager.registerFragment(fragment, itemCount = 3)

        processor.updateSelectionVisual()

        assertNull(stateManager.currentFragment)
        assertEquals(0, stateManager.currentMenuItemCount)
    }

    // --- navigateToSubmenu / closeMenuExternal ---

    @Test
    fun `navigateToSubmenu empilha o estado atual por padrao`() {
        stateManager.updateCurrentMenu(MenuType.PROGRESS)
        stateManager.updateSelectedIndex(1)

        processor.navigateToSubmenu(MenuType.SAVE_SLOTS)

        assertEquals(MenuType.SAVE_SLOTS, stateManager.currentMenu)
        assertEquals(0, stateManager.selectedItemIndex)
        assertEquals(MenuState(MenuType.PROGRESS, 1), stateManager.popState())
        verify { fragmentAdapter.showMenu(MenuType.SAVE_SLOTS) }
    }

    @Test
    fun `navigateToSubmenu sem salvar estado nao mexe na pilha`() {
        stateManager.updateCurrentMenu(MenuType.PROGRESS)

        processor.navigateToSubmenu(MenuType.LOAD_SLOTS, saveCurrentState = false)

        assertEquals(MenuType.LOAD_SLOTS, stateManager.currentMenu)
        assertTrue(stateManager.isStackEmpty())
    }

    @Test
    fun `closeMenuExternal esconde o menu, limpa a fila e repassa o botao`() {
        stateManager.registerFragment(fakeFragment(), itemCount = 3)
        eventQueue.enqueue(NavigationEvent.Navigate(Direction.DOWN, inputSource = InputSource.KEYBOARD))

        processor.closeMenuExternal(closingButton = 4)

        verify { fragmentAdapter.hideMenu() }
        assertNull(stateManager.currentFragment)
        assertNull(eventQueue.dequeue())
        assertEquals(listOf<Int?>(4), menuClosedCalls)
    }

    @Test
    fun `closeMenuExternal sem botao notifica null`() {
        processor.closeMenuExternal()

        assertEquals(listOf<Int?>(null), menuClosedCalls)
    }
}
