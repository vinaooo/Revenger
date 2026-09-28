package com.vinaooo.revenger.utils

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import com.vinaooo.revenger.R

/**
 * Font loading and caching for [FontUtils], split out purely to keep that object under the
 * project's function-count threshold, and exposed back on it via Kotlin interface delegation
 * (`by`). Every font is a file in `assets/fonts/`, loaded by name: the configured one
 * (`rm_font`), with arcade as the fallback.
 */
interface TypefaceLookup {
    /** Loads the arcade font (`assets/fonts/arcade.ttf`), or the system default if it can't. */
    fun getArcadeTypeface(context: Context): Typeface

    /** Loads `assets/fonts/<fontName>.ttf`, or null if there is no such font. */
    fun getDynamicTypeface(context: Context, fontName: String): Typeface?

    /** The font named by `rm_font`, falling back to arcade. */
    fun getSelectedTypeface(context: Context): Typeface
}

class TypefaceProvider : TypefaceLookup {

    companion object {
        private const val TAG = "FontUtils"
        private const val FALLBACK_FONT = "arcade"
    }

    private val typefaces: MutableMap<String, Typeface> = mutableMapOf()

    override fun getArcadeTypeface(context: Context): Typeface =
            getDynamicTypeface(context, FALLBACK_FONT) ?: Typeface.DEFAULT

    override fun getDynamicTypeface(context: Context, fontName: String): Typeface? {
        val cachedTypeface = typefaces[fontName]
        if (cachedTypeface != null) {
            return cachedTypeface
        }

        val fontPath = "fonts/$fontName.ttf"
        return try {
            val typeface = Typeface.createFromAsset(context.assets, fontPath)
            Log.d(TAG, "Font loaded from assets/$fontPath")
            typefaces[fontName] = typeface
            typeface
            // Typeface.createFromAsset() documents throwing RuntimeException directly (not a
            // subclass) when the asset can't be found/parsed; RuntimeException is itself on
            // detekt's generic-exception list, so there is no narrower type -- the name-based
            // escape hatch is used instead.
        } catch (expectedFontLoadFailure: RuntimeException) {
            Log.d(TAG, "No font at assets/$fontPath", expectedFontLoadFailure)
            null
        }
    }

    override fun getSelectedTypeface(context: Context): Typeface {
        val selectedFont = context.resources.getString(R.string.rm_font)
        val typeface = getDynamicTypeface(context, selectedFont)
        if (typeface != null) {
            return typeface
        }
        Log.w(TAG, "Unknown font selection: $selectedFont, using $FALLBACK_FONT")
        return getArcadeTypeface(context)
    }
}
