package com.vinaooo.revenger.utils

import com.swordfish.libretrodroid.ShaderConfig

/**
 * The shaders the app offers, in menu order: the display name shown to the player, the name used
 * in config and preferences, and the LibretroDroid [ShaderConfig] each one renders with. This is
 * the single table every shader lookup goes through.
 */
enum class ShaderType(val displayName: String, val configName: String) {
    DISABLED("Disabled", "disabled"),
    SHARP("Sharp", "sharp"),
    CRT("CRT", "crt"),
    LCD("LCD", "lcd"),
    UPSCALE1("Upscale 1", "upscale1"),
    UPSCALE2("Upscale 2", "upscale2"),
    UPSCALE3("Upscale 3", "upscale3");

    /** The LibretroDroid shader this type renders with. */
    fun toShaderConfig(): ShaderConfig =
            when (this) {
                DISABLED -> ShaderConfig.Default
                SHARP -> ShaderConfig.Sharp
                CRT -> ShaderConfig.CRT
                LCD -> ShaderConfig.LCD
                UPSCALE1 -> ShaderConfig.CUT()
                UPSCALE2 -> ShaderConfig.CUT2()
                UPSCALE3 -> ShaderConfig.CUT3()
            }

    companion object {
        /** The type whose [configName] matches [name], ignoring case, or null for an unknown name. */
        fun fromConfigName(name: String): ShaderType? =
                entries.firstOrNull { it.configName.equals(name, ignoreCase = true) }
    }
}
