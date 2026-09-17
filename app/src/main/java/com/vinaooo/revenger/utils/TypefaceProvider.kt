package com.vinaooo.revenger.utils

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import com.vinaooo.revenger.R

/**
 * Named fixed-font + dynamic-font loading/caching for [FontUtils], split out purely to keep that
 * object under the project's function-count threshold. The four fixed-font getters
 * ([getArcadeTypeface]/[getPixelifyTypeface]/[getMicro5Typeface]/[getTiny5Typeface]) used to be
 * four near-identical copies of the same load-cache-catch-fallback sequence; they now share
 * [loadNamedTypeface] instead of duplicating it. Exposed back on [FontUtils] via Kotlin interface
 * delegation (`by`) so its public surface stays identical -- [getSelectedTypeface] is called from
 * outside this file (e.g. `SaveSlotsFragment`); the rest are kept on the same interface so the
 * whole "typeface resolution" group moves together.
 */
interface TypefaceLookup {
    /** Carrega e retorna a fonte arcade de assets/fonts/ */
    fun getArcadeTypeface(context: Context): Typeface?

    /** Carrega e retorna a fonte pixelify de assets/fonts/ */
    fun getPixelifyTypeface(context: Context): Typeface?

    /** Carrega e retorna a fonte micro5 de assets/fonts/ */
    fun getMicro5Typeface(context: Context): Typeface?

    /** Carrega e retorna a fonte tiny5 de assets/fonts/ */
    fun getTiny5Typeface(context: Context): Typeface?

    /** Loads and returns a dynamic font from assets/fonts/ based on the given name */
    fun getDynamicTypeface(context: Context, fontName: String): Typeface?

    /** Returns the selected font based on XML configuration */
    fun getSelectedTypeface(context: Context): Typeface?
}

class TypefaceProvider : TypefaceLookup {

    private enum class NamedFont(val assetPath: String, val label: String) {
        ARCADE("fonts/arcade.ttf", "Arcade"),
        PIXELIFY("fonts/pixelify_sans_variable.ttf", "Pixelify"),
        MICRO5("fonts/micro5_regular.ttf", "Micro5"),
        TINY5("fonts/tiny5_regular.ttf", "Tiny5"),
    }

    companion object {
        private const val TAG = "FontUtils"
    }

    private val namedTypefaces = mutableMapOf<NamedFont, Typeface>()
    private val dynamicTypefaces: MutableMap<String, Typeface> = mutableMapOf()

    private fun loadNamedTypeface(context: Context, font: NamedFont): Typeface? {
        namedTypefaces[font]?.let {
            return it
        }

        return try {
            val typeface = Typeface.createFromAsset(context.assets, font.assetPath)
            Log.d(TAG, "${font.label} font loaded successfully from assets/${font.assetPath}")
            namedTypefaces[font] = typeface
            typeface
            // Typeface.createFromAsset() documents throwing RuntimeException directly (not a
            // subclass) when the asset can't be found/parsed; RuntimeException is itself on
            // detekt's generic-exception list, so there is no narrower type -- the name-based
            // escape hatch is used instead.
        } catch (expectedFontLoadFailure: RuntimeException) {
            Log.e(
                    TAG,
                    "Error loading ${font.label} font from assets/${font.assetPath}",
                    expectedFontLoadFailure
            )
            // Fallback to system default font, cached like a successful load so we don't retry.
            Typeface.DEFAULT.also { namedTypefaces[font] = it }
        }
    }

    override fun getArcadeTypeface(context: Context): Typeface? =
            loadNamedTypeface(context, NamedFont.ARCADE)

    override fun getPixelifyTypeface(context: Context): Typeface? =
            loadNamedTypeface(context, NamedFont.PIXELIFY)

    override fun getMicro5Typeface(context: Context): Typeface? =
            loadNamedTypeface(context, NamedFont.MICRO5)

    override fun getTiny5Typeface(context: Context): Typeface? =
            loadNamedTypeface(context, NamedFont.TINY5)

    override fun getDynamicTypeface(context: Context, fontName: String): Typeface? {
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
            // Same RuntimeException rationale as the fixed-font loaders above.
        } catch (expectedFontLoadFailure: RuntimeException) {
            Log.d(
                    TAG,
                    "Dynamic font not found at assets/$fontPath, will use fallback",
                    expectedFontLoadFailure
            )
            null
        }
    }

    override fun getSelectedTypeface(context: Context): Typeface? {
        val selectedFont = context.resources.getString(R.string.rm_font)

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
}
