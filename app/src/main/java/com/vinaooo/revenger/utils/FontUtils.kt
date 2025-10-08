package com.vinaooo.revenger.utils

import android.content.Context
import android.graphics.Typeface
import android.util.Log

object FontUtils {
    private const val TAG = "FontUtils"
    private const val ARCADE_FONT_PATH = "fonts/arcadenormal.ttf"

    private var arcadeTypeface: Typeface? = null

    /** Carrega e retorna a fonte arcade de assets/fonts/ */
    fun getArcadeTypeface(context: Context): Typeface? {
        if (arcadeTypeface == null) {
            try {
                arcadeTypeface = Typeface.createFromAsset(context.assets, ARCADE_FONT_PATH)
                Log.d(TAG, "Fonte arcade carregada com sucesso de assets/$ARCADE_FONT_PATH")
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar fonte arcade de assets/$ARCADE_FONT_PATH", e)
                // Fallback para fonte padrão do sistema
                arcadeTypeface = Typeface.DEFAULT
            }
        }
        return arcadeTypeface
    }

    /** Aplica a fonte arcade a um TextView */
    fun applyArcadeFont(context: Context, textView: android.widget.TextView) {
        val typeface = getArcadeTypeface(context)
        if (typeface != null) {
            textView.typeface = typeface
        }
    }

    /** Aplica a fonte arcade a múltiplos TextViews */
    fun applyArcadeFont(context: Context, vararg textViews: android.widget.TextView) {
        val typeface = getArcadeTypeface(context)
        if (typeface != null) {
            textViews.forEach { it.typeface = typeface }
        }
    }
}
