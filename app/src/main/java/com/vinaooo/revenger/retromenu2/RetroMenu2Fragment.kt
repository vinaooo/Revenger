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
 * RetroMenu2Fragment
 *
 * Fragment principal do RetroMenu2 - tela de pause com 6 opções:
 * 1. Continue
 * 2. Restart Game
 * 3. Save State
 * 4. Load State (disabled se não houver save)
 * 5. Settings (abre submenu)
 * 6. Exit Game (abre submenu)
 *
 * Design Philosophy:
 * - Fragment só cuida de UI (renderização, navegação visual)
 * - ViewModel cuida de lógica de negócio (pause, save, load, etc)
 * - ControllerInput2 cuida de input (detectar teclas, analog, touch)
 */
class RetroMenu2Fragment : Fragment() {

    companion object {
        private const val TAG = "RetroMenu2Fragment"

        fun newInstance(): RetroMenu2Fragment {
            return RetroMenu2Fragment()
        }
    }

    // ============================================================
    // DEPENDENCIES
    // ============================================================

    private lateinit var config: RetroMenu2Config
    private lateinit var viewModel: GameActivityViewModel
    private lateinit var controllerInput: ControllerInput2

    // ============================================================
    // UI STATE
    // ============================================================

    /** Views do layout */
    private lateinit var menuTitle: TextView
    private lateinit var optionContinue: TextView
    private lateinit var optionRestart: TextView
    private lateinit var optionSave: TextView
    private lateinit var optionLoad: TextView
    private lateinit var optionSettings: TextView
    private lateinit var optionExit: TextView
    private lateinit var loadingText: TextView
    private lateinit var buttonHint: TextView

    /** Lista de TextViews na ordem do menu */
    private val optionViews: MutableList<TextView> = mutableListOf()

    /** Índice da opção atualmente selecionada (0-5) */
    private var selectedOptionIndex = 0

    /** Lista de opções do menu */
    private val menuOptions =
            listOf(
                    MenuOption.CONTINUE,
                    MenuOption.RESTART,
                    MenuOption.SAVE_STATE,
                    MenuOption.LOAD_STATE,
                    MenuOption.SETTINGS,
                    MenuOption.EXIT
            )

    /** Load State está disponível? (false se não houver save) */
    private var isLoadStateAvailable = false

    // ============================================================
    // LIFECYCLE
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config = RetroMenu2Config(requireContext())
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]
        controllerInput = ControllerInput2(config)

        setupControllerCallbacks()

        Log.d(TAG, "RetroMenu2Fragment criado")
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView chamado")
        return inflater.inflate(R.layout.fragment_retro_menu2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar views
        menuTitle = view.findViewById(R.id.menuTitle)
        optionContinue = view.findViewById(R.id.optionContinue)
        optionRestart = view.findViewById(R.id.optionRestart)
        optionSave = view.findViewById(R.id.optionSave)
        optionLoad = view.findViewById(R.id.optionLoad)
        optionSettings = view.findViewById(R.id.optionSettings)
        optionExit = view.findViewById(R.id.optionExit)
        loadingText = view.findViewById(R.id.loadingText)
        buttonHint = view.findViewById(R.id.buttonHint)

        // Popular lista de opções na ordem
        optionViews.clear()
        optionViews.add(optionContinue)
        optionViews.add(optionRestart)
        optionViews.add(optionSave)
        optionViews.add(optionLoad)
        optionViews.add(optionSettings)
        optionViews.add(optionExit)

        // Aplicar fonte Arcada
        applyArcadaFont()

        // Configurar hint de botões
        buttonHint.text = config.buttonHintText

        // Configurar touch listeners para navegação direta
        setupTouchListeners()

        checkLoadStateAvailability()
        updateUI()

        Log.d(TAG, "onViewCreated concluído")
    }

    override fun onResume() {
        super.onResume()

        // Notificar ControllerInput que menu está aberto
        controllerInput.menuOpened()

        // Pausar emulador (frameSpeed = 0)
        viewModel.pauseEmulator()

        Log.d(TAG, "Menu exibido - emulador pausado")
    }

    override fun onPause() {
        super.onPause()

        // Notificar ControllerInput que menu fechou
        controllerInput.menuClosed()

        Log.d(TAG, "Menu fechado")
    }

    // ============================================================
    // CONTROLLER INPUT SETUP
    // ============================================================

    private fun setupControllerCallbacks() {
        controllerInput.onNavigateUp = { navigateUp() }

        controllerInput.onNavigateDown = { navigateDown() }

        controllerInput.onConfirm = { confirmSelection() }

        controllerInput.onCancel = { cancelAction() }

        // Menu não pode ser aberto a partir de dentro do menu
        // (SELECT+START só funciona durante gameplay)
        controllerInput.onMenuOpenRequested = null
    }

    // ============================================================
    // NAVIGATION
    // ============================================================

    /** Navega para cima na lista de opções. */
    private fun navigateUp() {
        do {
            selectedOptionIndex =
                    if (selectedOptionIndex > 0) {
                        selectedOptionIndex - 1
                    } else {
                        menuOptions.size - 1 // Wrap para o final
                    }
        } while (!isOptionEnabled(menuOptions[selectedOptionIndex]))

        Log.d(TAG, "Navegação UP - opção selecionada: ${menuOptions[selectedOptionIndex]}")
        updateUI()
    }

    /** Navega para baixo na lista de opções. */
    private fun navigateDown() {
        do {
            selectedOptionIndex =
                    if (selectedOptionIndex < menuOptions.size - 1) {
                        selectedOptionIndex + 1
                    } else {
                        0 // Wrap para o início
                    }
        } while (!isOptionEnabled(menuOptions[selectedOptionIndex]))

        Log.d(TAG, "Navegação DOWN - opção selecionada: ${menuOptions[selectedOptionIndex]}")
        updateUI()
    }

    /** Confirma a seleção atual. */
    private fun confirmSelection() {
        val option = menuOptions[selectedOptionIndex]

        if (!isOptionEnabled(option)) {
            Log.w(TAG, "Opção $option está desabilitada")
            return
        }

        Log.d(TAG, "Opção confirmada: $option")
        executeOption(option)
    }

    /** Cancela ação atual (equivalente a Continue). */
    private fun cancelAction() {
        Log.d(TAG, "Ação cancelada - fechando menu")
        closeMenu()
    }

    // ============================================================
    // MENU OPTIONS EXECUTION
    // ============================================================

    /** Executa a ação da opção selecionada. */
    private fun executeOption(option: MenuOption) {
        when (option) {
            MenuOption.CONTINUE -> {
                closeMenu()
            }
            MenuOption.RESTART -> {
                restartGame()
            }
            MenuOption.SAVE_STATE -> {
                saveState()
            }
            MenuOption.LOAD_STATE -> {
                loadState()
            }
            MenuOption.SETTINGS -> {
                openSettingsSubmenu()
            }
            MenuOption.EXIT -> {
                openExitSubmenu()
            }
        }
    }

    /** Fecha o menu e retoma gameplay. */
    private fun closeMenu() {
        // CRÍTICO: Limpar keyLog ANTES de retomar para evitar reabertura imediata!
        viewModel.clearInputKeyLog()
        
        // Retomar emulador (frameSpeed = 1)
        viewModel.resumeEmulator()

        // Fechar fragment
        parentFragmentManager.beginTransaction().remove(this).commit()

        Log.d(TAG, "Menu fechado - emulador retomado")
    }

    /** Reinicia o jogo usando reset(). */
    private fun restartGame() {
        Log.d(TAG, "Reiniciando jogo...")

        // CRÍTICO: Limpar keyLog ANTES de retomar para evitar reabertura imediata!
        viewModel.clearInputKeyLog()
        
        // Retomar emulador primeiro (frameSpeed = 1)
        viewModel.resumeEmulator()

        // Executar reset()
        viewModel.resetGame()

        // Fechar menu
        parentFragmentManager.beginTransaction().remove(this).commit()

        Log.d(TAG, "Jogo reiniciado")
    }

    /** Salva o estado do jogo. */
    private fun saveState() {
        Log.d(TAG, "Salvando estado...")

        // Retomar emulador temporariamente (frameSpeed = 1)
        viewModel.resumeEmulator()

        // TODO: Adicionar delay mínimo antes de salvar (evitar save corrupto)

        // Salvar estado
        viewModel.saveGameState()

        // Pausar novamente
        viewModel.pauseEmulator()

        // Atualizar disponibilidade de Load State
        isLoadStateAvailable = true
        updateUI()

        Log.d(TAG, "Estado salvo")
    }

    /** Carrega o estado do jogo. */
    private fun loadState() {
        if (!isLoadStateAvailable) {
            Log.w(TAG, "Load State não disponível")
            return
        }

        Log.d(TAG, "Carregando estado...")

        // TODO: Mostrar tela de loading (Fase 6)

        // Retomar emulador (frameSpeed = 1)
        viewModel.resumeEmulator()

        // Carregar estado
        viewModel.loadGameState()

        // TODO: Aguardar duração mínima de loading (2 segundos)

        // Fechar menu
        parentFragmentManager.beginTransaction().remove(this).commit()

        Log.d(TAG, "Estado carregado")
    }

    /** Abre submenu de Settings. */
    private fun openSettingsSubmenu() {
        Log.d(TAG, "Abrindo submenu Settings...")

        // TODO: Implementar na Fase 5
        // - Criar RetroMenu2SettingsFragment
        // - Transição com animação slide

        Log.w(TAG, "Submenu Settings ainda não implementado")
    }

    /** Abre submenu de Exit. */
    private fun openExitSubmenu() {
        Log.d(TAG, "Abrindo submenu Exit...")

        // TODO: Implementar na Fase 5
        // - Criar RetroMenu2ExitFragment
        // - Transição com animação slide

        Log.w(TAG, "Submenu Exit ainda não implementado")
    }

    // ============================================================
    // UI HELPERS
    // ============================================================

    /** Aplica a fonte Arcada em todos os TextViews. */
    private fun applyArcadaFont() {
        val arcadaFont = config.arcadaFont

        if (arcadaFont != null) {
            menuTitle.typeface = arcadaFont
            optionViews.forEach { it.typeface = arcadaFont }
            loadingText.typeface = arcadaFont
            buttonHint.typeface = arcadaFont

            Log.d(TAG, "Fonte Arcada aplicada com sucesso")
        } else {
            Log.w(TAG, "Fonte Arcada não encontrada - usando fonte padrão")
        }
    }

    /** Configura touch listeners para navegação direta (tap = selecionar + confirmar). */
    private fun setupTouchListeners() {
        optionViews.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                val option = menuOptions[index]

                if (isOptionEnabled(option)) {
                    Log.d(TAG, "Touch em opção $index: $option")
                    selectedOptionIndex = index
                    updateUI()
                    executeOption(option)
                } else {
                    Log.w(TAG, "Touch em opção desabilitada: $option")
                }
            }
        }
    }

    /** Atualiza a UI para refletir estado atual. */
    private fun updateUI() {
        // Atualizar cores de acordo com seleção e disponibilidade
        optionViews.forEachIndexed { index, textView ->
            val option = menuOptions[index]
            val isSelected = (index == selectedOptionIndex)
            val isEnabled = isOptionEnabled(option)

            textView.setTextColor(
                    when {
                        !isEnabled -> config.textDisabledColor
                        isSelected -> config.textSelectedColor
                        else -> config.textColor
                    }
            )
        }

        Log.d(TAG, "UI atualizada - opção selecionada: ${menuOptions[selectedOptionIndex]}")
    }

    /** Verifica se Load State está disponível (se existe save). */
    private fun checkLoadStateAvailability() {
        // TODO: Verificar se arquivo de save state existe
        // Por enquanto, assumir false
        isLoadStateAvailable = false

        Log.d(TAG, "Load State disponível: $isLoadStateAvailable")
    }

    /** Verifica se uma opção está habilitada. */
    private fun isOptionEnabled(option: MenuOption): Boolean {
        return when (option) {
            MenuOption.LOAD_STATE -> isLoadStateAvailable
            else -> true
        }
    }

    // ============================================================
    // PUBLIC INPUT METHODS
    // ============================================================

    // ============================================================
    // PUBLIC INPUT METHODS
    // ============================================================

    /**
     * Processa KeyEvent do controller.
     * Retorna true se o evento foi consumido pelo menu.
     */
    fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        if (!controllerInput.isMenuOpen) {
            return false // Menu não está aberto
        }
        
        // Processar via ControllerInput2
        return controllerInput.processKeyEvent(keyCode, event)
    }
    
    /**
     * Processa MotionEvent (analog stick).
     * Retorna true se o evento foi consumido pelo menu.
     */
    fun handleMotionEvent(event: MotionEvent): Boolean {
        if (!controllerInput.isMenuOpen) {
            return false // Menu não está aberto
        }
        
        // Processar via ControllerInput2
        return controllerInput.processMotionEvent(event)
    }

    // ============================================================
    // MENU OPTION ENUM
    // ============================================================

    enum class MenuOption {
        CONTINUE,
        RESTART,
        SAVE_STATE,
        LOAD_STATE,
        SETTINGS,
        EXIT
    }
}
