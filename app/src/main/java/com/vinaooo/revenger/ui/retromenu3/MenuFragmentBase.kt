package com.vinaooo.revenger.ui.retromenu3

import androidx.fragment.app.Fragment

/**
 * Abstract base class for all menu fragments in the RetroMenu3 system. Eliminates duplicate code by
 * providing default implementations for navigation and selection state management.
 *
 * Fragments that extend this class need to implement:
 * - getMenuItems(): List<MenuItem> - return standardized list of items
 * - performNavigateUp() - specific navigation logic for up
 * - performNavigateDown() - specific navigation logic for down
 * - performConfirm() - specific confirmation logic
 * - performBack(): Boolean - specific back logic
 * - updateSelectionVisual() - update selection visual
 */
abstract class MenuFragmentBase : Fragment(), MenuFragment {

    /** Índice atualmente selecionado */
    private var _currentSelectedIndex = 0

    /** Retorna a lista padronizada de itens do menu */
    abstract override fun getMenuItems(): List<MenuItem>

    /** Método abstrato para lidar com seleção de item do menu */
    abstract override fun onMenuItemSelected(item: MenuItem)

    /** Método abstrato para navegação para cima específica do fragment */
    protected abstract fun performNavigateUp()

    /** Método abstrato para navegação para baixo específica do fragment */
    protected abstract fun performNavigateDown()

    /** Método abstrato para confirmação específica do fragment */
    protected abstract fun performConfirm()

    /** Método abstrato para voltar específica do fragment */
    protected abstract fun performBack(): Boolean

    /** Método abstrato para atualizar visual da seleção */
    protected abstract fun updateSelectionVisualInternal()

    // ========== IMPLEMENTAÇÃO DA INTERFACE MenuFragment ==========

    override fun onNavigateUp(): Boolean {
        android.util.Log.d("MenuBase", "[NAV] ↑ Navigate Up triggered")
        performNavigateUp()
        return true
    }

    override fun onNavigateDown(): Boolean {
        android.util.Log.d("MenuBase", "[NAV] ↓ Navigate Down triggered")
        performNavigateDown()
        return true
    }

    override fun onConfirm(): Boolean {
        android.util.Log.d("MenuBase", "[ACTION] ✓ Confirm triggered")
        performConfirm()
        return true
    }

    override fun onBack(): Boolean {
        android.util.Log.d("MenuBase", "[ACTION] ← Back triggered")
        return performBack()
    }
    override fun getCurrentSelectedIndex(): Int = _currentSelectedIndex

    override fun setSelectedIndex(index: Int) {
        val menuItems = getMenuItems()
        if (index in 0 until menuItems.size) {
            _currentSelectedIndex = index
            updateSelectionVisualInternal()
        }
    }

    // ========== MÉTODOS AUXILIARES ==========

    /** Navegação circular para cima (último item volta para o primeiro) */
    protected fun navigateUpCircular(itemsCount: Int) {
        _currentSelectedIndex =
                if (_currentSelectedIndex > 0) {
                    _currentSelectedIndex - 1
                } else {
                    itemsCount - 1
                }
        updateSelectionVisualInternal()
    }

    /** Navegação circular para baixo (primeiro item volta para o último) */
    protected fun navigateDownCircular(itemsCount: Int) {
        _currentSelectedIndex =
                if (_currentSelectedIndex < itemsCount - 1) {
                    _currentSelectedIndex + 1
                } else {
                    0
                }
        updateSelectionVisualInternal()
    }

    /** Valida se o índice atual é válido */
    protected fun isValidSelection(itemsCount: Int): Boolean {
        return _currentSelectedIndex in 0 until itemsCount
    }

    /** Reseta seleção para o primeiro item */
    protected fun resetSelection() {
        _currentSelectedIndex = 0
        updateSelectionVisualInternal()
    }
}
