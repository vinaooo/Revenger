package com.vinaooo.revenger.gamepad

import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner

/**
 * Gerenciador híbrido que escolhe entre VirtualJoystick e RadialGamePad Integra automaticamente
 * baseado na configuração da ROM
 * @author vinaooo
 * @date 22 de Setembro de 2025 - ETAPA 5
 */
class VirtualGamePadManager private constructor() {

    companion object {
        private const val TAG = "VirtualGamePadManager"

        @Volatile private var instance: VirtualGamePadManager? = null

        fun getInstance(): VirtualGamePadManager {
            return instance
                    ?: synchronized(this) {
                        instance ?: VirtualGamePadManager().also { instance = it }
                    }
        }
    }

    // Modo de operação
    enum class GamePadMode {
        VIRTUAL_JOYSTICK, // Usa nossa implementação customizada
        RADIAL_GAMEPAD, // Usa implementação original
        AUTO_DETECT // Detecta automaticamente por ROM
    }

    // Estado atual
    private var currentMode: GamePadMode = GamePadMode.AUTO_DETECT

    // Instâncias dos GamePads
    private var virtualJoystickLeft: VirtualJoystickGamePad? = null
    private var radialGamePadLeft: GamePad? = null
    private var radialGamePadRight: GamePad? = null

    /** Inicializa o sistema híbrido */
    fun initialize(
            context: Context,
            lifecycleOwner: LifecycleOwner,
            resources: Resources,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout
    ): Boolean {
        Log.d(TAG, "Inicializando VirtualGamePadManager")

        try {
            // Detecta configuração atual
            val configId = resources.getString(com.vinaooo.revenger.R.string.config_id)
            val configCore = resources.getString(com.vinaooo.revenger.R.string.config_core)

            Log.d(TAG, "Config detectada - ID: $configId, Core: $configCore")

            // Determina modo baseado na configuração
            currentMode = detectBestMode(configId, configCore)
            Log.d(TAG, "Modo selecionado: $currentMode")

            // Inicializa GamePads baseado no modo
            return when (currentMode) {
                GamePadMode.VIRTUAL_JOYSTICK -> {
                    initializeVirtualJoystick(
                            context,
                            lifecycleOwner,
                            configId,
                            configCore,
                            leftContainer
                    )
                }
                GamePadMode.RADIAL_GAMEPAD -> {
                    initializeRadialGamePad(context, resources, leftContainer, rightContainer)
                }
                GamePadMode.AUTO_DETECT -> {
                    // Fallback para RadialGamePad
                    Log.w(TAG, "Auto-detecção falhou, usando RadialGamePad")
                    initializeRadialGamePad(context, resources, leftContainer, rightContainer)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro na inicialização", e)
            return false
        }
    }

    /** Detecta melhor modo baseado na configuração da ROM */
    private fun detectBestMode(configId: String, configCore: String): GamePadMode {
        // Lista de IDs/cores que funcionam melhor com VirtualJoystick
        val virtualJoystickPreferred = listOf("sak", "sth", "rrr", "loz")
        val virtualJoystickCores = listOf("genesis_plus_gx", "smsplus", "bsnes", "gambatte")

        return if (configId in virtualJoystickPreferred || configCore in virtualJoystickCores) {
            Log.d(TAG, "VirtualJoystick recomendado para $configId/$configCore")
            GamePadMode.VIRTUAL_JOYSTICK
        } else {
            Log.d(TAG, "RadialGamePad recomendado para $configId/$configCore")
            GamePadMode.RADIAL_GAMEPAD
        }
    }

    /** Inicializa VirtualJoystick customizado */
    private fun initializeVirtualJoystick(
            context: Context,
            lifecycleOwner: LifecycleOwner,
            configId: String,
            configCore: String,
            leftContainer: FrameLayout
    ): Boolean {
        Log.d(TAG, "Inicializando VirtualJoystick para $configId")

        return try {
            // Obtém configuração específica
            val config =
                    VirtualJoystickSystemConfigs.getConfigForAppId(configId)
                            ?: VirtualJoystickSystemConfigs.getConfigForCore(configCore)
                                    ?: VirtualJoystickConfig.defaultConfig()

            Log.d(TAG, "Configuração aplicada: ${config.name}")

            // Cria e configura VirtualJoystick
            virtualJoystickLeft =
                    VirtualJoystickGamePad(context, lifecycleOwner).apply {
                        configure(config)
                        attachToContainer(leftContainer)
                    }

            Log.d(TAG, "VirtualJoystick inicializado com sucesso")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar VirtualJoystick", e)
            false
        }
    }

    /** Inicializa RadialGamePad original (fallback) */
    private fun initializeRadialGamePad(
            context: Context,
            resources: Resources,
            leftContainer: FrameLayout,
            rightContainer: FrameLayout
    ): Boolean {
        Log.d(TAG, "Inicializando RadialGamePad")

        return try {
            val gamePadConfig = GamePadConfig(context, resources)

            radialGamePadLeft =
                    GamePad(context, gamePadConfig.left).also { gamePad ->
                        leftContainer.addView(gamePad.pad)
                    }

            radialGamePadRight =
                    GamePad(context, gamePadConfig.right).also { gamePad ->
                        rightContainer.addView(gamePad.pad)
                    }

            Log.d(TAG, "RadialGamePad inicializado com sucesso")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar RadialGamePad", e)
            false
        }
    }

    /** Conecta GamePads ao RetroView (simplificado para primeira integração) */
    fun connectToRetroView(retroView: Any, lifecycleOwner: LifecycleOwner) {
        when (currentMode) {
            GamePadMode.VIRTUAL_JOYSTICK -> {
                Log.d(TAG, "VirtualJoystick conectado ao RetroView")
                // Por enquanto, apenas log - implementação completa virá depois
            }
            GamePadMode.RADIAL_GAMEPAD -> {
                // RadialGamePad usa sistema tradicional
                try {
                    if (retroView is com.swordfish.libretrodroid.GLRetroView) {
                        radialGamePadLeft?.subscribe(lifecycleOwner.lifecycleScope, retroView)
                        radialGamePadRight?.subscribe(lifecycleOwner.lifecycleScope, retroView)
                        Log.d(TAG, "RadialGamePad conectado ao RetroView")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao conectar RadialGamePad", e)
                }
            }
            GamePadMode.AUTO_DETECT -> {
                Log.w(TAG, "Modo AUTO_DETECT não deveria chegar aqui")
            }
        }
    }

    /** Atualiza visibilidade dos GamePads */
    fun updateVisibility(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE

        when (currentMode) {
            GamePadMode.VIRTUAL_JOYSTICK -> {
                virtualJoystickLeft?.updateVisibility(isVisible)
                Log.d(TAG, "VirtualJoystick visibility: $isVisible")
            }
            GamePadMode.RADIAL_GAMEPAD -> {
                radialGamePadLeft?.pad?.visibility = visibility
                radialGamePadRight?.pad?.visibility = visibility
                Log.d(TAG, "RadialGamePad visibility: $isVisible")
            }
            GamePadMode.AUTO_DETECT -> {
                // Não faz nada
            }
        }
    }

    /** Força um modo específico (para testes) */
    fun forceMode(mode: GamePadMode) {
        Log.d(TAG, "Forçando modo: $mode")
        currentMode = mode
    }

    /** Obtém modo atual */
    fun getCurrentMode(): GamePadMode = currentMode

    /** Obtém informações de debug */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("VirtualGamePadManager Debug Info:")
            appendLine("Modo atual: $currentMode")
            appendLine("VirtualJoystick ativo: ${virtualJoystickLeft != null}")
            appendLine(
                    "RadialGamePad ativo: ${radialGamePadLeft != null && radialGamePadRight != null}"
            )
        }
    }

    /** Limpa recursos */
    fun cleanup() {
        virtualJoystickLeft = null
        radialGamePadLeft = null
        radialGamePadRight = null
        Log.d(TAG, "Recursos limpos")
    }
}
