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
    /**
     * Create the appropriate effect based on type
     * @param type Effect type (0=None, 3=Scanline)
     * @return Corresponding effect instance
     */
    fun create(type: Int): BackgroundEffect {
        return when (type) {
            3 -> ScanlineEffect()
            else -> NoEffect()
        }
    }

    /** Returns list with all available effect types */
    fun getAllEffectTypes(): List<Int> = listOf(0, 3)

    /** Returns descriptive name of the effect */
    fun getEffectName(type: Int): String {
        return when (type) {
            0 -> "None (Dimming Only)"
            3 -> "Scanline (CRT)"
            else -> "Unknown"
        }
    }
}
