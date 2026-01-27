package com.vinaooo.revenger.ui.retromenu3

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.viewmodels.InputViewModel

/**
 * Classe base abstrata para todos os fragments de menu no sistema RetroMenu3. Elimina código
 * duplicado fornecendo implementações padrão para navegação e gerenciamento de estado de seleção.
 *
 * **Arquitetura Multi-Input (Phase 3+)**:
 * - Suporta gamepad, teclado e touch simultaneamente
 * - Navegação via interface MenuFragment unificada
 * - Touch com delay de 100ms para evitar ativação acidental (TOUCH_ACTIVATION_DELAY_MS)
 *
 * **Fragments que estendem esta classe precisam implementar**:
 * - `getMenuItems()`: Retorna lista padronizada de itens
 * - `performNavigateUp()`: Lógica específica de navegação para cima
 * - `performNavigateDown()`: Lógica específica de navegação para baixo
 * - `performConfirm()`: Lógica específica de confirmação
 * - `performBack()`: Lógica específica de voltar
 * - `updateSelectionVisualInternal()`: Atualiza visual de seleção
 *
 * **Phase 3.3**: Sistema de touch integrado com highlight imediato + delay de ativação.
 *
 * **FIX ERRO 1 - Phase 4.2**: onPause() limpa estado de input para evitar vazamento de eventos
 * durante transições de fragmento (B/BackSpace residual após popBackStack).
 *
 * @see MenuFragment Interface unificada de navegação
 * @see MenuItem Modelo de dados para items de menu
 * @see MenuAction Ações padronizadas de menu
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

    // ========== LIFECYCLE HOOKS ==========

    /**
     * FIX ERRO 1: Limpa estado de input ao pausar fragment para evitar vazamento de eventos.
     *
     * Problema: Ao manter B/BackSpace em submenu, o evento KEY_DOWN fecha o submenu via
     * popBackStack(), mas o KEY_UP correspondente era processado no menu principal, causando
     * fechamento indesejado.
     *
     * Solução: Limpar todos os timestamps de debounce, keyLog e flags ao pausar fragmento,
     * garantindo que próximo fragmento comece com estado limpo.
     */
    override fun onPause() {
        super.onPause()
        try {
            // Acessar InputViewModel e limpar estado de input de forma segura
            val inputViewModel = ViewModelProvider(requireActivity())[InputViewModel::class.java]
            inputViewModel.getControllerInput().clearPendingInputsPreserveHeld()

            android.util.Log.d(
                    "MenuFragmentBase",
                    "[LIFECYCLE] onPause() - clearPendingInputsPreserveHeld() for ${this::class.simpleName}"
            )
        } catch (e: Exception) {
            android.util.Log.e(
                    "MenuFragmentBase",
                    "[LIFECYCLE] Failed to clear pending inputs in onPause()",
                    e
            )
        }
    }

    // ========== IMPLEMENTAÇÃO DA INTERFACE MenuFragment ==========

    override fun onNavigateUp(): Boolean {
        android.util.Log.d("MenuBase", "[NAV] ↑ Navigate Up triggered")
        performNavigateUp()
        return true
    }

    override fun onNavigateDown(): Boolean {
        android.util.Log.d(
                "MenuBase",
                "[NAV] ↓ onNavigateDown triggered - calling performNavigateDown"
        )
        val result = performNavigateDown()
        android.util.Log.d("MenuBase", "[NAV] ↓ onNavigateDown completed - result=$result")
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

            // PHASE 4: Log quando item do menu é selecionado (amarelo)
            val itemTitle = if (index < menuItems.size) menuItems[index].title else "UNKNOWN"
            android.util.Log.d(
                    "MenuBase",
                    "[MENU-SELECTION] ✅ Item selected (YELLOW): index=$index, title='$itemTitle'"
            )

            updateSelectionVisualInternal()
        } else {
            android.util.Log.w(
                    "MenuFragmentBase",
                    "Invalid index $index, valid range: 0..${menuItems.size-1}"
            )
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

    /**
     * Aplica as proporções de layout configuráveis ao menu.
     * Deve ser chamado no onViewCreated dos submenus.
     *
     * @param view A view raiz do menu inflado
     */
    protected fun applyLayoutProportions(view: android.view.View) {
        com.vinaooo.revenger.ui.retromenu3.config.MenuLayoutConfig.applyProportionsToMenuLayout(
                view
        )
    }

    companion object {
        /** Delay para ativação de item via touch em milissegundos */
        const val TOUCH_ACTIVATION_DELAY_MS = 100L
    }
}
