package com.vinaooo.revenger.ui.retromenu3.navigation.adapters

import android.view.KeyEvent
import com.vinaooo.revenger.ui.retromenu3.navigation.Direction
import com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
import com.vinaooo.revenger.ui.retromenu3.navigation.InputTranslator
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent

/**
 * Adaptador para entrada de teclado físico.
 * 
 * Suporta dois modos:
 * 
 * **MODO 1: NAVEGAÇÃO DE MENU** (atual):
 * - Arrow keys → Navegação (UP/DOWN)
 * - Enter → Confirmar seleção
 * - Escape → Voltar
 * - 1-9 → Salto rápido para item (quick jump)
 * 
 * **MODO 2: GAMEPLAY** (futuro):
 * - Arrow keys → D-Pad do jogo
 * - Z → Botão A
 * - X → Botão B
 * - etc.
 * 
 * O modo é controlado externamente baseado no contexto
 * (menu visível = MODO 1, jogo ativo = MODO 2).
 */
class KeyboardInputAdapter(
    private var isInMenuMode: Boolean = true
) : InputTranslator {
    
    override fun translate(input: Any): List<NavigationEvent> {
        if (input !is KeyEvent) {
            return emptyList()
        }
        
        // Só processa ACTION_DOWN
        if (input.action != KeyEvent.ACTION_DOWN) {
            return emptyList()
        }
        
        // Em modo de jogo, não traduz para NavigationEvents
        // (inputs vão direto para o emulador)
        if (!isInMenuMode) {
            return emptyList()
        }
        
        val event = when (input.keyCode) {
            // Navegação com setas
            KeyEvent.KEYCODE_DPAD_UP -> {
                NavigationEvent.Navigate(
                    direction = Direction.UP,
                    inputSource = InputSource.KEYBOARD
                )
            }
            
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                NavigationEvent.Navigate(
                    direction = Direction.DOWN,
                    inputSource = InputSource.KEYBOARD
                )
            }
            
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                NavigationEvent.Navigate(
                    direction = Direction.LEFT,
                    inputSource = InputSource.KEYBOARD
                )
            }
            
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                NavigationEvent.Navigate(
                    direction = Direction.RIGHT,
                    inputSource = InputSource.KEYBOARD
                )
            }
            
            // Confirmar com Enter
            KeyEvent.KEYCODE_ENTER -> {
                NavigationEvent.ActivateSelected(
                    inputSource = InputSource.KEYBOARD
                )
            }
            
            // Voltar com Escape
            KeyEvent.KEYCODE_ESCAPE -> {
                NavigationEvent.NavigateBack(
                    inputSource = InputSource.KEYBOARD
                )
            }
            
            // Quick jump com números 1-9
            KeyEvent.KEYCODE_1 -> createQuickJumpEvent(0)
            KeyEvent.KEYCODE_2 -> createQuickJumpEvent(1)
            KeyEvent.KEYCODE_3 -> createQuickJumpEvent(2)
            KeyEvent.KEYCODE_4 -> createQuickJumpEvent(3)
            KeyEvent.KEYCODE_5 -> createQuickJumpEvent(4)
            KeyEvent.KEYCODE_6 -> createQuickJumpEvent(5)
            KeyEvent.KEYCODE_7 -> createQuickJumpEvent(6)
            KeyEvent.KEYCODE_8 -> createQuickJumpEvent(7)
            KeyEvent.KEYCODE_9 -> createQuickJumpEvent(8)
            
            else -> null
        }
        
        return if (event != null) listOf(event) else emptyList()
    }
    
    /**
     * Cria evento de salto rápido para um índice específico.
     * 
     * Quick jump permite ir direto para um item pressionando seu número.
     * Útil para menus com muitos itens.
     */
    private fun createQuickJumpEvent(index: Int): NavigationEvent {
        return NavigationEvent.SelectItem(
            index = index,
            inputSource = InputSource.KEYBOARD
        )
    }
    
    /**
     * Define o modo de operação do adaptador.
     * 
     * @param menuMode true para modo menu, false para modo gameplay
     */
    fun setMenuMode(menuMode: Boolean) {
        isInMenuMode = menuMode
        android.util.Log.d(
            TAG,
            "Keyboard mode: ${if (menuMode) "MENU" else "GAMEPLAY"}"
        )
    }
}

private const val TAG = "KeyboardInputAdapter"
