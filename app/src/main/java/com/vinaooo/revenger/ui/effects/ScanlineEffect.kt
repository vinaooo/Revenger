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
                    alpha = 120 // 47% de escurecimento
                }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimmingPaint)

        // Intensity controls the thickness of scanlines
        // 0.1 = linhas muito finas (1px)
        // 1.0 = linhas grossas (4px)
        val lineThickness = (intensity * 4f).coerceIn(1f, 4f)
        val lineSpacing = lineThickness + 2f // Spacing between lines

        // Paint for scanlines
        val scanlinePaint =
                Paint().apply {
                    color = Color.BLACK
                    alpha = (intensity * 180).toInt().coerceIn(50, 180) // Variable opacity
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
                    color = Color.rgb(0, 255, 100) // Verde-azulado
                    alpha = 15 // Muito sutil
                }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)

        return result
    }
}
