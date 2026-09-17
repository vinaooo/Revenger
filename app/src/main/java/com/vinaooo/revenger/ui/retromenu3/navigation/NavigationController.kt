package com.vinaooo.revenger.ui.retromenu3.navigation

import android.util.Log
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.ui.retromenu3.MenuFragment

/**
 * Holds the two rotation-of-control callbacks [NavigationController] fires around menu
 * open/close (pause/resume the game, reset combo state, etc). Bundled into one object instead of
 * two separate constructor parameters purely to keep [NavigationController]'s constructor under
 * the project's parameter-count threshold; both fields stay mutable `var`s so callers can keep
 * assigning them after construction exactly as before.
 */
class NavigationCallbacks {
    /** Callback chamado quando o menu principal é aberto (para pausar jogo, etc.) */
    var onMenuOpened: (() -> Unit)? = null

    /** Callback chamado quando o menu é completamente fechado (para resumir jogo, etc.) */
    var onMenuClosed: ((closingButton: Int?) -> Unit)? = null
}

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
class NavigationController(
        private val activity: FragmentActivity,
        /** Adapter para gerenciar transações de fragmentos */
        private val fragmentAdapter: FragmentNavigationAdapter = FragmentNavigationAdapter(activity),
        /** Gerenciador de estado de navegação */
        private val stateManager: NavigationStateManager = NavigationStateManager(),
        /** Fila de eventos com debouncing */
        private val eventQueue: EventQueue = EventQueue(debounceWindowMs = DEBOUNCE_WINDOW_MS),
        private val callbacks: NavigationCallbacks = NavigationCallbacks(),
        /**
         * Processador de eventos de navegação. Exposto de volta aqui via delegação de interface
         * ([NavigationCommands]) para os comandos de navegação diretos (up/down/left/right,
         * selecionar, ativar, voltar, navegar-para-submenu) -- evita redeclarar um wrapper fino
         * para cada um só para manter a assinatura pública, mantendo esta classe sob o limite de
         * funções do projeto.
         */
        private val processor: NavigationEventProcessor =
                NavigationEventProcessor(
                        stateManager,
                        fragmentAdapter,
                        eventQueue,
                        onMenuOpened = { callbacks.onMenuOpened?.invoke() },
                        onMenuClosed = { callbacks.onMenuClosed?.invoke(it) }
                )
) : NavigationCommands by processor {

    /** Callback chamado quando o menu principal é aberto (para pausar jogo, etc.) */
    var onMenuOpenedCallback: (() -> Unit)?
        get() = callbacks.onMenuOpened
        set(value) {
            callbacks.onMenuOpened = value
        }

    /** Callback chamado quando o menu é completamente fechado (para resumir jogo, etc.) */
    var onMenuClosedCallback: ((closingButton: Int?) -> Unit)?
        get() = callbacks.onMenuClosed
        set(value) {
            callbacks.onMenuClosed = value
        }

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
            Log.d(TAG, "[DEBOUNCE] Event debounced: $event")
            return
        }

        try {
            Log.d(
                    TAG,
                    "[HANDLE_EVENT] ts=${System.currentTimeMillis()} " +
                            "thread=${Thread.currentThread().name} enqueuedEvent=$event"
            )
            // This only formats and logs a diagnostic string with no documented throwable
            // condition; kept as a safety net so a logging hiccup never blocks real event
            // processing below, via detekt's documented escape hatch.
        } catch (expectedUnreachable: Throwable) {
            Log.w(TAG, "[HANDLE_EVENT] failed to log event", expectedUnreachable)
        }

        // Processa o evento
        processNextEvent()
    }

    /** Processa o próximo evento da fila. */
    private fun processNextEvent() {
        val event = eventQueue.dequeue() ?: return
        processor.processEvent(event)
    }

    /** Sincroniza manualmente o estado de navegação (usado após rotação). */
    fun syncState(menuType: MenuType, selectedIndex: Int = 0, clearStack: Boolean = true) {
        if (clearStack) {
            stateManager.clearStack()
        }
        stateManager.updateCurrentMenu(menuType)
        stateManager.updateSelectedIndex(selectedIndex)
    }

    /**
     * Seleciona um item específico diretamente (normalmente touch).
     *
     * NOTE: navigateUp/navigateDown/navigateLeft/navigateRight/activateItem/navigateBack/
     * navigateToSubmenu are pure pass-throughs to [processor] and come for free via the
     * [NavigationCommands] delegation on the class header above; selectItem gets its own
     * override here only to keep the negative-index guard that predates that delegation.
     *
     * @param index Índice do item a selecionar (0-based)
     * @throws IllegalArgumentException se o índice for negativo
     */
    override fun selectItem(index: Int) {
        if (index < 0) {
            Log.e(TAG, "[ERROR] Item index cannot be negative: $index")
            throw IllegalArgumentException("Item index cannot be negative: $index")
        }

        processor.selectItem(index)
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
            Log.d(
                    TAG,
                    "Registered MAIN menu fragment with $itemCount items, " +
                            "currentIndex=${stateManager.selectedItemIndex}"
            )
        } else {
            Log.d(
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
        Log.d(TAG, "isMenuActive: hasFragment=$hasFragment")
        return hasFragment
    }

    /**
     * Salva o estado atual em um Bundle (para rotação de tela).
     *
     * @param outState Bundle para salvar o estado
     */
    fun saveState(outState: Bundle) {
        stateManager.saveState(outState)

        Log.d(
                TAG,
                "Saved state: menu=${stateManager.currentMenu}, " +
                        "index=${stateManager.selectedItemIndex}, " +
                        "stack size=${stateManager.getStackSize()}"
        )
    }

    /**
     * Restaura o estado de um Bundle (após rotação de tela).
     *
     * @param savedState Bundle contendo o estado salvo
     */
    fun restoreState(savedState: Bundle?) {
        stateManager.restoreState(savedState)

        Log.d(
                TAG,
                "Restored state: menu=${stateManager.currentMenu}, " +
                        "index=${stateManager.selectedItemIndex}, " +
                        "stack size=${stateManager.getStackSize()}"
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
