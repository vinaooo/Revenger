package com.vinaooo.revenger.utils

/**
 * Constants for menu animations, timings, and UI values. Eliminates magic numbers throughout the
 * codebase.
 */
object MenuConstants {
    const val ANIMATION_DURATION_MS = 200L
    const val SCALE_ANIMATION_FACTOR = 0.8f
    const val ALPHA_ANIMATION_START = 0f
    const val ALPHA_ANIMATION_END = 1f
}

/**
 * Enum representing different menu modes based on conf_menu_mode values. Provides type safety and
 * eliminates magic numbers.
 */
enum class MenuMode(val value: Int) {
    DISABLED(0),
    BACK_BUTTON_ONLY(1),
    SELECT_START_COMBO_ONLY(2),
    BOTH_BACK_AND_COMBO(3);

    companion object {
        fun fromValue(value: Int) = values().find { it.value == value } ?: DISABLED
    }
}

/**
 * Enum representing available shader types with their display names and configurations. Provides
 * type safety for shader management and eliminates string comparisons.
 */
enum class ShaderType(val displayName: String, val configName: String) {
    DISABLED("Disabled", "disabled"),
    SHARP("Sharp", "sharp"),
    CRT("CRT", "crt"),
    LCD("LCD", "lcd");

    companion object {
        fun fromName(name: String) = values().find { it.configName == name.lowercase() } ?: SHARP
        fun fromDisplayName(displayName: String) =
                values().find { it.displayName == displayName } ?: SHARP
    }
}
