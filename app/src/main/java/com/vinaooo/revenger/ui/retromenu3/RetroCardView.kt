package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * RetroCardView - View customizada que substitui MaterialCardView seguindo o estilo retro do
 * RetroMenu3. Características visuais:
 * - Bordas pixeladas (não arredondadas)
 * - Cores retro (preto, branco, amarelo)
 * - Estados: normal, selecionado, pressionado
 * - Sem dependências do Material Design
 */
class RetroCardView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        FrameLayout(context, attrs, defStyleAttr) {

    // Estados da view
    enum class State {
        NORMAL,
        SELECTED,
        PRESSED
    }

    // Estado atual
    private var currentState = State.NORMAL

    // Paint para desenhar as bordas
    private val borderPaint =
            Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = false // Pixel perfect para estilo retro
            }

    // Paint para o background
    private val backgroundPaint =
            Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = false
            }

    // Paint para efeito de glow no estado selecionado
    private val glowPaint =
            Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                isAntiAlias = false
                color = Color.YELLOW
            }

    // Cores retro baseadas no tema
    private val colorNormal = Color.BLACK
    private val colorSelected = Color.YELLOW
    private val colorPressed = Color.WHITE
    private val colorBorder = Color.WHITE

    // Padding interno para conteúdo (como MaterialCardView)
    private val internalPadding = 16 // dp convertido para px será feito depois

    // Retângulo para cálculos de desenho
    private val drawRect = Rect()
    private val contentRect = Rect()

    init {
        // Configurações iniciais
        setWillNotDraw(false) // Permite onDraw
        isClickable = true

        // Converte padding para pixels
        val density = context.resources.displayMetrics.density
        setPadding(
                (internalPadding * density).toInt(),
                (internalPadding * density).toInt(),
                (internalPadding * density).toInt(),
                (internalPadding * density).toInt()
        )

        updateVisualState()
    }

    /** Define o estado visual da view */
    fun setState(state: State) {
        if (currentState != state) {
            currentState = state
            updateVisualState()
            invalidate()
        }
    }

    /** Atualiza a aparência visual baseada no estado atual */
    private fun updateVisualState() {
        when (currentState) {
            State.NORMAL -> {
                backgroundPaint.color = colorNormal
                borderPaint.color = colorBorder
            }
            State.SELECTED -> {
                backgroundPaint.color = colorSelected
                borderPaint.color = Color.BLACK // Borda escura para contraste
            }
            State.PRESSED -> {
                backgroundPaint.color = colorPressed
                borderPaint.color = Color.BLACK
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calcula retângulo de desenho (preenchendo toda a view)
        drawRect.set(0, 0, width, height)

        // Calcula retângulo de conteúdo (com padding interno)
        contentRect.set(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom)

        // Desenha background
        canvas.drawRect(drawRect, backgroundPaint)

        // Desenha efeito de glow para estado selecionado
        if (currentState == State.SELECTED) {
            canvas.drawRect(drawRect, glowPaint)
        }

        // Desenha borda pixelada
        canvas.drawRect(drawRect, borderPaint)
    }

    /** Método utilitário para definir estado baseado em interações */
    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        setState(if (selected) State.SELECTED else State.NORMAL)
    }

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        setState(if (pressed) State.PRESSED else State.NORMAL)
    }

    /** Obtém o estado atual */
    fun getState(): State = currentState
}
