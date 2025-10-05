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

/**
 * RetroMenu3 Fragment - Cópia do ModernMenu Ativado pelo combo SELECT+START Menu fullscreen overlay
 * com Material Design 3
 */
class RetroMenu3Fragment : Fragment() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var menuContainer: LinearLayout
    private lateinit var continueMenu: MaterialCardView
    private lateinit var resetMenu: MaterialCardView
    private lateinit var saveStateMenu: MaterialCardView
    private lateinit var loadStateMenu: MaterialCardView
    private lateinit var audioToggleMenu: MaterialCardView
    private lateinit var fastForwardMenu: MaterialCardView
    private lateinit var exitMenu: MaterialCardView

    // Lista ordenada dos itens do menu para navegação
    private lateinit var menuItems: List<MaterialCardView>
    private var currentSelectedIndex = 0 // Começar com "Continue"

    // Dynamic content views (only views that exist in layout)
    private lateinit var audioToggleTitle: TextView
    private lateinit var fastForwardTitle: TextView

    // Menu option titles for color control
    private lateinit var continueTitle: TextView
    private lateinit var resetTitle: TextView
    private lateinit var saveStateTitle: TextView
    private lateinit var loadStateTitle: TextView
    private lateinit var exitTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowContinue: TextView
    private lateinit var selectionArrowReset: TextView
    private lateinit var selectionArrowSave: TextView
    private lateinit var selectionArrowLoad: TextView
    private lateinit var selectionArrowAudio: TextView
    private lateinit var selectionArrowFastForward: TextView
    private lateinit var selectionArrowExit: TextView

    // Callback interface
    interface RetroMenu3Listener {
        fun onResetGame()
        fun onSaveState()
        fun onLoadState()
        fun onToggleAudio()
        fun onFastForward()
        fun onExitGame(activity: androidx.fragment.app.FragmentActivity)
        fun getAudioState(): Boolean
        fun getFastForwardState(): Boolean
        fun hasSaveState(): Boolean
    }

    private var menuListener: RetroMenu3Listener? = null

    fun setMenuListener(listener: RetroMenu3Listener) {
        this.menuListener = listener
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.retro_menu3, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        setupViews(view)
        setupClickListeners()
        updateMenuState()
        animateMenuIn()

        // REMOVIDO: Não fecha mais ao tocar nas laterais
        // Menu só fecha quando pressionar START novamente ou selecionar Continue
    }

    private fun setupViews(view: View) {
        // Main container
        menuContainer = view.findViewById(R.id.menu_container)

        // Menu items
        continueMenu = view.findViewById(R.id.menu_continue)
        resetMenu = view.findViewById(R.id.menu_reset)
        saveStateMenu = view.findViewById(R.id.menu_save_state)
        loadStateMenu = view.findViewById(R.id.menu_load_state)
        audioToggleMenu = view.findViewById(R.id.menu_toggle_audio)
        fastForwardMenu = view.findViewById(R.id.menu_fast_forward)
        exitMenu = view.findViewById(R.id.menu_exit)

        // Inicializar lista ordenada dos itens do menu
        menuItems =
                listOf(
                        continueMenu,
                        resetMenu,
                        saveStateMenu,
                        loadStateMenu,
                        audioToggleMenu,
                        fastForwardMenu,
                        exitMenu
                )

        // Dynamic content views (only views that exist in layout)
        audioToggleTitle = view.findViewById(R.id.audio_toggle_title)
        fastForwardTitle = view.findViewById(R.id.fast_forward_title)

        // Initialize menu option titles
        continueTitle = view.findViewById(R.id.continue_title)
        resetTitle = view.findViewById(R.id.reset_title)
        saveStateTitle = view.findViewById(R.id.save_state_title)
        loadStateTitle = view.findViewById(R.id.load_state_title)
        exitTitle = view.findViewById(R.id.exit_title)

        // Initialize selection arrows
        selectionArrowContinue = view.findViewById(R.id.selection_arrow_continue)
        selectionArrowReset = view.findViewById(R.id.selection_arrow_reset)
        selectionArrowSave = view.findViewById(R.id.selection_arrow_save)
        selectionArrowLoad = view.findViewById(R.id.selection_arrow_load)
        selectionArrowAudio = view.findViewById(R.id.selection_arrow_audio)
        selectionArrowFastForward = view.findViewById(R.id.selection_arrow_fast_forward)
        selectionArrowExit = view.findViewById(R.id.selection_arrow_exit)

        // Definir primeiro item como selecionado
        updateSelectionVisual()
    }

    private fun setupClickListeners() {
        continueMenu.setOnClickListener {
            // Fecha o menu E limpa os estados do ControllerInput (incluindo comboAlreadyTriggered)
            animateMenuOut {
                dismissMenu()
                // Limpa keyLog e reseta comboAlreadyTriggered após fechar
                viewModel.clearControllerInputState()
            }
        }

        resetMenu.setOnClickListener {
            // Use centralized implementation
            viewModel.resetGameCentralized { animateMenuOut { dismissMenu() } }
        }

        saveStateMenu.setOnClickListener {
            // Use centralized implementation - no need for animateMenuOut since it's built-in
            viewModel.saveStateCentralized { dismissMenu() }
        }

        loadStateMenu.setOnClickListener { viewModel.loadStateCentralized { dismissMenu() } }

        audioToggleMenu.setOnClickListener {
            menuListener?.onToggleAudio()
            updateMenuState()
        }

        fastForwardMenu.setOnClickListener {
            menuListener?.onFastForward()
            updateMenuState()
        }

        exitMenu.setOnClickListener {
            activity?.let { menuListener?.onExitGame(it) }
            animateMenuOut { dismissMenu() }
        }
    }

    private fun updateMenuState() {
        val hasSaveState = menuListener?.hasSaveState() == true
        val isAudioEnabled = menuListener?.getAudioState() == true
        val isFastForwardEnabled = menuListener?.getFastForwardState() == true

        // Update load state appearance and enable/disable state
        loadStateMenu.isEnabled = hasSaveState
        loadStateMenu.alpha = if (hasSaveState) 1.0f else 0.5f

        // Update audio toggle
        audioToggleTitle.text =
                getString(if (isAudioEnabled) R.string.audio_on else R.string.audio_off)

        // Update fast forward toggle
        fastForwardTitle.text =
                getString(
                        if (isFastForwardEnabled) R.string.fast_forward_active
                        else R.string.fast_forward_inactive
                )
    }

    private fun animateMenuIn() {
        menuContainer.alpha = 0f
        menuContainer.scaleX = 0.8f
        menuContainer.scaleY = 0.8f

        menuContainer.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
    }

    private fun animateMenuOut(onEnd: () -> Unit) {
        menuContainer
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
            0 -> continueMenu.performClick() // Continue
            1 -> resetMenu.performClick() // Reset
            2 -> saveStateMenu.performClick() // Save State
            3 -> loadStateMenu.performClick() // Load State
            4 -> audioToggleMenu.performClick() // Audio Toggle
            5 -> fastForwardMenu.performClick() // Fast Forward
            6 -> exitMenu.performClick() // Exit
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
        continueTitle.setTextColor(
                if (currentSelectedIndex == 0) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        resetTitle.setTextColor(
                if (currentSelectedIndex == 1) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        saveStateTitle.setTextColor(
                if (currentSelectedIndex == 2) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        loadStateTitle.setTextColor(
                if (currentSelectedIndex == 3) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        audioToggleTitle.setTextColor(
                if (currentSelectedIndex == 4) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        fastForwardTitle.setTextColor(
                if (currentSelectedIndex == 5) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        exitTitle.setTextColor(
                if (currentSelectedIndex == 6) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )

        // Control selection arrows colors and visibility
        selectionArrowContinue.setTextColor(android.graphics.Color.YELLOW)
        selectionArrowContinue.visibility =
                if (currentSelectedIndex == 0) View.VISIBLE else View.GONE

        selectionArrowReset.setTextColor(android.graphics.Color.YELLOW)
        selectionArrowReset.visibility = if (currentSelectedIndex == 1) View.VISIBLE else View.GONE

        selectionArrowSave.setTextColor(android.graphics.Color.YELLOW)
        selectionArrowSave.visibility = if (currentSelectedIndex == 2) View.VISIBLE else View.GONE

        selectionArrowLoad.setTextColor(android.graphics.Color.YELLOW)
        selectionArrowLoad.visibility = if (currentSelectedIndex == 3) View.VISIBLE else View.GONE

        selectionArrowAudio.setTextColor(android.graphics.Color.YELLOW)
        selectionArrowAudio.visibility = if (currentSelectedIndex == 4) View.VISIBLE else View.GONE

        selectionArrowFastForward.setTextColor(android.graphics.Color.YELLOW)
        selectionArrowFastForward.visibility =
                if (currentSelectedIndex == 5) View.VISIBLE else View.GONE

        selectionArrowExit.setTextColor(android.graphics.Color.YELLOW)
        selectionArrowExit.visibility = if (currentSelectedIndex == 6) View.VISIBLE else View.GONE
    }

    /** Public method to dismiss the menu from outside */
    fun dismissMenuPublic() {
        dismissMenu()
    }

    companion object {
        fun newInstance(): RetroMenu3Fragment {
            return RetroMenu3Fragment()
        }
    }
}
