package com.vinaooo.revenger.ui.effects

import android.content.Context
import android.graphics.Bitmap

/**
 * Interface for RetroMenu2 background effects Each effect processes the paused game screenshot
 * differently
 */
interface BackgroundEffect {
    /**
     * Apply the effect to the captured bitmap
     * @param context Android context to access resources
     * @param screenshot Original game screenshot
     * @param intensity Effect intensity (0.0 to 1.0)
     * @return Processed bitmap with effect applied
     */
    fun apply(context: Context, screenshot: Bitmap, intensity: Float): Bitmap
}

/** Factory to create effect instances based on configured type */
object BackgroundEffectFactory {
    private const val EFFECT_TYPE_NONE = 0
    private const val EFFECT_TYPE_SCANLINE = 3

    /**
     * Create the appropriate effect based on type
     * @param type Effect type (0=None, 3=Scanline)
     * @return Corresponding effect instance
     */
    fun create(type: Int): BackgroundEffect {
        return when (type) {
            EFFECT_TYPE_SCANLINE -> ScanlineEffect()
            else -> NoEffect()
        }
    }

    /** Returns list with all available effect types */
    fun getAllEffectTypes(): List<Int> = listOf(EFFECT_TYPE_NONE, EFFECT_TYPE_SCANLINE)

    /** Returns descriptive name of the effect */
    fun getEffectName(type: Int): String {
        return when (type) {
            EFFECT_TYPE_NONE -> "None (Dimming Only)"
            EFFECT_TYPE_SCANLINE -> "Scanline (CRT)"
            else -> "Unknown"
        }
    }
}
