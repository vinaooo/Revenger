package com.vinaooo.revenger.ui.splash

/**
 * The CRT animation's timeline: what [CRTBootView] draws at a given progress. Boot runs progress
 * 0 → 1 (dot, then line, then expansion); shutdown (reverse) runs 1 → 0, so the same phases play
 * backwards and the expansion fill fades in instead of out. The view keeps only the drawing.
 */
object CRTBootTimeline {

    /** End of the dot phase (450 of 925 ms). */
    const val PHASE_1_END = 0.4865f

    /** End of the line phase (another 300 ms). */
    const val PHASE_2_END = 0.8108f

    /** Maximum value of an 8-bit alpha channel. */
    const val MAX_ALPHA = 255

    /** Maximum scanline opacity (~30%). */
    const val SCANLINE_MAX_OPACITY = 76

    enum class Phase {
        /** A white dot grows in the center. */
        DOT,

        /** The dot stays at full size while a horizontal line grows across the screen. */
        LINE,

        /** The picture expands vertically from the line, with scanlines. */
        EXPANSION
    }

    /**
     * Everything the view needs for one frame. Fractions are 0..1 of their maximum (dot radius,
     * screen width, screen height); alphas are 0..255.
     */
    data class Frame(
        val phase: Phase,
        val dotRadiusFraction: Float,
        val lineWidthFraction: Float,
        val expansionFraction: Float,
        val fillAlpha: Int,
        val scanlineAlpha: Int
    )

    /** Animator start and end progress: 0 → 1 for boot, 1 → 0 for shutdown. */
    fun progressRange(reverse: Boolean): Pair<Float, Float> = if (reverse) 1f to 0f else 0f to 1f

    fun phaseAt(progress: Float): Phase =
        when {
            progress < PHASE_1_END -> Phase.DOT
            progress < PHASE_2_END -> Phase.LINE
            else -> Phase.EXPANSION
        }

    fun frameAt(progress: Float, reverse: Boolean): Frame {
        val phase = phaseAt(progress)
        val dotRadius = if (phase == Phase.DOT) progress / PHASE_1_END else 1f
        val line =
            if (phase == Phase.LINE) (progress - PHASE_1_END) / (PHASE_2_END - PHASE_1_END) else 0f
        val expansion =
            if (phase == Phase.EXPANSION) (progress - PHASE_2_END) / (1f - PHASE_2_END) else 0f
        // Boot fades the picture out as it opens; shutdown fades it in as it closes.
        val fill = if (reverse) expansion else 1f - expansion
        return Frame(
            phase = phase,
            dotRadiusFraction = dotRadius,
            lineWidthFraction = line,
            expansionFraction = expansion,
            fillAlpha = if (phase == Phase.EXPANSION) (fill * MAX_ALPHA).toInt() else 0,
            scanlineAlpha = (expansion * SCANLINE_MAX_OPACITY).toInt()
        )
    }
}
