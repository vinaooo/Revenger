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
 * RetroMenu3 Fragment - Cópia do ModernMenu Ativado pelo combo // Adicionar listener para detectar
 * quando o back stack muda (submenu é removido) parentFragmentManager.addOnBackStackChangedListener
 * { android.util.Log.d( "RetroMenu3Fragment", "BackStack changed - backStackCount =
 * ${parentFragmentManager.backStackEntryCount}" )
 *
 * // Se o back stack está vazio, significa que o submenu foi removido if
 * (parentFragmentManager.backStackEntryCount == 0) { // Only show main menu if we're not dismissing
 * all menus at once if (viewModel.isDismissingAllMenus()) {
 * android.util.Log.d("RetroMenu3Fragment", "BackStack empty - NOT showing main menu (dismissing all
 * menus)") } else { android.util.Log.d("RetroMenu3Fragment", "BackStack empty - showing main menu")
 * // Mostrar o menu principal novamente showMainMenu() } } }
 *
 * parentFragmentManager .beginTransaction() .add(android.R.id.content, submenu2Fragment,
 * "ExitFragment")fullscreen overlay com Material Design 3
 */
class RetroMenu3Fragment : Fragment() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var menuContainer: LinearLayout
    private lateinit var continueMenu: MaterialCardView
    private lateinit var resetMenu: MaterialCardView
    private lateinit var progressMenu: MaterialCardView
    private lateinit var settingsMenu: MaterialCardView
    private lateinit var submenu2Menu: MaterialCardView

    // Lista ordenada dos itens do menu para navegação
    private lateinit var menuItems: List<MaterialCardView>
    private var currentSelectedIndex = 0 // Começar com "Continue"

    // Menu option titles for color control
    private lateinit var continueTitle: TextView
    private lateinit var resetTitle: TextView
    private lateinit var progressTitle: TextView
    private lateinit var settingsTitle: TextView
    private lateinit var submenu2Title: TextView

    // Selection arrows
    private lateinit var selectionArrowContinue: TextView
    private lateinit var selectionArrowReset: TextView
    private lateinit var selectionArrowProgress: TextView
    private lateinit var selectionArrowSettings: TextView
    private lateinit var selectionArrowSubmenu2: TextView

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
        setupDynamicTitle()
        setupClickListeners()
        updateMenuState()
        animateMenuIn()

        // REMOVIDO: Não fecha mais ao tocar nas laterais
        // Menu só fecha quando pressionar START novamente ou selecionar Continue
    }

    private fun setupDynamicTitle() {
        val titleTextView = view?.findViewById<TextView>(R.id.menu_title)

        val titleStyle = resources.getInteger(R.integer.retro_menu3_title_style)

        val titleText =
                when (titleStyle) {
                    1 -> {
                        resources.getString(R.string.config_name)
                    }
                    else -> {
                        resources.getString(R.string.retro_menu3_title)
                    }
                }
        titleTextView?.text = titleText
    }

    private fun setupViews(view: View) {
        // Main container
        menuContainer = view.findViewById(R.id.menu_container)

        // Menu items
        continueMenu = view.findViewById(R.id.menu_continue)
        resetMenu = view.findViewById(R.id.menu_reset)
        progressMenu = view.findViewById(R.id.menu_submenu1)
        settingsMenu = view.findViewById(R.id.menu_settings)
        submenu2Menu = view.findViewById(R.id.menu_submenu2)

        // Inicializar lista ordenada dos itens do menu
        menuItems = listOf(continueMenu, resetMenu, progressMenu, settingsMenu, submenu2Menu)

        // Dynamic content views (only views that exist in layout)
        // Initialize menu option titles
        continueTitle = view.findViewById(R.id.continue_title)
        resetTitle = view.findViewById(R.id.reset_title)
        progressTitle = view.findViewById(R.id.progress_menu_title)
        settingsTitle = view.findViewById(R.id.settings_title)
        submenu2Title = view.findViewById(R.id.submenu2_title)

        // Initialize selection arrows
        selectionArrowContinue = view.findViewById(R.id.selection_arrow_continue)
        selectionArrowReset = view.findViewById(R.id.selection_arrow_reset)
        selectionArrowProgress = view.findViewById(R.id.selection_arrow_submenu1)
        selectionArrowSettings = view.findViewById(R.id.selection_arrow_settings)
        selectionArrowSubmenu2 = view.findViewById(R.id.selection_arrow_submenu2)

        // Definir primeiro item como selecionado
        updateSelectionVisual()
    }

    private fun setupClickListeners() {
        continueMenu.setOnClickListener {
            // Continue - Close menu, set correct frameSpeed, then continue game
            // A) Close menu first
            animateMenuOut {
                dismissMenu()
                // Limpa keyLog e reseta comboAlreadyTriggered após fechar
                viewModel.clearControllerInputState()
            }

            // B) Set frameSpeed to correct value from Game Speed sharedPreference
            viewModel.restoreGameSpeedFromPreferences()

            // C) Continue game (no additional function needed - just close menu and restore speed)
        }

        resetMenu.setOnClickListener {
            // Reset - First close menu, then set correct frameSpeed, then reset game
            // A) Close menu first
            animateMenuOut {
                dismissMenu()
                // Limpa keyLog e reseta comboAlreadyTriggered após fechar
                viewModel.clearControllerInputState()
            }

            // B) Set frameSpeed to correct value (1 or 2) from Game Speed sharedPreference
            viewModel.restoreGameSpeedFromPreferences()

            // C) Apply reset function
            viewModel.resetGameCentralized()
        }

        progressMenu.setOnClickListener {
            // Abrir submenu Progress
            openProgress()
        }

        settingsMenu.setOnClickListener {
            // Abrir submenu de configurações
            openSettingsSubmenu()
        }

        submenu2Menu.setOnClickListener {
            // Abrir submenu 2
            openSubmenu2()
        }
    }

    private fun updateMenuState() {
        // Menu principal não tem mais opções dinâmicas - tudo foi movido para submenus
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
            2 -> openProgress() // Progress
            3 -> settingsMenu.performClick() // Settings
            4 -> openSubmenu2() // Submenu2
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
        android.util.Log.d(
                "RetroMenu3Fragment",
                "showMainMenu: BEFORE - visibility = ${menuContainer.visibility}"
        )
        android.util.Log.d(
                "RetroMenu3Fragment",
                "showMainMenu: BEFORE - alpha = ${menuContainer.alpha}"
        )

        // Tornar visível
        menuContainer.visibility = View.VISIBLE

        // Garantir que o alpha esteja em 1.0 (totalmente visível)
        menuContainer.alpha = 1.0f

        // Atualizar o estado do menu (incluindo áudio) quando volta do submenu
        updateMenuState()

        // Garantir que a seleção visual seja atualizada quando o menu voltar a ser visível
        updateSelectionVisual()

        // Forçar redesenho completo
        menuContainer.invalidate()
        menuContainer.requestLayout()

        // REMOVIDO: bringToFront() causa problema com layout_weight
        // O SettingsMenuFragment já foi completamente removido com popBackStack()
        // então não há necessidade de trazer para frente

        android.util.Log.d(
                "RetroMenu3Fragment",
                "showMainMenu: AFTER - visibility = ${menuContainer.visibility}"
        )
        android.util.Log.d(
                "RetroMenu3Fragment",
                "showMainMenu: AFTER - alpha = ${menuContainer.alpha}"
        )
        android.util.Log.d(
                "RetroMenu3Fragment",
                "showMainMenu: Menu should be fully visible now (VISIBLE=${View.VISIBLE}, actual=${menuContainer.visibility})"
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
        progressTitle.setTextColor(
                if (currentSelectedIndex == 2) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        settingsTitle.setTextColor(
                if (currentSelectedIndex == 3) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        submenu2Title.setTextColor(
                if (currentSelectedIndex == 4) android.graphics.Color.YELLOW
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

        // Progress
        if (currentSelectedIndex == 2) {
            selectionArrowProgress.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowProgress.visibility = View.VISIBLE
            (selectionArrowProgress.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowProgress.visibility = View.GONE
        }

        // Settings
        if (currentSelectedIndex == 3) {
            selectionArrowSettings.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowSettings.visibility = View.VISIBLE
            (selectionArrowSettings.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowSettings.visibility = View.GONE
        }

        // Submenu2
        if (currentSelectedIndex == 4) {
            selectionArrowSubmenu2.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowSubmenu2.visibility = View.VISIBLE
            (selectionArrowSubmenu2.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowSubmenu2.visibility = View.GONE
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

        // Adicionar listener para detectar quando o back stack muda (submenu é removido)
        parentFragmentManager.addOnBackStackChangedListener {
            android.util.Log.d(
                    "RetroMenu3Fragment",
                    "BackStack changed - backStackCount = ${parentFragmentManager.backStackEntryCount}"
            )

            // Se o back stack está vazio, significa que o submenu foi removido
            if (parentFragmentManager.backStackEntryCount == 0) {
                // Only show main menu if we're not dismissing all menus at once
                if (viewModel.isDismissingAllMenus()) {
                    android.util.Log.d(
                            "RetroMenu3Fragment",
                            "BackStack empty - NOT showing main menu (dismissing all menus)"
                    )
                } else {
                    android.util.Log.d("RetroMenu3Fragment", "BackStack empty - showing main menu")
                    // Mostrar o menu principal novamente
                    showMainMenu()
                }
            }
        }

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

    private fun openProgress() {
        android.util.Log.d("RetroMenu3Fragment", "openProgress: Starting to open progress submenu")
        // Tornar o menu principal invisível antes de abrir o submenu
        hideMainMenu()

        // Criar e mostrar o ProgressFragment
        val progressFragment = ProgressFragment.newInstance()

        android.util.Log.d("RetroMenu3Fragment", "openProgress: ProgressFragment created")
        // Registrar o fragment no ViewModel para que a navegação funcione
        viewModel.registerProgressFragment(progressFragment)

        android.util.Log.d(
                "RetroMenu3Fragment",
                "openProgress: ProgressFragment registered with ViewModel"
        )

        // Adicionar listener para detectar quando o back stack muda (submenu é removido)
        parentFragmentManager.addOnBackStackChangedListener {
            android.util.Log.d(
                    "RetroMenu3Fragment",
                    "BackStack changed - backStackCount = ${parentFragmentManager.backStackEntryCount}"
            )

            // Se o back stack está vazio, significa que o submenu foi removido
            if (parentFragmentManager.backStackEntryCount == 0) {
                // Only show main menu if we're not dismissing all menus at once
                if (viewModel.isDismissingAllMenus()) {
                    android.util.Log.d(
                            "RetroMenu3Fragment",
                            "BackStack empty - NOT showing main menu (dismissing all menus)"
                    )
                } else {
                    android.util.Log.d("RetroMenu3Fragment", "BackStack empty - showing main menu")
                    // Mostrar o menu principal novamente
                    showMainMenu()
                }
            }
        }

        parentFragmentManager
                .beginTransaction()
                .add(android.R.id.content, progressFragment, "ProgressFragment")
                .addToBackStack("ProgressFragment")
                .commit()
        // Garantir que a transação seja executada imediatamente
        parentFragmentManager.executePendingTransactions()
        android.util.Log.d(
                "RetroMenu3Fragment",
                "openProgress: Progress submenu should be open now"
        )
    }

    private fun openSubmenu2() {
        android.util.Log.d("RetroMenu3Fragment", "openSubmenu2: Starting to open submenu2")
        // Tornar o menu principal invisível antes de abrir o submenu
        hideMainMenu()

        // Criar e mostrar o ExitFragment
        val submenu2Fragment = ExitFragment.newInstance()

        android.util.Log.d("RetroMenu3Fragment", "openSubmenu2: ExitFragment created")
        // Registrar o fragment no ViewModel para que a navegação funcione
        viewModel.registerExitFragment(submenu2Fragment)

        android.util.Log.d(
                "RetroMenu3Fragment",
                "openSubmenu2: Submenu2Fragment registered with ViewModel"
        )

        // Adicionar listener para detectar quando o back stack muda (submenu é removido)
        parentFragmentManager.addOnBackStackChangedListener {
            android.util.Log.d(
                    "RetroMenu3Fragment",
                    "BackStack changed - backStackCount = ${parentFragmentManager.backStackEntryCount}"
            )

            // Se o back stack está vazio, significa que o submenu foi removido
            if (parentFragmentManager.backStackEntryCount == 0) {
                // Only show main menu if we're not dismissing all menus at once
                if (viewModel.isDismissingAllMenus()) {
                    android.util.Log.d(
                            "RetroMenu3Fragment",
                            "BackStack empty - NOT showing main menu (dismissing all menus)"
                    )
                } else {
                    android.util.Log.d("RetroMenu3Fragment", "BackStack empty - showing main menu")
                    // Mostrar o menu principal novamente
                    showMainMenu()
                }
            }
        }

        parentFragmentManager
                .beginTransaction()
                .add(android.R.id.content, submenu2Fragment, "Submenu2Fragment")
                .addToBackStack("Submenu2Fragment")
                .commit()
        // Garantir que a transação seja executada imediatamente
        parentFragmentManager.executePendingTransactions()
        android.util.Log.d("RetroMenu3Fragment", "openSubmenu2: Submenu2 should be open now")
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
