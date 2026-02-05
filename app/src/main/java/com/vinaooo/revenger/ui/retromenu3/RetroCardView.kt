package com.vinaooo.revenger.ui.retromenu3


import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.LinearLayout
import com.vinaooo.revenger.utils.MenuLogger

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

    companion object {
        // Constantes de cores para consistência visual - agora usando recursos XML
        val COLOR_SELECTED =
                android.graphics.Color.parseColor("#FFFF00") // Item selecionado na navegação
        val COLOR_PRESSED = android.graphics.Color.parseColor("#FFFFFF") // Item pressionado/toque
        val COLOR_NORMAL = android.graphics.Color.parseColor("#00000000") // Estado normal
    }

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

    // Cores retro baseadas no tema - usa constantes do companion object
    private val colorSelected = COLOR_SELECTED
    private val colorPressed = COLOR_PRESSED

    init {
        MenuLogger.lifecycle("RetroCardView init START")
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
            MenuLogger.lifecycle("RetroCardView init COMPLETED")
        } catch (e: Exception) {
            MenuLogger.e("RetroCardView initialization failed: ${e.message}", e)
            throw RuntimeException("Failed to initialize RetroCardView: ${e.message}", e)
        }
    }

    /** Define o estado visual da view */
    fun setState(state: State) {
        MenuLogger.state("setState called: $state (current: $currentState)")
        try {
            if (currentState != state) {
                currentState = state
                updateVisualState()
                MenuLogger.state("setState completed: new state $state")
            } else {
                MenuLogger.state("setState skipped: state already $state")
            }
        } catch (e: Exception) {
            MenuLogger.e("Failed to set RetroCardView state to $state: ${e.message}", e)
            throw RuntimeException(
                    "RetroCardView state change failed for state $state: ${e.message}",
                    e
            )
        }
    }

    /** Atualiza a aparência visual baseada no estado atual */
    private fun updateVisualState() {
        MenuLogger.state(
                "updateVisualState called, currentState: $currentState, useBackgroundColor: $useBackgroundColor"
        )
        try {
            when (currentState) {
                State.NORMAL -> {
                    // Background transparente para estado normal
                    setBackgroundColor(Color.TRANSPARENT)
                    MenuLogger.state("updateVisualState: NORMAL - background transparent")
                }
                State.SELECTED -> {
                    if (useBackgroundColor) {
                        // Background amarelo para estado selecionado (padrão)
                        setBackgroundColor(colorSelected)
                        MenuLogger.state("updateVisualState: SELECTED - background yellow")
                    } else {
                        // Background transparente para estado selecionado (ProgressFragment)
                        setBackgroundColor(Color.TRANSPARENT)
                        MenuLogger.state(
                                "updateVisualState: SELECTED - background transparent (no background mode)"
                        )
                    }
                }
                State.PRESSED -> {
                    if (useBackgroundColor) {
                        // Background branco para estado pressionado (padrão)
                        setBackgroundColor(colorPressed)
                        MenuLogger.state("updateVisualState: PRESSED - background white")
                    } else {
                        // Background transparente para estado pressionado (ProgressFragment)
                        setBackgroundColor(Color.TRANSPARENT)
                        MenuLogger.state(
                                "updateVisualState: PRESSED - background transparent (no background mode)"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            MenuLogger.e(
                    "Failed to update RetroCardView visual state ($currentState): ${e.message}",
                    e
            )
            throw RuntimeException(
                    "RetroCardView visual update failed for state $currentState: ${e.message}",
                    e
            )
        }
    }

    /** Define se deve usar cor de fundo nos estados selecionado/pressionado */
    fun setUseBackgroundColor(useBackground: Boolean) {
        MenuLogger.state("setUseBackgroundColor: $useBackground")
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
