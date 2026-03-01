package com.vinaooo.revenger

import android.content.Context
import android.util.Log
import com.vinaooo.revenger.models.OptimalSettingsProfile
import com.vinaooo.revenger.repositories.OptimalSettingsRepository
import com.vinaooo.revenger.utils.ConfigIdGenerator

/**
 * Centralized application configuration facade.
 * Provides unified access to both static config.xml values and dynamic optimal settings.
 * 
 * When conf_optimal_settings = false:
 *   All methods delegate to config.xml values
 * 
 * When conf_optimal_settings = true:
 *   Platform-specific settings are loaded from optimal_settings.json based on:
 *   1. conf_platform (explicit platform ID)
 *   2. conf_rom extension (automatic detection)
 *   
 * Identity/build settings (id, name, rom, target_abi, load_bytes) are always from config.xml.
 */
class AppConfig(private val context: Context) {
    private val TAG = "AppConfig"
    private val resources = context.resources

    private val optimalSettingsEnabled: Boolean by lazy {
        resources.getBoolean(R.bool.conf_optimal_settings)
    }

    private val profile: OptimalSettingsProfile? by lazy {
        if (!optimalSettingsEnabled) {
            null
        } else {
            val platformId = getPlatformId()
            val romName = getRomName()
            val extension = extractExtension(romName)
            
            Log.d(TAG, "Resolving optimal profile: platformId=$platformId, extension=$extension")
            
            val resolvedProfile = OptimalSettingsRepository.findProfile(platformId, extension)
            
            if (resolvedProfile == null) {
                Log.w(TAG, "No optimal profile found, falling back to config.xml")
            } else {
                Log.i(TAG, "Using optimal profile: ${resolvedProfile.platformId} (core: ${resolvedProfile.core})")
            }
            
            resolvedProfile
        }
    }

    // ========== Identity settings (always from config.xml) ==========

    fun getId(): String = ConfigIdGenerator.generate(
        resources.getString(R.string.conf_name),
        getCore()
    )

    fun getName(): String = resources.getString(R.string.conf_name)

    fun getRomName(): String = resources.getString(R.string.conf_rom)

    fun getTargetAbi(): String = resources.getString(R.string.conf_target_abi)

    fun getLoadBytes(): Boolean = resources.getBoolean(R.bool.conf_load_bytes)

    private fun getPlatformId(): String = resources.getString(R.string.conf_platform)

    // ========== Core and variables (optimal profile overrides) ==========

    fun getCore(): String {
        return profile?.core ?: resources.getString(R.string.conf_core)
    }

    fun getVariables(): String {
        return profile?.confVariables ?: resources.getString(R.string.conf_variables)
    }

    // ========== Performance settings (optimal profile overrides) ==========

    fun getFastForwardMultiplier(): Int {
        return profile?.confFastForwardMultiplier ?: resources.getInteger(R.integer.conf_fast_forward_multiplier)
    }

    // ========== Display settings (optimal profile overrides) ==========

    fun getFullscreen(): Boolean {
        return profile?.confFullscreen ?: resources.getBoolean(R.bool.conf_fullscreen)
    }

    fun getOrientation(): Int {
        return profile?.confOrientation ?: resources.getInteger(R.integer.conf_orientation)
    }

    fun getShader(): String {
        return profile?.confShader ?: resources.getString(R.string.conf_shader)
    }

    // ========== Menu settings (optimal profile overrides) ==========

    fun getMenuModeFab(): String {
        return profile?.confMenuModeFab ?: resources.getString(R.string.conf_menu_mode_fab)
    }

    fun getMenuModeGamepad(): Boolean {
        return profile?.confMenuModeGamepad ?: resources.getBoolean(R.bool.conf_menu_mode_gamepad)
    }

    fun getMenuModeBack(): Boolean {
        return profile?.confMenuModeBack ?: resources.getBoolean(R.bool.conf_menu_mode_back)
    }

    fun getMenuModeCombo(): Boolean {
        return profile?.confMenuModeCombo ?: resources.getBoolean(R.bool.conf_menu_mode_combo)
    }

    // ========== Gamepad settings (optimal profile overrides) ==========

    fun getGamepad(): Boolean {
        return profile?.confGamepad ?: resources.getBoolean(R.bool.conf_gamepad)
    }

    fun getGpHaptic(): Boolean {
        return profile?.confGpHaptic ?: resources.getBoolean(R.bool.conf_gp_haptic)
    }

    fun getGpAllowMultiplePressesAction(): Boolean {
        return profile?.confGpAllowMultiplePressesAction 
            ?: resources.getBoolean(R.bool.conf_gp_allow_multiple_presses_action)
    }

    fun getGpA(): Boolean {
        return profile?.confGpA ?: resources.getBoolean(R.bool.conf_gp_a)
    }

    fun getGpB(): Boolean {
        return profile?.confGpB ?: resources.getBoolean(R.bool.conf_gp_b)
    }

    fun getGpX(): Boolean {
        return profile?.confGpX ?: resources.getBoolean(R.bool.conf_gp_x)
    }

    fun getGpY(): Boolean {
        return profile?.confGpY ?: resources.getBoolean(R.bool.conf_gp_y)
    }

    fun getGpStart(): Boolean {
        return profile?.confGpStart ?: resources.getBoolean(R.bool.conf_gp_start)
    }

    fun getGpSelect(): Boolean {
        return profile?.confGpSelect ?: resources.getBoolean(R.bool.conf_gp_select)
    }

    fun getGpL1(): Boolean {
        return profile?.confGpL1 ?: resources.getBoolean(R.bool.conf_gp_l1)
    }

    fun getGpR1(): Boolean {
        return profile?.confGpR1 ?: resources.getBoolean(R.bool.conf_gp_r1)
    }

    fun getGpL2(): Boolean {
        return profile?.confGpL2 ?: resources.getBoolean(R.bool.conf_gp_l2)
    }

    fun getGpR2(): Boolean {
        return profile?.confGpR2 ?: resources.getBoolean(R.bool.conf_gp_r2)
    }

    fun getLeftAnalog(): Boolean {
        return profile?.confLeftAnalog ?: resources.getBoolean(R.bool.conf_left_analog)
    }

    // ========== Debug settings (optimal profile overrides) ==========

    fun getPerformanceOverlay(): Boolean {
        return profile?.confPerformanceOverlay ?: resources.getBoolean(R.bool.conf_performance_overlay)
    }

    // ========== Fake buttons (always from config.xml, not in optimal profiles) ==========

    fun getShowFakeButton0(): Boolean = resources.getBoolean(R.bool.conf_show_fake_button_0)
    fun getShowFakeButton1(): Boolean = resources.getBoolean(R.bool.conf_show_fake_button_1)
    fun getShowFakeButton5(): Boolean = resources.getBoolean(R.bool.conf_show_fake_button_5)
    fun getShowFakeButton6(): Boolean = resources.getBoolean(R.bool.conf_show_fake_button_6)
    fun getShowFakeButton7(): Boolean = resources.getBoolean(R.bool.conf_show_fake_button_7)
    fun getShowFakeButton9(): Boolean = resources.getBoolean(R.bool.conf_show_fake_button_9)
    fun getShowFakeButton10(): Boolean = resources.getBoolean(R.bool.conf_show_fake_button_10)
    fun getShowFakeButton11(): Boolean = resources.getBoolean(R.bool.conf_show_fake_button_11)

    // ========== Utility methods ==========

    /**
     * Extract file extension from ROM filename.
     * Handles cases with or without extension.
     */
    private fun extractExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot >= 0) {
            filename.substring(lastDot).lowercase()
        } else {
            ""
        }
    }

    /**
     * Check if optimal settings mode is enabled
     */
    fun isOptimalMode(): Boolean = optimalSettingsEnabled

    /**
     * Get the resolved profile (for debugging)
     */
    fun getResolvedProfile(): OptimalSettingsProfile? = profile
}
