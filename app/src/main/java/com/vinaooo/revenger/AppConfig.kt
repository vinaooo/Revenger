package com.vinaooo.revenger

import android.content.Context
import android.util.Log
import com.vinaooo.revenger.models.OptimalSettingsProfile
import com.vinaooo.revenger.repositories.ConfigRepository
import com.vinaooo.revenger.repositories.OptimalSettingsRepository
import com.vinaooo.revenger.utils.ConfigIdGenerator

/**
 * Centralized application configuration facade.
 * Provides unified access to both config.json values and dynamic optimal settings.
 * 
 * When identity.optimal_settings = false:
 *   All methods delegate to config.json manual_settings values
 * 
 * When identity.optimal_settings = true:
 *   Platform-specific settings are loaded from optimal_settings.json based on:
 *   1. identity.platform (explicit platform ID)
 *   2. identity.rom extension (automatic detection)
 *   
 * Identity/build settings (id, name, rom, target_abi, load_bytes) are always from config.json.
 * Fake buttons are always from config.json (not in optimal profiles).
 */
class AppConfig(private val context: Context) {
    private val TAG = "AppConfig"
    private val configRepo = ConfigRepository.getInstance(context)
    private val config get() = configRepo.get()

    private val optimalSettingsEnabled: Boolean by lazy {
        config.identity.optimalSettings
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
                Log.w(TAG, "No optimal profile found, falling back to config.json")
            } else {
                Log.i(TAG, "Using optimal profile: ${resolvedProfile.platformId} (core: ${resolvedProfile.core})")
            }
            
            resolvedProfile
        }
    }

    // ========== Identity settings (always from config.json) ==========

    fun getId(): String = ConfigIdGenerator.generate(
        config.identity.name,
        getCore()
    )

    fun getName(): String = config.identity.name

    fun getRomName(): String = config.identity.rom

    fun getTargetAbi(): String = config.identity.targetAbi

    fun getLoadBytes(): Boolean = config.identity.loadBytes

    private fun getPlatformId(): String = config.identity.platform

    // ========== Core and variables (optimal profile overrides) ==========

    fun getCore(): String {
        return profile?.core ?: config.identity.core
    }

    fun getVariables(): String {
        return profile?.confVariables ?: config.manualSettings.variables
    }

    // ========== Performance settings (optimal profile overrides) ==========

    fun getFastForwardMultiplier(): Int {
        return profile?.confFastForwardMultiplier ?: config.manualSettings.fastForwardMultiplier
    }

    // ========== Display settings (optimal profile overrides) ==========

    fun getFullscreen(): Boolean {
        return profile?.confFullscreen ?: config.manualSettings.fullscreen
    }

    fun getOrientation(): Int {
        return profile?.confOrientation ?: config.manualSettings.orientation
    }

    fun getShader(): String {
        return profile?.confShader ?: config.manualSettings.shader
    }

    // ========== Menu settings (optimal profile overrides) ==========

    fun getMenuModeFab(): String {
        return profile?.confMenuModeFab ?: config.manualSettings.menuModeFab
    }

    fun getMenuModeGamepad(): Boolean {
        return profile?.confMenuModeGamepad ?: config.manualSettings.menuModeGamepad
    }

    fun getMenuModeBack(): Boolean {
        return profile?.confMenuModeBack ?: config.manualSettings.menuModeBack
    }

    fun getMenuModeCombo(): Boolean {
        return profile?.confMenuModeCombo ?: config.manualSettings.menuModeCombo
    }

    // ========== Gamepad settings (optimal profile overrides) ==========

    fun getGamepad(): Boolean {
        return profile?.confGamepad ?: config.manualSettings.gamepad
    }

    fun getGpHaptic(): Boolean {
        return profile?.confGpHaptic ?: config.manualSettings.gpHaptic
    }

    fun getGpAllowMultiplePressesAction(): Boolean {
        return profile?.confGpAllowMultiplePressesAction 
            ?: config.manualSettings.gpAllowMultiplePressesAction
    }

    fun getGpA(): Boolean {
        return profile?.confGpA ?: config.manualSettings.gpA
    }

    fun getGpB(): Boolean {
        return profile?.confGpB ?: config.manualSettings.gpB
    }

    fun getGpX(): Boolean {
        return profile?.confGpX ?: config.manualSettings.gpX
    }

    fun getGpY(): Boolean {
        return profile?.confGpY ?: config.manualSettings.gpY
    }

    fun getGpStart(): Boolean {
        return profile?.confGpStart ?: config.manualSettings.gpStart
    }

    fun getGpSelect(): Boolean {
        return profile?.confGpSelect ?: config.manualSettings.gpSelect
    }

    fun getGpL1(): Boolean {
        return profile?.confGpL1 ?: config.manualSettings.gpL1
    }

    fun getGpR1(): Boolean {
        return profile?.confGpR1 ?: config.manualSettings.gpR1
    }

    fun getGpL2(): Boolean {
        return profile?.confGpL2 ?: config.manualSettings.gpL2
    }

    fun getGpR2(): Boolean {
        return profile?.confGpR2 ?: config.manualSettings.gpR2
    }

    fun getLeftAnalog(): Boolean {
        return profile?.confLeftAnalog ?: config.manualSettings.leftAnalog
    }

    // ========== Debug settings (optimal profile overrides) ==========

    fun getPerformanceOverlay(): Boolean {
        return profile?.confPerformanceOverlay ?: config.manualSettings.performanceOverlay
    }

    // ========== Fake buttons (always from config.json, not in optimal profiles) ==========

    fun getShowFakeButton0(): Boolean = config.fakeButtons.show0
    fun getShowFakeButton1(): Boolean = config.fakeButtons.show1
    fun getShowFakeButton5(): Boolean = config.fakeButtons.show5
    fun getShowFakeButton6(): Boolean = config.fakeButtons.show6
    fun getShowFakeButton7(): Boolean = config.fakeButtons.show7
    fun getShowFakeButton9(): Boolean = config.fakeButtons.show9
    fun getShowFakeButton10(): Boolean = config.fakeButtons.show10
    fun getShowFakeButton11(): Boolean = config.fakeButtons.show11

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
