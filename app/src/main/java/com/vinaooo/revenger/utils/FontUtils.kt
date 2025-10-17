package com.vinaooo.revenger.utils

import android.content.Context
import android.graphics.Typeface
import android.util.Log

object FontUtils {
    private const val TAG = "FontUtils"
    private const val ARCADE_FONT_PATH = "fonts/arcade.ttf"
    private const val PIXELIFY_FONT_PATH = "fonts/pixelify_sans_variable.ttf"
    private const val MICRO5_FONT_PATH = "fonts/micro5_regular.ttf"
    private const val TINY5_FONT_PATH = "fonts/tiny5_regular.ttf"

    private var arcadeTypeface: Typeface? = null
    private var pixelifyTypeface: Typeface? = null
    private var micro5Typeface: Typeface? = null
    private var tiny5Typeface: Typeface? = null

    /** Carrega e retorna a fonte arcade de assets/fonts/ */
    fun getArcadeTypeface(context: Context): Typeface? {
        if (arcadeTypeface == null) {
            try {
                arcadeTypeface = Typeface.createFromAsset(context.assets, ARCADE_FONT_PATH)
                Log.d(TAG, "Arcade font loaded successfully from assets/$ARCADE_FONT_PATH")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading arcade font from assets/$ARCADE_FONT_PATH", e)
                // Fallback to system default font
                arcadeTypeface = Typeface.DEFAULT
            }
        }
        return arcadeTypeface
    }

    /** Carrega e retorna a fonte pixelify de assets/fonts/ */
    fun getPixelifyTypeface(context: Context): Typeface? {
        if (pixelifyTypeface == null) {
            try {
                pixelifyTypeface = Typeface.createFromAsset(context.assets, PIXELIFY_FONT_PATH)
                Log.d(TAG, "Pixelify font loaded successfully from assets/$PIXELIFY_FONT_PATH")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading pixelify font from assets/$PIXELIFY_FONT_PATH", e)
                // Fallback to system default font
                pixelifyTypeface = Typeface.DEFAULT
            }
        }
        return pixelifyTypeface
    }

    /** Carrega e retorna a fonte micro5 de assets/fonts/ */
    fun getMicro5Typeface(context: Context): Typeface? {
        if (micro5Typeface == null) {
            try {
                micro5Typeface = Typeface.createFromAsset(context.assets, MICRO5_FONT_PATH)
                Log.d(TAG, "Micro5 font loaded successfully from assets/$MICRO5_FONT_PATH")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading micro5 font from assets/$MICRO5_FONT_PATH", e)
                // Fallback to system default font
                micro5Typeface = Typeface.DEFAULT
            }
        }
        return micro5Typeface
    }

    /** Carrega e retorna a fonte tiny5 de assets/fonts/ */
    fun getTiny5Typeface(context: Context): Typeface? {
        if (tiny5Typeface == null) {
            try {
                tiny5Typeface = Typeface.createFromAsset(context.assets, TINY5_FONT_PATH)
                Log.d(TAG, "Tiny5 font loaded successfully from assets/$TINY5_FONT_PATH")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading tiny5 font from assets/$TINY5_FONT_PATH", e)
                // Fallback to system default font
                tiny5Typeface = Typeface.DEFAULT
            }
        }
        return tiny5Typeface
    }

    /** Retorna a fonte selecionada baseada na configuração do XML */
    fun getSelectedTypeface(context: Context): Typeface? {
        val selectedFont =
                context.resources.getString(com.vinaooo.revenger.R.string.retro_menu3_font)
        return when (selectedFont) {
            "pixelify" -> getPixelifyTypeface(context)
            "micro5" -> getMicro5Typeface(context)
            "tiny5" -> getTiny5Typeface(context)
            "arcade" -> getArcadeTypeface(context)
            else -> {
                Log.w(TAG, "Unknown font selection: $selectedFont, using arcade as default")
                getArcadeTypeface(context)
            }
        }
    }

    /** Aplica a fonte selecionada a um TextView */
    fun applySelectedFont(context: Context, textView: android.widget.TextView) {
        val typeface = getSelectedTypeface(context)
        if (typeface != null) {
            textView.typeface = typeface
        }
    }

    /** Aplica a fonte selecionada a múltiplos TextViews */
    fun applySelectedFont(context: Context, vararg textViews: android.widget.TextView) {
        val typeface = getSelectedTypeface(context)
        if (typeface != null) {
            textViews.forEach { it.typeface = typeface }
        }
    }

    /** Aplica a fonte arcade a um TextView (para compatibilidade) */
    fun applyArcadeFont(context: Context, textView: android.widget.TextView) {
        val typeface = getArcadeTypeface(context)
        if (typeface != null) {
            textView.typeface = typeface
        }
    }

    /** Aplica a fonte arcade a múltiplos TextViews (para compatibilidade) */
    fun applyArcadeFont(context: Context, vararg textViews: android.widget.TextView) {
        val typeface = getArcadeTypeface(context)
        if (typeface != null) {
            textViews.forEach { it.typeface = typeface }
        }
    }
}
