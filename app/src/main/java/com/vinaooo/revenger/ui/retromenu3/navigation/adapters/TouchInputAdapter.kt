package com.vinaooo.revenger.ui.retromenu3.navigation.adapters

import android.view.View
import com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
import com.vinaooo.revenger.ui.retromenu3.navigation.InputTranslator
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Adaptador para entrada por toque (touch).
 * 
 * Traduz toques em views (RetroCardView) em NavigationEvents.
 * 
 * Comportamento especial para touch:
 * 1. Toque em um item → SelectItem (atualiza highlight)
 * 2. Aguarda 100ms (delay imperceptível, provê feedback visual)
 * 3. → ActivateSelected (executa ação)
 * 
 * Este comportamento satisfaz:
 * - Requisito de "focus antes de ativar"
 * - Sensação de resposta instantânea para usuário
 * - Atualização de estado para próxima navegação por gamepad
 * 
 * @property viewIndexMapper Função que mapeia view ID para índice do item no menu
 */
class TouchInputAdapter(
    private val viewIndexMapper: (View) -> Int
) : InputTranslator {
    
    /**
     * Delay entre SelectItem e ActivateSelected.
     * 
     * 100ms é imperceptível para o usuário mas fornece tempo
     * suficiente para o highlight aparecer antes da ação.
     */
    private val focusThenActivateDelayMs = 100L
    
    override fun translate(input: Any): List<NavigationEvent> {
        if (input !is View) {
            return emptyList()
        }
        
        val index = viewIndexMapper(input)
        
        // Touch gera dois eventos:
        // 1. SelectItem (imediato)
        val selectEvent = NavigationEvent.SelectItem(
            index = index,
            inputSource = InputSource.TOUCH
        )
        
        // 2. ActivateSelected (após delay)
        // Implementado via callback pois não podemos bloquear aqui
        scheduleActivateAfterDelay(selectEvent.timestamp)
        
        return listOf(selectEvent)
    }
    
    /**
     * Agenda o evento ActivateSelected após o delay.
     * 
     * Usa coroutine para não bloquear a thread de UI.
     * 
     * @param selectTimestamp Timestamp do SelectItem para manter ordem correta
     */
    private fun scheduleActivateAfterDelay(selectTimestamp: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(focusThenActivateDelayMs)
            
            // Callback será chamado externamente
            // Por enquanto apenas loga
            android.util.Log.d(
                TAG,
                "Touch: Ready to activate after ${focusThenActivateDelayMs}ms delay"
            )
        }
    }
    
    /**
     * Cria evento de ativação (chamado após delay).
     * 
     * Separado do translate() para permitir controle externo do timing.
     */
    fun createActivateEvent(): NavigationEvent {
        return NavigationEvent.ActivateSelected(
            inputSource = InputSource.TOUCH
        )
    }
    
    companion object {
        private const val TAG = "TouchInputAdapter"
    }
}
