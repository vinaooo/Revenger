package com.vinaooo.revenger.retromenu2

import android.os.Bundle
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
 * Submenu de confirmação de saída com 3 opções:
 * 1. Exit and Save (salva estado e fecha o app)
 * 2. Exit Without Save (não salva e fecha o app)
 * 3. Back (volta ao menu principal)
 *
 * Navegação: D-pad/Analog UP/DOWN + Touch Confirmação: Botão A executa ação Cancelamento: Botão B
 * volta ao menu principal
 */
class ExitSubmenuFragment : Fragment() {

    companion object {
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
    private lateinit var optionExitAndSave: TextView
    private lateinit var optionExitWithoutSave: TextView
    private lateinit var optionBack: TextView

    /** Lista de TextViews na ordem do submenu */
    private val optionViews: MutableList<TextView> = mutableListOf()

    /** Índice da opção atualmente selecionada (0-2) */
    private var selectedOptionIndex = 2 // Padrão: "Back" (mais seguro)

    /** Enum das opções */
    private enum class SubmenuOption {
        EXIT_AND_SAVE,
        EXIT_WITHOUT_SAVE,
        BACK
    }

    /** Lista de opções do submenu */
    private val submenuOptions =
            listOf(SubmenuOption.EXIT_AND_SAVE, SubmenuOption.EXIT_WITHOUT_SAVE, SubmenuOption.BACK)

    // ============================================================
    // LIFECYCLE
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar config
        config = RetroMenu2Config(requireContext())

        // Obter ViewModel da Activity pai
        viewModel = ViewModelProvider(requireActivity()).get(GameActivityViewModel::class.java)

        // Usar MenuSoundManager compartilhado do ViewModel
        soundManager =
                viewModel.getMenuSoundManager()
                        ?: MenuSoundManager(requireContext()).also { it.initialize() }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exit_submenu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Vincular views
        submenuTitle = view.findViewById(R.id.submenuTitle)
        optionExitAndSave = view.findViewById(R.id.optionExitAndSave)
        optionExitWithoutSave = view.findViewById(R.id.optionExitWithoutSave)
        optionBack = view.findViewById(R.id.optionBack)

        // Adicionar à lista (ordem importante para navegação)
        optionViews.clear()
        optionViews.add(optionExitAndSave)
        optionViews.add(optionExitWithoutSave)
        optionViews.add(optionBack)

        // Aplicar fonte Arcada
        applyArcadaFont()

        // Configurar touch listeners para navegação direta
        setupTouchListeners()

        // Atualizar UI inicial
        updateUI()
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
    }

    override fun onPause() {
        super.onPause()
        controllerInput.menuClosed()
    }

    override fun onDestroy() {
        super.onDestroy()

        // NÃO liberar SoundManager - ele é compartilhado pelo ViewModel

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
        } else {
            submenuTitle.typeface = null
        }
    }

    /** Configura touch listeners para navegação direta por toque. */
    private fun setupTouchListeners() {
        optionViews.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                if (selectedOptionIndex != index) {
                    selectedOptionIndex = index
                    updateUI()
                }
            }
        }
    }

    // ============================================================
    // NAVIGATION
    // ============================================================

    /** Navega para opção anterior (cíclica). */
    private fun navigateUp() {
        selectedOptionIndex =
                if (selectedOptionIndex > 0) {
                    selectedOptionIndex - 1
                } else {
                    submenuOptions.size - 1 // Volta para última opção
                }

        soundManager.playNavigation() // Som de navegação
        updateUI()
    }

    /** Navega para próxima opção (cíclica). */
    private fun navigateDown() {
        selectedOptionIndex =
                if (selectedOptionIndex < submenuOptions.size - 1) {
                    selectedOptionIndex + 1
                } else {
                    0 // Volta para primeira opção
                }

        soundManager.playNavigation() // Som de navegação
        updateUI()
    }

    /** Confirma opção selecionada. */
    private fun confirmOption() {
        val option = submenuOptions[selectedOptionIndex]

        // Som de confirmação ou cancelamento
        if (option == SubmenuOption.BACK) {
            soundManager.playCancel()
        } else {
            soundManager.playConfirm()
        }

        when (option) {
            SubmenuOption.EXIT_AND_SAVE -> exitGameWithSave()
            SubmenuOption.EXIT_WITHOUT_SAVE -> exitGameWithoutSave()
            SubmenuOption.BACK -> closeSubmenu()
        }
    }

    /** Fecha submenu (volta ao menu principal). */
    private fun closeSubmenu() {
        parentFragmentManager.popBackStack()
    }

    /** Sai do jogo salvando o estado. */
    private fun exitGameWithSave() {
        // CRÍTICO: Retomar emulador ANTES de salvar (senão salva estado pausado)
        viewModel.resumeEmulator()

        // Aguardar um frame para garantir que emulador processou o resume
        android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(
                        {
                            // Salvar estado antes de fechar
                            viewModel.saveStateCentralized { requireActivity().finish() }
                        },
                        100
                ) // 100ms delay para garantir que emulador processou
    }

    /** Sai do jogo sem salvar o estado. */
    private fun exitGameWithoutSave() {
        requireActivity().finish() // Fecha direto sem salvar
    } // ============================================================
    // UI UPDATE
    // ============================================================

    /** Atualiza UI para refletir seleção atual. */
    private fun updateUI() {
        // Atualizar textos com seta
        optionExitAndSave.text = getOptionText(SubmenuOption.EXIT_AND_SAVE)
        optionExitWithoutSave.text = getOptionText(SubmenuOption.EXIT_WITHOUT_SAVE)
        optionBack.text = getOptionText(SubmenuOption.BACK)

        // Atualizar cores
        optionViews.forEachIndexed { index, textView ->
            val isSelected = (index == selectedOptionIndex)

            textView.setTextColor(if (isSelected) config.textSelectedColor else config.textColor)
        }
    }

    /** Retorna texto formatado da opção com seta. */
    private fun getOptionText(option: SubmenuOption): String {
        val isSelected = (submenuOptions[selectedOptionIndex] == option)
        val arrow = if (isSelected) "> " else "  "

        return when (option) {
            SubmenuOption.EXIT_AND_SAVE -> "${arrow}Exit and Save"
            SubmenuOption.EXIT_WITHOUT_SAVE -> "${arrow}Exit Without Save"
            SubmenuOption.BACK -> "${arrow}Back"
        }
    }
}
