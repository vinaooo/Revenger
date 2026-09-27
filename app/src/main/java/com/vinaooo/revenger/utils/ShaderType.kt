package com.vinaooo.revenger.utils

/**
 * Enum representing available shader types with their display names and configurations. Provides
 * type safety for shader management and eliminates string comparisons.
 */
enum class ShaderType(val displayName: String, val configName: String) {
    DISABLED("Disabled", "disabled"),
    SHARP("Sharp", "sharp"),
    CRT("CRT", "crt"),
    LCD("LCD", "lcd")
}
