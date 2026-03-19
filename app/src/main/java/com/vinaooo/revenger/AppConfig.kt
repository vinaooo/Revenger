package com.vinaooo.revenger

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.vinaooo.revenger.models.DefaultSettingsProfile
import com.vinaooo.revenger.repositories.DefaultSettingsRepository
import com.vinaooo.revenger.utils.ConfigIdGenerator
import java.io.InputStreamReader

data class BaseConfig(
    val default_settings: Boolean = false,
    val platform: String = "",
    val name: String = "Revenger",
    val rom: String = "",
    val target_abi: String = ""
)

data class ManualConfig(
    val core: String = "",
    val variables: String = "",
    val fast_forward_multiplier: Int = 1,
    val fullscreen: Boolean = true,
    val orientation: String = "landscape",
    val menu_mode: String = "",
    val gamepad: Boolean = true,
    val gp_haptic: Boolean = true,
    val button_allow_multiple_presses_action: Boolean = false,
    val button_a: Boolean = true,
    val button_b: Boolean = true,
    val button_x: Boolean = false,
    val button_y: Boolean = false,
    val button_start: Boolean = true,
    val button_select: Boolean = true,
    val button_l1: Boolean = false,
    val button_r1: Boolean = false,
    val button_l2: Boolean = false,
    val button_r2: Boolean = false,
    val left_analog: Boolean = false,
    val fake_button_0: Boolean = false,
    val fake_button_1: Boolean = false,
    val fake_button_5: Boolean = false,
    val fake_button_6: Boolean = false,
    val fake_button_7: Boolean = false,
    val fake_button_9: Boolean = false,
    val fake_button_10: Boolean = false,
    val fake_button_11: Boolean = false,
    val shader: String = "",
    val performance_overlay: Boolean = false
)

data class GamePadAssetsConfig(
    val button_button_color: String = "#88ffffff",
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
    fun getName(): String = baseConfig.name.takeIf { it.isNotEmpty() } ?: "Revenger"
    fun getRomName(): String = baseConfig.rom
    fun getTargetAbi(): String = baseConfig.target_abi
    private fun getPlatformId(): String = baseConfig.platform

    // ========== Core and variables (default profile overrides) ==========

    fun getCore(): String = profile?.core ?: manualConfig.core
    fun getVariables(): String = profile?.confVariables ?: manualConfig.variables

    // ========== Performance settings (default profile overrides) ==========

    fun getFastForwardMultiplier(): Int = profile?.confFastForwardMultiplier ?: manualConfig.fast_forward_multiplier

    // ========== Display settings (default profile overrides) ==========

    fun getFullscreen(): Boolean = profile?.confFullscreen ?: manualConfig.fullscreen
    fun getOrientation(): String = profile?.confOrientation ?: manualConfig.orientation
    fun getShader(): String = profile?.confShader ?: manualConfig.shader

    // ========== Menu settings (default profile overrides) ==========

    fun getMenuModeFab(): String {
        val mode = profile?.confMenuMode ?: manualConfig.menu_mode
        val match = Regex("fab=([\\w-]+)").find(mode)
        return match?.groups?.get(1)?.value ?: ""
    }
    fun getMenuModeGamepad(): Boolean {
        val mode = profile?.confMenuMode ?: manualConfig.menu_mode
        val modes = mode.split(",").map { it.trim() }
        return modes.contains("gamepad")
    }
    fun getMenuModeBack(): Boolean {
        val mode = profile?.confMenuMode ?: manualConfig.menu_mode
        val modes = mode.split(",").map { it.trim() }
        return modes.contains("back")
    }
    fun getMenuModeCombo(): Boolean {
        val mode = profile?.confMenuMode ?: manualConfig.menu_mode
        val modes = mode.split(",").map { it.trim() }
        return modes.contains("combo")
    }

    // ========== Gamepad settings (default profile overrides) ==========

    fun getGamepad(): Boolean = profile?.confGamepad ?: manualConfig.gamepad
    fun getGpHaptic(): Boolean = profile?.confGpHaptic ?: manualConfig.gp_haptic
    fun getButtonAllowMultiplePressesAction(): Boolean = profile?.confButtonAllowMultiplePressesAction ?: manualConfig.button_allow_multiple_presses_action
    
    fun getButtonA(): Boolean = profile?.confButtonA ?: manualConfig.button_a
    fun getButtonB(): Boolean = profile?.confButtonB ?: manualConfig.button_b
    fun getButtonX(): Boolean = profile?.confButtonX ?: manualConfig.button_x
    fun getButtonY(): Boolean = profile?.confButtonY ?: manualConfig.button_y
    fun getButtonStart(): Boolean = profile?.confButtonStart ?: manualConfig.button_start
    fun getButtonSelect(): Boolean = profile?.confButtonSelect ?: manualConfig.button_select
    fun getButtonL1(): Boolean = profile?.confButtonL1 ?: manualConfig.button_l1
    fun getButtonR1(): Boolean = profile?.confButtonR1 ?: manualConfig.button_r1
    fun getButtonL2(): Boolean = profile?.confButtonL2 ?: manualConfig.button_l2
    fun getButtonR2(): Boolean = profile?.confButtonR2 ?: manualConfig.button_r2
    fun getLeftAnalog(): Boolean = profile?.confLeftAnalog ?: manualConfig.left_analog

    // ========== Debug settings (default profile overrides) ==========

    fun getPerformanceOverlay(): Boolean = profile?.confPerformanceOverlay ?: manualConfig.performance_overlay

    // ========== Fake buttons (always from manualConfig, not in default profiles) ==========

    fun getFakeButton0(): Boolean = manualConfig.fake_button_0
    fun getFakeButton1(): Boolean = manualConfig.fake_button_1
    fun getFakeButton5(): Boolean = manualConfig.fake_button_5
    fun getFakeButton6(): Boolean = manualConfig.fake_button_6
    fun getFakeButton7(): Boolean = manualConfig.fake_button_7
    fun getFakeButton9(): Boolean = manualConfig.fake_button_9
    fun getFakeButton10(): Boolean = manualConfig.fake_button_10
    fun getFakeButton11(): Boolean = manualConfig.fake_button_11

    // ========== Utility methods ==========

    private fun extractExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot >= 0) {
            filename.substring(lastDot).lowercase()
        } else {
            ""
        }
    }

    fun isDefaultMode(): Boolean = baseConfig.default_settings
    fun getResolvedProfile(): DefaultSettingsProfile? = profile
}
