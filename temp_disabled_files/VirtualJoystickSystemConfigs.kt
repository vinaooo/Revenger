package com.vinaooo.revenger.gamepad

import android.content.res.Resources
import com.vinaooo.revenger.R

/**
 * Configurações específicas do VirtualJoystick para diferentes sistemas de emulação Baseado nos
 * configs de teste disponíveis e configurações do config.xml
 * @author vinaooo
 * @date 22 de Setembro de 2025 - ATUALIZADO para respeitar config.xml
 */
object VirtualJoystickSystemConfigs {

    /** Cria configuração respeitando as configurações do config.xml */
    fun createConfigFromResources(
            resources: Resources,
            baseConfig: VirtualJoystickConfig
    ): VirtualJoystickConfig {
        return baseConfig.copy(
                // Respeitar se deve usar analógico ou digital
                leftJoystickEnabled = true, // Sempre habilitado para movimento
                rightJoystickEnabled = false, // Botões vêm do RadialGamePad

                // Sensibilidade baseada no tipo (analógico vs digital)
                sensitivity =
                        if (resources.getBoolean(R.bool.config_left_analog)) {
                            baseConfig.sensitivity * 1.2f // Mais sensível para analógico
                        } else {
                            baseConfig.sensitivity * 0.8f // Menos sensível para digital (D-Pad)
                        },

                // Vibração
                name =
                        "${baseConfig.name} ${if (resources.getBoolean(R.bool.config_left_analog)) "(Analógico)" else "(Digital)"}"
        )
    }

    /**
     * Configuração para Mega Drive/Genesis (Sonic and Knuckles) Core: genesis_plus_gx
     * Características: Jogos de plataforma com movimento preciso
     */
    fun megaDriveConfig() =
            VirtualJoystickConfig(
                    name = "Mega Drive",
                    leftJoystickEnabled = true,
                    rightJoystickEnabled = false, // Não usa dual analog
                    joystickRadius = 130,
                    buttonRadius = 55,
                    sensitivity = 1.0f, // Movimento preciso para plataforma
                    autoVisibility = true,
                    backgroundColor = 0x70000080, // Azul escuro Sonic
                    borderColor = 0xFFFFD700.toInt(), // Dourado
                    knobColor = 0xFF0066FF.toInt() // Azul Sonic
            )

    /**
     * Configuração para Super Nintendo (Rock and Roll Racing) Core: bsnes Características: Jogos de
     * corrida com controle suave
     */
    fun snesConfig() =
            VirtualJoystickConfig(
                    name = "Super Nintendo",
                    leftJoystickEnabled = true,
                    rightJoystickEnabled = false,
                    joystickRadius = 140, // Maior para controle de corrida
                    buttonRadius = 60,
                    sensitivity = 1.2f, // Mais sensível para corridas
                    autoVisibility = true,
                    backgroundColor = 0x70800080, // Roxo SNES
                    borderColor = 0xFFE6E6FA.toInt(), // Lavanda
                    knobColor = 0xFF9370DB.toInt() // Roxo médio
            )

    /**
     * Configuração para Game Boy (Legend of Zelda) Core: gambatte Características: D-Pad digital,
     * movimentos em 8 direções
     */
    fun gameBoyConfig() =
            VirtualJoystickConfig(
                    name = "Game Boy",
                    leftJoystickEnabled = true,
                    rightJoystickEnabled = false,
                    joystickRadius = 120, // Menor, simulando D-Pad
                    buttonRadius = 50,
                    sensitivity = 0.9f, // Menos sensível, mais digital
                    autoVisibility = true,
                    backgroundColor = 0x70404040, // Cinza Game Boy
                    borderColor = 0xFF9BBB59.toInt(), // Verde Game Boy
                    knobColor = 0xFF8FBC8F.toInt() // Verde claro
            )

    /**
     * Configuração para Master System (Sonic - Master System) Core: smsplus Características: Jogos
     * clássicos 8-bit - CORES CORRIGIDAS PARA AZUL
     */
    fun masterSystemConfig() =
            VirtualJoystickConfig(
                    name = "Master System",
                    leftJoystickEnabled = true,
                    rightJoystickEnabled = false,
                    joystickRadius = 125,
                    buttonRadius = 52,
                    sensitivity = 0.95f, // Meio termo entre digital e analog
                    autoVisibility = true,
                    backgroundColor = 0x70000080, // Azul Sonic (corrigido)
                    borderColor = 0xFF4A90E2.toInt(), // Azul claro
                    knobColor = 0xFF1E90FF.toInt() // Azul Sonic
            )

    /**
     * Configuração para jogos que precisam de dual analog Para sistemas mais modernos ou homebrew
     */
    fun dualAnalogConfig() =
            VirtualJoystickConfig(
                    name = "Dual Analog",
                    leftJoystickEnabled = true,
                    rightJoystickEnabled = true, // Ambos ativos
                    joystickRadius = 110, // Menor para caber dois
                    buttonRadius = 45,
                    sensitivity = 1.1f,
                    autoVisibility = true,
                    backgroundColor = 0x60606060, // Cinza neutro
                    borderColor = 0xFFFFFFFF.toInt(), // Branco
                    knobColor = 0xFF00FFFF.toInt() // Ciano
            )

    /** Retorna configuração baseada no core do LibRetro */
    fun getConfigForCore(core: String): VirtualJoystickConfig {
        return when (core) {
            "genesis_plus_gx" -> megaDriveConfig()
            "smsplus" -> masterSystemConfig() // Master System específico
            "bsnes" -> snesConfig()
            "gambatte" -> gameBoyConfig()
            else -> VirtualJoystickConfig.defaultConfig()
        }
    }

    /** Retorna configuração baseada no ID do app RESPEITANDO config.xml */
    fun getConfigForAppId(appId: String, resources: Resources): VirtualJoystickConfig {
        val baseConfig =
                when (appId) {
                    "sak" -> megaDriveConfig() // Sonic and Knuckles
                    "sth" -> masterSystemConfig() // Sonic The Hedgehog (Master System)
                    "rrr" -> snesConfig() // Rock and Roll Racing
                    "loz" -> gameBoyConfig() // Legend of Zelda
                    else -> VirtualJoystickConfig.defaultConfig()
                }

        // Aplica configurações do config.xml
        return createConfigFromResources(resources, baseConfig)
    }

    /** Versão legacy sem resources (para compatibilidade) */
    fun getConfigForAppId(appId: String): VirtualJoystickConfig {
        return when (appId) {
            "sak" -> megaDriveConfig() // Sonic and Knuckles
            "sth" -> masterSystemConfig() // Sonic The Hedgehog (Master System)
            "rrr" -> snesConfig() // Rock and Roll Racing
            "loz" -> gameBoyConfig() // Legend of Zelda
            else -> VirtualJoystickConfig.defaultConfig()
        }
    }

    /** Lista todas as configurações disponíveis */
    fun getAllConfigs(): List<VirtualJoystickConfig> {
        return listOf(
                megaDriveConfig(),
                snesConfig(),
                gameBoyConfig(),
                masterSystemConfig(),
                dualAnalogConfig(),
                VirtualJoystickConfig.defaultConfig()
        )
    }
}
