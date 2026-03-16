package com.vinaooo.revenger.models

import org.json.JSONArray
import org.json.JSONObject

/**
 * Platform-specific optimal configuration profile.
 * Used when default_settings is enabled to auto-configure
 * emulator settings based on ROM extension or explicit platform ID.
 */
data class DefaultSettingsProfile(
    val platformId: String,
    val extensions: List<String>,
    val core: String,
    val confVariables: String,
    val confFastForwardMultiplier: Int,
    val confFullscreen: Boolean,
    val confOrientation: Int,
    val confMenuModeFab: String,
    val confMenuModeGamepad: Boolean,
    val confMenuModeBack: Boolean,
    val confMenuModeCombo: Boolean,
    val confGamepad: Boolean,
    val confGpHaptic: Boolean,
    val confGpAllowMultiplePressesAction: Boolean,
    val confGpA: Boolean,
    val confGpB: Boolean,
    val confGpX: Boolean,
    val confGpY: Boolean,
    val confGpStart: Boolean,
    val confGpSelect: Boolean,
    val confGpL1: Boolean,
    val confGpR1: Boolean,
    val confGpL2: Boolean,
    val confGpR2: Boolean,
    val confLeftAnalog: Boolean,
    val confShader: String,
    val confPerformanceOverlay: Boolean
) {
    companion object {
        /**
         * Parse a single profile from JSON object
         */
        fun fromJson(json: JSONObject): DefaultSettingsProfile {
            val extensionArray = json.getJSONArray("extensions")
            val extensions = mutableListOf<String>()
            for (i in 0 until extensionArray.length()) {
                extensions.add(extensionArray.getString(i))
            }

            return DefaultSettingsProfile(
                platformId = json.getString("platform_id"),
                extensions = extensions,
                core = json.getString("core"),
                confVariables = json.getString("variables"),
                confFastForwardMultiplier = json.getInt("fast_forward_multiplier"),
                confFullscreen = json.getBoolean("fullscreen"),
                confOrientation = json.getInt("orientation"),
                confMenuModeFab = json.getString("menu_mode_fab"),
                confMenuModeGamepad = json.getBoolean("menu_mode_gamepad"),
                confMenuModeBack = json.getBoolean("menu_mode_back"),
                confMenuModeCombo = json.getBoolean("menu_mode_combo"),
                confGamepad = json.getBoolean("gamepad"),
                confGpHaptic = json.getBoolean("gp_haptic"),
                confGpAllowMultiplePressesAction = json.getBoolean("gp_allow_multiple_presses_action"),
                confGpA = json.getBoolean("gp_a"),
                confGpB = json.getBoolean("gp_b"),
                confGpX = json.getBoolean("gp_x"),
                confGpY = json.getBoolean("gp_y"),
                confGpStart = json.getBoolean("gp_start"),
                confGpSelect = json.getBoolean("gp_select"),
                confGpL1 = json.getBoolean("gp_l1"),
                confGpR1 = json.getBoolean("gp_r1"),
                confGpL2 = json.getBoolean("gp_l2"),
                confGpR2 = json.getBoolean("gp_r2"),
                confLeftAnalog = json.getBoolean("left_analog"),
                confShader = json.getString("shader"),
                confPerformanceOverlay = json.getBoolean("performance_overlay")
            )
        }

        /**
         * Parse all profiles from JSON array
         */
        fun parseProfiles(jsonArray: JSONArray): List<DefaultSettingsProfile> {
            val profiles = mutableListOf<DefaultSettingsProfile>()
            for (i in 0 until jsonArray.length()) {
                profiles.add(fromJson(jsonArray.getJSONObject(i)))
            }
            return profiles
        }
    }
}
