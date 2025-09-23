package com.vinaooo.revenger.gamepad

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

/**
 * View customizada de joystick virtual Implementação própria para substituir bibliotecas externas
 * @author vinaooo
 * @date 22 de Setembro de 2025
 */
class CustomJoystickView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "CustomJoystickView"
        private const val DEFAULT_SIZE_DP = 200
        private const val DEFAULT_BUTTON_SIZE_RATIO = 0.25f
        private const val DEFAULT_BACKGROUND_SIZE_RATIO = 0.75f
    }

    // Configurações visuais
    private var config: VirtualJoystickConfig = VirtualJoystickConfig.defaultConfig()

    // Paints para desenho
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Dimensões e posições
    private var centerX = 0f
    private var centerY = 0f
    private var backgroundRadius = 0f
    private var knobRadius = 0f
    private var knobX = 0f
    private var knobY = 0f

    // Estado do joystick
    private var isDragging = false
    private var currentAngle = 0
    private var currentStrength = 0

    // Listener para eventos
    private var onMoveListener: OnMoveListener? = null

    /** Interface para callbacks de movimento */
    interface OnMoveListener {
        fun onMove(angle: Int, strength: Int, xAxis: Float, yAxis: Float)
    }

    init {
        setupPaints()
        Log.d(TAG, "CustomJoystickView inicializado")
    }

    /** Configura os paints de desenho */
    private fun setupPaints() {
        backgroundPaint.apply {
            color = config.backgroundColor
            style = Paint.Style.FILL
        }

        borderPaint.apply {
            color = config.borderColor
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }

        knobPaint.apply {
            color = config.knobColor
            style = Paint.Style.FILL
        }
    }

    /** Aplica nova configuração ao joystick */
    fun applyConfig(newConfig: VirtualJoystickConfig) {
        this.config = newConfig
        setupPaints()
        requestLayout()
        invalidate()
        Log.d(TAG, "Nova configuração aplicada: ${config.name}")
    }

    /** Define o listener de movimento */
    fun setOnMoveListener(listener: OnMoveListener?) {
        this.onMoveListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (DEFAULT_SIZE_DP * resources.displayMetrics.density).toInt()
        val finalWidth = resolveSize(size, widthMeasureSpec)
        val finalHeight = resolveSize(size, heightMeasureSpec)
        setMeasuredDimension(finalWidth, finalHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        centerX = w / 2f
        centerY = h / 2f

        val minDimension = min(w, h)
        backgroundRadius = (minDimension * DEFAULT_BACKGROUND_SIZE_RATIO) / 2f
        knobRadius = (minDimension * DEFAULT_BUTTON_SIZE_RATIO) / 2f

        resetKnobPosition()
        Log.d(
                TAG,
                "Tamanho alterado: ${w}x${h}, backgroundRadius=$backgroundRadius, knobRadius=$knobRadius"
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Desenha o fundo do joystick
        canvas.drawCircle(centerX, centerY, backgroundRadius, backgroundPaint)
        canvas.drawCircle(centerX, centerY, backgroundRadius, borderPaint)

        // Desenha o botão/knob
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)

        // Desenha uma borda no knob
        canvas.drawCircle(knobX, knobY, knobRadius, borderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val distance = calculateDistance(event.x, event.y, centerX, centerY)
                if (distance <= backgroundRadius) {
                    isDragging = true
                    updateKnobPosition(event.x, event.y)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    updateKnobPosition(event.x, event.y)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    resetKnobPosition()
                    notifyListener()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    /** Atualiza a posição do knob baseado no toque */
    private fun updateKnobPosition(touchX: Float, touchY: Float) {
        val deltaX = touchX - centerX
        val deltaY = touchY - centerY
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

        if (distance <= backgroundRadius) {
            knobX = touchX
            knobY = touchY
        } else {
            // Limita o knob dentro do círculo de fundo
            val ratio = backgroundRadius / distance
            knobX = centerX + deltaX * ratio
            knobY = centerY + deltaY * ratio
        }

        calculateAngleAndStrength()
        invalidate()
        notifyListener()
    }

    /** Reseta o knob para o centro */
    private fun resetKnobPosition() {
        knobX = centerX
        knobY = centerY
        currentAngle = 0
        currentStrength = 0
        invalidate()
    }

    /** Calcula ângulo e força baseados na posição atual */
    private fun calculateAngleAndStrength() {
        val deltaX = knobX - centerX
        val deltaY = knobY - centerY
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

        // Calcula o ângulo (0-360, com 0 sendo direita)
        currentAngle =
                ((atan2(-deltaY.toDouble(), deltaX.toDouble()) * 180 / PI) + 360).toInt() % 360

        // Calcula a força (0-100)
        currentStrength = ((distance / backgroundRadius) * 100).toInt().coerceIn(0, 100)
    }

    /** Notifica o listener sobre mudanças */
    private fun notifyListener() {
        onMoveListener?.let { listener ->
            val xAxis =
                    (currentStrength * cos(Math.toRadians(currentAngle.toDouble()))).toFloat() /
                            100f
            val yAxis =
                    -(currentStrength * sin(Math.toRadians(currentAngle.toDouble()))).toFloat() /
                            100f

            listener.onMove(currentAngle, currentStrength, xAxis, yAxis)
        }
    }

    /** Calcula a distância entre dois pontos */
    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val deltaX = x1 - x2
        val deltaY = y1 - y2
        return sqrt(deltaX * deltaX + deltaY * deltaY)
    }
}
