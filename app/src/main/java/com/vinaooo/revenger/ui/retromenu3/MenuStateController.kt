package com.vinaooo.revenger.ui.retromenu3

import com.vinaooo.revenger.utils.MenuLogger

/**
 * Interface para controle de estado do menu. Gerencia seleção de itens, navegação e estado atual do
 * menu.
 */
interface MenuStateController {
    fun initializeState(menuViews: MenuViews)
    fun getCurrentSelection(): MenuItem
    fun selectNextItem(): Boolean
    fun selectPreviousItem(): Boolean
    fun selectItemAt(index: Int): Boolean
    fun getSelectedIndex(): Int
    fun getTotalItems(): Int
    fun updateSelectionVisuals()
}

/** Implementação do MenuStateController. Gerencia o estado de seleção e navegação do menu. */
class MenuStateControllerImpl(
        private val fragment: MenuFragmentBase,
        private val animationController: MenuAnimationController
) : MenuStateController {

    private lateinit var menuViews: MenuViews
    private val menuItems = mutableListOf<MenuItem>()

    init {
        initializeMenuItems()
    }

    init {
        initializeMenuItems()
    }

    private fun initializeMenuItems() {
        // Use the fragment's menu items instead of hardcoded list
        menuItems.addAll(fragment.getMenuItems())
    }

    override fun initializeState(menuViews: MenuViews) {
        MenuLogger.state("MenuStateController: initializeState")
        this.menuViews = menuViews
        // Estado inicial já é gerenciado pelo MenuFragmentBase
        updateSelectionVisuals()
    }

    override fun getCurrentSelection(): MenuItem {
        val currentIndex = fragment.getCurrentSelectedIndex()
        return menuItems.getOrElse(currentIndex) { menuItems.first() }
    }

    override fun selectNextItem(): Boolean {
        val currentIndex = fragment.getCurrentSelectedIndex()
        val newIndex = (currentIndex + 1) % menuItems.size
        return selectItemAt(newIndex)
    }

    override fun selectPreviousItem(): Boolean {
        val currentIndex = fragment.getCurrentSelectedIndex()
        val newIndex = if (currentIndex - 1 < 0) menuItems.size - 1 else currentIndex - 1
        return selectItemAt(newIndex)
    }

    override fun selectItemAt(index: Int): Boolean {
        if (index < 0 || index >= menuItems.size) {
            MenuLogger.state("MenuStateController: selectItemAt - Invalid index: $index")
            return false
        }

        val currentIndex = fragment.getCurrentSelectedIndex()
        MenuLogger.state(
                "MenuStateController: selectItemAt - Changing from $currentIndex to $index"
        )
        fragment.setSelectedIndex(index)
        updateSelectionVisuals()
        return true
    }

    override fun getSelectedIndex(): Int {
        return fragment.getCurrentSelectedIndex()
    }

    override fun getTotalItems(): Int {
        return menuItems.size
    }

    override fun updateSelectionVisuals() {
        if (!::menuViews.isInitialized) {
            MenuLogger.state(
                    "MenuStateController: updateSelectionVisuals - MenuViews not initialized"
            )
            return
        }

        val currentSelectionIndex = fragment.getCurrentSelectedIndex()
        MenuLogger.state(
                "MenuStateController: updateSelectionVisuals - Updating visuals for index $currentSelectionIndex"
        )

        // Esconder todas as setas primeiro
        menuViews.selectionArrowContinue.visibility = android.view.View.GONE
        menuViews.selectionArrowReset.visibility = android.view.View.GONE
        menuViews.selectionArrowProgress.visibility = android.view.View.GONE
        menuViews.selectionArrowSettings.visibility = android.view.View.GONE
        menuViews.selectionArrowAbout.visibility = android.view.View.GONE
        menuViews.selectionArrowExit.visibility = android.view.View.GONE

        // Mostrar seta do item selecionado
        when (currentSelectionIndex) {
            0 -> menuViews.selectionArrowContinue.visibility = android.view.View.VISIBLE
            1 -> menuViews.selectionArrowReset.visibility = android.view.View.VISIBLE
            2 -> menuViews.selectionArrowProgress.visibility = android.view.View.VISIBLE
            3 -> menuViews.selectionArrowSettings.visibility = android.view.View.VISIBLE
            4 -> menuViews.selectionArrowAbout.visibility = android.view.View.VISIBLE
            5 -> menuViews.selectionArrowExit.visibility = android.view.View.VISIBLE
        }

        // DELEGAR PARA MenuAnimationController PARA ATUALIZAR CORES DO TEXTO
        animationController.updateSelectionVisual(currentSelectionIndex)
        MenuLogger.state(
                "MenuStateController: updateSelectionVisuals - Delegated to MenuAnimationController"
        )
    }
}
