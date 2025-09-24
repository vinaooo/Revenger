package com.vinaooo.revenger.ui

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.vinaooo.revenger.R

class CustomGameMenuDialog(private val context: Context, private val menuActions: MenuActions) {

    companion object {
        private const val TAG = "CustomGameMenuDialog"
    }

    interface MenuActions {
        fun onReset()
        fun onSaveState()
        fun onLoadState()
        fun onMute()
        fun onFastForward()
        fun onClose()
    }

    private var dialog: Dialog? = null
    private var muteIcon: ImageView? = null

    fun show(isAudioEnabled: Boolean) {
        Log.d(TAG, "show() chamado - isAudioEnabled: $isAudioEnabled")

        if (dialog?.isShowing == true) {
            Log.w(TAG, "Dialog já está sendo exibido, ignorando nova chamada")
            return
        }

        try {
            Log.d(TAG, "Inflating layout dialog_menu")
            val inflater = LayoutInflater.from(context)
            val dialogView = inflater.inflate(R.layout.dialog_menu, null)
            Log.d(TAG, "Layout inflado com sucesso")

            Log.d(TAG, "Criando Dialog")
            dialog =
                    Dialog(context).apply {
                        requestWindowFeature(Window.FEATURE_NO_TITLE)
                        setContentView(dialogView)
                        window?.setBackgroundDrawableResource(android.R.color.transparent)
                        setCancelable(true)
                    }
            Log.d(TAG, "Dialog criado com sucesso")

            // Temporariamente desabilitado até corrigir layout XML
            // applyResponsiveDimensions(dialogView)

            Log.d(TAG, "Configurando click listeners")
            setupClickListeners(dialogView, isAudioEnabled)
            Log.d(TAG, "Click listeners configurados")

            Log.d(TAG, "Exibindo dialog")
            dialog?.show()
            Log.i(TAG, "Dialog exibido com sucesso!")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao exibir dialog", e)
        }
    }

    fun dismiss() {
        Log.d(TAG, "dismiss() chamado")
        try {
            dialog?.dismiss()
            Log.d(TAG, "Dialog dismissed com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao dismiss dialog", e)
        }
    }

    fun isShowing(): Boolean {
        val showing = dialog?.isShowing == true
        Log.d(TAG, "isShowing() = $showing")
        return showing
    }

    private fun setupClickListeners(view: View, isAudioEnabled: Boolean) {
        Log.d(TAG, "setupClickListeners() iniciado")

        try {
            val resetCard = view.findViewById<View>(R.id.menu_reset)
            val saveCard = view.findViewById<View>(R.id.menu_save)
            val loadCard = view.findViewById<View>(R.id.menu_load)
            val muteCard = view.findViewById<View>(R.id.menu_mute)
            val fastForwardCard = view.findViewById<View>(R.id.menu_fast)
            val closeCard = view.findViewById<View>(R.id.menu_close)

            Log.d(
                    TAG,
                    "Views encontradas - reset: ${resetCard != null}, save: ${saveCard != null}, load: ${loadCard != null}"
            )
            Log.d(
                    TAG,
                    "Views encontradas - mute: ${muteCard != null}, fast: ${fastForwardCard != null}, close: ${closeCard != null}"
            )

            // Para o ícone do mute, vamos procurar dentro do layout mute
            muteIcon =
                    muteCard?.findViewById<ImageView>(android.R.id.icon)
                            ?: muteCard?.let { muteCardView ->
                                // Se não encontrar, procurar pelo ImageView dentro do mute card
                                if (muteCardView is ViewGroup) {
                                    for (i in 0 until muteCardView.childCount) {
                                        val child = muteCardView.getChildAt(i)
                                        if (child is ImageView) {
                                            Log.d(TAG, "Mute icon encontrado na posição $i")
                                            return@let child
                                        }
                                    }
                                }
                                Log.w(TAG, "Mute icon não encontrado")
                                null
                            }

            // Atualizar ícone do mute baseado no estado do áudio
            updateMuteIcon(isAudioEnabled)

            resetCard?.setOnClickListener {
                Log.d(TAG, "Reset button clicado")
                menuActions.onReset()
                dismiss()
            }

            saveCard?.setOnClickListener {
                Log.d(TAG, "Save button clicado")
                menuActions.onSaveState()
                dismiss()
            }

            loadCard?.setOnClickListener {
                Log.d(TAG, "Load button clicado")
                menuActions.onLoadState()
                dismiss()
            }

            muteCard?.setOnClickListener {
                Log.d(TAG, "Mute button clicado")
                menuActions.onMute()
                // Atualizar ícone após o clique
                updateMuteIcon(!isAudioEnabled)
                dismiss()
            }

            fastForwardCard?.setOnClickListener {
                Log.d(TAG, "Fast Forward button clicado")
                menuActions.onFastForward()
                dismiss()
            }

            closeCard?.setOnClickListener {
                Log.d(TAG, "Close button clicado")
                menuActions.onClose()
                dismiss()
            }

            // Fechar ao clicar fora
            view.setOnClickListener {
                Log.d(TAG, "Clique fora do menu - fechando")
                dismiss()
            }

            Log.d(TAG, "setupClickListeners() concluído com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao configurar click listeners", e)
        }
    }

    private fun updateMuteIcon(isAudioEnabled: Boolean) {
        Log.d(TAG, "updateMuteIcon() - isAudioEnabled: $isAudioEnabled")
        try {
            muteIcon?.setImageResource(
                    if (isAudioEnabled) R.drawable.ic_volume_up else R.drawable.ic_volume_off
            )
            Log.d(TAG, "Mute icon atualizado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar mute icon", e)
        }
    }

    /**
     * Aplica dimensões responsivas baseadas no tamanho da tela e orientação Em landscape: altura =
     * 90% da tela, largura = mesma medida (quadrado) Em portrait: largura = 90% da tela, altura =
     * mesma medida (quadrado)
     */
    private fun applyResponsiveDimensions(dialogView: View) {
        val displayMetrics = context.resources.displayMetrics

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Determinar orientação
        val isLandscape =
                context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Calcular tamanho do menu (90% da dimensão relevante)
        val menuSize =
                if (isLandscape) {
                    // Em landscape: baseado na altura (menor dimensão)
                    (screenHeight * 0.90f).toInt()
                } else {
                    // Em portrait: baseado na largura (menor dimensão)
                    (screenWidth * 0.90f).toInt()
                }

        // Aplicar tamanho calculado ao container principal (LinearLayout)
        val menuContainer = dialogView.findViewById<LinearLayout>(R.id.menu_container)
        if (menuContainer != null) {
            val layoutParams = menuContainer.layoutParams
            layoutParams.width = menuSize
            layoutParams.height = menuSize
            menuContainer.layoutParams = layoutParams
        }

        // Calcular e aplicar dimensões proporcionais
        applyProportionalDimensions(dialogView, menuSize)
    }

    /** Aplica dimensões proporcionais baseadas no tamanho calculado do menu */
    private fun applyProportionalDimensions(dialogView: View, menuSize: Int) {
        // Usar ratios fixos diretamente no código por enquanto
        val paddingRatio = 0.07f
        val titleSizeRatio = 0.053f
        val titleMarginRatio = 0.047f
        val cardHeightRatio = 0.188f
        val iconSizeRatio = 0.075f
        val textSizeRatio = 0.026f
        val textMarginRatio = 0.009f
        val marginLargeRatio = 0.047f
        val marginSmallRatio = 0.024f

        // Calcular dimensões em pixels
        val padding = (menuSize * paddingRatio).toInt()
        val titleSize = menuSize * titleSizeRatio
        val titleMargin = (menuSize * titleMarginRatio).toInt()
        val cardHeight = (menuSize * cardHeightRatio).toInt()
        val iconSize = (menuSize * iconSizeRatio).toInt()
        val textSize = menuSize * textSizeRatio
        val textMargin = (menuSize * textMarginRatio).toInt()
        val marginLarge = (menuSize * marginLargeRatio).toInt()
        val marginSmall = (menuSize * marginSmallRatio).toInt()

        // Aplicar padding ao container principal
        val menuContainer = dialogView as? LinearLayout
        menuContainer?.setPadding(padding, padding, padding, padding)

        // Aplicar dimensões ao título (procurar pelo primeiro TextView)
        applyTitleDimensions(dialogView, titleSize, titleMargin)

        // Aplicar dimensões aos cartões
        applyCardDimensions(
                dialogView,
                R.id.menu_reset,
                cardHeight,
                iconSize,
                textSize,
                textMargin,
                marginLarge,
                0
        )
        applyCardDimensions(
                dialogView,
                R.id.menu_save,
                cardHeight,
                iconSize,
                textSize,
                textMargin,
                marginSmall,
                marginSmall
        )
        applyCardDimensions(
                dialogView,
                R.id.menu_load,
                cardHeight,
                iconSize,
                textSize,
                textMargin,
                0,
                marginLarge
        )
        applyCardDimensions(
                dialogView,
                R.id.menu_mute,
                cardHeight,
                iconSize,
                textSize,
                textMargin,
                marginLarge,
                0
        )
        applyCardDimensions(
                dialogView,
                R.id.menu_fast,
                cardHeight,
                iconSize,
                textSize,
                textMargin,
                marginSmall,
                marginSmall
        )
        applyCardDimensions(
                dialogView,
                R.id.menu_close,
                cardHeight,
                iconSize,
                textSize,
                textMargin,
                0,
                marginLarge
        )
    }

    /** Aplica dimensões ao título */
    private fun applyTitleDimensions(dialogView: View, titleSize: Float, titleMargin: Int) {
        // Procurar pelo primeiro TextView na hierarquia (título)
        val container = dialogView as? LinearLayout
        if (container != null) {
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is TextView) {
                    child.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize)
                    val layoutParams = child.layoutParams as? ViewGroup.MarginLayoutParams
                    layoutParams?.bottomMargin = titleMargin
                    child.layoutParams = layoutParams
                    break
                }
            }
        }
    }

    /** Aplica dimensões a um cartão específico */
    private fun applyCardDimensions(
            dialogView: View,
            cardId: Int,
            height: Int,
            iconSize: Int,
            textSize: Float,
            textMargin: Int,
            marginStart: Int,
            marginEnd: Int
    ) {
        val card = dialogView.findViewById<LinearLayout>(cardId)
        card?.apply {
            // Definir altura do cartão
            val layoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.height = height
            layoutParams.marginStart = marginStart
            layoutParams.marginEnd = marginEnd
            this.layoutParams = layoutParams

            // Aplicar tamanho do ícone
            val icon = findViewById<ImageView>(android.R.id.icon)
            if (icon == null) {
                // Buscar por qualquer ImageView dentro do cartão
                for (i in 0 until childCount) {
                    val child = getChildAt(i)
                    if (child is ImageView) {
                        val iconLayoutParams = child.layoutParams
                        iconLayoutParams.width = iconSize
                        iconLayoutParams.height = iconSize
                        child.layoutParams = iconLayoutParams
                        break
                    }
                }
            } else {
                val iconLayoutParams = icon.layoutParams
                iconLayoutParams.width = iconSize
                iconLayoutParams.height = iconSize
                icon.layoutParams = iconLayoutParams
            }

            // Aplicar tamanho e margem do texto
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child is TextView) {
                    child.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                    val textLayoutParams = child.layoutParams as ViewGroup.MarginLayoutParams
                    textLayoutParams.topMargin = textMargin
                    child.layoutParams = textLayoutParams
                    break
                }
            }
        }
    }
}
