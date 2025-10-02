package com.vinaooo.revenger.ui.effects

import android.content.Context
import android.graphics.Bitmap

/**
 * Interface para efeitos de background do RetroMenu2 Cada efeito processa o screenshot do jogo
 * pausado de forma diferente
 */
interface BackgroundEffect {
    /**
     * Aplica o efeito no bitmap capturado
     * @param context Contexto Android para acessar recursos
     * @param screenshot Screenshot original do jogo
     * @param intensity Intensidade do efeito (0.0 a 1.0)
     * @return Bitmap processado com o efeito aplicado
     */
    fun apply(context: Context, screenshot: Bitmap, intensity: Float): Bitmap
}

/** Factory para criar instâncias de efeitos baseado no tipo configurado */
object BackgroundEffectFactory {
    /**
     * Cria o efeito apropriado baseado no tipo
     * @param type Tipo do efeito (0=None, 3=Scanline)
     * @return Instância do efeito correspondente
     */
    fun create(type: Int): BackgroundEffect {
        return when (type) {
            3 -> ScanlineEffect()
            else -> NoEffect()
        }
    }

    /** Retorna lista com todos os tipos de efeitos disponíveis */
    fun getAllEffectTypes(): List<Int> = listOf(0, 3)

    /** Retorna nome descritivo do efeito */
    fun getEffectName(type: Int): String {
        return when (type) {
            0 -> "None (Dimming Only)"
            3 -> "Scanline (CRT)"
            else -> "Unknown"
        }
    }
}
