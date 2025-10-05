package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/** SettingsMenuFragment - Submenu de configurações com visual idêntico ao RetroMenu3 */
class SettingsMenuFragment : Fragment() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var settingsMenuContainer: LinearLayout
    private lateinit var videoSettings: MaterialCardView
    private lateinit var audioSettings: MaterialCardView
    private lateinit var controlsSettings: MaterialCardView
    private lateinit var backSettings: MaterialCardView

    // Lista ordenada dos itens do menu para navegação
    private lateinit var menuItems: List<MaterialCardView>
    private var currentSelectedIndex = 0 // Começar com "Video Settings"

    // Menu option titles for color control
    private lateinit var videoTitle: TextView
    private lateinit var audioSettingsTitle: TextView
    private lateinit var controlsTitle: TextView
    private lateinit var backTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowVideo: TextView
    private lateinit var selectionArrowAudioSettings: TextView
    private lateinit var selectionArrowControls: TextView
    private lateinit var selectionArrowBack: TextView

    // Callback interface
    interface SettingsMenuListener {
        fun onBackToMainMenu()
    }

    private var settingsListener: SettingsMenuListener? = null

    fun setSettingsListener(listener: SettingsMenuListener) {
        this.settingsListener = listener
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.settings_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d(
                "SettingsMenuFragment",
                "onViewCreated: SettingsMenuFragment view created"
        )

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        setupViews(view)
        setupClickListeners()
        animateMenuIn()

        android.util.Log.d(
                "SettingsMenuFragment",
                "onViewCreated: SettingsMenuFragment setup completed"
        )
        // REMOVIDO: Não fecha mais ao tocar nas laterais
        // Menu só fecha quando selecionar Back
    }

    private fun setupViews(view: View) {
        // Main container
        settingsMenuContainer = view.findViewById(R.id.settings_menu_container)

        // Menu items
        videoSettings = view.findViewById(R.id.settings_video)
        audioSettings = view.findViewById(R.id.settings_audio)
        controlsSettings = view.findViewById(R.id.settings_controls)
        backSettings = view.findViewById(R.id.settings_back)

        // Inicializar lista ordenada dos itens do menu
        menuItems = listOf(videoSettings, audioSettings, controlsSettings, backSettings)

        // Initialize menu option titles
        videoTitle = view.findViewById(R.id.video_title)
        audioSettingsTitle = view.findViewById(R.id.audio_settings_title)
        controlsTitle = view.findViewById(R.id.controls_title)
        backTitle = view.findViewById(R.id.back_title)

        // Initialize selection arrows
        selectionArrowVideo = view.findViewById(R.id.selection_arrow_video)
        selectionArrowAudioSettings = view.findViewById(R.id.selection_arrow_audio_settings)
        selectionArrowControls = view.findViewById(R.id.selection_arrow_controls)
        selectionArrowBack = view.findViewById(R.id.selection_arrow_back)

        // Definir primeiro item como selecionado
        updateSelectionVisual()
    }

    private fun setupClickListeners() {
        videoSettings.setOnClickListener {
            // Video Settings - Not implemented yet
            android.widget.Toast.makeText(
                            requireContext(),
                            "Video Settings - Not implemented",
                            android.widget.Toast.LENGTH_SHORT
                    )
                    .show()
        }

        audioSettings.setOnClickListener {
            // Audio Settings - Not implemented yet
            android.widget.Toast.makeText(
                            requireContext(),
                            "Audio Settings - Not implemented",
                            android.widget.Toast.LENGTH_SHORT
                    )
                    .show()
        }

        controlsSettings.setOnClickListener {
            // Controls - Not implemented yet
            android.widget.Toast.makeText(
                            requireContext(),
                            "Controls - Not implemented",
                            android.widget.Toast.LENGTH_SHORT
                    )
                    .show()
        }

        backSettings.setOnClickListener {
            // Voltar ao menu principal
            // Apenas notificar o listener, a animação será feita pelo dismissSettingsMenu()
            settingsListener?.onBackToMainMenu()
        }
    }

    private fun animateMenuIn() {
        settingsMenuContainer.alpha = 0f
        settingsMenuContainer.scaleX = 0.8f
        settingsMenuContainer.scaleY = 0.8f

        settingsMenuContainer.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
    }

    private fun animateMenuOut(onEnd: () -> Unit) {
        settingsMenuContainer
                .animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(150)
                .setListener(
                        object : android.animation.Animator.AnimatorListener {
                            override fun onAnimationStart(animation: android.animation.Animator) {}
                            override fun onAnimationEnd(animation: android.animation.Animator) {
                                onEnd()
                            }
                            override fun onAnimationCancel(animation: android.animation.Animator) {}
                            override fun onAnimationRepeat(animation: android.animation.Animator) {}
                        }
                )
                .start()
    }

    private fun dismissMenu() {
        // IMPORTANTE: Não chamar dismissRetroMenu3() aqui para evitar crashes
        // Apenas remover o fragment visualmente
        animateMenuOut { parentFragmentManager.beginTransaction().remove(this).commit() }
    }

    /** Navegar para cima no menu */
    fun navigateUp() {
        currentSelectedIndex = (currentSelectedIndex - 1 + menuItems.size) % menuItems.size
        updateSelectionVisual()
    }

    /** Navegar para baixo no menu */
    fun navigateDown() {
        currentSelectedIndex = (currentSelectedIndex + 1) % menuItems.size
        updateSelectionVisual()
    }

    /** Confirmar seleção atual */
    fun confirmSelection() {
        when (currentSelectedIndex) {
            0 -> videoSettings.performClick() // Video Settings
            1 -> audioSettings.performClick() // Audio Settings
            2 -> controlsSettings.performClick() // Controls
            3 -> backSettings.performClick() // Back
        }
    }

    /** Atualizar visual da seleção */
    private fun updateSelectionVisual() {
        menuItems.forEach { item ->
            // Removido: background color dos cards individuais
            // Seleção agora indicada apenas por texto amarelo e setas
            item.strokeWidth = 0
            item.strokeColor = android.graphics.Color.TRANSPARENT
            item.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        // Control text colors based on selection
        videoTitle.setTextColor(
                if (currentSelectedIndex == 0) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        audioSettingsTitle.setTextColor(
                if (currentSelectedIndex == 1) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        controlsTitle.setTextColor(
                if (currentSelectedIndex == 2) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        backTitle.setTextColor(
                if (currentSelectedIndex == 3) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )

        // Control selection arrows colors and visibility
        // CORREÇÃO: Item selecionado mostra seta sem margem (colada ao texto)
        val arrowMarginEnd = resources.getDimensionPixelSize(R.dimen.retro_menu3_arrow_margin_end)

        // Video Settings
        if (currentSelectedIndex == 0) {
            selectionArrowVideo.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowVideo.visibility = View.VISIBLE
            (selectionArrowVideo.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowVideo.visibility = View.GONE
        }

        // Audio Settings
        if (currentSelectedIndex == 1) {
            selectionArrowAudioSettings.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowAudioSettings.visibility = View.VISIBLE
            (selectionArrowAudioSettings.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowAudioSettings.visibility = View.GONE
        }

        // Controls
        if (currentSelectedIndex == 2) {
            selectionArrowControls.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowControls.visibility = View.VISIBLE
            (selectionArrowControls.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowControls.visibility = View.GONE
        }

        // Back
        if (currentSelectedIndex == 3) {
            selectionArrowBack.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowBack.visibility = View.VISIBLE
            (selectionArrowBack.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowBack.visibility = View.GONE
        }

        // Forçar atualização do layout
        settingsMenuContainer.requestLayout()
    }

    /** Public method to dismiss the menu from outside */
    fun dismissMenuPublic() {
        dismissMenu()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Garantir que comboAlreadyTriggered seja resetado quando o fragment for destruído
        try {
            (settingsListener as? com.vinaooo.revenger.viewmodels.GameActivityViewModel)?.let {
                    viewModel ->
                // Chamar clearKeyLog através do ViewModel para resetar o estado do combo
                viewModel.clearControllerKeyLog()
            }
        } catch (e: Exception) {
            android.util.Log.w(
                    "SettingsMenuFragment",
                    "Erro ao resetar combo state no onDestroy",
                    e
            )
        }
    }

    companion object {
        fun newInstance(): SettingsMenuFragment {
            return SettingsMenuFragment()
        }
    }
}
