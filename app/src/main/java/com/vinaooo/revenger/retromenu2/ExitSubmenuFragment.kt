package com.vinaooo.revenger.retromenu2

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * ExitSubmenuFragment
 *
 * Submenu de confirmação de saída com 2 opções:
 * 1. Yes, Exit Game (fecha o app)
 * 2. No, Continue (volta ao menu principal)
 *
 * Navegação: D-pad/Analog UP/DOWN + Touch Confirmação: Botão A executa ação Cancelamento: Botão B
 * equivale a "No, Continue"
 */
class ExitSubmenuFragment : Fragment() {

    companion object {
        private const val TAG = "ExitSubmenu"

        fun newInstance(): ExitSubmenuFragment {
            return ExitSubmenuFragment()
        }
    }

    // ============================================================
    // DEPENDENCIES
    // ============================================================

    private lateinit var config: RetroMenu2Config
    private lateinit var viewModel: GameActivityViewModel
    private lateinit var controllerInput: ControllerInput2
    private lateinit var soundManager: MenuSoundManager

    // ============================================================
    // PUBLIC INPUT ROUTING (chamado pelo parent fragment)
    // ============================================================

    /** Processa KeyEvent. Chamado pelo RetroMenu2Fragment quando submenu está ativo. */
    fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        if (!::controllerInput.isInitialized) {
            return true // Bloquear se ainda não inicializou
        }
        return controllerInput.processKeyEvent(keyCode, event)
    }

    /** Processa MotionEvent. Chamado pelo RetroMenu2Fragment quando submenu está ativo. */
    fun handleMotionEvent(event: MotionEvent): Boolean {
        if (!::controllerInput.isInitialized) {
            return true // Bloquear se ainda não inicializou
        }
        return controllerInput.processMotionEvent(event)
    }

    // ============================================================
    // UI STATE
    // ============================================================

    /** Views do layout */
    private lateinit var submenuTitle: TextView
    private lateinit var optionYes: TextView
    private lateinit var optionNo: TextView

    /** Lista de TextViews na ordem do submenu */
    private val optionViews: MutableList<TextView> = mutableListOf()

    /** Índice da opção atualmente selecionada (0-1) */
    private var selectedOptionIndex = 1 // Padrão: "No, Continue" (mais seguro)

    /** Enum das opções */
    private enum class SubmenuOption {
        YES_EXIT,
        NO_CONTINUE
    }

    /** Lista de opções do submenu */
    private val submenuOptions = listOf(SubmenuOption.YES_EXIT, SubmenuOption.NO_CONTINUE)

    // ============================================================
    // LIFECYCLE
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar config
        config = RetroMenu2Config(requireContext())

        // Obter ViewModel da Activity pai
        viewModel = ViewModelProvider(requireActivity()).get(GameActivityViewModel::class.java)
        
        // Inicializar SoundManager
        soundManager = MenuSoundManager(requireContext())
        soundManager.initialize()

        Log.d(TAG, "ExitSubmenuFragment criado")
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView chamado")
        return inflater.inflate(R.layout.fragment_exit_submenu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar views
        submenuTitle = view.findViewById(R.id.submenuTitle)
        optionYes = view.findViewById(R.id.optionYes)
        optionNo = view.findViewById(R.id.optionNo)

        // Popular lista de opções na ordem
        optionViews.clear()
        optionViews.add(optionYes)
        optionViews.add(optionNo)

        // Aplicar fonte Arcada
        applyArcadaFont()

        // Configurar touch listeners para navegação direta
        setupTouchListeners()

        // Atualizar UI inicial
        updateUI()

        Log.d(TAG, "onViewCreated concluído")
    }

    override fun onResume() {
        super.onResume()

        // Configurar input do controller
        controllerInput = ControllerInput2(config)
        controllerInput.onNavigateUp = { navigateUp() }
        controllerInput.onNavigateDown = { navigateDown() }
        controllerInput.onConfirm = { confirmOption() }
        controllerInput.onCancel = { closeSubmenu() }
        controllerInput.menuOpened()

        Log.d(TAG, "ExitSubmenu ativo - input configurado")
    }

    override fun onPause() {
        super.onPause()
        controllerInput.menuClosed()
        Log.d(TAG, "ExitSubmenu pausado")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Aguardar 500ms antes de liberar SoundManager
        // Isso garante que sons de cancelamento/confirmação terminem de tocar
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            soundManager.release()
            Log.d(TAG, "SoundManager liberado após delay")
        }, 500)
        
        Log.d(TAG, "ExitSubmenu destruído")
    }

    // ============================================================
    // UI SETUP
    // ============================================================

    /** Aplica fonte Arcada em todos os textos. */
    private fun applyArcadaFont() {
        val arcadaFont = config.arcadaFont
        if (arcadaFont != null) {
            submenuTitle.typeface = arcadaFont
            optionViews.forEach { it.typeface = arcadaFont }
            Log.d(TAG, "Fonte Arcada aplicada")
        } else {
            Log.w(TAG, "Fonte Arcada não encontrada - usando fonte padrão")
        }
    }

    /** Configura touch listeners para navegação direta por toque. */
    private fun setupTouchListeners() {
        optionViews.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                if (selectedOptionIndex != index) {
                    selectedOptionIndex = index
                    updateUI()
                    Log.d(TAG, "Touch selecionou: ${submenuOptions[index]}")
                }
            }
        }
    }

    // ============================================================
    // NAVIGATION
    // ============================================================

    /** Navega para opção anterior (cíclica). */
    private fun navigateUp() {
        selectedOptionIndex = if (selectedOptionIndex > 0) 0 else 1

        soundManager.playNavigation() // Som de navegação
        Log.d(TAG, "Navegação UP - opção selecionada: ${submenuOptions[selectedOptionIndex]}")
        updateUI()
    }

    /** Navega para próxima opção (cíclica). */
    private fun navigateDown() {
        selectedOptionIndex = if (selectedOptionIndex < 1) 1 else 0

        soundManager.playNavigation() // Som de navegação
        Log.d(TAG, "Navegação DOWN - opção selecionada: ${submenuOptions[selectedOptionIndex]}")
        updateUI()
    }

    /** Confirma opção selecionada. */
    private fun confirmOption() {
        val option = submenuOptions[selectedOptionIndex]
        
        // Som de confirmação ou cancelamento
        if (option == SubmenuOption.NO_CONTINUE) {
            soundManager.playCancel()
        } else {
            soundManager.playConfirm()
        }
        
        Log.d(TAG, "Opção confirmada: $option")

        when (option) {
            SubmenuOption.YES_EXIT -> exitGame()
            SubmenuOption.NO_CONTINUE -> closeSubmenu()
        }
    }

    /** Fecha submenu (volta ao menu principal). */
    private fun closeSubmenu() {
        soundManager.playCancel() // Som de cancelamento
        Log.d(TAG, "Cancelado - voltando ao menu principal")
        parentFragmentManager.popBackStack()
    }

    /** Sai do jogo (fecha a Activity). */
    private fun exitGame() {
        Log.d(TAG, "Saindo do jogo...")
        requireActivity().finish() // Fecha a Activity (GameActivity)
    }

    // ============================================================
    // UI UPDATE
    // ============================================================

    /** Atualiza UI para refletir seleção atual. */
    private fun updateUI() {
        // Atualizar textos com seta
        optionYes.text = getOptionText(SubmenuOption.YES_EXIT)
        optionNo.text = getOptionText(SubmenuOption.NO_CONTINUE)

        // Atualizar cores
        optionViews.forEachIndexed { index, textView ->
            val isSelected = (index == selectedOptionIndex)

            textView.setTextColor(if (isSelected) config.textSelectedColor else config.textColor)
        }

        Log.d(TAG, "UI atualizada - opção selecionada: ${submenuOptions[selectedOptionIndex]}")
    }

    /** Retorna texto formatado da opção com seta. */
    private fun getOptionText(option: SubmenuOption): String {
        val isSelected = (submenuOptions[selectedOptionIndex] == option)
        val arrow = if (isSelected) "> " else "  "

        return when (option) {
            SubmenuOption.YES_EXIT -> "${arrow}Yes, Exit Game"
            SubmenuOption.NO_CONTINUE -> "${arrow}No, Continue"
        }
    }
}
