package com.vinaooo.revenger.ui.retromenu3.navigation

import android.util.Log
import android.os.Bundle
import com.vinaooo.revenger.ui.retromenu3.MenuFragment

/**
 * Gerenciador de estado de navegação.
 *
 * Responsável por manter e manipular o estado atual do menu, incluindo:
 * - Menu ativo e item selecionado
 * - Pilha de histórico de navegação
 * - Referência ao fragmento atual
 * - Controle de concorrência (mutex)
 */
class NavigationStateManager {
    /** Menu atualmente ativo */
    var currentMenu: MenuType = MenuType.MAIN
        private set

    /** Índice do item atualmente selecionado (0-based) */
    var selectedItemIndex: Int = 0
        private set

    /** Pilha de navegação para implementar "voltar" */
    private val navigationStack = NavigationStack()

    /** Referência ao fragmento atualmente visível (para atualizar UI) */
    var currentFragment: MenuFragment? = null
        private set

    /** Número de itens no menu atual (para bounds checking) */
    var currentMenuItemCount: Int = 0
        private set

    /**
     * Atualiza o índice selecionado.
     * @param index Novo índice
     * @throws IllegalArgumentException se o índice for negativo
     */
    fun updateSelectedIndex(index: Int) {
        if (index < 0) {
            Log.e(TAG, "[ERROR] Selected index cannot be negative: $index")
            throw IllegalArgumentException("Selected index cannot be negative: $index")
        }
        selectedItemIndex = index
    }

    /**
     * Atualiza o menu atual.
     * @param menu Novo menu
     */
    fun updateCurrentMenu(menu: MenuType) {
        currentMenu = menu
    }

    /** Empilha o estado atual na pilha de navegação. */
    fun pushCurrentState() {
        navigationStack.push(MenuState(currentMenu, selectedItemIndex))
    }

    /**
     * Desempilha o último estado salvo.
     * @return O estado anterior ou null se a pilha estiver vazia
     */
    fun popState(): MenuState? {
        return navigationStack.pop()
    }

    /** Limpa a pilha de navegação. */
    fun clearStack() {
        navigationStack.clear()
    }

    /** Verifica se a pilha de navegação está vazia. */
    fun isStackEmpty(): Boolean = navigationStack.isEmpty()

    /** Retorna o tamanho da pilha de navegação. */
    fun getStackSize(): Int = navigationStack.size()

    /**
     * Registra o fragmento atualmente visível.
     *
     * @param fragment Fragmento ativo
     * @param itemCount Número de itens no menu
     */
    fun registerFragment(fragment: MenuFragment, itemCount: Int) {
        currentFragment = fragment
        currentMenuItemCount = itemCount
    }

    /** Desregistra o fragmento atual. */
    fun unregisterFragment() {
        currentFragment = null
        currentMenuItemCount = 0
    }

    /**
     * Verifica se algum menu está ativo (principal ou submenu).
     *
     * @return true se há um fragmento registrado, false caso contrário
     */
    fun isMenuActive(): Boolean {
        return currentFragment != null &&
                (currentFragment as? androidx.fragment.app.Fragment)?.isAdded == true
    }

    /**
     * Salva o estado atual em um Bundle.
     *
     * @param outState Bundle para salvar o estado
     */
    fun saveState(outState: Bundle) {
        outState.putString(KEY_CURRENT_MENU, currentMenu.name)
        outState.putInt(KEY_SELECTED_INDEX, selectedItemIndex)

        val stackBundle = navigationStack.toBundle()
        outState.putBundle(KEY_NAV_STACK, stackBundle)
    }

    /**
     * Restaura o estado de um Bundle.
     *
     * @param savedState Bundle contendo o estado salvo
     */
    fun restoreState(savedState: Bundle?) {
        if (savedState == null) return

        val menuString = savedState.getString(KEY_CURRENT_MENU)
        if (menuString != null) {
            try {
                currentMenu = MenuType.valueOf(menuString)
                selectedItemIndex = savedState.getInt(KEY_SELECTED_INDEX, 0)

                val stackBundle = savedState.getBundle(KEY_NAV_STACK)
                navigationStack.fromBundle(stackBundle)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid menu type in saved state: $menuString")
            }
        }
    }

    companion object {
        private const val TAG = "NavigationStateManager"
        private const val KEY_CURRENT_MENU = "nav_current_menu"
        private const val KEY_SELECTED_INDEX = "nav_selected_index"
        private const val KEY_NAV_STACK = "nav_stack"
    }
}
