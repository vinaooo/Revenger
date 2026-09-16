package com.vinaooo.revenger.utils

import android.util.Rational

/**
 * Resolves the aspect ratio to request for the Picture-in-Picture window.
 *
 * Android only accepts a PiP aspect ratio between 1:2.39 and 2.39:1 (`0.418f` is `1 / 2.39`).
 * The platform's preferred ratio is used when it falls in that range; otherwise a fallback
 * ratio derived from the current container dimensions is tried.
 */
object PipAspectRatioResolver {
    private const val MIN_RATIO = 0.418f
    private const val MAX_RATIO = 2.39f

    /**
     * Returns the [Rational] aspect ratio to use for the PiP window, or null if neither the
     * platform's preferred ratio ([ratioW]/[ratioH]) nor a fallback derived from the container's
     * current dimensions ([containerWidth]/[containerHeight]) falls within Android's allowed PiP
     * aspect-ratio range.
     */
    fun resolve(
        ratioW: Int,
        ratioH: Int,
        containerWidth: Int,
        containerHeight: Int
    ): Rational? {
        val primary = Rational(ratioW, ratioH)
        if (isInAllowedRange(primary)) return primary

        val fallback = containerRatioOrNull(containerWidth, containerHeight)
        return fallback?.takeIf { isInAllowedRange(it) }
    }

    private fun isInAllowedRange(ratio: Rational): Boolean =
        ratio.toFloat() in MIN_RATIO..MAX_RATIO

    private fun containerRatioOrNull(width: Int, height: Int): Rational? =
        if (width > 0 && height > 0) Rational(width, height) else null
}
