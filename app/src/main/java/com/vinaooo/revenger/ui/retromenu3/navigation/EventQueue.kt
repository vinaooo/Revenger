package com.vinaooo.revenger.ui.retromenu3.navigation

import android.util.Log
import java.util.LinkedList

/**
 * Fila de eventos com debouncing para prevenir ações duplas e conflitos entre múltiplas fontes de
 * entrada.
 *
 * O debouncing funciona ignorando eventos que chegam muito rápido após outro evento (dentro da
 * janela de debounce). Isso previne:
 * - Duplo-clique acidental
 * - Touch + gamepad simultâneos
 * - Taxa de repetição do teclado causando overshoot
 * - Condições de corrida entre inputs
 *
 * Exemplo:
 * ```
 * Event 1: Touch em Item 3 (timestamp: 100ms) → Processado
 * Event 2: Gamepad A (timestamp: 102ms) → Ignorado (dentro de 200ms)
 * Event 3: DPAD Down (timestamp: 350ms) → Processado (fora da janela)
 * ```
 *
 * @property debounceWindowMs Janela de debounce em milissegundos (padrão 200ms)
 */
class EventQueue(private val debounceWindowMs: Long = 200) {
    private val queue = LinkedList<NavigationEvent>()
    private var lastProcessedTimestamp: Long = 0

    /**
     * Adiciona um evento à fila, aplicando debouncing.
     *
     * @param event Evento a ser enfileirado
     * @return true se o evento foi adicionado, false se foi debounced (ignorado)
     */
    @Synchronized
    fun enqueue(event: NavigationEvent): Boolean {
        if (shouldDebounce(event)) {
            Log.d(
                    TAG,
                    "Debounced: ${event.javaClass.simpleName} from ${event.inputSource} " +
                            "(${event.timestamp - lastProcessedTimestamp}ms since last event)"
            )
            return false
        }

        queue.add(event)
        Log.d(
                TAG,
                "Enqueued: ${event.javaClass.simpleName} from ${event.inputSource} " +
                        "(queue size: ${queue.size})"
        )
        return true
    }

    /**
     * Remove e retorna o próximo evento da fila.
     *
     * @return Próximo evento, ou null se a fila estiver vazia
     */
    @Synchronized
    fun dequeue(): NavigationEvent? {
        val event =
                if (queue.isNotEmpty()) {
                    queue.removeFirst()
                } else {
                    null
                }

        if (event != null) {
            lastProcessedTimestamp = event.timestamp
            Log.d(
                    TAG,
                    "Dequeued: ${event.javaClass.simpleName} from ${event.inputSource} " +
                            "(queue size: ${queue.size})"
            )
        }

        return event
    }

    /**
     * Verifica o próximo evento sem removê-lo da fila.
     *
     * @return Próximo evento, ou null se a fila estiver vazia
     */
    @Synchronized fun peek(): NavigationEvent? = queue.firstOrNull()

    /** Verifica se a fila está vazia. */
    @Synchronized fun isEmpty(): Boolean = queue.isEmpty()

    /** Retorna o tamanho atual da fila. */
    @Synchronized fun size(): Int = queue.size

    /**
     * Limpa todos os eventos da fila.
     *
     * Útil quando o menu fecha ou quando queremos descartar eventos pendentes.
     */
    @Synchronized
    fun clear() {
        val size = queue.size
        queue.clear()
        if (size > 0) {
            Log.d(TAG, "Cleared $size pending events")
        }
    }

    /**
     * Verifica se um evento deve ser ignorado por debouncing.
     *
     * Usa debounce adaptativo:
     * - Navigate (UP/DOWN): 30ms - ultra-responsivo (~33 eventos/seg)
     * - Activate/Select: 200ms - previne duplo-clique acidental
     *
     * 30ms = 1 frame @ 30 FPS, maior que hardware bounce (<10ms), permite navegação rápida sem
     * pular itens.
     *
     * @param event Evento a verificar
     * @return true se o evento deve ser ignorado, false se deve ser processado
     */
    private fun shouldDebounce(event: NavigationEvent): Boolean {
        // Primeiro evento sempre é processado
        if (lastProcessedTimestamp == 0L) {
            return false
        }

        val timeSinceLastEvent = event.timestamp - lastProcessedTimestamp

        // Debounce adaptativo baseado no tipo de evento
        val effectiveDebounceWindow =
                when (event) {
                    is NavigationEvent.Navigate ->
                            30L // Ultra-responsivo (30ms = ~33 FPS frame time)
                    is NavigationEvent.ActivateSelected, is NavigationEvent.SelectItem ->
                            debounceWindowMs // Ativação com debounce maior
                    is NavigationEvent.NavigateBack,
                    is NavigationEvent.OpenMenu,
                    is NavigationEvent.CloseAllMenus -> debounceWindowMs
                }

        // Se passou tempo suficiente, não faz debounce
        if (timeSinceLastEvent >= effectiveDebounceWindow) {
            return false
        }

        // Evento muito próximo ao anterior, ignora
        return true
    }

    /**
     * Reseta o timestamp do último evento processado.
     *
     * Útil quando queremos garantir que o próximo evento não sofra debounce (ex: após abrir menu).
     */
    @Synchronized
    fun resetDebounceWindow() {
        lastProcessedTimestamp = 0
        Log.d(TAG, "Debounce window reset")
    }

    /** Retorna estatísticas da fila para debugging. */
    fun getStats(): QueueStats {
        return QueueStats(
                queueSize = queue.size,
                lastProcessedTimestamp = lastProcessedTimestamp,
                debounceWindowMs = debounceWindowMs
        )
    }

    companion object {
        private const val TAG = "EventQueue"
    }
}

/** Estatísticas da fila de eventos para debugging. */
data class QueueStats(
        val queueSize: Int,
        val lastProcessedTimestamp: Long,
        val debounceWindowMs: Long
)
