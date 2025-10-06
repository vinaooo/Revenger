package com.vinaooo.revenger.ui.effects

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Efeito de scanlines simulando TV de tubo CRT (anos 80/90) Desenha linhas horizontais sobre a
 * imagem Usado quando retromenu2_background_effect = 3
 */
class ScanlineEffect : BackgroundEffect {
    override fun apply(context: Context, screenshot: Bitmap, intensity: Float): Bitmap {
        val width = screenshot.width
        val height = screenshot.height

        // Cria cópia mutável
        val result = screenshot.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Aplica dimming base
        val dimmingPaint =
                Paint().apply {
                    color = Color.BLACK
                    alpha = 120 // 47% de escurecimento
                }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimmingPaint)

        // Intensity controla a espessura das scanlines
        // 0.1 = linhas muito finas (1px)
        // 1.0 = linhas grossas (4px)
        val lineThickness = (intensity * 4f).coerceIn(1f, 4f)
        val lineSpacing = lineThickness + 2f // Espaçamento entre linhas

        // Paint para as scanlines
        val scanlinePaint =
                Paint().apply {
                    color = Color.BLACK
                    alpha = (intensity * 180).toInt().coerceIn(50, 180) // Opacidade variável
                    strokeWidth = lineThickness
                    isAntiAlias = false // Linhas pixeladas para efeito retro
                }

        // Desenha scanlines horizontais
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, scanlinePaint)
            y += lineSpacing
        }

        // Adiciona leve "glow" verde/azul característico de monitores CRT
        // (opcional - pode ser desabilitado se não ficar bom)
        val glowPaint =
                Paint().apply {
                    color = Color.rgb(0, 255, 100) // Verde-azulado
                    alpha = 15 // Muito sutil
                }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)

        return result
    }
}
