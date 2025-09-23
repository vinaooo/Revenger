package com.libretro.frontend.gamepad.padkit

import android.content.Context

/**
 * Sistema universal de configuração que lê config.xml e cria configuração genérica para qualquer
 * ROM/core sem dependências específicas.
 */
class UniversalConfigManager(private val context: Context) {

    /** Configuração principal do gamepad universal */
    data class GamepadConfiguration(
            val useAnalogStick: Boolean,
            val enableHapticFeedback: Boolean,
            val buttonVisibility: Map<String, Boolean>,
            val layoutPreferences: LayoutPreferences
    )

    /** Preferências de layout configuráveis */
    data class LayoutPreferences(
            val leftSideControl: ControlType,
            val rightSideButtons: List<String>,
            val auxiliaryButtons: Map<String, Float>, // botão -> ângulo em graus
            val buttonScale: Float = 1.0f,
            val spacing: Float = 1.0f
    )

    /** Tipo de controle principal no lado esquerdo */
    enum class ControlType {
        ANALOG, // Joystick analógico
        DPAD // D-Pad digital
    }

    /** Carrega configuração universal baseada no config.xml */
    fun loadConfiguration(): GamepadConfiguration {
        return GamepadConfiguration(
                useAnalogStick = loadBooleanConfig("config_left_analog", false),
                enableHapticFeedback = loadBooleanConfig("config_haptic_feedback", true),
                buttonVisibility = loadButtonVisibility(),
                layoutPreferences = loadLayoutPreferences()
        )
    }

    /** Carrega visibilidade de todos os botões baseado no config.xml */
    private fun loadButtonVisibility(): Map<String, Boolean> {
        return mapOf(
                "button_a" to loadBooleanConfig("config_gamepad_a", true),
                "button_b" to loadBooleanConfig("config_gamepad_b", true),
                "button_x" to loadBooleanConfig("config_gamepad_x", true),
                "button_y" to loadBooleanConfig("config_gamepad_y", true),
                "button_l1" to loadBooleanConfig("config_gamepad_l1", true),
                "button_r1" to loadBooleanConfig("config_gamepad_r1", true),
                "button_l2" to loadBooleanConfig("config_gamepad_l2", false),
                "button_r2" to loadBooleanConfig("config_gamepad_r2", false),
                "button_start" to loadBooleanConfig("config_gamepad_start", true),
                "button_select" to loadBooleanConfig("config_gamepad_select", true)
        )
    }

    /** Carrega preferências de layout */
    private fun loadLayoutPreferences(): LayoutPreferences {
        val useAnalog = loadBooleanConfig("config_left_analog", false)

        return LayoutPreferences(
                leftSideControl = if (useAnalog) ControlType.ANALOG else ControlType.DPAD,
                rightSideButtons = listOf("button_r1", "button_start"),
                auxiliaryButtons = mapOf("button_l1" to -120f, "button_select" to -90f),
                buttonScale = loadFloatConfig("config_button_scale", 1.0f),
                spacing = loadFloatConfig("config_layout_spacing", 1.0f)
        )
    }

    /** Carrega configuração booleana do config.xml com valor padrão */
    private fun loadBooleanConfig(resourceName: String, defaultValue: Boolean): Boolean {
        return try {
            val resourceId =
                    context.resources.getIdentifier(resourceName, "bool", context.packageName)
            if (resourceId != 0) {
                context.resources.getBoolean(resourceId)
            } else {
                defaultValue
            }
        } catch (e: Exception) {
            defaultValue
        }
    }

    /** Carrega configuração float do config.xml com valor padrão */
    private fun loadFloatConfig(resourceName: String, defaultValue: Float): Float {
        return try {
            val resourceId =
                    context.resources.getIdentifier(resourceName, "dimen", context.packageName)
            if (resourceId != 0) {
                context.resources.getDimension(resourceId)
            } else {
                defaultValue
            }
        } catch (e: Exception) {
            defaultValue
        }
    }

    /** Obtém ID numérico para um botão específico */
    fun getButtonId(buttonName: String): Int {
        return when (buttonName) {
            "button_a" -> 0
            "button_b" -> 1
            "button_x" -> 2
            "button_y" -> 3
            "button_l1" -> 10
            "button_r1" -> 11
            "button_l2" -> 12
            "button_r2" -> 13
            "button_start" -> 20
            "button_select" -> 21
            else -> 999 // ID padrão para botões não mapeados
        }
    }
}
