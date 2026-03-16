package com.vinaooo.revenger

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.vinaooo.revenger.models.DefaultSettingsProfile
import com.vinaooo.revenger.repositories.DefaultSettingsRepository
import com.vinaooo.revenger.utils.ConfigIdGenerator
import java.io.InputStreamReader

data class BaseConfig(
    val conf_default_settings: Boolean = false,
    val conf_platform: String = "",
    val conf_name: String = "Revenger",
    val conf_rom: String = "",
    val conf_target_abi: String = ""
)

data class ManualConfig(
    val conf_core: String = "",
    val conf_variables: String = "",
    val conf_fast_forward_multiplier: Int = 1,
    val conf_fullscreen: Boolean = true,
    val conf_orientation: Int = 0,
    val conf_menu_mode_fab: String = "",
    val conf_menu_mode_gamepad: Boolean = false,
    val conf_menu_mode_back: Boolean = false,
    val conf_menu_mode_combo: Boolean = false,
    val conf_gamepad: Boolean = true,
    val conf_gp_haptic: Boolean = true,
    val conf_gp_allow_multiple_presses_action: Boolean = false,
    val conf_gp_a: Boolean = true,
    val conf_gp_b: Boolean = true,
    val conf_gp_x: Boolean = false,
    val conf_gp_y: Boolean = false,
    val conf_gp_start: Boolean = true,
    val conf_gp_select: Boolean = true,
    val conf_gp_l1: Boolean = false,
    val conf_gp_r1: Boolean = false,
    val conf_gp_l2: Boolean = false,
    val conf_gp_r2: Boolean = false,
    val conf_left_analog: Boolean = false,
    val conf_show_fake_button_0: Boolean = false,
    val conf_show_fake_button_1: Boolean = false,
    val conf_show_fake_button_5: Boolean = false,
    val conf_show_fake_button_6: Boolean = false,
    val conf_show_fake_button_7: Boolean = false,
    val conf_show_fake_button_9: Boolean = false,
    val conf_show_fake_button_10: Boolean = false,
    val conf_show_fake_button_11: Boolean = false,
    val conf_shader: String = "",
    val conf_performance_overlay: Boolean = false
)

data class GamePadAssetsConfig(
    val gp_button_color: String = "#88ffffff",
    val gp_pressed_color: String = "#66ffffff",
    val gp_padding_vertical: String = "20dp",
    val gp_offset_portrait: Int = 100,
    val gp_offset_landscape: Int = 50
)

/**
 * Centralized application configuration facade.
 * Provides unified access to both static config.xml values and dynamic default settings.
 */
class AppConfig(private val context: Context) {
    private val TAG = "AppConfig"
    
    private val baseConfig: BaseConfig
    private val manualConfig: ManualConfig
    val gamePadConfigModel: GamePadAssetsConfig

    init {
        val gson = Gson()
        baseConfig = loadJsonAsset("config/config.json", BaseConfig::class.java, gson) ?: BaseConfig()
        manualConfig = loadJsonAsset("config/config_manual.json", ManualConfig::class.java, gson) ?: ManualConfig()
        gamePadConfigModel = loadJsonAsset("config/gamepad.json", GamePadAssetsConfig::class.java, gson) ?: GamePadAssetsConfig()
    }

    private fun <T> loadJsonAsset(path: String, type: Class<T>, gson: Gson): T? {
        return try {
            context.assets.open(path).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    gson.fromJson(reader, type)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load config from $path: ${e.message}")
            null
        }
    }

    private val profile: DefaultSettingsProfile? by lazy {
        if (!isDefaultMode()) {
            null
        } else {
            val platformId = getPlatformId()
            val romName = getRomName()
            val extension = extractExtension(romName)
            
            Log.d(TAG, "Resolving default profile: platformId=$platformId, extension=$extension")
            
            val resolvedProfile = DefaultSettingsRepository.findProfile(platformId, extension)
            
            if (resolvedProfile == null) {
                Log.w(TAG, "No default profile found, falling back to assets config")
            } else {
                Log.i(TAG, "Using default profile: ${resolvedProfile.platformId} (core: ${resolvedProfile.core})")
            }
            
            resolvedProfile
        }
    }

    // ========== Identity settings (always from config.json) ==========

    fun getId(): String = ConfigIdGenerator.generate(getName(), getCore())
    fun getName(): String = baseConfig.conf_name.takeIf { it.isNotEmpty() } ?: "Revenger"
    fun getRomName(): String = baseConfig.conf_rom
    fun getTargetAbi(): String = baseConfig.conf_target_abi
    private fun getPlatformId(): String = baseConfig.conf_platform

    // ========== Core and variables (default profile overrides) ==========

    fun getCore(): String = profile?.core ?: manualConfig.conf_core
    fun getVariables(): String = profile?.confVariables ?: manualConfig.conf_variables

    // ========== Performance settings (default profile overrides) ==========

    fun getFastForwardMultiplier(): Int = profile?.confFastForwardMultiplier ?: manualConfig.conf_fast_forward_multiplier

    // ========== Display settings (default profile overrides) ==========

    fun getFullscreen(): Boolean = profile?.confFullscreen ?: manualConfig.conf_fullscreen
    fun getOrientation(): Int = profile?.confOrientation ?: manualConfig.conf_orientation
    fun getShader(): String = profile?.confShader ?: manualConfig.conf_shader

    // ========== Menu settings (default profile overrides) ==========

    fun getMenuModeFab(): String = profile?.confMenuModeFab ?: manualConfig.conf_menu_mode_fab
    fun getMenuModeGamepad(): Boolean = profile?.confMenuModeGamepad ?: manualConfig.conf_menu_mode_gamepad
    fun getMenuModeBack(): Boolean = profile?.confMenuModeBack ?: manualConfig.conf_menu_mode_back
    fun getMenuModeCombo(): Boolean = profile?.confMenuModeCombo ?: manualConfig.conf_menu_mode_combo

    // ========== Gamepad settings (default profile overrides) ==========

    fun getGamepad(): Boolean = profile?.confGamepad ?: manualConfig.conf_gamepad
    fun getGpHaptic(): Boolean = profile?.confGpHaptic ?: manualConfig.conf_gp_haptic
    fun getGpAllowMultiplePressesAction(): Boolean = profile?.confGpAllowMultiplePressesAction ?: manualConfig.conf_gp_allow_multiple_presses_action
    
    fun getGpA(): Boolean = profile?.confGpA ?: manualConfig.conf_gp_a
    fun getGpB(): Boolean = profile?.confGpB ?: manualConfig.conf_gp_b
    fun getGpX(): Boolean = profile?.confGpX ?: manualConfig.conf_gp_x
    fun getGpY(): Boolean = profile?.confGpY ?: manualConfig.conf_gp_y
    fun getGpStart(): Boolean = profile?.confGpStart ?: manualConfig.conf_gp_start
    fun getGpSelect(): Boolean = profile?.confGpSelect ?: manualConfig.conf_gp_select
    fun getGpL1(): Boolean = profile?.confGpL1 ?: manualConfig.conf_gp_l1
    fun getGpR1(): Boolean = profile?.confGpR1 ?: manualConfig.conf_gp_r1
    fun getGpL2(): Boolean = profile?.confGpL2 ?: manualConfig.conf_gp_l2
    fun getGpR2(): Boolean = profile?.confGpR2 ?: manualConfig.conf_gp_r2
    fun getLeftAnalog(): Boolean = profile?.confLeftAnalog ?: manualConfig.conf_left_analog

    // ========== Debug settings (default profile overrides) ==========

    fun getPerformanceOverlay(): Boolean = profile?.confPerformanceOverlay ?: manualConfig.conf_performance_overlay

    // ========== Fake buttons (always from manualConfig, not in default profiles) ==========

    fun getShowFakeButton0(): Boolean = manualConfig.conf_show_fake_button_0
    fun getShowFakeButton1(): Boolean = manualConfig.conf_show_fake_button_1
    fun getShowFakeButton5(): Boolean = manualConfig.conf_show_fake_button_5
    fun getShowFakeButton6(): Boolean = manualConfig.conf_show_fake_button_6
    fun getShowFakeButton7(): Boolean = manualConfig.conf_show_fake_button_7
    fun getShowFakeButton9(): Boolean = manualConfig.conf_show_fake_button_9
    fun getShowFakeButton10(): Boolean = manualConfig.conf_show_fake_button_10
    fun getShowFakeButton11(): Boolean = manualConfig.conf_show_fake_button_11

    // ========== Utility methods ==========

    private fun extractExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot >= 0) {
            filename.substring(lastDot).lowercase()
        } else {
            ""
        }
    }

    fun isDefaultMode(): Boolean = baseConfig.conf_default_settings
    fun getResolvedProfile(): DefaultSettingsProfile? = profile
}
