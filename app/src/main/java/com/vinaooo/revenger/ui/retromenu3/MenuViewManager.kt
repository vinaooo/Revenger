package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils

/** Representa um item de menu composto por título, seta de seleção e card view */
data class MenuItemView(
        val titleTextView: TextView,
        val arrowTextView: TextView,
        val cardView: RetroCardView
)

/**
 * Gerencia a configuração e atualização visual das views do menu RetroMenu3. Responsável por:
 * - Configuração inicial das views
 * - Atualização visual da seleção
 * - Animações de entrada/saída
 * - Estado visual dinâmico
 */
class MenuViewManager(private val fragment: Fragment) {

    // View hint
    private lateinit var controlsHint: TextView

    // Menu item views
    private lateinit var menuContainerView: LinearLayout
    private lateinit var continueMenu: RetroCardView
    private lateinit var resetMenu: RetroCardView
    private lateinit var progressMenu: RetroCardView
    private lateinit var settingsMenu: RetroCardView
    private lateinit var exitMenu: RetroCardView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<RetroCardView>

    // Ordered list of menu item views for unified selection handling
    private lateinit var menuItemViews: List<MenuItemView>

    // Menu option titles for color control
    private lateinit var continueTitle: TextView
    private lateinit var resetTitle: TextView
    private lateinit var progressTitle: TextView
    private lateinit var settingsTitle: TextView
    private lateinit var exitTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowContinue: TextView
    private lateinit var selectionArrowReset: TextView
    private lateinit var selectionArrowProgress: TextView
    private lateinit var selectionArrowSettings: TextView
    private lateinit var selectionArrowExit: TextView

    /** Configura o título dinâmico do menu baseado no estilo configurado */
    fun setupDynamicTitle(view: View) {
        val titleTextView = view.findViewById<TextView>(R.id.menu_title)
        val titleStyle = fragment.resources.getInteger(R.integer.retro_menu3_title_style)

        val titleText =
                when (titleStyle) {
                    1 -> fragment.resources.getString(R.string.config_name)
                    else -> fragment.resources.getString(R.string.retro_menu3_title)
                }

        titleTextView?.text = titleText

        // Garante fonte Arcade no título
        titleTextView?.let { FontUtils.applyArcadeFont(fragment.requireContext(), it) }
    }

    /** Inicializa todas as views do menu e estruturas de dados relacionadas */
    fun setupViews(view: View) {
        // Main container
        menuContainerView = view.findViewById(R.id.menu_container)

        // Controls hint
        controlsHint = view.findViewById(R.id.retro_menu3_controls_hint)

        // Menu items
        continueMenu = view.findViewById(R.id.menu_continue)
        resetMenu = view.findViewById(R.id.menu_reset)
        progressMenu = view.findViewById(R.id.menu_submenu2)
        settingsMenu = view.findViewById(R.id.menu_submenu1)
        exitMenu = view.findViewById(R.id.menu_exit)

        // Initialize ordered list of menu items
        menuItems = listOf(continueMenu, resetMenu, progressMenu, settingsMenu, exitMenu)

        // Dynamic content views (only views that exist in layout)
        // Initialize menu option titles
        continueTitle = view.findViewById(R.id.continue_title)
        resetTitle = view.findViewById(R.id.reset_title)
        settingsTitle = view.findViewById(R.id.submenu1_title)
        progressTitle = view.findViewById(R.id.submenu2_title)
        exitTitle = view.findViewById(R.id.exit_title)

        // Initialize selection arrows
        selectionArrowContinue = view.findViewById(R.id.selection_arrow_continue)
        selectionArrowReset = view.findViewById(R.id.selection_arrow_reset)
        selectionArrowSettings = view.findViewById(R.id.selection_arrow_submenu1)
        selectionArrowProgress = view.findViewById(R.id.selection_arrow_submenu2)
        selectionArrowExit = view.findViewById(R.id.selection_arrow_exit)

        // Force zero marginStart for all arrows to prevent spacing issues
        listOf(
                        selectionArrowContinue,
                        selectionArrowReset,
                        selectionArrowSettings,
                        selectionArrowProgress,
                        selectionArrowExit
                )
                .forEach { arrow ->
                    (arrow.layoutParams as? LinearLayout.LayoutParams)?.apply {
                        marginStart = 0
                        marginEnd = 0
                    }
                }

        // Initialize menu item views list
        menuItemViews =
                listOf(
                        MenuItemView(continueTitle, selectionArrowContinue, continueMenu),
                        MenuItemView(resetTitle, selectionArrowReset, resetMenu),
                        MenuItemView(progressTitle, selectionArrowProgress, progressMenu),
                        MenuItemView(settingsTitle, selectionArrowSettings, settingsMenu),
                        MenuItemView(exitTitle, selectionArrowExit, exitMenu)
                )

        // Apply arcade font to all text views
        ViewUtils.applyArcadeFontToViews(
                fragment.requireContext(),
                controlsHint,
                continueTitle,
                resetTitle,
                progressTitle,
                settingsTitle,
                exitTitle,
                selectionArrowContinue,
                selectionArrowReset,
                selectionArrowProgress,
                selectionArrowSettings,
                selectionArrowExit
        )

        // Set first item as selected
        updateSelectionVisual(0)
    }

    /** Atualiza o estado visual do menu (itens dinâmicos, estados, etc.) */
    fun updateMenuState() {
        // Main menu no longer has dynamic options - everything was moved to submenus
    }

    /** Anima a entrada do menu na tela */
    fun animateMenuIn() {
        // Use optimized batch animation for better performance
        ViewUtils.animateMenuViewsBatchOptimized(
                arrayOf(menuContainerView, controlsHint),
                toAlpha = 1f,
                toScale = 1f,
                duration = 200
        )
    }

    /** Anima a saída do menu da tela */
    fun animateMenuOut(onEnd: () -> Unit) {
        // Use optimized batch animation with callback
        ViewUtils.animateMenuViewsBatchOptimized(
                arrayOf(menuContainerView, controlsHint),
                toAlpha = 0f,
                toScale = 0.8f,
                duration = 150
        ) { onEnd() }
    }

    /** Define um item de menu como selecionado */
    private fun setItemSelected(menuItemView: MenuItemView) {
        menuItemView.titleTextView.setTextColor(android.graphics.Color.YELLOW)
        menuItemView.arrowTextView.apply {
            setTextColor(android.graphics.Color.YELLOW)
            visibility = View.VISIBLE
            (layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Force zero margin - critical fix
                marginEnd =
                        fragment.resources.getDimensionPixelSize(
                                R.dimen.retro_menu3_arrow_margin_end
                        )
                leftMargin = 0 // Additional force for left margin
            }
        }
        // RetroCardView usa estados internos para visual
        menuItemView.cardView.setState(RetroCardView.State.SELECTED)
    }

    /** Define um item de menu como não selecionado */
    private fun setItemUnselected(menuItemView: MenuItemView) {
        menuItemView.titleTextView.setTextColor(android.graphics.Color.WHITE)
        menuItemView.arrowTextView.visibility = View.GONE
        // RetroCardView volta ao estado normal
        menuItemView.cardView.setState(RetroCardView.State.NORMAL)
    }

    /** Atualiza a visualização da seleção baseado no índice atual */
    fun updateSelectionVisual(currentIndex: Int) {
        // Update each menu item view based on selection state
        menuItemViews.forEachIndexed { index, menuItemView ->
            if (index == currentIndex) {
                setItemSelected(menuItemView)
            } else {
                setItemUnselected(menuItemView)
            }
        }

        // Layout will be updated automatically when visibility changes
    }

    /** Faz o menu principal invisível (quando submenu é aberto) */
    fun hideMainMenu() {
        // Check if menuContainer is initialized
        if (!::menuContainerView.isInitialized) {
            android.util.Log.e(
                    "MenuViewManager",
                    "[HIDE] MenuContainer not initialized, cannot hide main menu"
            )
            return
        }

        // Hide only the menu content, keeping the background for the submenu
        menuContainerView.visibility = View.INVISIBLE
    }

    /** Faz o menu principal visível novamente (quando submenu é fechado) */
    fun showMainMenu(currentSelectedIndex: Int = 0) {
        // Check if menuContainer is initialized
        if (!::menuContainerView.isInitialized) {
            android.util.Log.e(
                    "MenuViewManager",
                    "[SHOW] MenuContainer not initialized, cannot show main menu"
            )
            return
        }

        // Make visible
        menuContainerView.visibility = View.VISIBLE

        // Ensure alpha is at 1.0 (fully visible)
        menuContainerView.alpha = 1.0f

        // Update menu state (including audio) when returning from submenu
        updateMenuState()

        // Ensure visual selection is updated when menu becomes visible again
        updateSelectionVisual(currentSelectedIndex)

        // Layout will be updated automatically when properties change
    }

    // Getters para acesso às views quando necessário
    fun getMenuItems(): List<RetroCardView> = menuItems
    fun getMenuItemViews(): List<MenuItemView> = menuItemViews

    // Getters para itens específicos do menu
    val continueMenuItem: RetroCardView
        get() = continueMenu
    val resetMenuItem: RetroCardView
        get() = resetMenu
    val progressMenuItem: RetroCardView
        get() = progressMenu
    val settingsMenuItem: RetroCardView
        get() = settingsMenu
    val exitMenuItem: RetroCardView
        get() = exitMenu

    // Getter para o container do menu
    val menuContainer: LinearLayout
        get() = menuContainerView
}
