package com.vinaooo.revenger.ui.retromenu3.navigation

import android.util.Log
import android.view.KeyEvent
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Adaptador de entrada de teclado físico para navegação nos menus.
 *
 * Traduz eventos de teclas do Android (KeyEvent) em NavigationEvents unificados, permitindo que
 * teclados físicos (USB, Bluetooth) controlem os menus da mesma forma que gamepads e touch.
 *
 * Mapeamento de teclas:
 * - Arrow UP/DOWN / W/S → Navigate(UP/DOWN)
 * - Arrow LEFT/RIGHT / A/D → Navigate(LEFT/RIGHT) [reservado]
 * - Enter/Space → ActivateSelected
 * - Escape → CloseAllMenus (EXIT - fecha tudo)
 * - Backspace → NavigateBack (volta um nível)
 * - F12 → Toggle menu (abrir/fechar)
 *
 * Nota: Ctrl+M e F1 são interceptados pelo sistema/emulador
 *
 * @property navigationController Controlador central para enviar eventos
 * @property isMenuOpenCallback Callback para verificar se menu está aberto (para F12)
 */
class KeyboardInputAdapter(
        private val navigationController: NavigationController,
        private val isMenuOpenCallback: () -> Boolean
) {

    companion object {
        private const val TAG = "KeyboardInputAdapter"

        // PHASE 4.3: Press Cycle Timeout
        // CORREÇÃO: Aumentado de 200ms para 500ms para cobrir o "key repeat delay" do Android
        // Android tipicamente espera ~400-500ms antes de começar a enviar eventos de repeat
        // Isso previne que o primeiro repeat seja considerado "novo ciclo"
        private const val PRESS_CYCLE_TIMEOUT_MS = 500L

        /** Timestamp inicial antes de qualquer evento ser processado (PHASE 3.1) */
        private const val NO_EVENT_TIME = 0L

        // V4.5: GLOBAL STATIC LOCK - Compartilhado por TODAS as instâncias/threads
        // CRÍTICO: Lock DEVE ser companion object (static) para proteger contra
        // múltiplas instâncias ou chamadas paralelas de GameActivityViewModel
        private val GLOBAL_STATE_LOCK = ReentrantLock()

        // V4.5: GLOBAL STATIC STATE - Compartilhado por TODAS as instâncias
        // Necessário porque GameActivityViewModel pode ter múltiplas instâncias
        // ou chamar onKeyDown() de threads paralelas
        private val pressCycleStates = mutableMapOf<Int, PressCycleState>()

        // FIX ERRO 1: Tracking de KEY_DOWN/KEY_UP para ações (Back/Backspace)
        // Previne vazamento de KEY_UP residual após transições de fragmento
        private val actionKeyDownTimestamps = mutableMapOf<Int, Long>()
        private const val KEY_UP_TIMEOUT_MS = 500L // Timeout para considerar KEY_UP órfão
    }

    // PHASE 4.6: Press Cycle Tracking (V4.6 - Global Static Thread-Safe Single-Trigger with KEY_UP
    // Reset)
    //
    // Rastreamento de "ciclos de pressão" por tecla:
    // - Ciclo = sequência de eventos DOWN/UP enquanto usuário "segura"
    // - Android gera DOWN/UP pairs rápidos (~50ms) ao segurar
    // - Permitimos apenas 1 navegação por ciclo
    // - Novo ciclo detectado via KEY_UP explícito OU timeout (>500ms sem KEY_UP)
    //
    // Fix V4.1: Usar apenas DOWN→DOWN para timeout, ignorar UP intermediários
    // Isso previne "novo ciclo" falso quando UP acontece antes do timeout
    //
    // Fix V4.2: Event Deduplication - Previne processar eventos duplicados
    // GameActivityViewModel pode enviar mesmo evento múltiplas vezes
    // Usamos event.eventTime (hardware timestamp) para detectar duplicatas
    //
    // Fix V4.3: Thread-Safe Synchronization - Previne race condition
    // GameActivityViewModel chama onKeyDown() 4x SIMULTANEAMENTE (threads paralelas)
    // Isso causava 4 navegações antes de hasNavigatedInCycle = true
    // Solução: synchronized block protege seção crítica
    //
    // Fix V4.5: GLOBAL STATIC Lock e State - Previne múltiplas instâncias
    // Lock e state DEVEM ser companion object (static) para funcionar
    // corretamente entre múltiplas instâncias de KeyboardInputAdapter
    //
    // Fix V4.6: KEY_UP Reset - Permite navegação rápida (3+ toques/segundo)
    // KEY_UP reseta hasNavigatedInCycle imediatamente, distinguindo:
    // - Segurar (sem KEY_UP) = 1 navegação
    // - Toques rápidos (com KEY_UP) = múltiplas navegações
    //
    // Exemplo Timeline 1 (segurar DOWN por 2s):
    // t=0ms    DOWN → ALLOW (primeiro do ciclo, hasNavigatedInCycle=true)
    // t=50ms   DOWN → BLOCK (mesmo ciclo, hasNavigatedInCycle=true)
    // t=100ms  DOWN → BLOCK (mesmo ciclo, hasNavigatedInCycle=true)
    // ... continua bloqueando até soltar ou timeout
    // t=2000ms UP → RESET (hasNavigatedInCycle=false)
    //
    // Exemplo Timeline 2 (toques rápidos - 3x/segundo):
    // t=0ms    DOWN → ALLOW (hasNavigatedInCycle=true)
    // t=100ms  UP → RESET (hasNavigatedInCycle=false)
    // t=333ms  DOWN → ALLOW (hasNavigatedInCycle=true) - novo toque
    // t=433ms  UP → RESET (hasNavigatedInCycle=false)
    // t=666ms  DOWN → ALLOW (hasNavigatedInCycle=true) - novo toque
    //
    private data class PressCycleState(
            var lastDownTime: Long = NO_EVENT_TIME, // Timestamp do último KEY_DOWN (para timeout)
            var lastUpTime: Long =
                    NO_EVENT_TIME, // Timestamp do último KEY_UP (não usado para timeout)
            var hasNavigatedInCycle: Boolean = false, // Já navegou neste ciclo?
            var lastProcessedEventTime: Long =
                    NO_EVENT_TIME // V4.2: eventTime do último evento processado (deduplicação)
    )

    /**
     * Processa um evento de tecla pressionada.
     *
     * PHASE 4.3: Sistema de Press Cycle Tracking (Thread-Safe Single-Trigger)
     *
     * Implementa verdadeiro single-trigger: segurar tecla = navega APENAS 1 vez.
     *
     * Android envia pares DOWN/UP rápidos (~50ms) ao segurar tecla física. Agrupamos esses eventos
     * em "ciclos de pressão" usando timeout:
     * - Mesmo ciclo: eventos < 200ms
     * - Novo ciclo: após KEY_UP explícito OU timeout (>200ms sem eventos)
     * - Permite apenas 1 navegação por ciclo
     *
     * V4.3: synchronized block protege contra race condition (4 threads simultâneas)
     *
     * @param keyCode Código da tecla (KeyEvent.KEYCODE_*)
     * @param event Evento completo (para metadados)
     * @return true se o evento foi consumido, false caso contrário
     */
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val currentTime = System.currentTimeMillis()

        // Identificar teclas de navegação
        val isNavigationKey =
                keyCode in
                        listOf(
                                KeyEvent.KEYCODE_DPAD_UP,
                                KeyEvent.KEYCODE_W,
                                KeyEvent.KEYCODE_DPAD_DOWN,
                                KeyEvent.KEYCODE_S,
                                KeyEvent.KEYCODE_DPAD_LEFT,
                                KeyEvent.KEYCODE_A,
                                KeyEvent.KEYCODE_DPAD_RIGHT,
                                KeyEvent.KEYCODE_D
                        )

        if (isNavigationKey) {
            // LOG DETALHADO: Sempre logar quando evento chega
            Log.d(
                    TAG,
                    "[KEY_DOWN-RECEIVED] keyCode=$keyCode, repeatCount=${event.repeatCount}, eventTime=${event.eventTime}, downTime=${event.downTime}"
            )

            // CORREÇÃO: Ignorar eventos de repeat para teclas de navegação
            // Quando usuário segura uma tecla, Android gera KEY_DOWN com repeatCount > 0
            // Queremos processar apenas o primeiro evento (repeatCount = 0)
            if (event.repeatCount > 0) {
                Log.d(
                        TAG,
                        "[KEY_DOWN-IGNORED] keyCode=$keyCode (repeat=${event.repeatCount}) - BLOQUEADO"
                )
                return true // Consumir mas não processar repeat
            }
            // V4.5: GLOBAL_STATE_LOCK (static companion) - Garante thread-safety REAL
            // CRITICAL: Lock DEVE ser companion object para funcionar entre múltiplas instâncias
            // TODO o código de decisão DEVE estar dentro do withLock
            // Usar return@withLock (não return!) para manter lock ativo
            val shouldNavigate =
                    GLOBAL_STATE_LOCK.withLock {
                        val state = pressCycleStates.getOrPut(keyCode) { PressCycleState() }

                        // V4.2: Event Deduplication - RETORNA FALSE se duplicado (não navega)
                        if (event.eventTime == state.lastProcessedEventTime) {
                            android.util.Log.d(
                                    TAG,
                                    "[DUPLICATE-EVENT] keyCode=$keyCode, eventTime=${event.eventTime}, SKIPPED"
                            )
                            return@withLock false // ← Retorna do LAMBDA, não do método!
                        }

                        // Marcar evento como processado
                        state.lastProcessedEventTime = event.eventTime

                        // Calcular timeout desde ÚLTIMO KEY_DOWN
                        val timeSinceLastDown = currentTime - state.lastDownTime

                        // Detectar novo ciclo: timeout >200ms entre KEY_DOWN events
                        val isNewCycle = timeSinceLastDown > PRESS_CYCLE_TIMEOUT_MS

                        if (isNewCycle) {
                            // NOVO CICLO - reseta flag
                            state.hasNavigatedInCycle = false
                            android.util.Log.d(
                                    TAG,
                                    "[CYCLE-START] keyCode=$keyCode, timeSinceLastDown=${timeSinceLastDown}ms → NEW CYCLE"
                            )
                        }

                        // Atualizar timestamp do KEY_DOWN
                        state.lastDownTime = currentTime

                        // Decidir se permite navegação (DENTRO do withLock!)
                        val allowNav = !state.hasNavigatedInCycle
                        if (allowNav) {
                            state.hasNavigatedInCycle = true
                            android.util.Log.d(
                                    TAG,
                                    "[NAV-ALLOW] keyCode=$keyCode, cycle=${if (isNewCycle) "new" else "same"}, action=NAVIGATE"
                            )
                        } else {
                            android.util.Log.d(
                                    TAG,
                                    "[NAV-BLOCK] keyCode=$keyCode, cycle=same, timeSinceLastDown=${timeSinceLastDown}ms, reason=already_navigated"
                            )
                        }
                        allowNav // Retorna decisão do withLock block
                    }

            // Se bloqueado (false), retorna true (consumido mas não navegou)
            if (!shouldNavigate) {
                return true
            }

            // Processar navegação APENAS nesta transição
            val navigationEvent =
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> {
                            android.util.Log.d(TAG, "[KEY_DOWN] Arrow UP - navigating up")
                            NavigationEvent.Navigate(
                                    direction = Direction.UP,
                                    inputSource = InputSource.KEYBOARD
                            )
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> {
                            android.util.Log.d(TAG, "[KEY_DOWN] Arrow DOWN - navigating down")
                            NavigationEvent.Navigate(
                                    direction = Direction.DOWN,
                                    inputSource = InputSource.KEYBOARD
                            )
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                            android.util.Log.d(TAG, "[KEY_DOWN] Arrow LEFT - navigating left")
                            NavigationEvent.Navigate(
                                    direction = Direction.LEFT,
                                    inputSource = InputSource.KEYBOARD
                            )
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                            android.util.Log.d(TAG, "[KEY_DOWN] Arrow RIGHT - navigating right")
                            NavigationEvent.Navigate(
                                    direction = Direction.RIGHT,
                                    inputSource = InputSource.KEYBOARD
                            )
                        }
                        else -> null
                    }

            // Enviar evento e retornar
            if (navigationEvent != null) {
                navigationController.handleNavigationEvent(navigationEvent)
                return true
            }
        }

        // PHASE 4.2a: Ignorar eventos de repeat para teclas de ação
        if (!isNavigationKey && event.repeatCount > 0) {
            android.util.Log.d(
                    TAG,
                    "[KEY_DOWN] Ignoring action key repeat for keyCode=$keyCode (repeat=${event.repeatCount})"
            )
            return true // Consumir mas não processar repeat
        }

        // PHASE 4.2a: Suporte para F12 (toggle menu)
        if (keyCode == KeyEvent.KEYCODE_F12) {
            android.util.Log.d(TAG, "[KEY_DOWN] F12 pressed - toggling menu")
            // Se menu está aberto, fechar tudo
            if (isMenuOpenCallback()) {
                android.util.Log.d(TAG, "[KEY_DOWN] Menu is open - closing all menus")
                navigationController.handleNavigationEvent(
                        NavigationEvent.CloseAllMenus(
                                keyCode = keyCode,
                                inputSource = InputSource.KEYBOARD
                        )
                )
            } else {
                // Se menu está fechado, abrir
                android.util.Log.d(TAG, "[KEY_DOWN] Menu is closed - opening menu")
                navigationController.handleNavigationEvent(
                        NavigationEvent.OpenMenu(inputSource = InputSource.KEYBOARD)
                )
            }
            return true
        }

        // Traduzir KeyEvent em NavigationEvent (para teclas de ação)
        val actionEvent =
                when (keyCode) {
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_SPACE -> {
                        android.util.Log.d(
                                TAG,
                                "[KEY_DOWN] Enter/Space pressed - activating selected"
                        )
                        NavigationEvent.ActivateSelected(
                                keyCode = keyCode,
                                inputSource = InputSource.KEYBOARD
                        )
                    }
                    KeyEvent.KEYCODE_ESCAPE -> {
                        android.util.Log.d(
                                TAG,
                                "[KEY_DOWN] Escape pressed - closing all menus (EXIT)"
                        )
                        NavigationEvent.CloseAllMenus(
                                keyCode = keyCode,
                                inputSource = InputSource.KEYBOARD
                        )
                    }
                    KeyEvent.KEYCODE_DEL -> {
                        // FIX ERRO 1: Registrar timestamp do KEY_DOWN para validar KEY_UP futuro
                        actionKeyDownTimestamps[keyCode] = System.currentTimeMillis()

                        android.util.Log.d(TAG, "[KEY_DOWN] Backspace pressed - navigating back")
                        NavigationEvent.NavigateBack(
                                keyCode = keyCode,
                                inputSource = InputSource.KEYBOARD
                        )
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        android.util.Log.d(TAG, "[KEY_DOWN] Back pressed - navigating back")
                        NavigationEvent.NavigateBack(
                                keyCode = keyCode,
                                inputSource = InputSource.KEYBOARD
                        )
                    }
                    else -> {
                        // Tecla não mapeada - não consumir
                        android.util.Log.d(TAG, "[KEY_DOWN] Unmapped key: $keyCode")
                        return false
                    }
                }

        // Enviar evento para o NavigationController
        navigationController.handleNavigationEvent(actionEvent)
        return true // Evento consumido
    }

    /**
     * Processa um evento de tecla solta.
     *
     * PHASE 4.6: Press Cycle Reset - KEY_UP reseta o ciclo IMEDIATAMENTE
     *
     * Quando usuário solta a tecla (KEY_UP), resetamos `hasNavigatedInCycle = false`, permitindo
     * que o próximo KEY_DOWN navegue novamente, independente do tempo decorrido.
     *
     * Isso permite navegação rápida (3+ toques/segundo) enquanto mantém single-trigger ao segurar:
     * - Toques rápidos: DOWN → UP (reset) → DOWN → UP (reset) = múltiplas navegações ✓
     * - Segurar: DOWN → (repeat DOWN bloqueados, sem UP) = 1 navegação ✓
     *
     * V4.2: Também implementa deduplicação de eventos KEY_UP V4.6: Reseta hasNavigatedInCycle no
     * KEY_UP (dentro do GLOBAL_STATE_LOCK) V4.7: Processa teclas de ação em KEY_UP se KEY_DOWN não
     * foi recebido (fallback)
     *
     * @param keyCode Código da tecla (KeyEvent.KEYCODE_*)
     * @param event Evento completo (para deduplicação)
     * @return true se o evento foi consumido, false caso contrário
     */
    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val currentTime = System.currentTimeMillis()

        // Identificar teclas de navegação
        val isNavigationKey =
                keyCode in
                        listOf(
                                KeyEvent.KEYCODE_DPAD_UP,
                                KeyEvent.KEYCODE_W,
                                KeyEvent.KEYCODE_DPAD_DOWN,
                                KeyEvent.KEYCODE_S,
                                KeyEvent.KEYCODE_DPAD_LEFT,
                                KeyEvent.KEYCODE_A,
                                KeyEvent.KEYCODE_DPAD_RIGHT,
                                KeyEvent.KEYCODE_D
                        )

        if (isNavigationKey) {
            // V4.2: Event Deduplication + V4.6: Cycle Reset
            GLOBAL_STATE_LOCK.withLock {
                val state = pressCycleStates.getOrPut(keyCode) { PressCycleState() }

                if (event.eventTime == state.lastProcessedEventTime) {
                    // EVENTO UP DUPLICADO - ignora silenciosamente
                    android.util.Log.d(
                            TAG,
                            "[DUPLICATE-UP] keyCode=$keyCode, eventTime=${event.eventTime}, SKIPPED"
                    )
                    return true
                }

                // Marcar UP como processado
                state.lastProcessedEventTime = event.eventTime

                // V4.6: KEY_UP RESETA o ciclo explicitamente
                // CRÍTICO: Permite navegação rápida (3+ toques/segundo)
                // Distingue: segurar (sem KEY_UP) vs toques rápidos (com KEY_UP)
                val timeSinceLastDown = currentTime - state.lastDownTime

                // Resetar flag APENAS se KEY_UP veio relativamente rápido após KEY_DOWN
                // Isso previne resetar flags de outros ciclos ativos
                if (timeSinceLastDown < PRESS_CYCLE_TIMEOUT_MS) {
                    state.hasNavigatedInCycle = false
                    android.util.Log.d(
                            TAG,
                            "[KEY_UP-RESET] keyCode=$keyCode, timeSinceLastDown=${timeSinceLastDown}ms → CYCLE RESET"
                    )
                } else {
                    android.util.Log.d(
                            TAG,
                            "[KEY_UP] keyCode=$keyCode, timeSinceLastDown=${timeSinceLastDown}ms (no reset)"
                    )
                }

                // Atualizar timestamp do UP
                state.lastUpTime = currentTime
            }

            return true // Consumir evento
        }

        // V4.7: FALLBACK para teclas de ação quando KEY_DOWN não foi recebido
        // Alguns teclados/sistemas enviam apenas KEY_UP para certas teclas (ex: Backspace)
        // Processamos as ações aqui como fallback
        val currentTimeForActions = System.currentTimeMillis()

        // FIX ERRO 1: Validar se este KEY_UP corresponde a um KEY_DOWN recente
        val keyDownTime = actionKeyDownTimestamps[keyCode]
        if (keyDownTime != null && (currentTimeForActions - keyDownTime) <= KEY_UP_TIMEOUT_MS) {
            // KEY_UP válido - limpar timestamp
            actionKeyDownTimestamps.remove(keyCode)
            android.util.Log.d(TAG, "[KEY_UP] Backspace released (matched KEY_DOWN) - processing")
            // Não processar - já foi processado no KEY_DOWN
            return true
        } else if (keyDownTime != null) {
            // KEY_UP órfão (timeout excedido)
            android.util.Log.w(
                    TAG,
                    "🚨 ORPHAN KEY_UP detected for Backspace - timeout exceeded (${currentTimeForActions - keyDownTime}ms)"
            )
            actionKeyDownTimestamps.remove(keyCode)
            return true // Discard orphan
        }

        // Fallback: KEY_UP sem KEY_DOWN registrado (modo legacy)
        val actionEvent =
                when (keyCode) {
                    KeyEvent.KEYCODE_DEL -> {
                        android.util.Log.d(
                                TAG,
                                "[KEY_UP-FALLBACK] Backspace released - navigating back"
                        )
                        NavigationEvent.NavigateBack(
                                keyCode = keyCode,
                                inputSource = InputSource.KEYBOARD
                        )
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        android.util.Log.d(TAG, "[KEY_UP-FALLBACK] Back released - navigating back")
                        NavigationEvent.NavigateBack(
                                keyCode = keyCode,
                                inputSource = InputSource.KEYBOARD
                        )
                    }
                    KeyEvent.KEYCODE_ESCAPE -> {
                        android.util.Log.d(
                                TAG,
                                "[KEY_UP-FALLBACK] Escape released - closing all menus"
                        )
                        NavigationEvent.CloseAllMenus(
                                keyCode = keyCode,
                                inputSource = InputSource.KEYBOARD
                        )
                    }
                    else -> {
                        // Tecla não é de ação - não processar
                        return false
                    }
                }

        // Enviar evento para o NavigationController
        navigationController.handleNavigationEvent(actionEvent)
        return true // Evento consumido
    }

    /**
     * Verifica se uma tecla é suportada para navegação.
     *
     * Útil para decidir se devemos interceptar a tecla antes de chegar ao jogo.
     *
     * @param keyCode Código da tecla a verificar
     * @return true se a tecla é usada para navegação
     */
    fun isNavigationKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_W,
            KeyEvent.KEYCODE_A,
            KeyEvent.KEYCODE_S,
            KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_ESCAPE,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_DEL, // PHASE 4.2a: Backspace (back navigation)
            KeyEvent.KEYCODE_F12 -> true // PHASE 4.2a: F12 (toggle menu)
            else -> false
        }
    }
}
