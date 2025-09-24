package com.vinaooo.revenger.ui.menu

import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.vinaooo.revenger.R

/**
 * Menu Theme Provider
 * Sprint 1: Base theming system for Material You Expressive menu
 * 
 * Provides dynamic colors and theming following Material You guidelines
 * Will be enhanced with full dynamic color support in Sprint 3
 */
class MenuThemeProvider(private val context: Context) {
    
    /**
     * Get primary color for menu elements
     * Sprint 1: Basic color from theme
     */
    @ColorInt
    fun getPrimaryColor(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getDynamicPrimaryColor()
        } else {
            getFallbackPrimaryColor()
        }
    }
    
    /**
     * Get secondary color for menu elements
     * Sprint 1: Basic color from theme
     */
    @ColorInt
    fun getSecondaryColor(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getDynamicSecondaryColor()
        } else {
            getFallbackSecondaryColor()
        }
    }
    
    /**
     * Get surface color for cards
     * Sprint 1: Basic surface color
     */
    @ColorInt
    fun getSurfaceColor(): Int {
        return MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorSurface,
            ContextCompat.getColor(context, android.R.color.white)
        )
    }
    
    /**
     * Get on-surface color for text on cards
     * Sprint 1: Basic on-surface color
     */
    @ColorInt
    fun getOnSurfaceColor(): Int {
        return MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurface,
            ContextCompat.getColor(context, android.R.color.black)
        )
    }
    
    /**
     * Get background color for menu
     * Sprint 1: Basic background
     */
    @ColorInt
    fun getBackgroundColor(): Int {
        return MaterialColors.getColor(
            context,
            android.R.attr.colorBackground,
            ContextCompat.getColor(context, android.R.color.white)
        )
    }
    
    /**
     * Android 12+ Dynamic Colors
     * Sprint 1: Basic implementation
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun getDynamicPrimaryColor(): Int {
        return ContextCompat.getColor(context, android.R.color.system_accent1_500)
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun getDynamicSecondaryColor(): Int {
        return ContextCompat.getColor(context, android.R.color.system_accent2_500)
    }
    
    /**
     * Fallback colors for older Android versions
     * Sprint 1: Basic Material Design colors
     */
    private fun getFallbackPrimaryColor(): Int {
        return MaterialColors.getColor(
            context,
            androidx.appcompat.R.attr.colorPrimary,
            ContextCompat.getColor(context, R.color.menu_primary_fallback)
        )
    }
    
    private fun getFallbackSecondaryColor(): Int {
        return MaterialColors.getColor(
            context,
            androidx.appcompat.R.attr.colorAccent,
            ContextCompat.getColor(context, R.color.menu_secondary_fallback)
        )
    }
    
    /**
     * Get corner radius for menu elements
     * Sprint 1: Basic radius
     */
    fun getCornerRadius(): Float {
        return context.resources.getDimension(R.dimen.menu_corner_radius)
    }
    
    /**
     * Get elevation for menu elements
     * Sprint 1: Basic elevation
     */
    fun getElevation(): Float {
        return context.resources.getDimension(R.dimen.menu_elevation)
    }
    
    /**
     * Check if dark theme is active
     * Sprint 1: Basic dark mode detection
     */
    fun isDarkTheme(): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and 
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}