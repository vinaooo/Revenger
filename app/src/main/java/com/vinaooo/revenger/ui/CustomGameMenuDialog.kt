package com.vinaooo.revenger.ui

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
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
        if (dialog?.isShowing == true) return

        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_menu, null)

        dialog =
                Dialog(context).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setContentView(dialogView)
                    window?.setBackgroundDrawableResource(android.R.color.transparent)
                    setCancelable(true)
                }

        // Desabilitando dimensões responsivas temporariamente para evitar crash
        // applyResponsiveDimensions(dialogView)

        setupClickListeners(dialogView, isAudioEnabled)

        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    fun isShowing(): Boolean = dialog?.isShowing == true

    private fun setupClickListeners(view: View, isAudioEnabled: Boolean) {
        val resetCard = view.findViewById<View>(R.id.menu_reset)
        val saveCard = view.findViewById<View>(R.id.menu_save_state)
        val loadCard = view.findViewById<View>(R.id.menu_load_state)
        val muteCard = view.findViewById<View>(R.id.menu_mute)
        val fastForwardCard = view.findViewById<View>(R.id.menu_fast_forward)
        val closeCard = view.findViewById<View>(R.id.menu_close)

        muteIcon = view.findViewById(R.id.mute_icon)

        // Atualizar ícone do mute baseado no estado do áudio
        updateMuteIcon(isAudioEnabled)

        resetCard.setOnClickListener {
            menuActions.onReset()
            dismiss()
        }

        saveCard.setOnClickListener {
            menuActions.onSaveState()
            dismiss()
        }

        loadCard.setOnClickListener {
            menuActions.onLoadState()
            dismiss()
        }

        muteCard.setOnClickListener {
            menuActions.onMute()
            // Atualizar ícone após o clique
            updateMuteIcon(!isAudioEnabled)
            dismiss()
        }

        fastForwardCard.setOnClickListener {
            menuActions.onFastForward()
            dismiss()
        }

        closeCard.setOnClickListener {
            menuActions.onClose()
            dismiss()
        }

        // Fechar ao clicar fora
        view.setOnClickListener { dismiss() }
    }

    private fun updateMuteIcon(isAudioEnabled: Boolean) {
        muteIcon?.setImageResource(
                if (isAudioEnabled) R.drawable.ic_volume_up else R.drawable.ic_volume_off
        )
    }

    /**
     * Aplica dimensões responsivas baseadas no tamanho da tela e orientação Em landscape: altura =
     * 85% da tela, largura = mesma medida (quadrado) Em portrait: largura = 85% da tela, altura =
     * mesma medida (quadrado)
     */
    private fun applyResponsiveDimensions(dialogView: View) {
        val displayMetrics = context.resources.displayMetrics

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Determinar orientação
        val isLandscape =
                context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Calcular tamanho do menu (85% da dimensão relevante)
        val menuSize =
                if (isLandscape) {
                    // Em landscape: baseado na altura (menor dimensão)
                    (screenHeight * 0.85f).toInt()
                } else {
                    // Em portrait: baseado na largura (menor dimensão)
                    (screenWidth * 0.85f).toInt()
                }

        // Aplicar tamanho calculado ao container principal
        val menuContainer = dialogView.findViewById<LinearLayout>(R.id.menu_container)
        if (menuContainer != null) {
            menuContainer.layoutParams = LinearLayout.LayoutParams(menuSize, menuSize)
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
        val menuContainer = dialogView.findViewById<LinearLayout>(R.id.menu_container)
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
                R.id.menu_save_state,
                cardHeight,
                iconSize,
                textSize,
                textMargin,
                marginSmall,
                marginSmall
        )
        applyCardDimensions(
                dialogView,
                R.id.menu_load_state,
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
                R.id.menu_fast_forward,
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
        val titleView = dialogView.findViewById<TextView>(R.id.menu_title)
        titleView?.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize)
            val layoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin = titleMargin
            this.layoutParams = layoutParams
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
