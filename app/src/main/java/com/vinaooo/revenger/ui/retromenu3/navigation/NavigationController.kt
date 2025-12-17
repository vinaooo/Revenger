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
 * Todos os inputs (gamepad, touch, teclado) passam por este controlador, garantindo comportamento
 * consistente independente da fonte de entrada.
 *
 * @property activity Referência à activity para gerenciar fragmentos
 */
class NavigationController(private val activity: FragmentActivity) {
    /** Adapter para gerenciar transações de fragmentos */
    private val fragmentAdapter = FragmentNavigationAdapter(activity)

    /** Gerenciador de estado de navegação */
    private val stateManager = NavigationStateManager()

    /** Fila de eventos com debouncing */
    private val eventQueue = EventQueue(debounceWindowMs = DEBOUNCE_WINDOW_MS)

    /** Processador de eventos de navegação */
    private val processor =
            NavigationEventProcessor(
                    stateManager,
                    fragmentAdapter,
                    eventQueue,
                    onMenuOpened = { onMenuOpenedCallback?.invoke() },
                    onMenuClosed = { onMenuClosedCallback?.invoke(it) }
            )

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
            android.util.Log.d(TAG, "[DEBOUNCE] Event debounced: $event")
            return
        }

        // Processa o evento
        processNextEvent()
    }

    /** Processa o próximo evento da fila. */
    private fun processNextEvent() {
        val event = eventQueue.dequeue() ?: return
        processor.processEvent(event)
    }

    /** Navega para o item acima (UP). */
    fun navigateUp() {
        processor.navigateUp()
    }

    /** Navega para o item abaixo (DOWN). */
    fun navigateDown() {
        processor.navigateDown()
    }

    /**
     * Seleciona um item específico diretamente (normalmente touch).
     *
     * @param index Índice do item a selecionar (0-based)
     * @throws IllegalArgumentException se o índice for negativo
     */
    fun selectItem(index: Int) {
        if (index < 0) {
            android.util.Log.e(TAG, "[ERROR] Item index cannot be negative: $index")
            throw IllegalArgumentException("Item index cannot be negative: $index")
        }

        processor.selectItem(index)
    }

    /** Ativa o item atualmente selecionado. */
    fun activateItem() {
        processor.activateItem()
    }

    /**
     * Navega para trás (volta ao menu anterior).
     *
     * @return true se navegou para trás, false se já estava no menu principal
     */
    fun navigateBack(): Boolean {
        return processor.navigateBack()
    }

    /**
     * Registra o fragmento atualmente visível.
     * @param fragment Fragmento ativo
     * @param itemCount Número de itens no menu
     */
    fun registerFragment(fragment: MenuFragment, itemCount: Int) {
        stateManager.registerFragment(fragment, itemCount)

        // Inferir qual menu está ativo baseado no back stack
        // Se back stack está vazio, estamos no MAIN
        val backStackCount = fragmentAdapter.getBackStackCount()
        if (backStackCount == 0) {
            stateManager.updateCurrentMenu(MenuType.MAIN)
            android.util.Log.d(
                    TAG,
                    "Registered MAIN menu fragment with $itemCount items, currentIndex=${stateManager.selectedItemIndex}"
            )
        } else {
            android.util.Log.d(
                    TAG,
                    "Registered submenu fragment (backStack=$backStackCount) with $itemCount items"
            )
        }

        // IMPORTANTE: Sincronizar UI do fragment com o estado atual do NavigationController
        // Isso garante que após rotação, a seleção correta seja exibida
        processor.updateSelectionVisual()
    }

    /** Desregistra o fragmento atual. */
    fun unregisterFragment() {
        stateManager.unregisterFragment()
    }

    /**
     * Verifica se algum menu está ativo (principal ou submenu). Usado para determinar se eventos
     * DPAD devem ser interceptados.
     *
     * @return true se há um fragmento registrado, false caso contrário
     */
    fun isMenuActive(): Boolean {
        val hasFragment = stateManager.isMenuActive()
        android.util.Log.d(TAG, "isMenuActive: hasFragment=$hasFragment")
        return hasFragment
    }

    /**
     * Salva o estado atual em um Bundle (para rotação de tela).
     *
     * @param outState Bundle para salvar o estado
     */
    fun saveState(outState: Bundle) {
        stateManager.saveState(outState)

        android.util.Log.d(
                TAG,
                "Saved state: menu=${stateManager.currentMenu}, index=${stateManager.selectedItemIndex}, stack size=${stateManager.getStackSize()}"
        )
    }

    /**
     * Restaura o estado de um Bundle (após rotação de tela).
     *
     * @param savedState Bundle contendo o estado salvo
     */
    fun restoreState(savedState: Bundle?) {
        stateManager.restoreState(savedState)

        android.util.Log.d(
                TAG,
                "Restored state: menu=${stateManager.currentMenu}, index=${stateManager.selectedItemIndex}, stack size=${stateManager.getStackSize()}"
        )
    }

    /**
     * Limpa a fila de eventos pendentes.
     *
     * Útil quando o menu fecha ou quando queremos descartar inputs pendentes.
     */
    fun clearPendingEvents() {
        eventQueue.clear()
    }

    /**
     * Fecha o menu externamente (não via navegação interna). Chamado quando o menu é fechado por
     * ações como Reset ou Continue.
     */
    fun closeMenuExternal(closingButton: Int? = null) {
        processor.closeMenuExternal(closingButton)
    }

    companion object {
        private const val TAG = "NavigationController"

        /** Janela de debounce para eventos de navegação em milissegundos */
        const val DEBOUNCE_WINDOW_MS = 200L
    }
}
