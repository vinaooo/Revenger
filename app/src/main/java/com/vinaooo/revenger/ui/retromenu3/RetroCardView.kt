package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.LinearLayout

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
        LinearLayout(context, attrs, defStyleAttr) {

    // Estados da view
    enum class State {
        NORMAL,
        SELECTED,
        PRESSED
    }

    // Estado atual
    private var currentState = State.NORMAL

    // Propriedade para controlar se deve usar cor de fundo
    private var useBackgroundColor = true

    // Cores retro baseadas no tema
    private val colorSelected = Color.YELLOW
    private val colorPressed = Color.WHITE

    init {
        android.util.Log.d("RetroCardView", "RetroCardView init START")
        try {
            // Configurações iniciais
            isClickable = true

            // Background transparente para não interferir com conteúdo filho
            setBackgroundColor(Color.TRANSPARENT)

            // Orientação será definida pelo XML (horizontal para itens do menu)
            // Não definir orientação padrão para evitar conflitos com XML

            // Não aplica padding interno para compatibilidade com layouts existentes
            // O padding é controlado pelos layouts XML individuais

            updateVisualState()
            android.util.Log.d("RetroCardView", "RetroCardView init COMPLETED")
        } catch (e: Exception) {
            android.util.Log.e("RetroCardView", "RetroCardView init ERROR", e)
            throw e
        }
    }

    /** Define o estado visual da view */
    fun setState(state: State) {
        android.util.Log.d("RetroCardView", "setState called: $state (current: $currentState)")
        try {
            if (currentState != state) {
                currentState = state
                updateVisualState()
                android.util.Log.d("RetroCardView", "setState completed: new state $state")
            } else {
                android.util.Log.d("RetroCardView", "setState skipped: state already $state")
            }
        } catch (e: Exception) {
            android.util.Log.e("RetroCardView", "setState ERROR", e)
            throw e
        }
    }

    /** Atualiza a aparência visual baseada no estado atual */
    private fun updateVisualState() {
        android.util.Log.d(
                "RetroCardView",
                "updateVisualState called, currentState: $currentState, useBackgroundColor: $useBackgroundColor"
        )
        try {
            when (currentState) {
                State.NORMAL -> {
                    // Background transparente para estado normal
                    setBackgroundColor(Color.TRANSPARENT)
                    android.util.Log.d(
                            "RetroCardView",
                            "updateVisualState: NORMAL - background transparent"
                    )
                }
                State.SELECTED -> {
                    if (useBackgroundColor) {
                        // Background amarelo para estado selecionado (padrão)
                        setBackgroundColor(colorSelected)
                        android.util.Log.d(
                                "RetroCardView",
                                "updateVisualState: SELECTED - background yellow"
                        )
                    } else {
                        // Background transparente para estado selecionado (ProgressFragment)
                        setBackgroundColor(Color.TRANSPARENT)
                        android.util.Log.d(
                                "RetroCardView",
                                "updateVisualState: SELECTED - background transparent (no background mode)"
                        )
                    }
                }
                State.PRESSED -> {
                    if (useBackgroundColor) {
                        // Background branco para estado pressionado (padrão)
                        setBackgroundColor(colorPressed)
                        android.util.Log.d(
                                "RetroCardView",
                                "updateVisualState: PRESSED - background white"
                        )
                    } else {
                        // Background transparente para estado pressionado (ProgressFragment)
                        setBackgroundColor(Color.TRANSPARENT)
                        android.util.Log.d(
                                "RetroCardView",
                                "updateVisualState: PRESSED - background transparent (no background mode)"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RetroCardView", "updateVisualState ERROR", e)
            throw e
        }
    }

    /** Define se deve usar cor de fundo nos estados selecionado/pressionado */
    fun setUseBackgroundColor(useBackground: Boolean) {
        android.util.Log.d("RetroCardView", "setUseBackgroundColor: $useBackground")
        useBackgroundColor = useBackground
        updateVisualState() // Atualiza visual imediatamente
    }

    /** Obtém se está usando cor de fundo */
    fun getUseBackgroundColor(): Boolean = useBackgroundColor

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        setState(if (pressed) State.PRESSED else State.NORMAL)
    }

    /** Obtém o estado atual */
    fun getState(): State = currentState

    /** Garante que os LayoutParams sejam do tipo correto para LinearLayout */
    override fun generateLayoutParams(
            attrs: android.util.AttributeSet?
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    override fun generateLayoutParams(
            lp: android.view.ViewGroup.LayoutParams?
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(lp)
    }
}
