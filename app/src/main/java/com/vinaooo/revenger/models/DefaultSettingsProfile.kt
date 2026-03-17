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
    val confMenuMode: String,
    val confGamepad: Boolean,
    val confGpHaptic: Boolean,
    val confButtonAllowMultiplePressesAction: Boolean,
    val confButtonA: Boolean,
    val confButtonB: Boolean,
    val confButtonX: Boolean,
    val confButtonY: Boolean,
    val confButtonStart: Boolean,
    val confButtonSelect: Boolean,
    val confButtonL1: Boolean,
    val confButtonR1: Boolean,
    val confButtonL2: Boolean,
    val confButtonR2: Boolean,
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
                confMenuMode = json.getString("menu_mode"),
                confGamepad = json.getBoolean("gamepad"),
                confGpHaptic = json.getBoolean("gp_haptic"),
                confButtonAllowMultiplePressesAction = json.getBoolean("button_allow_multiple_presses_action"),
                confButtonA = json.getBoolean("button_a"),
                confButtonB = json.getBoolean("button_b"),
                confButtonX = json.getBoolean("button_x"),
                confButtonY = json.getBoolean("button_y"),
                confButtonStart = json.getBoolean("button_start"),
                confButtonSelect = json.getBoolean("button_select"),
                confButtonL1 = json.getBoolean("button_l1"),
                confButtonR1 = json.getBoolean("button_r1"),
                confButtonL2 = json.getBoolean("button_l2"),
                confButtonR2 = json.getBoolean("button_r2"),
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
