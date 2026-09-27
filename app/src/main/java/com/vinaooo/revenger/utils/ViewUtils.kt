package com.vinaooo.revenger.utils

import android.view.View
import android.view.ViewGroup

/**
 * Utility class for common view operations used across menu fragments. Eliminates code duplication
 * and provides centralized view management.
 */
object ViewUtils {

    /**
     * Recursively sets z=0, elevation=0, and translationZ=0 on all views to ensure menu stays below
     * gamepad layer. Some views (cards, buttons) get a default elevation from their style that
     * overrides the XML attributes.
     *
     * @param view The root view to process recursively
     */
    fun forceZeroElevationRecursively(view: View) {
        view.z = 0f
        view.elevation = 0f
        view.translationZ = 0f

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                forceZeroElevationRecursively(view.getChildAt(i))
            }
        }
    }

    /**
     * Applies the selected font (based on configuration) to multiple TextViews. Supports both
     * arcade and pixelify fonts according to the rm_font setting.
     *
     * @param context The context to use for font loading
     * @param views Variable number of TextViews to apply font to
     */
    fun applySelectedFontToViews(
            context: android.content.Context,
            vararg views: android.widget.TextView
    ) {
        views.forEach { view -> FontUtils.applySelectedFont(context, view) }
    }

    /** Same as [applySelectedFontToViews], for a caller that already holds a collection. */
    fun applySelectedFontToViews(
            context: android.content.Context,
            views: Collection<android.widget.TextView>
    ) {
        views.forEach { view -> FontUtils.applySelectedFont(context, view) }
    }

    /** Batch menu animation through [AnimationOptimizer.animateViewsBatchOptimized]. */
    fun animateMenuViewsBatchOptimized(
            views: Array<View>,
            toAlpha: Float,
            toScale: Float,
            duration: Long = 200,
            onEnd: (() -> Unit)? = null
    ) {
        AnimationOptimizer.animateViewsBatchOptimized(views, toAlpha, toScale, duration, onEnd)
    }
}
