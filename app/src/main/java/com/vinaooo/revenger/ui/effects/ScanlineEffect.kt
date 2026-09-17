package com.vinaooo.revenger.ui.effects

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Scanline effect simulating CRT tube TV (80s/90s) Draws horizontal lines over the
 * image Used when retromenu2_background_effect = 3
 */
class ScanlineEffect : BackgroundEffect {

    companion object {
        private const val DIMMING_ALPHA = 120 // 47% de escurecimento
        private const val SCANLINE_THICKNESS_MAX_PX = 4f
        private const val SCANLINE_SPACING_GAP_PX = 2f
        private const val SCANLINE_OPACITY_MAX = 180
        private const val SCANLINE_OPACITY_MIN = 50
        private const val GLOW_TINT_GREEN = 255
        private const val GLOW_TINT_BLUE = 100
        private const val GLOW_ALPHA = 15 // Muito sutil
    }

    override fun apply(context: Context, screenshot: Bitmap, intensity: Float): Bitmap {
        val width = screenshot.width
        val height = screenshot.height

        // Create mutable copy
        val result = screenshot.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Apply base dimming
        val dimmingPaint =
                Paint().apply {
                    color = Color.BLACK
                    alpha = DIMMING_ALPHA
                }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimmingPaint)

        // Intensity controls the thickness of scanlines
        // 0.1 = linhas muito finas (1px)
        // 1.0 = linhas grossas (4px)
        val lineThickness = (intensity * SCANLINE_THICKNESS_MAX_PX).coerceIn(1f, SCANLINE_THICKNESS_MAX_PX)
        val lineSpacing = lineThickness + SCANLINE_SPACING_GAP_PX // Spacing between lines

        // Paint for scanlines
        val scanlinePaint =
                Paint().apply {
                    color = Color.BLACK
                    // Variable opacity
                    alpha = (intensity * SCANLINE_OPACITY_MAX).toInt()
                            .coerceIn(SCANLINE_OPACITY_MIN, SCANLINE_OPACITY_MAX)
                    strokeWidth = lineThickness
                    isAntiAlias = false // Pixelated lines for retro effect
                }

        // Draw horizontal scanlines
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, scanlinePaint)
            y += lineSpacing
        }

        // Add light green/blue "glow" characteristic of CRT monitors
        // (optional - can be disabled if it doesn't look good)
        val glowPaint =
                Paint().apply {
                    color = Color.rgb(0, GLOW_TINT_GREEN, GLOW_TINT_BLUE) // Verde-azulado
                    alpha = GLOW_ALPHA
                }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)

        return result
    }
}
