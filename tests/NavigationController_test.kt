package com.vinaooo.revenger.ui.retromenu3.navigation

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.ui.retromenu3.MenuFragment
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // --- clearPendingEvents ---

    @Test
    fun `clearPendingEvents nao lanca quando chamado sem eventos pendentes`() {
        controller.registerFragment(fakeFragment(), itemCount = 5)
        controller.handleNavigationEvent(selectItemEvent(index = 2, timestamp = 1_000))

        controller.clearPendingEvents()

        controller.handleNavigationEvent(selectItemEvent(index = 4, timestamp = 5_000))
        val bundle = Bundle()
        controller.saveState(bundle)
        assertEquals(4, bundle.getInt("nav_selected_index"))
    }
}
