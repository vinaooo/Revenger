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

/** ExitFragment - Menu de opções de saída com visual idêntico ao RetroMenu3 */
class ExitFragment : Fragment() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var submenu2Container: LinearLayout
    private lateinit var saveAndExit: MaterialCardView
    private lateinit var exitWithoutSave: MaterialCardView
    private lateinit var backSubmenu2: MaterialCardView

    // Lista ordenada dos itens do menu para navegação
    private lateinit var menuItems: List<MaterialCardView>
    private var currentSelectedIndex = 0 // Começar com "Option A"

    // Menu option titles for color control
    private lateinit var saveAndExitTitle: TextView
    private lateinit var exitWithoutSaveTitle: TextView
    private lateinit var backTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowSaveAndExit: TextView
    private lateinit var selectionArrowExitWithoutSave: TextView
    private lateinit var selectionArrowBack: TextView

    // Callback interface
    interface ExitListener {
        fun onBackToMainMenu()
    }

    private var exitListener: ExitListener? = null

    fun setExitListener(listener: ExitListener) {
        this.exitListener = listener
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.submenu2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("ExitFragment", "onViewCreated: ExitFragment view created")

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        setupViews(view)
        setupClickListeners()
        // REMOVIDO: animateMenuIn() - submenu agora aparece instantaneamente sem animação

        android.util.Log.d("ExitFragment", "onViewCreated: ExitFragment setup completed")
        // REMOVIDO: Não fecha mais ao tocar nas laterais
        // Menu só fecha quando selecionar Back
    }

    private fun setupViews(view: View) {
        // Main container
        submenu2Container = view.findViewById(R.id.submenu2_container)

        // Menu items
        saveAndExit = view.findViewById(R.id.submenu2_option_a)
        exitWithoutSave = view.findViewById(R.id.submenu2_option_b)
        backSubmenu2 = view.findViewById(R.id.submenu2_option_c)

        // Inicializar lista ordenada dos itens do menu
        menuItems = listOf(saveAndExit, exitWithoutSave, backSubmenu2)

        // Initialize menu option titles
        saveAndExitTitle = view.findViewById(R.id.option_a_title)
        exitWithoutSaveTitle = view.findViewById(R.id.option_b_title)
        backTitle = view.findViewById(R.id.option_c_title)

        // Initialize selection arrows
        selectionArrowSaveAndExit = view.findViewById(R.id.selection_arrow_option_a)
        selectionArrowExitWithoutSave = view.findViewById(R.id.selection_arrow_option_b)
        selectionArrowBack = view.findViewById(R.id.selection_arrow_option_c)

        // Definir primeiro item como selecionado
        updateSelectionVisual()
    }

    private fun setupClickListeners() {
        saveAndExit.setOnClickListener {
            // Save and Exit - Save state and then exit game
            viewModel.saveStateCentralized {
                // After saving, exit the game completely
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }

        exitWithoutSave.setOnClickListener {
            // Exit without Save - Exit game immediately without saving
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        backSubmenu2.setOnClickListener {
            // Back - Return to main menu
            viewModel.dismissExit()
        }
    }

    private fun dismissMenu() {
        // IMPORTANTE: Não chamar dismissRetroMenu3() aqui para evitar crashes
        // Apenas remover o fragment visualmente - SEM animação
        parentFragmentManager.beginTransaction().remove(this).commit()
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
            0 -> saveAndExit.performClick() // Save and Exit
            1 -> exitWithoutSave.performClick() // Exit without Save
            2 -> backSubmenu2.performClick() // Back
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
        saveAndExitTitle.setTextColor(
                if (currentSelectedIndex == 0) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        exitWithoutSaveTitle.setTextColor(
                if (currentSelectedIndex == 1) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        backTitle.setTextColor(
                if (currentSelectedIndex == 2) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )

        // Control selection arrows colors and visibility
        // CORREÇÃO: Item selecionado mostra seta sem margem (colada ao texto)
        val arrowMarginEnd = resources.getDimensionPixelSize(R.dimen.retro_menu3_arrow_margin_end)

        // Save and Exit
        if (currentSelectedIndex == 0) {
            selectionArrowSaveAndExit.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowSaveAndExit.visibility = View.VISIBLE
            (selectionArrowSaveAndExit.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowSaveAndExit.visibility = View.GONE
        }

        // Exit without Save
        if (currentSelectedIndex == 1) {
            selectionArrowExitWithoutSave.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowExitWithoutSave.visibility = View.VISIBLE
            (selectionArrowExitWithoutSave.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // Sem espaço antes da seta
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowExitWithoutSave.visibility = View.GONE
        }

        // Back
        if (currentSelectedIndex == 2) {
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
        submenu2Container.requestLayout()
    }

    /** Public method to dismiss the menu from outside */
    fun dismissMenuPublic() {
        dismissMenu()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Garantir que comboAlreadyTriggered seja resetado quando o fragment for destruído
        try {
            viewModel.clearControllerKeyLog()
        } catch (e: Exception) {
            android.util.Log.w("ExitFragment", "Erro ao resetar combo state no onDestroy", e)
        }
    }

    companion object {
        fun newInstance(): ExitFragment {
            return ExitFragment()
        }
    }
}
