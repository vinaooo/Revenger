package com.vinaooo.revenger.ui.retromenu3.navigation

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
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
 * Todos os inputs (gamepad, touch, teclado) passam por este controlador,
 * garantindo comportamento consistente independente da fonte de entrada.
 * 
 * @property activity Referência à activity para gerenciar fragmentos
 */
class NavigationController(
    private val activity: FragmentActivity
) {
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
    
    /**
     * Processa um evento de navegação.
     * 
     * Este é o ponto de entrada principal para todos os inputs.
     * O evento passa por debouncing na fila antes de ser processado.
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
    
    /**
     * Processa o próximo evento da fila.
     */
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
                activateItem()
            }
            is NavigationEvent.NavigateBack -> {
                navigateBack()
            }
            is NavigationEvent.OpenMenu -> {
                openMainMenu()
            }
        }
    }
    
    /**
     * Navega para o item acima (UP).
     */
    fun navigateUp() {
        if (isNavigating) {
            android.util.Log.d(TAG, "Navigation in progress, ignoring UP")
            return
        }
        
        isNavigating = true
        
        try {
            val previousIndex = selectedItemIndex
            selectedItemIndex = if (selectedItemIndex > 0) {
                selectedItemIndex - 1
            } else {
                // Circular: volta para o último item
                currentMenuItemCount - 1
            }
            
            android.util.Log.d(
                TAG,
                "Navigate UP: $previousIndex -> $selectedItemIndex (menu: $currentMenu)"
            )
            
            updateSelectionVisual()
        } finally {
            isNavigating = false
        }
    }
    
    /**
     * Navega para o item abaixo (DOWN).
     */
    fun navigateDown() {
        if (isNavigating) {
            android.util.Log.d(TAG, "Navigation in progress, ignoring DOWN")
            return
        }
        
        isNavigating = true
        
        try {
            val previousIndex = selectedItemIndex
            selectedItemIndex = (selectedItemIndex + 1) % currentMenuItemCount
            
            android.util.Log.d(
                TAG,
                "Navigate DOWN: $previousIndex -> $selectedItemIndex (menu: $currentMenu)"
            )
            
            updateSelectionVisual()
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
    
    /**
     * Ativa o item atualmente selecionado.
     */
    fun activateItem() {
        if (isNavigating) {
            android.util.Log.d(TAG, "Navigation in progress, ignoring activate")
            return
        }
        
        android.util.Log.d(
            TAG,
            "Activate item: index=$selectedItemIndex (menu: $currentMenu)"
        )
        
        // TODO Phase 3: Implementar ativação via FragmentNavigationAdapter
        // Por enquanto só loga a ação
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
        
        // Se já está no menu principal, não navega (fecha o menu)
        if (currentMenu == MenuType.MAIN && navigationStack.isEmpty()) {
            android.util.Log.d(TAG, "Navigate back: already at main menu, closing")
            return false
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
                
                // TODO Phase 3: Atualizar UI via FragmentNavigationAdapter
                return true
            } else {
                // Pilha vazia, volta para main menu
                currentMenu = MenuType.MAIN
                selectedItemIndex = 0
                
                android.util.Log.d(TAG, "Navigate back: returned to main menu")
                
                // TODO Phase 3: Atualizar UI via FragmentNavigationAdapter
                return true
            }
        } finally {
            isNavigating = false
        }
    }
    
    /**
     * Abre o menu principal (chamado quando SELECT+START ou START pressionado).
     */
    private fun openMainMenu() {
        android.util.Log.d(TAG, "Open main menu")
        
        // TODO Phase 3: Implementar via FragmentNavigationAdapter
    }
    
    /**
     * Atualiza o visual de seleção no fragmento atual.
     */
    private fun updateSelectionVisual() {
        currentFragment?.setSelectedIndex(selectedItemIndex)
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
        
        android.util.Log.d(
            TAG,
            "Registered fragment for menu=$currentMenu with $itemCount items"
        )
    }
    
    /**
     * Desregistra o fragmento atual.
     */
    fun unregisterFragment() {
        currentFragment = null
        currentMenuItemCount = 0
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
     * Útil quando o menu fecha ou quando queremos descartar
     * inputs pendentes.
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
