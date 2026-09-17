package com.vinaooo.revenger.ui.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

/**
 * CRTBootView - View customizada que simula efeito de TV CRT ligando
 *
 * Animation phases:
 * - Phase 1 (0.0 - 0.3): White dot in center (no icon)
 * - Phase 2 (0.3 - 0.5): Horizontal line expanding
 * - Phase 3 (0.5 - 1.0): Vertical expansion + scanlines with fade-in
 */
class CRTBootView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "CRTBootView"

        // Animation settings
        const val ANIMATION_DURATION = 925L // Total duration (ms) - 450ms + 300ms + 175ms
        const val PHASE_1_END = 0.4865f // End of dot phase (450ms)
        const val PHASE_2_END = 0.8108f // End of line phase (additional 300ms)
        const val DOT_MAX_RADIUS = 8f // Maximum radius of the white dot (dp)
        const val LINE_HEIGHT = 1f // Line height (dp) - thinner
        const val SCANLINE_SPACING = 4 // Spacing between scanlines (px)
        const val SCANLINE_MAX_OPACITY = 76 // Maximum opacity of scanlines (0-255, ~30%)

        // Maximum value of an 8-bit alpha/color channel
        private const val MAX_ALPHA_VALUE = 255

        // Phase 1 (dot) glow effect
        private const val DOT_GLOW_LAYER_COUNT = 5
        private const val DOT_GLOW_RADIUS_STEP_PX = 4f
        private const val DOT_GLOW_ALPHA_FACTOR = 0.6f

        // Phase 2 (line) needle tips and glow effect
        private const val NEEDLE_LENGTH_MULTIPLIER = 3f
        private const val LINE_GLOW_LAYER_COUNT = 8
        private const val LINE_GLOW_HEIGHT_STEP_PX = 5f
        private const val LINE_GLOW_NEEDLE_STEP_PX = 3f
        private const val LINE_GLOW_ALPHA_FACTOR = 0.65f

        // Phase 3 (expansion) rounded corners
        private const val EXPANSION_CORNER_RADIUS_DP = 30f
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f
    private var animator: ValueAnimator? = null
    private var isAnimationStarted = false

    // Animation mode: false = forward (boot), true = reverse (shutdown)
    private var isReverseMode = false

    // Callback to notify end of animation
    var onAnimationEndListener: (() -> Unit)? = null

    // DP to px conversion
    private val dotMaxRadiusPx: Float
        get() = DOT_MAX_RADIUS * resources.displayMetrics.density

    private val lineHeightPx: Float
        get() = LINE_HEIGHT * resources.displayMetrics.density

    init {
        // Garantir que a view seja desenhada
        setWillNotDraw(false)
        Log.d(TAG, "CRTBootView initialized")
    }

    /** Start the CRT animation */
    fun startAnimation() {
        Log.d(TAG, "Starting CRT animation")

        isAnimationStarted = true
        isReverseMode = false
        animator?.cancel()

        animator =
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = ANIMATION_DURATION
                    interpolator = DecelerateInterpolator()

                    addUpdateListener { animation ->
                        progress = animation.animatedValue as Float
                        invalidate()
                    }

                    addListener(
                            object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    Log.d(TAG, "CRT animation completed")
                                    onAnimationEndListener?.invoke()
                                }
                            }
                    )
                }

        animator?.start()
    }

    /** Start the CRT reverse animation (shutdown) */
    fun startReverseAnimation() {
        Log.d(TAG, "Starting CRT reverse animation (shutdown)")

        isAnimationStarted = true
        isReverseMode = true
        animator?.cancel()

        animator =
                ValueAnimator.ofFloat(1f, 0f).apply {
                    duration = ANIMATION_DURATION
                    interpolator = AccelerateInterpolator()

                    addUpdateListener { animation ->
                        progress = animation.animatedValue as Float
                        invalidate()
                    }

                    addListener(
                            object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    Log.d(TAG, "CRT reverse animation completed")
                                    onAnimationEndListener?.invoke()
                                }
                            }
                    )
                }

        animator?.start()
    }

    /** Stop the animation (if running) */
    fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // ALWAYS draw black background to avoid flashing unwanted content
        canvas.drawColor(Color.BLACK)

        // Only draw animation if it has started
        if (!isAnimationStarted) {
            return
        }

        val centerX = width / 2f
        val centerY = height / 2f

        // Configurar paint para branco
        paint.color = Color.WHITE

        when {
            // Phase 1: White dot (0.0 - PHASE_1_END)
            progress < PHASE_1_END -> {
                drawDotPhase(canvas, centerX, centerY)
            }

            // Phase 2: Horizontal line + FIXED dot at center (PHASE_1_END to PHASE_2_END)
            // The dot remains at maximum size and opacity
            progress < PHASE_2_END -> {
                // Draw dot (fixed opacity at 100%)
                drawDotPhaseWithAlpha(canvas, centerX, centerY, 1.0f)

                // Draw horizontal line
                drawLinePhase(canvas, centerX, centerY)
            }

            // Phase 3: Vertical expansion + scanlines (PHASE_2_END to 1.0)
            else -> {
                drawExpansionPhase(canvas, centerY)
            }
        }
    }

    /** Phase 1: Draw a white dot growing in the center with glow/fade effect */
    private fun drawDotPhase(canvas: Canvas, centerX: Float, centerY: Float) {
        drawDotPhaseWithAlpha(canvas, centerX, centerY, 1f)
    }

    /**
     * Phase 1 with controlled opacity (for smooth transition). Dot size is fixed at max
     * when entering Phase 2
     */
    private fun drawDotPhaseWithAlpha(
            canvas: Canvas,
            centerX: Float,
            centerY: Float,
            alphaMultiplier: Float
    ) {
        // Calculate radius: during Phase 1 it grows, during Phase 2+ it stays at max size
        val radius =
                if (progress < PHASE_1_END) {
                    // Phase 1: grow normally
                    (progress / PHASE_1_END) * dotMaxRadiusPx
                } else {
                    // Phase 2 and beyond: fixed max size
                    dotMaxRadiusPx
                }

        // Opacity increases rapidly, multiplied by transition factor
        val mainAlpha = MAX_ALPHA_VALUE
        val finalAlpha = (mainAlpha * alphaMultiplier).toInt()

        // Draw multiple glow/fade layers around the circle
        val glowLayers = DOT_GLOW_LAYER_COUNT
        for (layer in glowLayers downTo 1) {
            // Aumenta o raio para cada camada de glow
            val glowRadius = radius + (layer * DOT_GLOW_RADIUS_STEP_PX)

            // Opacidade diminui para camadas externas
            val glowAlpha =
                    (finalAlpha * (1f - layer / glowLayers.toFloat()) * DOT_GLOW_ALPHA_FACTOR)
                            .toInt()
            paint.alpha = glowAlpha.coerceIn(0, MAX_ALPHA_VALUE)

            // Draw glow circle
            canvas.drawCircle(centerX, centerY, glowRadius, paint)
        }

        // Draw main circle
        paint.alpha = finalAlpha
        canvas.drawCircle(centerX, centerY, radius, paint)
    }

    /** Phase 2: Draw horizontal line expanding with glow/fade effect */
    private fun drawLinePhase(canvas: Canvas, centerX: Float, centerY: Float) {
        drawLinePhaseWithAlpha(canvas, centerX, centerY, 1f)
    }

    /**
     * Phase 2 with controlled opacity (for smooth transition). Draw line with needle tips
     * (triangulares)
     */
    private fun drawLinePhaseWithAlpha(
            canvas: Canvas,
            centerX: Float,
            centerY: Float,
            alphaMultiplier: Float
    ) {
        // Normalizar progress para esta fase (0.0 - 1.0)
        val phaseProgress = (progress - PHASE_1_END) / (PHASE_2_END - PHASE_1_END)

        // Line width grows from 0 to full screen width
        val lineWidth = phaseProgress * width

        // Length of needle tip (proportional to line height)
        val needleLength = lineHeightPx * NEEDLE_LENGTH_MULTIPLIER

        // Draw multiple glow/fade layers around the line
        val glowLayers = LINE_GLOW_LAYER_COUNT
        for (layer in glowLayers downTo 1) {
            // Increase height (thickness) for each glow layer
            val glowHeight = lineHeightPx + (layer * LINE_GLOW_HEIGHT_STEP_PX)
            val glowNeedleLength = needleLength + (layer * LINE_GLOW_NEEDLE_STEP_PX)

            // Opacidade diminui para camadas externas
            val glowAlpha =
                    (MAX_ALPHA_VALUE *
                                    (1f - layer / glowLayers.toFloat()) *
                                    LINE_GLOW_ALPHA_FACTOR *
                                    alphaMultiplier)
                            .toInt()
            paint.alpha = glowAlpha.coerceIn(0, MAX_ALPHA_VALUE)

            // Draw glow line with needle tips
            drawNeedleLine(
                    canvas,
                    NeedleLineGeometry(centerX, centerY, lineWidth, glowHeight, glowNeedleLength)
            )
        }

        // Draw main line with needle tips
        paint.alpha = (MAX_ALPHA_VALUE * alphaMultiplier).toInt()
        drawNeedleLine(
                canvas,
                NeedleLineGeometry(centerX, centerY, lineWidth, lineHeightPx, needleLength)
        )
    }

    /**
     * Geometry parameters for [drawNeedleLine], grouped into a single value to keep that
     * function's parameter list short.
     */
    private data class NeedleLineGeometry(
            val centerX: Float,
            val centerY: Float,
            val lineWidth: Float,
            val lineHeight: Float,
            val needleLength: Float
    )

    /** Desenha uma linha com pontas de agulha (triangulares) nas extremidades */
    private fun drawNeedleLine(canvas: Canvas, geometry: NeedleLineGeometry) {
        val path = Path()

        val left = geometry.centerX - geometry.lineWidth / 2
        val right = geometry.centerX + geometry.lineWidth / 2
        val top = geometry.centerY - geometry.lineHeight / 2
        val bottom = geometry.centerY + geometry.lineHeight / 2
        val centerY = geometry.centerY
        val needleLength = geometry.needleLength

        // Draw shape with needle tips:
        //     ←─────────────────────────→
        //    ◁═════════════════════════════▷
        //     ←─────────────────────────→

        path.moveTo(left - needleLength, centerY) // Ponta esquerda (agulha)
        path.lineTo(left, top) // Canto superior esquerdo
        path.lineTo(right, top) // Canto superior direito
        path.lineTo(right + needleLength, centerY) // Ponta direita (agulha)
        path.lineTo(right, bottom) // Canto inferior direito
        path.lineTo(left, bottom) // Canto inferior esquerdo
        path.close() // Returns to leftmost point

        canvas.drawPath(path, paint)
    }

    /** Phase 3: Draw vertical expansion + scanlines with fade-out (or fade-in if reverse) */
    private fun drawExpansionPhase(canvas: Canvas, centerY: Float) {
        // Normalizar progress para esta fase (0.0 - 1.0)
        val phaseProgress = (progress - PHASE_2_END) / (1f - PHASE_2_END)

        // Clip height grows from center toward edges
        val clipHeight = height * phaseProgress
        val clipTop = centerY - clipHeight / 2
        val clipBottom = centerY + clipHeight / 2

        // Raio dos cantos arredondados
        val cornerRadius = EXPANSION_CORNER_RADIUS_DP * resources.displayMetrics.density

        // Fade out no modo normal, fade in no modo reverso
        val fadeAlpha =
                if (isReverseMode) {
                    phaseProgress * MAX_ALPHA_VALUE // Fade in: opacidade aumenta
                } else {
                    (1f - phaseProgress) * MAX_ALPHA_VALUE // Fade out: opacidade diminui
                }

        // Salvar estado do canvas
        canvas.save()

        // Apply clip with rounded corners to create vertical expansion effect
        val clipPath = Path()
        clipPath.addRoundRect(
                0f,
                clipTop,
                width.toFloat(),
                clipBottom,
                cornerRadius,
                cornerRadius,
                Path.Direction.CW
        )
        canvas.clipPath(clipPath)

        // Fill area with white (simulates the TV "image")
        paint.color = Color.WHITE
        paint.alpha = fadeAlpha.toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Restore canvas before drawing scanlines
        canvas.restore()

        // Draw scanlines with gradual fade
        drawScanlines(canvas, phaseProgress, clipTop, clipBottom)
    }

    /** Desenha efeito de scanlines (linhas horizontais simulando CRT) */
    private fun drawScanlines(
            canvas: Canvas,
            phaseProgress: Float,
            clipTop: Float,
            clipBottom: Float
    ) {
        // Scanlines aparecem gradualmente
        val scanlineOpacity = (phaseProgress * SCANLINE_MAX_OPACITY).toInt()

        if (scanlineOpacity <= 0) return

        paint.color = Color.argb(scanlineOpacity, 0, 0, 0)
        paint.strokeWidth = 1f

        // Draw lines only in visible area (between clipTop and clipBottom)
        val startY = (clipTop.toInt() / SCANLINE_SPACING) * SCANLINE_SPACING
        val endY = clipBottom.toInt()

        var y = startY
        while (y <= endY) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
            y += SCANLINE_SPACING
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
        Log.d(TAG, "CRTBootView detached, animation stopped")
    }
}
