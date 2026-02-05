package com.vinaooo.revenger.ui.retromenu3


import android.view.View
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils

/**
 * Representa um item de menu composto por título, seta de seleção e card view.
 *
 * **Padrão Value Object**: Encapsula os 3 componentes visuais de cada item de menu.
 *
 * @property titleTextView TextView do título do item
 * @property arrowTextView TextView da seta de seleção (→)
 * @property cardView RetroCardView que contém o item completo
 */
data class MenuItemView(
        val titleTextView: TextView,
        val arrowTextView: TextView,
        val cardView: RetroCardView
)

/**
 * Gerenciador de views do menu RetroMenu3.
 *
 * **Responsabilidades**:
 * - Configuração inicial de todas as views do menu
 * - Atualização visual da seleção (cores, setas, highlight)
 * - Gerenciamento de animações de entrada/saída
 * - Estado visual dinâmico (enabled/disabled items)
 *
 * **Padrão Manager**: Centraliza toda lógica de UI do menu em uma classe dedicada.
 *
 * **Integração**:
 * - Trabalha com MenuViewInitializer para setup inicial
 * - Usa FontUtils e ViewUtils para estilização
 * - Coordena com MenuAnimationController para transições
 *
 * **Phase 3**: Suporta multi-input (gamepad, keyboard, touch) com feedback visual unificado.
 *
 * @param fragment Fragment que contém as views (RetroMenu3Fragment)
 *
 * @see MenuViewInitializer Inicializa sistema de navegação touch
 * @see MenuAnimationController Controla animações do menu
 * @see RetroCardView Componente customizado de card
 */
class MenuViewManager(private val fragment: Fragment) {

    companion object {
        private const val TAG = "MenuViewManager"
    }

    // Menu item views
    private lateinit var menuContainerView: LinearLayout
    private lateinit var continueMenu: RetroCardView
    private lateinit var resetMenu: RetroCardView
    private lateinit var progressMenu: RetroCardView
    private lateinit var settingsMenu: RetroCardView
    private lateinit var aboutMenu: RetroCardView
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
    private lateinit var aboutTitle: TextView
    private lateinit var exitTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowContinue: TextView
    private lateinit var selectionArrowReset: TextView
    private lateinit var selectionArrowProgress: TextView
    private lateinit var selectionArrowSettings: TextView
    private lateinit var selectionArrowAbout: TextView
    private lateinit var selectionArrowExit: TextView

    // Main menu title
    private var menuTitleTextView: TextView? = null

    /** Configura o título dinâmico do menu baseado no estilo configurado */
    fun setupDynamicTitle(view: View) {
        val titleTextView = view.findViewById<TextView>(R.id.menu_title)
        menuTitleTextView = titleTextView // Store reference for hiding/showing
        Log.d(
                TAG,
                "[VIEW] setupDynamicTitle - menuTitleTextView initialized: ${menuTitleTextView != null}, id: ${titleTextView?.id}"
        )
        val titleStyle = fragment.resources.getInteger(R.integer.rm_title_style)

        val titleText =
                when (titleStyle) {
                    1 -> fragment.resources.getString(R.string.conf_name)
                    else -> fragment.resources.getString(R.string.rm_title)
                }

        titleTextView?.text = titleText

        // Garante fonte Arcade no título
        titleTextView?.let { FontUtils.applySelectedFont(fragment.requireContext(), it) }
    }

    /** Inicializa todas as views do menu e estruturas de dados relacionadas */
    fun setupViews(view: View) {
        // Main container
        menuContainerView = view.findViewById(R.id.menu_container)

        // Menu items
        continueMenu = view.findViewById(R.id.menu_continue)
        resetMenu = view.findViewById(R.id.menu_reset)
        progressMenu = view.findViewById(R.id.menu_submenu2)
        settingsMenu = view.findViewById(R.id.menu_submenu1)
        aboutMenu = view.findViewById(R.id.menu_about)
        exitMenu = view.findViewById(R.id.menu_exit)

        // Initialize ordered list of menu items
        menuItems = listOf(continueMenu, resetMenu, progressMenu, settingsMenu, aboutMenu, exitMenu)

        // Dynamic content views (only views that exist in layout)
        // Initialize menu option titles
        continueTitle = view.findViewById(R.id.continue_title)
        resetTitle = view.findViewById(R.id.reset_title)
        settingsTitle = view.findViewById(R.id.submenu1_title)
        progressTitle = view.findViewById(R.id.submenu2_title)
        aboutTitle = view.findViewById(R.id.about_title)
        exitTitle = view.findViewById(R.id.exit_title)

        // Initialize selection arrows
        selectionArrowContinue = view.findViewById(R.id.selection_arrow_continue)
        selectionArrowReset = view.findViewById(R.id.selection_arrow_reset)
        selectionArrowSettings = view.findViewById(R.id.selection_arrow_submenu1)
        selectionArrowProgress = view.findViewById(R.id.selection_arrow_submenu2)
        selectionArrowAbout = view.findViewById(R.id.selection_arrow_about)
        selectionArrowExit = view.findViewById(R.id.selection_arrow_exit)

        // Force zero marginStart for all arrows to prevent spacing issues
        listOf(
                        selectionArrowContinue,
                        selectionArrowReset,
                        selectionArrowSettings,
                        selectionArrowProgress,
                        selectionArrowAbout,
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
                        MenuItemView(aboutTitle, selectionArrowAbout, aboutMenu),
                        MenuItemView(exitTitle, selectionArrowExit, exitMenu)
                )

        // Apply arcade font to all text views
        ViewUtils.applySelectedFontToViews(
                fragment.requireContext(),
                continueTitle,
                resetTitle,
                progressTitle,
                settingsTitle,
                aboutTitle,
                exitTitle,
                selectionArrowContinue,
                selectionArrowReset,
                selectionArrowProgress,
                selectionArrowSettings,
                selectionArrowAbout,
                selectionArrowExit
        )

        // Aplicar capitalização configurada aos textos dos menus (depois da fonte)
        FontUtils.applyTextCapitalization(
                fragment.requireContext(),
                continueTitle,
                resetTitle,
                progressTitle,
                settingsTitle,
                aboutTitle,
                exitTitle
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
                arrayOf(menuContainerView),
                toAlpha = 1f,
                toScale = 1f,
                duration = 200
        )
    }

    /** Anima a saída do menu da tela */
    fun animateMenuOut(onEnd: () -> Unit) {
        // Use optimized batch animation with callback
        ViewUtils.animateMenuViewsBatchOptimized(
                arrayOf(menuContainerView),
                toAlpha = 0f,
                toScale = 0.8f,
                duration = 150
        ) { onEnd() }
    }

    /** Define um item de menu como selecionado */
    private fun setItemSelected(menuItemView: MenuItemView) {
        menuItemView.titleTextView.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                        fragment.requireContext(),
                        R.color.rm_selected_color
                )
        )
        menuItemView.arrowTextView.apply {
            setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            fragment.requireContext(),
                            R.color.rm_selected_color
                    )
            )
            visibility = View.VISIBLE
            (layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Force zero margin - critical fix
                marginEnd =
                        fragment.resources.getDimensionPixelSize(
                                R.dimen.rm_arrow_margin_end
                        )
                leftMargin = 0 // Additional force for left margin
            }
        }
        // RetroCardView usa estados internos para visual
        menuItemView.cardView.setState(RetroCardView.State.SELECTED)
    }

    /** Define um item de menu como não selecionado */
    private fun setItemUnselected(menuItemView: MenuItemView) {
        menuItemView.titleTextView.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                        fragment.requireContext(),
                        R.color.rm_normal_color
                )
        )
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

    /** Faz o menu principal ficar em segundo plano (quando submenu é aberto) */
    fun dimMainMenu() {
        Log.d(TAG, "[VIEW] dimMainMenu called - checking menuContainerView initialization")
        if (!::menuContainerView.isInitialized) {
            Log.e(TAG, "[VIEW] MenuContainer not initialized, cannot dim main menu")
            return
        }
        Log.d(
                TAG,
                "[VIEW] dimMainMenu: menuContainerView is initialized, current alpha: ${menuContainerView.alpha}"
        )
        menuContainerView.alpha = 0.3f // Opacidade reduzida para segundo plano
        Log.d(
                TAG,
                "[VIEW] dimMainMenu completed - main menu dimmed to alpha: ${menuContainerView.alpha}"
        )
    }

    /** Oculta completamente os textos do menu principal (quando submenu está ativo) */
    fun hideMainMenuTexts() {
        Log.d(
                TAG,
                "[VIEW] hideMainMenuTexts called - menuTitleTextView: ${menuTitleTextView?.hashCode()}, visibility before: ${menuTitleTextView?.visibility}"
        )
        if (!::menuItemViews.isInitialized) {
            Log.e(TAG, "[VIEW] MenuItemViews not initialized, cannot hide main menu texts")
            return
        }

        // Ocultar todos os textos dos itens do menu principal
        menuItemViews.forEach { menuItemView ->
            menuItemView.titleTextView.visibility = View.INVISIBLE
            menuItemView.arrowTextView.visibility = View.INVISIBLE
        }

        // Ocultar o título principal do menu
        menuTitleTextView?.visibility = View.INVISIBLE
        Log.d(
                TAG,
                "[VIEW] hideMainMenuTexts - menuTitleTextView visibility after: ${menuTitleTextView?.visibility}"
        )

        Log.d(TAG, "[VIEW] hideMainMenuTexts completed - main menu texts hidden")
    }

    /** Mostra novamente os textos do menu principal (quando submenu é fechado) */
    fun showMainMenuTexts() {
        Log.d(TAG, "[VIEW] showMainMenuTexts called")
        if (!::menuItemViews.isInitialized) {
            Log.e(TAG, "[VIEW] MenuItemViews not initialized, cannot show main menu texts")
            return
        }

        // Mostrar todos os textos dos itens do menu principal
        menuItemViews.forEach { menuItemView ->
            menuItemView.titleTextView.visibility = View.VISIBLE
            menuItemView.arrowTextView.visibility = View.VISIBLE
        }

        // Mostrar o título principal do menu
        menuTitleTextView?.visibility = View.VISIBLE

        Log.d(TAG, "[VIEW] showMainMenuTexts completed - main menu texts shown")
    }

    /** Faz o menu principal voltar ao normal (quando submenu é fechado) */
    fun restoreMainMenu() {
        Log.d(TAG, "[VIEW] restoreMainMenu called")
        if (!::menuContainerView.isInitialized) {
            Log.e(TAG, "[VIEW] MenuContainer not initialized, cannot restore main menu")
            return
        }
        menuContainerView.alpha = 1.0f // Opacidade normal
        Log.d(TAG, "[VIEW] restoreMainMenu completed - main menu restored")
    }

    /** Make main menu invisible (when submenu is opened) */
    fun hideMainMenu() {
        Log.d(TAG, "[VIEW] hideMainMenu called")
        if (!::menuContainerView.isInitialized) {
            Log.e(TAG, "[VIEW] MenuContainer not initialized, cannot hide main menu")
            return
        }
        // Hide only the menu content, keeping the background for the submenu
        menuContainerView.visibility = View.INVISIBLE
        Log.d(TAG, "[VIEW] hideMainMenu completed - main menu hidden")
    }

    /** Oculta completamente o menu principal de uma vez (para evitar piscada visual) */
    fun hideMainMenuCompletely() {
        Log.d(TAG, "[VIEW] hideMainMenuCompletely called")
        if (!::menuContainerView.isInitialized) {
            Log.e(TAG, "[VIEW] MenuContainer not initialized, cannot hide main menu completely")
            return
        }

        // Ocultar o container do menu
        menuContainerView.visibility = View.INVISIBLE

        // Ocultar todos os textos dos itens do menu principal em uma única operação
        if (::menuItemViews.isInitialized) {
            menuItemViews.forEach { menuItemView ->
                menuItemView.titleTextView.visibility = View.INVISIBLE
                menuItemView.arrowTextView.visibility = View.INVISIBLE
            }
        }

        // Ocultar o título principal do menu
        menuTitleTextView?.visibility = View.INVISIBLE

        Log.d(
                TAG,
                "[VIEW] hideMainMenuCompletely completed - main menu completely hidden without flicker"
        )
    }

    /** Make main menu visible again (when submenu is closed) */
    fun showMainMenu(preserveSelection: Boolean = false) {
        Log.d(TAG, "[VIEW] showMainMenu called with preserveSelection=$preserveSelection")
        if (!::menuContainerView.isInitialized) {
            Log.e(TAG, "[VIEW] MenuContainer not initialized, cannot show main menu")
            return
        }

        // Make visible
        menuContainerView.visibility = View.VISIBLE

        // Ensure alpha is at 1.0 (fully visible)
        menuContainerView.alpha = 1.0f

        Log.d(TAG, "[VIEW] showMainMenu completed - main menu shown")
    }

    // Getters para acesso às views quando necessário
    fun getMenuItems(): List<RetroCardView> = menuItems
    fun getMenuItemViews(): List<MenuItemView> = menuItemViews

    /** Verifica se as views do menu foram inicializadas */
    fun isViewsInitialized(): Boolean = ::menuItemViews.isInitialized
}
