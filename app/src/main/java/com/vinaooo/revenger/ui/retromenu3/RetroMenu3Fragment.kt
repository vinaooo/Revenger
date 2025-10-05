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
    private lateinit var settingsMenu: MaterialCardView
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
    private lateinit var settingsTitle: TextView
    private lateinit var exitTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowContinue: TextView
    private lateinit var selectionArrowReset: TextView
    private lateinit var selectionArrowSave: TextView
    private lateinit var selectionArrowLoad: TextView
    private lateinit var selectionArrowSettings: TextView
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
        settingsMenu = view.findViewById(R.id.menu_settings)
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
                        settingsMenu,
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
        settingsTitle = view.findViewById(R.id.settings_title)
        exitTitle = view.findViewById(R.id.exit_title)

        // Initialize selection arrows
        selectionArrowContinue = view.findViewById(R.id.selection_arrow_continue)
        selectionArrowReset = view.findViewById(R.id.selection_arrow_reset)
        selectionArrowSave = view.findViewById(R.id.selection_arrow_save)
        selectionArrowLoad = view.findViewById(R.id.selection_arrow_load)
        selectionArrowSettings = view.findViewById(R.id.selection_arrow_settings)
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

        settingsMenu.setOnClickListener {
            // Abrir submenu de configurações
            openSettingsSubmenu()
        }

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
            0 -> continueMenu.performClick() // Continue
            1 -> resetMenu.performClick() // Reset
            2 -> saveStateMenu.performClick() // Save State
            3 -> loadStateMenu.performClick() // Load State
            4 -> settingsMenu.performClick() // Settings
            5 -> audioToggleMenu.performClick() // Audio Toggle
            6 -> fastForwardMenu.performClick() // Fast Forward
            7 -> exitMenu.performClick() // Exit
        }
    }

    /** Tornar o menu principal invisível (quando submenu é aberto) */
    fun hideMainMenu() {
        android.util.Log.d(
                "RetroMenu3Fragment",
                "hideMainMenu: Hiding menu content but keeping background"
        )
        // Esconder apenas o conteúdo do menu, mantendo o fundo para o submenu
        menuContainer.visibility = View.INVISIBLE
        android.util.Log.d(
                "RetroMenu3Fragment",
                "hideMainMenu: Menu content hidden, background should remain visible"
        )
    }

    /** Tornar o menu principal visível novamente (quando submenu é fechado) */
    fun showMainMenu() {
        android.util.Log.d("RetroMenu3Fragment", "showMainMenu: Showing menu content again")
        menuContainer.visibility = View.VISIBLE
        // Garantir que a seleção visual seja atualizada quando o menu voltar a ser visível
        updateSelectionVisual()
        android.util.Log.d(
                "RetroMenu3Fragment",
                "showMainMenu: Menu should be fully visible now, visibility = ${menuContainer.visibility}"
        )
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
        settingsTitle.setTextColor(
                if (currentSelectedIndex == 4) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        audioToggleTitle.setTextColor(
                if (currentSelectedIndex == 5) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        fastForwardTitle.setTextColor(
                if (currentSelectedIndex == 6) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        exitTitle.setTextColor(
                if (currentSelectedIndex == 7) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )

        // Control selection arrows colors and visibility
        // CORREÇÃO: Item selecionado mostra seta sem margem (colada ao texto)
        val arrowMarginEnd = resources.getDimensionPixelSize(R.dimen.retro_menu3_arrow_margin_end)

        // Continue
        if (currentSelectedIndex == 0) {
            selectionArrowContinue.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowContinue.visibility = View.VISIBLE
            (selectionArrowContinue.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowContinue.visibility = View.GONE
        }

        // Reset
        if (currentSelectedIndex == 1) {
            selectionArrowReset.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowReset.visibility = View.VISIBLE
            (selectionArrowReset.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowReset.visibility = View.GONE
        }

        // Save State
        if (currentSelectedIndex == 2) {
            selectionArrowSave.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowSave.visibility = View.VISIBLE
            (selectionArrowSave.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowSave.visibility = View.GONE
        }

        // Load State
        if (currentSelectedIndex == 3) {
            selectionArrowLoad.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowLoad.visibility = View.VISIBLE
            (selectionArrowLoad.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowLoad.visibility = View.GONE
        }

        // Settings
        if (currentSelectedIndex == 4) {
            selectionArrowSettings.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowSettings.visibility = View.VISIBLE
            (selectionArrowSettings.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowSettings.visibility = View.GONE
        }

        // Audio Toggle
        if (currentSelectedIndex == 5) {
            selectionArrowAudio.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowAudio.visibility = View.VISIBLE
            (selectionArrowAudio.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowAudio.visibility = View.GONE
        }

        // Fast Forward
        if (currentSelectedIndex == 6) {
            selectionArrowFastForward.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowFastForward.visibility = View.VISIBLE
            (selectionArrowFastForward.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowFastForward.visibility = View.GONE
        }

        // Exit
        if (currentSelectedIndex == 7) {
            selectionArrowExit.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowExit.visibility = View.VISIBLE
            (selectionArrowExit.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowExit.visibility = View.GONE
        }

        // Forçar atualização do layout
        menuContainer.requestLayout()
    }

    /** Public method to dismiss the menu from outside */
    fun dismissMenuPublic() {
        dismissMenu()
    }

    /** Abrir submenu de configurações */
    private fun openSettingsSubmenu() {
        android.util.Log.d(
                "RetroMenu3Fragment",
                "openSettingsSubmenu: Starting to open settings submenu"
        )
        // Tornar o menu principal invisível antes de abrir o submenu
        hideMainMenu()

        // Criar e mostrar o SettingsMenuFragment com visual idêntico ao RetroMenu3
        val settingsFragment =
                SettingsMenuFragment.newInstance().apply { setSettingsListener(viewModel) }

        android.util.Log.d(
                "RetroMenu3Fragment",
                "openSettingsSubmenu: SettingsFragment created, registering with ViewModel"
        )
        // Registrar o fragment no ViewModel para que a navegação funcione
        viewModel.registerSettingsMenuFragment(settingsFragment)

        android.util.Log.d(
                "RetroMenu3Fragment",
                "openSettingsSubmenu: Adding fragment to back stack"
        )
        parentFragmentManager
                .beginTransaction()
                .add(android.R.id.content, settingsFragment, "SettingsMenuFragment")
                .addToBackStack("SettingsMenuFragment")
                .commit()
        // Garantir que a transação seja executada imediatamente
        parentFragmentManager.executePendingTransactions()
        android.util.Log.d(
                "RetroMenu3Fragment",
                "openSettingsSubmenu: Settings submenu should be open now"
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Garantir que comboAlreadyTriggered seja resetado quando o fragment for destruído
        try {
            (menuListener as? com.vinaooo.revenger.viewmodels.GameActivityViewModel)?.let {
                    viewModel ->
                // Chamar clearKeyLog através do ViewModel para resetar o estado do combo
                viewModel.clearControllerKeyLog()
            }
        } catch (e: Exception) {
            android.util.Log.w("RetroMenu3Fragment", "Erro ao resetar combo state no onDestroy", e)
        }
    }

    companion object {
        fun newInstance(): RetroMenu3Fragment {
            return RetroMenu3Fragment()
        }
    }
}
