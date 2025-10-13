package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils

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
    private lateinit var continueMenu: MaterialCardView
    private lateinit var resetMenu: MaterialCardView
    private lateinit var progressMenu: MaterialCardView
    private lateinit var settingsMenu: MaterialCardView
    private lateinit var exitMenu: MaterialCardView
    private lateinit var saveLogMenu: MaterialCardView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<MaterialCardView>

    // Ordered list of menu item views for unified selection handling
    private lateinit var menuItemViews: List<MenuItemView>

    // Menu option titles for color control
    private lateinit var continueTitle: TextView
    private lateinit var resetTitle: TextView
    private lateinit var progressTitle: TextView
    private lateinit var settingsTitle: TextView
    private lateinit var exitTitle: TextView
    private lateinit var saveLogTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowContinue: TextView
    private lateinit var selectionArrowReset: TextView
    private lateinit var selectionArrowProgress: TextView
    private lateinit var selectionArrowSettings: TextView
    private lateinit var selectionArrowExit: TextView
    private lateinit var selectionArrowSaveLog: TextView

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
        progressMenu = view.findViewById(R.id.menu_submenu1)
        settingsMenu = view.findViewById(R.id.menu_settings)
        exitMenu = view.findViewById(R.id.menu_exit)
        saveLogMenu = view.findViewById(R.id.menu_save_log)

        // Initialize ordered list of menu items
        menuItems =
                listOf(continueMenu, resetMenu, progressMenu, settingsMenu, exitMenu, saveLogMenu)

        // Dynamic content views (only views that exist in layout)
        // Initialize menu option titles
        continueTitle = view.findViewById(R.id.continue_title)
        resetTitle = view.findViewById(R.id.reset_title)
        progressTitle = view.findViewById(R.id.progress_menu_title)
        settingsTitle = view.findViewById(R.id.settings_title)
        exitTitle = view.findViewById(R.id.exit_title)
        saveLogTitle = view.findViewById(R.id.save_log_title)

        // Initialize selection arrows
        selectionArrowContinue = view.findViewById(R.id.selection_arrow_continue)
        selectionArrowReset = view.findViewById(R.id.selection_arrow_reset)
        selectionArrowProgress = view.findViewById(R.id.selection_arrow_submenu1)
        selectionArrowSettings = view.findViewById(R.id.selection_arrow_settings)
        selectionArrowExit = view.findViewById(R.id.selection_arrow_exit)
        selectionArrowSaveLog = view.findViewById(R.id.selection_arrow_save_log)

        // Initialize menu item views list
        menuItemViews =
                listOf(
                        MenuItemView(continueTitle, selectionArrowContinue, continueMenu),
                        MenuItemView(resetTitle, selectionArrowReset, resetMenu),
                        MenuItemView(progressTitle, selectionArrowProgress, progressMenu),
                        MenuItemView(settingsTitle, selectionArrowSettings, settingsMenu),
                        MenuItemView(exitTitle, selectionArrowExit, exitMenu),
                        MenuItemView(saveLogTitle, selectionArrowSaveLog, saveLogMenu)
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
                saveLogTitle,
                selectionArrowContinue,
                selectionArrowReset,
                selectionArrowProgress,
                selectionArrowSettings,
                selectionArrowExit,
                selectionArrowSaveLog
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
        ViewUtils.animateMenuViewsBatch(
                arrayOf(menuContainerView, controlsHint),
                toAlpha = 1f,
                toScale = 1f,
                duration = 200
        )
    }

    /** Anima a saída do menu da tela */
    fun animateMenuOut(onEnd: () -> Unit) {
        // Use optimized batch animation with callback
        ViewUtils.animateMenuViewsBatch(
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
                marginStart = 0 // No space before the arrow
                marginEnd =
                        fragment.resources.getDimensionPixelSize(
                                R.dimen.retro_menu3_arrow_margin_end
                        )
            }
        }
        menuItemView.cardView.apply {
            strokeWidth = 0
            strokeColor = android.graphics.Color.TRANSPARENT
            setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }

    /** Define um item de menu como não selecionado */
    private fun setItemUnselected(menuItemView: MenuItemView) {
        menuItemView.titleTextView.setTextColor(android.graphics.Color.WHITE)
        menuItemView.arrowTextView.visibility = View.GONE
        menuItemView.cardView.apply {
            strokeWidth = 0
            strokeColor = android.graphics.Color.TRANSPARENT
            setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
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
    } // Getters para acesso às views quando necessário
    fun getMenuItems(): List<MaterialCardView> = menuItems
    fun getMenuItemViews(): List<MenuItemView> = menuItemViews

    // Getters para itens específicos do menu
    val continueMenuItem: MaterialCardView
        get() = continueMenu
    val resetMenuItem: MaterialCardView
        get() = resetMenu
    val progressMenuItem: MaterialCardView
        get() = progressMenu
    val settingsMenuItem: MaterialCardView
        get() = settingsMenu
    val exitMenuItem: MaterialCardView
        get() = exitMenu
    val saveLogMenuItem: MaterialCardView
        get() = saveLogMenu

    // Getter para o container do menu
    val menuContainer: LinearLayout
        get() = menuContainerView
}
