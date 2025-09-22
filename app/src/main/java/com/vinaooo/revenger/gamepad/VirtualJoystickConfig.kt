package com.vinaooo.revenger.gamepad

/**
 * Configurações para o VirtualJoystickGamePad
 * @author vinaooo
 * @date 22 de Setembro de 2025
 */
data class VirtualJoystickConfig(
        val name: String,
        val leftJoystickEnabled: Boolean = true,
        val rightJoystickEnabled: Boolean = true,
        val joystickRadius: Int = 100,
        val buttonRadius: Int = 50,
        val sensitivity: Float = 1.0f,
        val autoVisibility: Boolean = true,
        val backgroundColor: Int = 0x80000000.toInt(), // Semi-transparente preto
        val borderColor: Int = 0xFFFFFFFF.toInt(), // Branco
        val knobColor: Int = 0xFF00FF00.toInt() // Verde
) {

    companion object {

        /** Configuração padrão para jogos de plataforma */
        fun defaultConfig() =
                VirtualJoystickConfig(
                        name = "Default",
                        leftJoystickEnabled = true,
                        rightJoystickEnabled = false,
                        joystickRadius = 120,
                        buttonRadius = 60,
                        sensitivity = 0.8f,
                        autoVisibility = true
                )

        /** Configuração para jogos que precisam de dual stick */
        fun dualStickConfig() =
                VirtualJoystickConfig(
                        name = "Dual Stick",
                        leftJoystickEnabled = true,
                        rightJoystickEnabled = true,
                        joystickRadius = 100,
                        buttonRadius = 50,
                        sensitivity = 1.0f,
                        autoVisibility = true
                )

        /** Configuração para jogos de corrida */
        fun racingConfig() =
                VirtualJoystickConfig(
                        name = "Racing",
                        leftJoystickEnabled = true,
                        rightJoystickEnabled = false,
                        joystickRadius = 150,
                        buttonRadius = 70,
                        sensitivity = 1.2f,
                        autoVisibility = true,
                        backgroundColor = 0x60000000,
                        borderColor = 0xFFFF4444.toInt(),
                        knobColor = 0xFFFFFF44.toInt()
                )

        /** Configuração personalizada para diferentes sistemas de emulação */
        fun customConfig(
                systemName: String,
                hasAnalogSticks: Boolean = true,
                hasDualAnalog: Boolean = false,
                customSensitivity: Float = 1.0f
        ) =
                VirtualJoystickConfig(
                        name = systemName,
                        leftJoystickEnabled = hasAnalogSticks,
                        rightJoystickEnabled = hasDualAnalog,
                        sensitivity = customSensitivity,
                        autoVisibility = true
                )
    }

    /** Valida se a configuração é válida */
    fun isValid(): Boolean {
        return joystickRadius > 0 && buttonRadius > 0 && sensitivity > 0 && name.isNotBlank()
    }

    /** Retorna uma cópia com configurações ajustadas para performance */
    fun optimizedForPerformance() =
            copy(sensitivity = (sensitivity * 0.9f).coerceAtLeast(0.1f), autoVisibility = true)
}
