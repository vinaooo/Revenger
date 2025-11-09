package com.vinaooo.revenger.ui.retromenu3.navigation

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.FeatureFlags
import com.vinaooo.revenger.ui.retromenu3.MenuFragment

/**
 * Controlador central de navegação - Single Source of Truth.
 *
 * Gerencia todo o estado de navegação do sistema de menus:
 * - Qual menu está ativo
 * - Qual item está selecionado
 * - Histórico de navegação (para voltar)
 * - Mutex para prevenir navegação concorrente
 *
 * Todos os inputs (gamepad, touch, teclado) passam por este controlador, garantindo comportamento
 * consistente independente da fonte de entrada.
 *
 * @property activity Referência à activity para gerenciar fragmentos
 */
class NavigationController(private val activity: FragmentActivity) {
    /** Adapter para gerenciar transações de fragmentos */
    private val fragmentAdapter = FragmentNavigationAdapter(activity)

    /** Menu atualmente ativo */
    private var currentMenu: MenuType = MenuType.MAIN

    /** Índice do item atualmente selecionado (0-based) */
    private var selectedItemIndex: Int = 0

    /** Pilha de navegação para implementar "voltar" */
    private val navigationStack = NavigationStack()

    /** Fila de eventos com debouncing */
    private val eventQueue = EventQueue(debounceWindowMs = 200)

    /** Mutex para prevenir navegação concorrente */
    private var isNavigating: Boolean = false

    /** Referência ao fragmento atualmente visível (para atualizar UI) */
    private var currentFragment: MenuFragment? = null

    /** Número de itens no menu atual (para bounds checking) */
    private var currentMenuItemCount: Int = 0

    /** Rastreia o último botão que causou uma ação (para grace period) */
    private var lastActionButton: Int? = null

    /** Callback chamado quando o menu principal é aberto (para pausar jogo, etc.) */
    var onMenuOpenedCallback: (() -> Unit)? = null

    /** Callback chamado quando o menu é completamente fechado (para resumir jogo, etc.) */
    var onMenuClosedCallback: ((closingButton: Int?) -> Unit)? = null

    /**
     * Processa um evento de navegação.
     *
     * Este é o ponto de entrada principal para todos os inputs. O evento passa por debouncing na
     * fila antes de ser processado.
     *
     * @param event Evento de navegação a processar
     */
    fun handleNavigationEvent(event: NavigationEvent) {
        // Adiciona à fila com debouncing
        if (!eventQueue.enqueue(event)) {
            // Evento foi debounced (ignorado)
            return
        }

        // Processa o evento
        processNextEvent()
    }

    /** Processa o próximo evento da fila. */
    private fun processNextEvent() {
        val event = eventQueue.dequeue() ?: return

        when (event) {
            is NavigationEvent.Navigate -> {
                when (event.direction) {
                    Direction.UP -> navigateUp()
                    Direction.DOWN -> navigateDown()
                    Direction.LEFT -> {
                        /* Reservado para uso futuro */
                    }
                    Direction.RIGHT -> {
                        /* Reservado para uso futuro */
                    }
                }
            }
            is NavigationEvent.SelectItem -> {
                selectItem(event.index)
            }
            is NavigationEvent.ActivateSelected -> {
                lastActionButton = event.keyCode // Salvar botão que ativou
                activateItem()
            }
            is NavigationEvent.NavigateBack -> {
                lastActionButton = event.keyCode // Salvar botão que voltou
                navigateBack()
            }
            is NavigationEvent.OpenMenu -> {
                openMainMenu()
            }
        }
    }

    /** Navega para o item acima (UP). */
    fun navigateUp() {
        if (isNavigating) {
            if (FeatureFlags.DEBUG_NAVIGATION) {
                android.util.Log.d(TAG, "Navigation in progress, ignoring UP")
            }
            return
        }

        isNavigating = true

        try {
            // PHASE 3.2: Delegate navigation to fragment to support custom logic
            // (e.g., skipping disabled items in ProgressFragment)
            currentFragment?.onNavigateUp()

            // Sync selectedItemIndex with fragment's current selection
            selectedItemIndex = currentFragment?.getCurrentSelectedIndex() ?: 0

            if (FeatureFlags.DEBUG_NAVIGATION) {
                android.util.Log.d(
                        TAG,
                        "Navigate UP: selectedItemIndex now at $selectedItemIndex (menu: $currentMenu)"
                )
            }
        } finally {
            isNavigating = false
        }
    }

    /** Navega para o item abaixo (DOWN). */
    fun navigateDown() {
        if (isNavigating) {
            if (FeatureFlags.DEBUG_NAVIGATION) {
                android.util.Log.d(TAG, "Navigation in progress, ignoring DOWN")
            }
            return
        }

        isNavigating = true

        try {
            // PHASE 3.2: Delegate navigation to fragment to support custom logic
            // (e.g., skipping disabled items in ProgressFragment)
            currentFragment?.onNavigateDown()

            // Sync selectedItemIndex with fragment's current selection
            selectedItemIndex = currentFragment?.getCurrentSelectedIndex() ?: 0

            if (FeatureFlags.DEBUG_NAVIGATION) {
                android.util.Log.d(
                        TAG,
                        "Navigate DOWN: selectedItemIndex now at $selectedItemIndex (menu: $currentMenu)"
                )
            }
        } finally {
            isNavigating = false
        }
    }

    /**
     * Seleciona um item específico diretamente (normalmente touch).
     *
     * @param index Índice do item a selecionar (0-based)
     */
    fun selectItem(index: Int) {
        if (isNavigating) {
            android.util.Log.d(TAG, "Navigation in progress, ignoring select $index")
            return
        }

        if (index < 0 || index >= currentMenuItemCount) {
            android.util.Log.w(TAG, "Invalid item index: $index (max: $currentMenuItemCount)")
            return
        }

        isNavigating = true

        try {
            val previousIndex = selectedItemIndex
            selectedItemIndex = index

            android.util.Log.d(
                    TAG,
                    "Select item: $previousIndex -> $selectedItemIndex (menu: $currentMenu)"
            )

            updateSelectionVisual()
        } finally {
            isNavigating = false
        }
    }

    /** Ativa o item atualmente selecionado. */
    fun activateItem() {
        if (isNavigating) {
            android.util.Log.d(TAG, "Navigation in progress, ignoring activate")
            return
        }

        android.util.Log.d(TAG, "Activate item: index=$selectedItemIndex (menu: $currentMenu)")

        // PHASE 3.2b: Implementar ativação via FragmentNavigationAdapter
        // Por enquanto, vamos apenas determinar qual submenu abrir baseado no índice
        // A lógica completa de mapeamento virá depois

        if (currentMenu == MenuType.MAIN) {
            // Mapeamento correto dos índices do menu principal:
            // 0: Continue (ação direta, não abre submenu)
            // 1: Reset (ação direta, não abre submenu)
            // 2: Progress (abre submenu)
            // 3: Settings (abre submenu)
            // 4: About (abre submenu)
            // 5: Exit (abre submenu)

            val targetMenu =
                    when (selectedItemIndex) {
                        0 -> {
                            // Continue: ação direta que fecha o menu
                            android.util.Log.d(TAG, "Continue selected - closing menu")

                            // Fechar menu diretamente
                            fragmentAdapter.hideMenu()
                            currentFragment = null
                            eventQueue.clear()

                            // Chamar callback com o botão que ativou
                            onMenuClosedCallback?.invoke(lastActionButton)
                            lastActionButton = null

                            return
                        }
                        1 -> {
                            // Reset: delega para o fragment
                            android.util.Log.d(TAG, "Reset selected")
                            val handled = currentFragment?.onConfirm() ?: false
                            if (handled) {
                                android.util.Log.d(TAG, "Reset handled by fragment")
                            } else {
                                android.util.Log.w(TAG, "Reset NOT handled by fragment")
                            }
                            return
                        }
                        2 -> MenuType.PROGRESS
                        3 -> MenuType.SETTINGS
                        4 -> MenuType.ABOUT
                        5 -> MenuType.EXIT
                        else -> {
                            android.util.Log.w(TAG, "Unknown menu item index: $selectedItemIndex")
                            return
                        }
                    }

            // Salvar estado atual na pilha antes de navegar
            navigationStack.push(MenuState(currentMenu, selectedItemIndex))

            // Atualizar estado interno
            currentMenu = targetMenu
            selectedItemIndex = 0

            // Exibir novo menu
            fragmentAdapter.showMenu(targetMenu)

            android.util.Log.d(TAG, "Navigated to submenu: $targetMenu")
        } else {
            // Em submenus, delegar a ativação para o fragment atual
            android.util.Log.d(
                    TAG,
                    "Activating item in submenu $currentMenu at index $selectedItemIndex"
            )
            val handled = currentFragment?.onConfirm() ?: false
            if (handled) {
                android.util.Log.d(TAG, "Item activation handled by fragment")
            } else {
                android.util.Log.w(TAG, "Item activation NOT handled by fragment")
            }
        }
    }

    /**
     * Navega para trás (volta ao menu anterior).
     *
     * @return true se navegou para trás, false se já estava no menu principal
     */
    fun navigateBack(): Boolean {
        if (isNavigating) {
            android.util.Log.d(TAG, "Navigation in progress, ignoring back")
            return false
        }

        // Se já está no menu principal, fechar o menu completamente
        if (currentMenu == MenuType.MAIN && navigationStack.isEmpty()) {
            android.util.Log.d(TAG, "Navigate back: at main menu, closing menu completely")
            fragmentAdapter.hideMenu()
            currentFragment = null // Limpar referência
            eventQueue.clear() // CRITICAL: Limpar fila de eventos pendentes

            // PHASE 3.2b: Chamar callback para resumir o jogo após fechar o menu
            // Passar o botão que causou o fechamento para grace period
            onMenuClosedCallback?.invoke(lastActionButton)
            lastActionButton = null // Reset após uso

            return true // Menu fechado com sucesso
        }

        isNavigating = true

        try {
            val previousState = navigationStack.pop()

            if (previousState != null) {
                // Restaura estado anterior da pilha
                currentMenu = previousState.menuType
                selectedItemIndex = previousState.selectedIndex

                android.util.Log.d(
                        TAG,
                        "Navigate back: restored menu=$currentMenu, index=$selectedItemIndex"
                )

                // PHASE 3.2b: Usar FragmentNavigationAdapter para voltar
                val success = fragmentAdapter.navigateBack()
                android.util.Log.d(TAG, "FragmentAdapter.navigateBack() returned: $success")
                return success
            } else {
                // Pilha vazia, volta para main menu
                currentMenu = MenuType.MAIN
                selectedItemIndex = 0

                android.util.Log.d(TAG, "Navigate back: returned to main menu")

                // PHASE 3.2b: Usar FragmentNavigationAdapter para voltar ao main
                val success = fragmentAdapter.navigateBack()
                android.util.Log.d(TAG, "FragmentAdapter.navigateBack() returned: $success")
                return success
            }
        } finally {
            isNavigating = false
        }
    }

    /** Abre o menu principal (chamado quando SELECT+START ou START pressionado). */
    private fun openMainMenu() {
        android.util.Log.d(TAG, "Open main menu")

        // PHASE 3.2b: Chamar callback para pausar o jogo antes de mostrar o menu
        onMenuOpenedCallback?.invoke()

        // PHASE 3.2b: Implementar via FragmentNavigationAdapter
        currentMenu = MenuType.MAIN
        selectedItemIndex = 0
        navigationStack.clear()

        fragmentAdapter.showMenu(MenuType.MAIN)
        android.util.Log.d(TAG, "Main menu opened successfully")
    }

    /** Atualiza o visual de seleção no fragmento atual. */
    private fun updateSelectionVisual() {
        // SAFETY: Verificar se fragment ainda está disponível antes de atualizar
        if (currentFragment == null) {
            android.util.Log.w(TAG, "updateSelectionVisual: currentFragment is null")
            return
        }

        try {
            currentFragment?.setSelectedIndex(selectedItemIndex)
        } catch (e: IllegalStateException) {
            // Fragment was detached - clear reference
            android.util.Log.w(
                    TAG,
                    "updateSelectionVisual: fragment detached, clearing reference",
                    e
            )
            currentFragment = null
        }
    }

    /**
     * Registra o fragmento atualmente visível.
     *
     * @param fragment Fragmento ativo
     * @param itemCount Número de itens no menu
     */
    fun registerFragment(fragment: MenuFragment, itemCount: Int) {
        currentFragment = fragment
        currentMenuItemCount = itemCount

        // Inferir qual menu está ativo baseado no back stack
        // Se back stack está vazio, estamos no MAIN
        val backStackCount = fragmentAdapter.getBackStackCount()
        if (backStackCount == 0) {
            currentMenu = MenuType.MAIN
            android.util.Log.d(
                    TAG,
                    "Registered MAIN menu fragment with $itemCount items, currentIndex=$selectedItemIndex"
            )
        } else {
            android.util.Log.d(
                    TAG,
                    "Registered submenu fragment (backStack=$backStackCount) with $itemCount items"
            )
        }

        // IMPORTANTE: Sincronizar UI do fragment com o estado atual do NavigationController
        // Isso garante que após rotação, a seleção correta seja exibida
        updateSelectionVisual()
    }

    /** Desregistra o fragmento atual. */
    fun unregisterFragment() {
        currentFragment = null
        currentMenuItemCount = 0
    }

    /**
     * Verifica se algum menu está ativo (principal ou submenu). Usado para determinar se eventos
     * DPAD devem ser interceptados.
     *
     * @return true se há um fragmento registrado, false caso contrário
     */
    fun isMenuActive(): Boolean {
        val hasFragment = currentFragment != null
        android.util.Log.d(TAG, "isMenuActive: hasFragment=$hasFragment")
        return hasFragment
    }

    /**
     * Salva o estado atual em um Bundle (para rotação de tela).
     *
     * @param outState Bundle para salvar o estado
     */
    fun saveState(outState: Bundle) {
        outState.putString(KEY_CURRENT_MENU, currentMenu.name)
        outState.putInt(KEY_SELECTED_INDEX, selectedItemIndex)

        val stackBundle = navigationStack.toBundle()
        outState.putBundle(KEY_NAV_STACK, stackBundle)

        android.util.Log.d(
                TAG,
                "Saved state: menu=$currentMenu, index=$selectedItemIndex, stack size=${navigationStack.size()}"
        )
    }

    /**
     * Restaura o estado de um Bundle (após rotação de tela).
     *
     * @param savedState Bundle contendo o estado salvo
     */
    fun restoreState(savedState: Bundle?) {
        if (savedState == null) {
            android.util.Log.d(TAG, "No saved state to restore")
            return
        }

        val menuString = savedState.getString(KEY_CURRENT_MENU)
        if (menuString != null) {
            try {
                currentMenu = MenuType.valueOf(menuString)
                selectedItemIndex = savedState.getInt(KEY_SELECTED_INDEX, 0)

                val stackBundle = savedState.getBundle(KEY_NAV_STACK)
                navigationStack.fromBundle(stackBundle)

                android.util.Log.d(
                        TAG,
                        "Restored state: menu=$currentMenu, index=$selectedItemIndex, stack size=${navigationStack.size()}"
                )
            } catch (e: IllegalArgumentException) {
                android.util.Log.w(TAG, "Invalid menu type in saved state: $menuString")
            }
        }
    }

    /**
     * Limpa a fila de eventos pendentes.
     *
     * Útil quando o menu fecha ou quando queremos descartar inputs pendentes.
     */
    fun clearPendingEvents() {
        eventQueue.clear()
    }

    companion object {
        private const val TAG = "NavigationController"

        private const val KEY_CURRENT_MENU = "nav_current_menu"
        private const val KEY_SELECTED_INDEX = "nav_selected_index"
        private const val KEY_NAV_STACK = "nav_stack"
    }
}
