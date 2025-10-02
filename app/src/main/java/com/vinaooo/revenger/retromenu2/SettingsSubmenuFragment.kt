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
 * SettingsSubmenuFragment
 *
 * Submenu de configurações do RetroMenu2 com 4 opções:
 * 1. Game Sounds: ON / OFF
 * 2. Menu Sounds: ON / OFF
 * 3. Game Speed: Normal / Fast (x2)
 * 4. Back (volta ao menu principal)
 *
 * Navegação: D-pad/Analog UP/DOWN + Touch Confirmação: Botão A alterna toggle ou volta (Back)
 * Cancelamento: Botão B volta ao menu principal
 */
class SettingsSubmenuFragment : Fragment() {

    companion object {
        private const val TAG = "SettingsSubmenu"

        fun newInstance(): SettingsSubmenuFragment {
            return SettingsSubmenuFragment()
        }
    }

    // ============================================================
    // DEPENDENCIES
    // ============================================================

    private lateinit var config: RetroMenu2Config
    private lateinit var viewModel: GameActivityViewModel
    private lateinit var soundManager: MenuSoundManager
    private lateinit var controllerInput: ControllerInput2

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
    private lateinit var optionGameSounds: TextView
    private lateinit var optionMenuSounds: TextView
    private lateinit var optionGameSpeed: TextView
    private lateinit var optionBack: TextView

    /** Lista de TextViews na ordem do submenu */
    private val optionViews: MutableList<TextView> = mutableListOf()

    /** Índice da opção atualmente selecionada (0-3) */
    private var selectedOptionIndex = 0

    /** Enum das opções */
    private enum class SubmenuOption {
        GAME_SOUNDS,
        MENU_SOUNDS,
        GAME_SPEED,
        BACK
    }

    /** Lista de opções do submenu */
    private val submenuOptions =
            listOf(
                    SubmenuOption.GAME_SOUNDS,
                    SubmenuOption.MENU_SOUNDS,
                    SubmenuOption.GAME_SPEED,
                    SubmenuOption.BACK
            )

    // ============================================================
    // STATE VARIABLES
    // ============================================================

    /** Game sounds habilitado? */
    private var isGameSoundsEnabled = true

    /** Menu sounds habilitado? */
    private var isMenuSoundsEnabled = true

    /** Game speed: false=Normal (1.0), true=Fast (2.0) */
    private var isFastSpeed = false

    /** Pending game speed change (aplicado ao sair do menu) */
    private var pendingGameSpeedChange: Int? = null

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

        Log.d(TAG, "SettingsSubmenuFragment criado")
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView chamado")
        return inflater.inflate(R.layout.fragment_settings_submenu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar views
        submenuTitle = view.findViewById(R.id.submenuTitle)
        optionGameSounds = view.findViewById(R.id.optionGameSounds)
        optionMenuSounds = view.findViewById(R.id.optionMenuSounds)
        optionGameSpeed = view.findViewById(R.id.optionGameSpeed)
        optionBack = view.findViewById(R.id.optionBack)

        // Popular lista de opções na ordem
        optionViews.clear()
        optionViews.add(optionGameSounds)
        optionViews.add(optionMenuSounds)
        optionViews.add(optionGameSpeed)
        optionViews.add(optionBack)

        // Aplicar fonte Arcada
        applyArcadaFont()

        // Carregar configurações atuais
        loadCurrentSettings()

        // Configurar touch listeners para navegação direta
        setupTouchListeners()

        // Atualizar UI inicial
        updateUI()

        Log.d(TAG, "onViewCreated concluído")
    }

    override fun onResume() {
        super.onResume()

        // Configurar input do controller (os submenus precisam do próprio ControllerInput)
        // O parent RetroMenu2Fragment já pausou o emulador, só precisamos configurar input
        controllerInput = ControllerInput2(config)
        controllerInput.onNavigateUp = { navigateUp() }
        controllerInput.onNavigateDown = { navigateDown() }
        controllerInput.onConfirm = { confirmOption() }
        controllerInput.onCancel = { closeSubmenu() }
        controllerInput.menuOpened()

        Log.d(TAG, "SettingsSubmenu ativo - input configurado")
    }

    override fun onPause() {
        super.onPause()
        controllerInput.menuClosed()

        // Passar mudança pendente de velocidade para o menu principal
        pendingGameSpeedChange?.let { newSpeed ->
            val parentMenu = parentFragment as? RetroMenu2Fragment
            parentMenu?.pendingGameSpeedChange = newSpeed
            Log.d(
                    TAG,
                    "Passando Game Speed pendente para menu principal: ${if (newSpeed == 2) "Fast (x2)" else "Normal (x1)"}"
            )
            pendingGameSpeedChange = null
        }

        Log.d(TAG, "SettingsSubmenu pausado")
    }

    override fun onDestroy() {
        super.onDestroy()

        // Aguardar 500ms antes de liberar SoundManager
        // Isso garante que sons de cancelamento/confirmação terminem de tocar
        android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(
                        {
                            soundManager.release()
                            Log.d(TAG, "SoundManager liberado após delay")
                        },
                        500
                )

        Log.d(TAG, "SettingsSubmenu destruído")
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
    // SETTINGS MANAGEMENT
    // ============================================================

    /** Carrega configurações atuais do jogo/app. */
    private fun loadCurrentSettings() {
        // Game sounds: obtém do ViewModel
        isGameSoundsEnabled = viewModel.getAudioState()

        // Menu sounds: obtém do SoundManager (SharedPreferences)
        isMenuSoundsEnabled = soundManager.isEnabled

        // Game speed: obtém do SpeedController (SharedPreferences)
        val currentSpeed = viewModel.getSpeedController()?.getCurrentSpeed() ?: 1
        isFastSpeed = currentSpeed > 1

        Log.d(
                TAG,
                "Configurações carregadas: GameSounds=$isGameSoundsEnabled, MenuSounds=$isMenuSoundsEnabled, FastSpeed=$isFastSpeed (speed=$currentSpeed)"
        )
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
                    submenuOptions.size - 1 // Volta pro último
                }

        soundManager.playNavigation() // Som de navegação
        Log.d(TAG, "Navegação UP - opção selecionada: ${submenuOptions[selectedOptionIndex]}")
        updateUI()
    }

    /** Navega para próxima opção (cíclica). */
    private fun navigateDown() {
        selectedOptionIndex =
                if (selectedOptionIndex < submenuOptions.size - 1) {
                    selectedOptionIndex + 1
                } else {
                    0 // Volta pro primeiro
                }

        soundManager.playNavigation() // Som de navegação
        Log.d(TAG, "Navegação DOWN - opção selecionada: ${submenuOptions[selectedOptionIndex]}")
        updateUI()
    }

    /** Confirma opção selecionada (toggle ou back). */
    private fun confirmOption() {
        val option = submenuOptions[selectedOptionIndex]

        // Som de confirmação (exceto para BACK que usa cancel)
        if (option == SubmenuOption.BACK) {
            soundManager.playCancel()
        } else {
            soundManager.playConfirm()
        }

        Log.d(TAG, "Opção confirmada: $option")

        when (option) {
            SubmenuOption.GAME_SOUNDS -> toggleGameSounds()
            SubmenuOption.MENU_SOUNDS -> toggleMenuSounds()
            SubmenuOption.GAME_SPEED -> toggleGameSpeed()
            SubmenuOption.BACK -> closeSubmenu()
        }
    }

    /** Fecha submenu (volta ao menu principal). */
    private fun closeSubmenu() {
        Log.d(TAG, "Fechando submenu - voltando ao menu principal")
        // Parent fragment (RetroMenu2Fragment) vai gerenciar o pop do back stack
        parentFragmentManager.popBackStack()
    }

    // ============================================================
    // TOGGLE ACTIONS
    // ============================================================

    /** Toggle Game Sounds ON/OFF. */
    private fun toggleGameSounds() {
        isGameSoundsEnabled = !isGameSoundsEnabled
        viewModel.setAudioEnabled(isGameSoundsEnabled)

        Log.d(TAG, "Game Sounds: ${if (isGameSoundsEnabled) "ON" else "OFF"}")
        updateUI()
    }

    /** Toggle Menu Sounds ON/OFF. */
    private fun toggleMenuSounds() {
        isMenuSoundsEnabled = !isMenuSoundsEnabled
        soundManager.isEnabled = isMenuSoundsEnabled

        Log.d(TAG, "Menu Sounds: ${if (isMenuSoundsEnabled) "ON" else "OFF"}")
        updateUI()
    }

    /** Toggle Game Speed Normal/Fast. */
    private fun toggleGameSpeed() {
        isFastSpeed = !isFastSpeed
        val newSpeed = if (isFastSpeed) 2 else 1

        // NÃO aplicar imediatamente - guardar mudança pendente
        pendingGameSpeedChange = newSpeed

        Log.d(
                TAG,
                "Game Speed: ${if (isFastSpeed) "Fast (x2)" else "Normal (x1)"} (será aplicado ao sair do menu)"
        )
        updateUI()
    }

    // ============================================================
    // UI UPDATE
    // ============================================================

    /** Atualiza UI para refletir seleção e estados atuais. */
    private fun updateUI() {
        // Atualizar textos com estados atuais
        optionGameSounds.text = getOptionText(SubmenuOption.GAME_SOUNDS)
        optionMenuSounds.text = getOptionText(SubmenuOption.MENU_SOUNDS)
        optionGameSpeed.text = getOptionText(SubmenuOption.GAME_SPEED)
        optionBack.text = getOptionText(SubmenuOption.BACK)

        // Atualizar cores e seta
        optionViews.forEachIndexed { index, textView ->
            val isSelected = (index == selectedOptionIndex)

            textView.setTextColor(if (isSelected) config.textSelectedColor else config.textColor)
        }

        Log.d(TAG, "UI atualizada - opção selecionada: ${submenuOptions[selectedOptionIndex]}")
    }

    /** Retorna texto formatado da opção com seta e estado. */
    private fun getOptionText(option: SubmenuOption): String {
        val isSelected = (submenuOptions[selectedOptionIndex] == option)
        val arrow = if (isSelected) "> " else "  "

        return when (option) {
            SubmenuOption.GAME_SOUNDS ->
                    "$arrow Game Sounds: ${if (isGameSoundsEnabled) "ON" else "OFF"}"
            SubmenuOption.MENU_SOUNDS ->
                    "$arrow Menu Sounds: ${if (isMenuSoundsEnabled) "ON" else "OFF"}"
            SubmenuOption.GAME_SPEED ->
                    "$arrow Game Speed: ${if (isFastSpeed) "Fast" else "Normal"}"
            SubmenuOption.BACK -> "$arrow Back"
        }
    }
}
