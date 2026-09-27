package com.vinaooo.revenger.utils

import android.content.Context

object FontUtils : TypefaceLookup by TypefaceProvider() {

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

    /** Same as [applyTextCapitalization], for a caller that already holds a collection. */
    fun applyTextCapitalization(context: Context, textViews: Collection<android.widget.TextView>) {
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
}
