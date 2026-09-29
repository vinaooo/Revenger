package com.vinaooo.revenger.ui.retromenu3.navigation

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.ui.retromenu3.MenuFragment
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * NavigationController é o ponto de entrada único para todo input de navegação (gamepad, touch,
 * teclado) e o dono da fila de debounce. Estes testes cobrem [NavigationController.syncState] —
 * usado explicitamente para ressincronizar o estado após rotação de tela, um bug já corrigido no
 * histórico do projeto — e o debounce que a fila de eventos aplica antes de repassar ao
 * processor.
 *
 * `registerFragment`/eventos `SelectItem`/`Navigate` não disparam transações reais de fragment
 * (a única dependência real de UI, `FragmentNavigationAdapter`, só é acionada por `OpenMenu`,
 * `navigateBack` ou `activateItem` em um item que não seja Continue/Reset), então são usados aqui
 * para exercitar o controller de ponta a ponta sem precisar montar fragments concretos do app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class NavigationController_test {

    private lateinit var activity: FragmentActivity
    private lateinit var controller: NavigationController

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        controller = NavigationController(activity)
    }

    private fun selectItemEvent(index: Int, timestamp: Long) =
        NavigationEvent.SelectItem(index, timestamp = timestamp, inputSource = InputSource.TOUCH)

    private fun fakeFragment(): MenuFragment {
        val fragment = mockk<MenuFragment>(relaxed = true)
        every { fragment.getCurrentSelectedIndex() } returns 0
        return fragment
    }

    // --- Regressão: ressincronização de estado após rotação de tela ---

    @Test
    fun `syncState atualiza menu, indice e limpa a pilha`() {
        controller.registerFragment(fakeFragment(), itemCount = 5)
        controller.handleNavigationEvent(selectItemEvent(index = 2, timestamp = 1_000))

        controller.syncState(MenuType.PROGRESS, selectedIndex = 4)

        val bundle = Bundle()
        controller.saveState(bundle)
        assertEquals("PROGRESS", bundle.getString("nav_current_menu"))
        assertEquals(4, bundle.getInt("nav_selected_index"))
        assertEquals(0, bundle.getBundle("nav_stack")?.getInt("stack_size"))
    }

    // --- Debounce da fila de eventos ---

    @Test
    fun `evento muito proximo no tempo do anterior e debounced e ignorado`() {
        controller.registerFragment(fakeFragment(), itemCount = 5)

        controller.handleNavigationEvent(selectItemEvent(index = 2, timestamp = 1_000))
        // 50ms depois: dentro da janela de debounce de 200ms para SelectItem.
        controller.handleNavigationEvent(selectItemEvent(index = 4, timestamp = 1_050))

        val bundle = Bundle()
        controller.saveState(bundle)
        assertEquals(2, bundle.getInt("nav_selected_index"))
    }

    @Test
    fun `evento fora da janela de debounce e processado normalmente`() {
        controller.registerFragment(fakeFragment(), itemCount = 5)

        controller.handleNavigationEvent(selectItemEvent(index = 2, timestamp = 1_000))
        // 300ms depois: fora da janela de debounce de 200ms para SelectItem.
        controller.handleNavigationEvent(selectItemEvent(index = 4, timestamp = 1_300))

        val bundle = Bundle()
        controller.saveState(bundle)
        assertEquals(4, bundle.getInt("nav_selected_index"))
    }

    // --- selectItem: validação de bordas ---

    @Test(expected = IllegalArgumentException::class)
    fun `selectItem lanca excecao para indice negativo`() {
        controller.selectItem(-1)
    }

    // --- isMenuActive / unregisterFragment ---

    @Test
    fun `isMenuActive e falso sem nenhum fragment registrado`() {
        assertFalse(controller.isMenuActive())
    }

    @Test
    fun `unregisterFragment nao lanca quando nenhum fragment esta registrado`() {
        controller.unregisterFragment()

        assertFalse(controller.isMenuActive())
    }

    // --- registerFragment / restoreState / closeMenuExternal ---

    private fun controllerWithBackStack(count: Int, state: NavigationStateManager): NavigationController {
        val adapter = mockk<FragmentNavigationAdapter>(relaxed = true)
        every { adapter.getBackStackCount() } returns count
        return NavigationController(activity, fragmentAdapter = adapter, stateManager = state)
    }

    @Test
    fun `registrar com a pilha de fragments vazia marca o menu principal`() {
        val state = NavigationStateManager().apply { updateCurrentMenu(MenuType.SETTINGS) }

        controllerWithBackStack(0, state).registerFragment(fakeFragment(), itemCount = 4)

        assertEquals(MenuType.MAIN, state.currentMenu)
        assertEquals(4, state.currentMenuItemCount)
    }

    @Test
    fun `registrar um submenu mantem o menu atual`() {
        val state = NavigationStateManager().apply { updateCurrentMenu(MenuType.SETTINGS) }

        controllerWithBackStack(1, state).registerFragment(fakeFragment(), itemCount = 4)

        assertEquals(MenuType.SETTINGS, state.currentMenu)
        assertEquals(4, state.currentMenuItemCount)
    }

    @Test
    fun `restoreState recupera o menu e o indice salvos`() {
        controller.registerFragment(fakeFragment(), itemCount = 5)
        controller.syncState(MenuType.PROGRESS, selectedIndex = 3)
        val saved = Bundle().also { controller.saveState(it) }

        val restored = NavigationController(activity)
        restored.restoreState(saved)

        val bundle = Bundle().also { restored.saveState(it) }
        assertEquals("PROGRESS", bundle.getString("nav_current_menu"))
        assertEquals(3, bundle.getInt("nav_selected_index"))
    }

    @Test
    fun `closeMenuExternal repassa o botao que fechou o menu ao processador`() {
        val processor = mockk<NavigationEventProcessor>(relaxed = true)
        val withProcessor = NavigationController(activity, processor = processor)

        withProcessor.closeMenuExternal(closingButton = 7)

        verify { processor.closeMenuExternal(7) }
    }

    private fun controllerWith(state: NavigationStateManager) =
            NavigationController(activity, stateManager = state)

    @Test
    fun `syncState limpa uma pilha com entradas por padrao`() {
        val state = NavigationStateManager().apply { pushCurrentState() }

        controllerWith(state).syncState(MenuType.PROGRESS, selectedIndex = 1)

        assertEquals(0, state.getStackSize())
    }

    @Test
    fun `syncState com clearStack false mantem a pilha`() {
        val state = NavigationStateManager().apply { pushCurrentState() }

        controllerWith(state).syncState(MenuType.PROGRESS, selectedIndex = 1, clearStack = false)

        assertEquals(1, state.getStackSize())
        assertEquals(MenuType.PROGRESS, state.currentMenu)
    }

    @Test
    fun `selectItem seleciona o item pedido`() {
        val state = NavigationStateManager()
        val navigation = controllerWith(state)
        navigation.registerFragment(fakeFragment(), itemCount = 5)

        navigation.selectItem(3)

        assertEquals(3, state.selectedItemIndex)
    }

    @Test
    fun `registrar um fragment mostra nele a selecao atual`() {
        val state = NavigationStateManager().apply { updateSelectedIndex(2) }
        val fragment = fakeFragment()

        controllerWith(state).registerFragment(fragment, itemCount = 5)

        verify { fragment.setSelectedIndex(2) }
    }

    @Test
    fun `unregisterFragment solta o fragment registrado`() {
        val state = NavigationStateManager()
        val navigation = controllerWith(state)
        navigation.registerFragment(fakeFragment(), itemCount = 5)

        navigation.unregisterFragment()

        assertNull(state.currentFragment)
        assertEquals(0, state.currentMenuItemCount)
    }
}
