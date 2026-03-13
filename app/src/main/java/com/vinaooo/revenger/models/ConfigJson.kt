package com.vinaooo.revenger.models

import com.google.gson.annotations.SerializedName

/**
 * Root data class for config.json deserialization.
 * Replaces config.xml + config_manual.xml.
 */
data class ConfigJson(
    val identity: IdentityConfig = IdentityConfig(),
    @SerializedName("manual_settings")
    val manualSettings: ManualSettingsConfig = ManualSettingsConfig(),
    @SerializedName("fake_buttons")
    val fakeButtons: FakeButtonsConfig = FakeButtonsConfig()
)

/**
 * Identity & build settings — always read regardless of optimal_settings mode.
 */
data class IdentityConfig(
    @SerializedName("optimal_settings")
    val optimalSettings: Boolean = true,
    val platform: String = "",
    val name: String = "",
    val core: String = "",
    val rom: String = "",
    @SerializedName("target_abi")
    val targetAbi: String = "arm64-v8a",
    @SerializedName("load_bytes")
    val loadBytes: Boolean = false
)

/**
 * Manual gameplay settings — used as fallback when optimal_settings is disabled.
 * When optimal_settings is true, these are overridden by optimal_settings.json profiles.
 */
data class ManualSettingsConfig(
    val variables: String = "",
    @SerializedName("fast_forward_multiplier")
    val fastForwardMultiplier: Int = 2,
    val fullscreen: Boolean = true,
    val orientation: Int = 2,
    val shader: String = "disabled",
    @SerializedName("menu_mode_fab")
    val menuModeFab: String = "bottom-right",
    @SerializedName("menu_mode_gamepad")
    val menuModeGamepad: Boolean = true,
    @SerializedName("menu_mode_back")
    val menuModeBack: Boolean = true,
    @SerializedName("menu_mode_combo")
    val menuModeCombo: Boolean = false,
    val gamepad: Boolean = true,
    @SerializedName("gp_haptic")
    val gpHaptic: Boolean = true,
    @SerializedName("gp_allow_multiple_presses_action")
    val gpAllowMultiplePressesAction: Boolean = false,
    @SerializedName("gp_a")
    val gpA: Boolean = true,
    @SerializedName("gp_b")
    val gpB: Boolean = true,
    @SerializedName("gp_x")
    val gpX: Boolean = false,
    @SerializedName("gp_y")
    val gpY: Boolean = false,
    @SerializedName("gp_start")
    val gpStart: Boolean = true,
    @SerializedName("gp_select")
    val gpSelect: Boolean = true,
    @SerializedName("gp_l1")
    val gpL1: Boolean = false,
    @SerializedName("gp_r1")
    val gpR1: Boolean = false,
    @SerializedName("gp_l2")
    val gpL2: Boolean = false,
    @SerializedName("gp_r2")
    val gpR2: Boolean = false,
    @SerializedName("left_analog")
    val leftAnalog: Boolean = false,
    @SerializedName("performance_overlay")
    val performanceOverlay: Boolean = false
)

/**
 * Fake/custom button visibility — always read from config.json (not in optimal profiles).
 */
data class FakeButtonsConfig(
    @SerializedName("show_0")
    val show0: Boolean = false,
    @SerializedName("show_1")
    val show1: Boolean = false,
    @SerializedName("show_5")
    val show5: Boolean = false,
    @SerializedName("show_6")
    val show6: Boolean = false,
    @SerializedName("show_7")
    val show7: Boolean = false,
    @SerializedName("show_9")
    val show9: Boolean = false,
    @SerializedName("show_10")
    val show10: Boolean = false,
    @SerializedName("show_11")
    val show11: Boolean = false
)
