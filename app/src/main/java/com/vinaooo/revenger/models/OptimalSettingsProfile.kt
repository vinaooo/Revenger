package com.vinaooo.revenger.models

import org.json.JSONArray
import org.json.JSONObject

/**
 * Platform-specific optimal configuration profile.
 * Used when conf_optimal_settings is enabled to auto-configure
 * emulator settings based on ROM extension or explicit platform ID.
 */
data class OptimalSettingsProfile(
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
        fun fromJson(json: JSONObject): OptimalSettingsProfile {
            val extensionArray = json.getJSONArray("extensions")
            val extensions = mutableListOf<String>()
            for (i in 0 until extensionArray.length()) {
                extensions.add(extensionArray.getString(i))
            }

            return OptimalSettingsProfile(
                platformId = json.getString("platform_id"),
                extensions = extensions,
                core = json.getString("core"),
                confVariables = json.getString("conf_variables"),
                confFastForwardMultiplier = json.getInt("conf_fast_forward_multiplier"),
                confFullscreen = json.getBoolean("conf_fullscreen"),
                confOrientation = json.getInt("conf_orientation"),
                confMenuModeFab = json.getString("conf_menu_mode_fab"),
                confMenuModeGamepad = json.getBoolean("conf_menu_mode_gamepad"),
                confMenuModeBack = json.getBoolean("conf_menu_mode_back"),
                confMenuModeCombo = json.getBoolean("conf_menu_mode_combo"),
                confGamepad = json.getBoolean("conf_gamepad"),
                confGpHaptic = json.getBoolean("conf_gp_haptic"),
                confGpAllowMultiplePressesAction = json.getBoolean("conf_gp_allow_multiple_presses_action"),
                confGpA = json.getBoolean("conf_gp_a"),
                confGpB = json.getBoolean("conf_gp_b"),
                confGpX = json.getBoolean("conf_gp_x"),
                confGpY = json.getBoolean("conf_gp_y"),
                confGpStart = json.getBoolean("conf_gp_start"),
                confGpSelect = json.getBoolean("conf_gp_select"),
                confGpL1 = json.getBoolean("conf_gp_l1"),
                confGpR1 = json.getBoolean("conf_gp_r1"),
                confGpL2 = json.getBoolean("conf_gp_l2"),
                confGpR2 = json.getBoolean("conf_gp_r2"),
                confLeftAnalog = json.getBoolean("conf_left_analog"),
                confShader = json.getString("conf_shader"),
                confPerformanceOverlay = json.getBoolean("conf_performance_overlay")
            )
        }

        /**
         * Parse all profiles from JSON array
         */
        fun parseProfiles(jsonArray: JSONArray): List<OptimalSettingsProfile> {
            val profiles = mutableListOf<OptimalSettingsProfile>()
            for (i in 0 until jsonArray.length()) {
                profiles.add(fromJson(jsonArray.getJSONObject(i)))
            }
            return profiles
        }
    }
}
