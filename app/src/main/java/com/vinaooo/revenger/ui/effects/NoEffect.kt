package com.vinaooo.revenger.ui.effects

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Basic effect that only darkens the image without additional processing. Used when
 * retromenu2_background_effect = 0
 */
class NoEffect : BackgroundEffect {
    override fun apply(context: Context, screenshot: Bitmap, intensity: Float): Bitmap {
        // Create copy of original bitmap
        val result = screenshot.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Aplica overlay escuro (intensity controla opacidade)
        val paint =
                Paint().apply {
                    color = Color.BLACK
                    alpha = (intensity * 255).toInt().coerceIn(0, 255)
                }

        canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), paint)

        return result
    }
}
