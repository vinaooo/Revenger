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
 * Fases da animação:
 * - Fase 1 (0.0 - 0.3): Ponto branco no centro (sem ícone)
 * - Fase 2 (0.3 - 0.5): Linha horizontal expandindo
 * - Fase 3 (0.5 - 1.0): Expansão vertical + scanlines com fade-in
 */
class CRTBootView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "CRTBootView"

        // Configurações da animação
        const val ANIMATION_DURATION = 925L // Duração total (ms) - 450ms + 300ms + 175ms
        const val PHASE_1_END = 0.4865f // Fim da fase do ponto (450ms)
        const val PHASE_2_END = 0.8108f // Fim da fase da linha (300ms mais)
        const val DOT_MAX_RADIUS = 8f // Raio máximo do ponto branco (dp)
        const val LINE_HEIGHT = 1f // Altura da linha (dp) - mais fina
        const val SCANLINE_SPACING = 4 // Espaçamento entre scanlines (px)
        const val SCANLINE_MAX_OPACITY = 76 // Opacidade máxima das scanlines (0-255, ~30%)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f
    private var animator: ValueAnimator? = null
    private var isAnimationStarted = false

    // Modo da animação: false = forward (boot), true = reverse (shutdown)
    private var isReverseMode = false

    // Callback para notificar término da animação
    var onAnimationEndListener: (() -> Unit)? = null

    // Conversão dp para px
    private val dotMaxRadiusPx: Float
        get() = DOT_MAX_RADIUS * resources.displayMetrics.density

    private val lineHeightPx: Float
        get() = LINE_HEIGHT * resources.displayMetrics.density

    init {
        // Garantir que a view seja desenhada
        setWillNotDraw(false)
        Log.d(TAG, "CRTBootView initialized")
    }

    /** Inicia a animação CRT */
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

    /** Inicia a animação CRT reversa (shutdown) */
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

    /** Para a animação (se estiver rodando) */
    fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // SEMPRE desenhar fundo preto para evitar flash de conteúdo indesejado
        canvas.drawColor(Color.BLACK)

        // Só desenhar animação se começou
        if (!isAnimationStarted) {
            return
        }

        val centerX = width / 2f
        val centerY = height / 2f

        // Configurar paint para branco
        paint.color = Color.WHITE

        when {
            // Fase 1: Ponto branco (0.0 - PHASE_1_END)
            progress < PHASE_1_END -> {
                drawDotPhase(canvas, centerX, centerY)
            }

            // Fase 2: Linha horizontal + Ponto FIXO no centro (PHASE_1_END até PHASE_2_END)
            // O ponto permanece com tamanho e opacidade máximos
            progress < PHASE_2_END -> {
                // Desenhar ponto (opacidade FIXA em 100%)
                drawDotPhaseWithAlpha(canvas, centerX, centerY, 1.0f)

                // Desenhar linha horizontal
                drawLinePhase(canvas, centerX, centerY)
            }

            // Fase 3: Expansão vertical + scanlines (PHASE_2_END até 1.0)
            else -> {
                drawExpansionPhase(canvas, centerX, centerY)
            }
        }
    }

    /** Fase 1: Desenha ponto branco crescendo no centro com efeito de glow/fade */
    private fun drawDotPhase(canvas: Canvas, centerX: Float, centerY: Float) {
        drawDotPhaseWithAlpha(canvas, centerX, centerY, 1f)
    }

    /**
     * Fase 1 com opacidade controlada (para transição suave) O tamanho do ponto fica fixo no
     * tamanho máximo quando entra Fase 2
     */
    private fun drawDotPhaseWithAlpha(
            canvas: Canvas,
            centerX: Float,
            centerY: Float,
            alphaMultiplier: Float
    ) {
        // Calcular o raio: durante Fase 1 cresce, durante Fase 2+ mantém tamanho máximo
        val radius =
                if (progress < PHASE_1_END) {
                    // Fase 1: crescer normalmente
                    (progress / PHASE_1_END) * dotMaxRadiusPx
                } else {
                    // Fase 2 e além: tamanho máximo fixo
                    dotMaxRadiusPx
                }

        // Opacidade aumenta rapidamente, multiplicada pelo fator de transição
        val mainAlpha = 255
        val finalAlpha = (mainAlpha * alphaMultiplier).toInt()

        // Desenhar múltiplas camadas de glow/fade ao redor do círculo
        val glowLayers = 5
        for (layer in glowLayers downTo 1) {
            // Aumenta o raio para cada camada de glow
            val glowRadius = radius + (layer * 4f)

            // Opacidade diminui para camadas externas
            val glowAlpha = (finalAlpha * (1f - layer / glowLayers.toFloat()) * 0.6f).toInt()
            paint.alpha = glowAlpha.coerceIn(0, 255)

            // Desenhar círculo de glow
            canvas.drawCircle(centerX, centerY, glowRadius, paint)
        }

        // Desenhar círculo principal
        paint.alpha = finalAlpha
        canvas.drawCircle(centerX, centerY, radius, paint)
    }

    /** Fase 2: Desenha linha horizontal expandindo com efeito de glow/fade */
    private fun drawLinePhase(canvas: Canvas, centerX: Float, centerY: Float) {
        drawLinePhaseWithAlpha(canvas, centerX, centerY, 1f)
    }

    /**
     * Fase 2 com opacidade controlada (para transição suave) Desenha linha com pontas de agulha
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

        // Largura da linha cresce de 0 até a largura total da tela
        val lineWidth = phaseProgress * width

        // Tamanho da ponta de agulha (proporcional à altura da linha)
        val needleLength = lineHeightPx * 3f

        // Desenhar múltiplas camadas de glow/fade ao redor da linha
        val glowLayers = 8
        for (layer in glowLayers downTo 1) {
            // Aumenta a altura (espessura) para cada camada de glow
            val glowHeight = lineHeightPx + (layer * 5f)
            val glowNeedleLength = needleLength + (layer * 3f)

            // Opacidade diminui para camadas externas
            val glowAlpha =
                    (255 * (1f - layer / glowLayers.toFloat()) * 0.65f * alphaMultiplier).toInt()
            paint.alpha = glowAlpha.coerceIn(0, 255)

            // Desenhar linha de glow com pontas de agulha
            drawNeedleLine(canvas, centerX, centerY, lineWidth, glowHeight, glowNeedleLength)
        }

        // Desenhar linha principal com pontas de agulha
        paint.alpha = (255 * alphaMultiplier).toInt()
        drawNeedleLine(canvas, centerX, centerY, lineWidth, lineHeightPx, needleLength)
    }

    /** Desenha uma linha com pontas de agulha (triangulares) nas extremidades */
    private fun drawNeedleLine(
            canvas: Canvas,
            centerX: Float,
            centerY: Float,
            lineWidth: Float,
            lineHeight: Float,
            needleLength: Float
    ) {
        val path = Path()

        val left = centerX - lineWidth / 2
        val right = centerX + lineWidth / 2
        val top = centerY - lineHeight / 2
        val bottom = centerY + lineHeight / 2

        // Desenhar forma com pontas de agulha:
        //     ←─────────────────────────→
        //    ◁═════════════════════════════▷
        //     ←─────────────────────────→

        path.moveTo(left - needleLength, centerY) // Ponta esquerda (agulha)
        path.lineTo(left, top) // Canto superior esquerdo
        path.lineTo(right, top) // Canto superior direito
        path.lineTo(right + needleLength, centerY) // Ponta direita (agulha)
        path.lineTo(right, bottom) // Canto inferior direito
        path.lineTo(left, bottom) // Canto inferior esquerdo
        path.close() // Volta para ponta esquerda

        canvas.drawPath(path, paint)
    }

    /** Fase 3: Desenha expansão vertical + scanlines com fade-out (ou fade-in se reverso) */
    private fun drawExpansionPhase(canvas: Canvas, centerX: Float, centerY: Float) {
        // Normalizar progress para esta fase (0.0 - 1.0)
        val phaseProgress = (progress - PHASE_2_END) / (1f - PHASE_2_END)

        // Altura do clip cresce do centro para as bordas
        val clipHeight = height * phaseProgress
        val clipTop = centerY - clipHeight / 2
        val clipBottom = centerY + clipHeight / 2

        // Raio dos cantos arredondados (30dp)
        val cornerRadius = 30f * resources.displayMetrics.density

        // Fade out no modo normal, fade in no modo reverso
        val fadeAlpha =
                if (isReverseMode) {
                    phaseProgress * 255 // Fade in: opacidade aumenta
                } else {
                    (1f - phaseProgress) * 255 // Fade out: opacidade diminui
                }

        // Salvar estado do canvas
        canvas.save()

        // Aplicar clip com cantos arredondados para criar efeito de expansão vertical
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

        // Preencher área com branco (simula a "imagem" da TV)
        paint.color = Color.WHITE
        paint.alpha = fadeAlpha.toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Restaurar canvas antes de desenhar scanlines
        canvas.restore()

        // Desenhar scanlines com fade gradual
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

        // Desenhar linhas apenas na área visível (entre clipTop e clipBottom)
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
