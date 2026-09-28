package com.vinaooo.revenger.utils

import android.content.Context

object FontUtils : TypefaceLookup by TypefaceProvider() {

    /** Aplica a fonte selecionada a um TextView */
    fun applySelectedFont(context: Context, textView: android.widget.TextView) {
        textView.typeface = getSelectedTypeface(context)
    }

    /** Applies configured capitalization to the text of a TextView */
    fun applyTextCapitalization(context: Context, textView: android.widget.TextView) {
        val originalText = textView.text.toString()
        val capitalizedText = capitalize(context, originalText)

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

        return capitalize(context, raw)
    }

    // rm_text_capitalization: 1 = first letter only, 2 = all uppercase, anything else = unchanged.
    private fun capitalize(context: Context, text: String): String =
            when (context.resources.getInteger(com.vinaooo.revenger.R.integer.rm_text_capitalization)) {
                1 -> text.lowercase().replaceFirstChar { it.uppercase() }
                2 -> text.uppercase()
                else -> text
            }
}
