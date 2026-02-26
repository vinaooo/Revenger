package com.vinaooo.revenger.utils

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import com.vinaooo.revenger.R

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
    private var dynamicTypefaces: MutableMap<String, Typeface> = mutableMapOf()

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

    /** Loads and returns a dynamic font from assets/fonts/ based on the given name */
    fun getDynamicTypeface(context: Context, fontName: String): Typeface? {
        // Check if we already have this font cached
        val cacheKey = "dynamic_$fontName"
        val cachedTypeface = dynamicTypefaces[cacheKey]
        if (cachedTypeface != null) {
            return cachedTypeface
        }

        // Tentar carregar o arquivo .ttf
        val fontPath = "fonts/$fontName.ttf"
        return try {
            val typeface = Typeface.createFromAsset(context.assets, fontPath)
            Log.d(TAG, "Dynamic font loaded successfully from assets/$fontPath")
            // Cachear a fonte carregada
            dynamicTypefaces[cacheKey] = typeface
            typeface
        } catch (e: Exception) {
            Log.d(TAG, "Dynamic font not found at assets/$fontPath, will use fallback")
            null
        }
    }

    /** Returns the selected font based on XML configuration */
    fun getSelectedTypeface(context: Context): Typeface? {
        val selectedFont =
                context.resources.getString(com.vinaooo.revenger.R.string.rm_font)

        // Primeiro tentar carregar dinamicamente como arquivo .ttf
        val dynamicTypeface = getDynamicTypeface(context, selectedFont)
        if (dynamicTypeface != null) {
            return dynamicTypeface
        }

        // Fallback to hardcoded options
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

    /** Applies the selected font to multiple TextViews */
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

    /** Applies the arcade font to multiple TextViews (for compatibility) */
    fun applyArcadeFont(context: Context, vararg textViews: android.widget.TextView) {
        val typeface = getArcadeTypeface(context)
        if (typeface != null) {
            textViews.forEach { it.typeface = typeface }
        }
    }

    /** Applies configured capitalization to the text of a TextView */
    fun applyTextCapitalization(context: Context, textView: android.widget.TextView) {
        val capitalizationStyle =
                context.resources.getInteger(
                        com.vinaooo.revenger.R.integer.rm_text_capitalization
                )
        val originalText = textView.text.toString()

        val capitalizedText =
                when (capitalizationStyle) {
                    1 -> {
                        // First letter uppercase - more robust
                        originalText.lowercase().replaceFirstChar { it.uppercase() }
                    }
                    2 -> originalText.uppercase() // All uppercase
                    else -> originalText // Normal (default)
                }

        if (capitalizedText != originalText) {
            textView.text = capitalizedText
        }
    }

    /** Applies configured capitalization to the text of multiple TextViews */
    fun applyTextCapitalization(context: Context, vararg textViews: android.widget.TextView) {
        textViews.forEach { applyTextCapitalization(context, it) }
    }

    /**
     * Returns a string already formatted according to the `rm_text_capitalization` setting.
     * Use em Toasts, hints e em qualquer lugar que construa texto programaticamente.
     */
    fun getCapitalizedString(context: Context, resId: Int, vararg formatArgs: Any?): String {
        val raw = if (formatArgs.isNotEmpty()) {
            context.resources.getString(resId, *formatArgs)
        } else {
            context.resources.getString(resId)
        }

        val capitalizationStyle =
                context.resources.getInteger(
                        com.vinaooo.revenger.R.integer.rm_text_capitalization
                )

        return when (capitalizationStyle) {
            1 -> raw.lowercase().replaceFirstChar { it.uppercase() }
            2 -> raw.uppercase()
            else -> raw
        }
    }

    /** Returns the selected text color based on RetroMenu3 configuration */
    fun getSelectedTextColor(context: Context): Int {
        return androidx.core.content.ContextCompat.getColor(
                context,
                com.vinaooo.revenger.R.color.rm_selected_color
        )
    }

    /** Returns the color of unselected text based on RetroMenu3 configuration */
    fun getUnselectedTextColor(context: Context): Int {
        return androidx.core.content.ContextCompat.getColor(
                context,
                com.vinaooo.revenger.R.color.rm_normal_color
        )
    }
}
