package com.vinaooo.revenger.retromenu2

import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.event.Event
import kotlin.math.abs

/**
 * ControllerInput2
 *
 * Handler de input simplificado para RetroMenu2. Design Philosophy: Apenas detecta inputs e
 * notifica callbacks - sem lógica de estado complexa.
 *
 * Responsabilidades:
 * - Detectar combo SELECT+START para abrir menu
 * - Detectar navegação DPAD/Analog (UP/DOWN) com sistema single-trigger
 * - Detectar ações de confirmação/cancelamento (A/B)
 * - Bloquear inputs quando menu estiver aberto
 * - Bloquear START por 500ms após fechar menu
 *
 * Não responsável por:
 * - Gerenciar estado do emulador (frameSpeed, saves, etc) - isso é job do ViewModel
 * - Renderizar UI - isso é job do Fragment
 */
class ControllerInput2(private val config: RetroMenu2Config) {

    companion object {
        private const val TAG = "ControllerInput2"
    }

    // ============================================================
    // STATE TRACKING
    // ============================================================

    /** Menu está aberto no momento */
    var isMenuOpen = false
        private set

    /** START está bloqueado (após fechar menu) */
    private var isStartBlocked = false

    /** Timestamp do último bloqueio de START (para calcular 500ms) */
    private var startBlockedUntil = 0L

    /** Teclas pressionadas no momento */
    private val pressedKeys = mutableSetOf<Int>()

    /** Última direção do analog (para single-trigger) */
    private var lastAnalogDirection: AnalogDirection = AnalogDirection.NONE

    // ============================================================
    // CALLBACKS
    // ============================================================

    /** Callback para quando SELECT+START for detectado (abrir menu) */
    var onMenuOpenRequested: (() -> Unit)? = null

    /** Callback para navegação UP no menu */
    var onNavigateUp: (() -> Unit)? = null

    /** Callback para navegação DOWN no menu */
    var onNavigateDown: (() -> Unit)? = null

    /** Callback para ação de confirmar (A ou B dependendo de config) */
    var onConfirm: (() -> Unit)? = null

    /** Callback para ação de cancelar (B ou A dependendo de config) */
    var onCancel: (() -> Unit)? = null

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    /** Notifica que o menu foi aberto. Bloqueia todos os inputs para o core do emulador. */
    fun menuOpened() {
        isMenuOpen = true
        pressedKeys.clear()
        lastAnalogDirection = AnalogDirection.NONE
        Log.d(TAG, "Menu aberto - inputs bloqueados para core")
    }

    /** Notifica que o menu foi fechado. Inicia bloqueio temporário de 500ms para o botão START. */
    fun menuClosed() {
        isMenuOpen = false
        pressedKeys.clear()
        lastAnalogDirection = AnalogDirection.NONE

        // Bloquear START por 500ms
        isStartBlocked = true
        startBlockedUntil = System.currentTimeMillis() + config.startBlockDuration
        Log.d(TAG, "Menu fechado - START bloqueado por ${config.startBlockDuration}ms")
    }

    /**
     * Processa KeyEvent do controller. Retorna true se o evento deve ser bloqueado (não passar para
     * o core).
     */
    fun processKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        Log.d(TAG, "processKeyEvent - keyCode: $keyCode, action: ${event.action}, isMenuOpen: $isMenuOpen")
        
        // Menu aberto = bloquear todos os eventos para o core
        if (isMenuOpen) {
            processMenuInput(keyCode, event)
            return true // Bloquear
        }

        // Menu fechado = detectar combo SELECT+START
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                pressedKeys.add(keyCode)

                // Detectar SELECT+START combo
                if (pressedKeys.contains(KeyEvent.KEYCODE_BUTTON_SELECT) &&
                                pressedKeys.contains(KeyEvent.KEYCODE_BUTTON_START)
                ) {
                    Log.d(TAG, "SELECT+START detectado - abrindo menu")
                    onMenuOpenRequested?.invoke()
                    return true // Bloquear combo
                }

                // Bloquear START se estiver no período de bloqueio
                if (keyCode == KeyEvent.KEYCODE_BUTTON_START) {
                    if (isStartBlocked) {
                        val now = System.currentTimeMillis()
                        if (now < startBlockedUntil) {
                            Log.d(TAG, "START bloqueado (${startBlockedUntil - now}ms restantes)")
                            return true // Bloquear
                        } else {
                            // Período expirou
                            isStartBlocked = false
                            Log.d(TAG, "Bloqueio de START expirado")
                        }
                    }
                }
            }
            KeyEvent.ACTION_UP -> {
                pressedKeys.remove(keyCode)
            }
        }

        return false // Não bloquear (passar para core)
    }

    /** Processa MotionEvent do analog stick. Retorna true se o evento deve ser bloqueado. */
    fun processMotionEvent(event: MotionEvent): Boolean {
        if (!isMenuOpen) {
            return false // Menu fechado = não bloquear
        }

        // Menu aberto = processar navegação analog
        // Suporta AXIS_X/Y (analog stick) E AXIS_HAT_X/HAT_Y (DPAD físico mapeado como HAT)
        val x = event.getAxisValue(MotionEvent.AXIS_X)
        val y = event.getAxisValue(MotionEvent.AXIS_Y)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        // Usar o eixo com maior magnitude (suporta DPAD E analog ao mesmo tempo)
        val effectiveX = if (abs(hatX) > abs(x)) hatX else x
        val effectiveY = if (abs(hatY) > abs(y)) hatY else y

        Log.d(TAG, "MotionEvent - x: $x, y: $y, hatX: $hatX, hatY: $hatY -> effectiveY: $effectiveY")

        val currentDirection =
                when {
                    effectiveY < -config.analogThreshold -> AnalogDirection.UP
                    effectiveY > config.analogThreshold -> AnalogDirection.DOWN
                    else -> AnalogDirection.NONE
                }

        // Single-trigger: só dispara quando direção muda
        if (currentDirection != lastAnalogDirection) {
            when (currentDirection) {
                AnalogDirection.UP -> {
                    Log.d(TAG, "✅ Analog/HAT UP detectado")
                    onNavigateUp?.invoke()
                }
                AnalogDirection.DOWN -> {
                    Log.d(TAG, "✅ Analog/HAT DOWN detectado")
                    onNavigateDown?.invoke()
                }
                AnalogDirection.NONE -> {
                    // Stick/HAT voltou ao centro
                }
            }
            lastAnalogDirection = currentDirection
        }

        return true // Bloquear (menu aberto)
    }

    /** Processa Event do RadialGamePad. Retorna true se o evento deve ser bloqueado. */
    fun processRadialEvent(event: Event): Boolean {
        if (!isMenuOpen) {
            return false // Menu fechado = não bloquear
        }

        // Menu aberto = processar navegação por D-Pad virtual
        when (event) {
            is Event.Direction -> {
                // D-Pad events vem como Direction com eixos
                // UP = yAxis < 0, DOWN = yAxis > 0
                when (event.id) {
                    GLRetroView.MOTION_SOURCE_DPAD -> {
                        val direction =
                                when {
                                    event.yAxis < -0.5f -> AnalogDirection.UP
                                    event.yAxis > 0.5f -> AnalogDirection.DOWN
                                    else -> AnalogDirection.NONE
                                }

                        // Single-trigger
                        if (direction != lastAnalogDirection) {
                            when (direction) {
                                AnalogDirection.UP -> {
                                    Log.d(TAG, "DPAD UP detectado (RadialGamePad)")
                                    onNavigateUp?.invoke()
                                }
                                AnalogDirection.DOWN -> {
                                    Log.d(TAG, "DPAD DOWN detectado (RadialGamePad)")
                                    onNavigateDown?.invoke()
                                }
                                AnalogDirection.NONE -> {
                                    // Direção voltou ao centro
                                }
                            }
                            lastAnalogDirection = direction
                        }
                    }
                // LEFT/RIGHT são ignorados (bloqueados)
                }
            }
            is Event.Button -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    processMenuButton(event.id)
                }
            }
            else -> {
                // Outros eventos ignorados
            }
        }

        return true // Bloquear (menu aberto)
    }

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    /** Processa input de teclado quando menu está aberto. */
    private fun processMenuInput(keyCode: Int, event: KeyEvent) {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return // Só processar ACTION_DOWN
        }

        Log.d(TAG, "processMenuInput - keyCode: $keyCode (${KeyEvent.keyCodeToString(keyCode)})")
        
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                Log.d(TAG, "✅ KEYCODE_DPAD_UP detectado")
                onNavigateUp?.invoke()
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                Log.d(TAG, "✅ KEYCODE_DPAD_DOWN detectado")
                onNavigateDown?.invoke()
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                Log.d(TAG, "🔒 DPAD LEFT ignorado (sem ação horizontal no menu)")
                // Ignorar LEFT no menu (sem navegação horizontal)
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                Log.d(TAG, "🔒 DPAD RIGHT ignorado (sem ação horizontal no menu)")
                // Ignorar RIGHT no menu (sem navegação horizontal)
            }
            else -> {
                Log.d(TAG, "Tentando processar como botão de ação...")
                // Processar botões de ação (A/B)
                processMenuButton(keyCode)
            }
        }
    }

    /** Processa botões de ação (A/B) respeitando configuração de swap. */
    private fun processMenuButton(keyCode: Int) {
        val isAButton = keyCode == KeyEvent.KEYCODE_BUTTON_A
        val isBButton = keyCode == KeyEvent.KEYCODE_BUTTON_B

        if (!isAButton && !isBButton) {
            return // Não é um botão de ação
        }

        // Determinar ação baseado em config.swapAB
        val isConfirmButton =
                if (config.swapAB) {
                    isBButton // B = Confirm (Xbox/SEGA style)
                } else {
                    isAButton // A = Confirm (Nintendo style)
                }

        if (isConfirmButton) {
            Log.d(TAG, "Botão CONFIRMAR pressionado")
            onConfirm?.invoke()
        } else {
            Log.d(TAG, "Botão CANCELAR pressionado")
            onCancel?.invoke()
        }
    }

    // ============================================================
    // HELPER ENUM
    // ============================================================

    private enum class AnalogDirection {
        UP,
        DOWN,
        NONE
    }
}
