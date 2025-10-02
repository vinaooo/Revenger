package com.vinaooo.revenger.retromenu2

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.effects.BackgroundEffectFactory
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

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
    private lateinit var soundManager: MenuSoundManager

    // ============================================================
    // UI STATE
    // ============================================================

    /** Views do layout */
    private lateinit var menuContainer: ViewGroup
    private lateinit var backgroundImage: ImageView
    private lateinit var backgroundOverlay: View
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

    /** Pending game speed change from Settings submenu (aplicado ao fechar menu principal) */
    var pendingGameSpeedChange: Int? = null

    // ============================================================
    // LIFECYCLE
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config = RetroMenu2Config(requireContext())
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]
        controllerInput = ControllerInput2(config)
        
        // Inicializar MenuSoundManager
        soundManager = MenuSoundManager(requireContext())
        soundManager.initialize()

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
        menuContainer = view.findViewById(R.id.menuContainer)
        backgroundImage = view.findViewById(R.id.backgroundImage)
        backgroundOverlay = view.findViewById(R.id.backgroundOverlay)
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

        // Capturar screenshot e aplicar efeito de fundo
        captureAndApplyBackground()

        checkLoadStateAvailability()
        updateUI()

        // Configurar listener para voltar do submenu
        setupBackStackListener()

        Log.d(TAG, "onViewCreated concluído")
    }

    override fun onResume() {
        super.onResume()

        // Notificar ControllerInput que menu está aberto
        controllerInput.menuOpened()

        // NÃO pausar aqui - já foi pausado 300ms antes no callback
        // viewModel.pauseEmulator() foi movido para selectStartPauseCallback
        
        // Tocar som de abertura do menu
        soundManager.playOpen()

        Log.d(TAG, "Menu exibido - emulador já pausado")
    }

    override fun onPause() {
        super.onPause()

        // Notificar ControllerInput que menu fechou
        controllerInput.menuClosed()

        Log.d(TAG, "Menu fechado")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Aguardar 500ms antes de liberar SoundManager
        // Isso garante que sons de cancelamento/confirmação terminem de tocar
        Handler(Looper.getMainLooper()).postDelayed({
            soundManager.release()
            Log.d(TAG, "SoundManager liberado após delay")
        }, 500)
        
        Log.d(TAG, "RetroMenu2Fragment destruído")
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

        soundManager.playNavigation() // Som de navegação
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

        soundManager.playNavigation() // Som de navegação
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

        soundManager.playConfirm() // Som de confirmação
        Log.d(TAG, "Opção confirmada: $option")
        executeOption(option)
    }

    /** Cancela ação atual (equivalente a Continue). */
    private fun cancelAction() {
        soundManager.playCancel() // Som de cancelamento
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

        // Aplicar mudança pendente de velocidade ANTES de retomar
        pendingGameSpeedChange?.let { newSpeed ->
            viewModel.setGameSpeed(newSpeed)
            Log.d(
                    TAG,
                    "Aplicando Game Speed pendente: ${if (newSpeed == 2) "Fast (x2)" else "Normal (x1)"}"
            )
            pendingGameSpeedChange = null
        }

        // PASSO 1: Fechar fragment PRIMEIRO
        parentFragmentManager.beginTransaction().remove(this).commit()

        // PASSO 2: Aguardar 300ms para evitar sobreposição de sons
        Handler(Looper.getMainLooper()).postDelayed({
            // PASSO 3: Retomar emulador (frameSpeed = 1 ou o valor pendente aplicado acima)
            viewModel.resumeEmulator()
            Log.d(TAG, "Menu fechado - emulador retomado após delay")
        }, 300)
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

        // CRÍTICO: Desabilitar input do menu principal
        controllerInput.menuClosed()

        // Esconder menu principal (mantém background)
        menuContainer.visibility = View.GONE

        // Abrir SettingsSubmenuFragment como child fragment
        childFragmentManager
                .beginTransaction()
                .replace(R.id.submenuContainer, SettingsSubmenuFragment.newInstance())
                .addToBackStack("settings")
                .commit()
    }

    /** Abre submenu de Exit. */
    private fun openExitSubmenu() {
        Log.d(TAG, "Abrindo submenu Exit...")

        // CRÍTICO: Desabilitar input do menu principal
        controllerInput.menuClosed()

        // Esconder menu principal (mantém background)
        menuContainer.visibility = View.GONE

        // Abrir ExitSubmenuFragment como child fragment
        childFragmentManager
                .beginTransaction()
                .replace(R.id.submenuContainer, ExitSubmenuFragment.newInstance())
                .addToBackStack("exit")
                .commit()
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

    /** Configura listener para detectar quando volta de submenu. */
    private fun setupBackStackListener() {
        childFragmentManager.addOnBackStackChangedListener {
            // Quando back stack fica vazio (voltou do submenu), mostrar menu principal
            if (childFragmentManager.backStackEntryCount == 0) {
                // CRÍTICO: Reabilitar input do menu principal
                controllerInput.menuOpened()

                menuContainer.visibility = View.VISIBLE
                Log.d(TAG, "Voltou do submenu - menu principal visível e input reativado")
            }
        }
    }
    /** Atualiza a UI para refletir estado atual. */
    private fun updateUI() {
        // Atualizar cores e seta de acordo com seleção e disponibilidade
        optionViews.forEachIndexed { index, textView ->
            val option = menuOptions[index]
            val isSelected = (index == selectedOptionIndex)
            val isEnabled = isOptionEnabled(option)

            // Adicionar seta '> ' antes da opção selecionada (estilo retrô)
            val baseText = getOptionBaseText(option)
            textView.text = if (isSelected) "> $baseText" else "  $baseText"

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

    /** Retorna o texto base da opção (sem seta ou espaçamento). */
    private fun getOptionBaseText(option: MenuOption): String {
        return when (option) {
            MenuOption.CONTINUE -> getString(R.string.retromenu2_option_continue)
            MenuOption.RESTART -> getString(R.string.retromenu2_option_restart)
            MenuOption.SAVE_STATE -> getString(R.string.retromenu2_option_save)
            MenuOption.LOAD_STATE -> getString(R.string.retromenu2_option_load)
            MenuOption.SETTINGS -> getString(R.string.retromenu2_option_settings)
            MenuOption.EXIT -> getString(R.string.retromenu2_option_exit)
        }
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

    /** Processa KeyEvent do controller. Retorna true se o evento foi consumido pelo menu. */
    fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        // PRIORITY: Se há submenu ativo, rotear para ele
        val activeSubmenu = childFragmentManager.findFragmentById(R.id.submenuContainer)
        if (activeSubmenu != null && activeSubmenu.isVisible) {
            // Submenu ativo - chamar handleKeyEvent do submenu
            return when (activeSubmenu) {
                is SettingsSubmenuFragment -> activeSubmenu.handleKeyEvent(keyCode, event)
                is ExitSubmenuFragment -> activeSubmenu.handleKeyEvent(keyCode, event)
                else -> true // Bloquear evento se submenu desconhecido
            }
        }

        if (!controllerInput.isMenuOpen) {
            return false // Menu não está aberto
        }

        // Processar via ControllerInput2 do menu principal
        return controllerInput.processKeyEvent(keyCode, event)
    }

    /** Processa MotionEvent (analog stick). Retorna true se o evento foi consumido pelo menu. */
    fun handleMotionEvent(event: MotionEvent): Boolean {
        // PRIORITY: Se há submenu ativo, rotear para ele
        val activeSubmenu = childFragmentManager.findFragmentById(R.id.submenuContainer)
        if (activeSubmenu != null && activeSubmenu.isVisible) {
            // Submenu ativo - chamar handleMotionEvent do submenu
            return when (activeSubmenu) {
                is SettingsSubmenuFragment -> activeSubmenu.handleMotionEvent(event)
                is ExitSubmenuFragment -> activeSubmenu.handleMotionEvent(event)
                else -> true // Bloquear evento se submenu desconhecido
            }
        }

        if (!controllerInput.isMenuOpen) {
            return false // Menu não está aberto
        }

        // Processar via ControllerInput2 do menu principal
        return controllerInput.processMotionEvent(event)
    }

    // ============================================================
    // BACKGROUND EFFECT SYSTEM
    // ============================================================

    /**
     * Captura screenshot do jogo e aplica efeito de fundo configurado Executa em background thread
     * para não bloquear UI
     */
    private fun captureAndApplyBackground() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Capturar screenshot da Activity inteira (inclui RetroView + gamepad)
                val screenshot = captureFullScreen()

                if (screenshot != null) {
                    // Ler configuração do efeito
                    val effectType = config.backgroundEffectType
                    val intensity = config.backgroundEffectIntensity

                    Log.d(
                            TAG,
                            "Aplicando efeito tipo $effectType (${BackgroundEffectFactory.getEffectName(effectType)}) com intensidade $intensity"
                    )
                    Log.d(TAG, "Screenshot capturado: ${screenshot.width}x${screenshot.height}")

                    // Processar efeito em IO thread (processamento pesado)
                    val processedBitmap =
                            withContext(Dispatchers.IO) {
                                val effect = BackgroundEffectFactory.create(effectType)
                                Log.d(
                                        TAG,
                                        "Efeito criado: ${effect.javaClass.simpleName}, aplicando..."
                                )
                                val result = effect.apply(requireContext(), screenshot, intensity)
                                Log.d(TAG, "Efeito aplicado: ${result.width}x${result.height}")
                                result
                            }

                    // Atualizar UI (já em Main thread)
                    backgroundImage.setImageBitmap(processedBitmap)
                    backgroundImage.visibility = View.VISIBLE
                    backgroundOverlay.visibility = View.GONE // Esconde overlay de fallback

                    // Mostrar menu após efeito aplicado
                    menuContainer.visibility = View.VISIBLE

                    Log.d(
                            TAG,
                            "Background atualizado: ImageView VISIBLE, Overlay GONE, Menu VISIBLE, Bitmap ${processedBitmap.width}x${processedBitmap.height}"
                    )

                    // Liberar bitmap original
                    if (screenshot != processedBitmap) {
                        screenshot.recycle()
                    }
                } else {
                    Log.w(TAG, "Screenshot null - usando fallback overlay")
                    backgroundImage.visibility = View.GONE
                    backgroundOverlay.visibility = View.VISIBLE
                    menuContainer.visibility = View.VISIBLE // Mostrar menu mesmo sem efeito
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao capturar/processar background", e)
                backgroundOverlay.visibility = View.VISIBLE // Fallback para overlay
                menuContainer.visibility = View.VISIBLE // Mostrar menu mesmo com erro
            }
        }
    }

    /**
     * Captura screenshot da Activity inteira (inclui RetroView + gamepad) para que o fundo do menu
     * fique perfeitamente alinhado sem zoom ou crop
     */
    private suspend fun captureFullScreen(): Bitmap? =
            withContext(Dispatchers.Main) {
                return@withContext try {
                    // Capturar a Activity inteira através da decorView (root view)
                    val rootView = requireActivity().window.decorView.rootView

                    if (rootView.width <= 0 || rootView.height <= 0) {
                        Log.w(
                                TAG,
                                "RootView tem tamanho inválido: ${rootView.width}x${rootView.height}"
                        )
                        return@withContext null
                    }

                    // Criar bitmap com tamanho exato da tela
                    val bitmap =
                            Bitmap.createBitmap(
                                    rootView.width,
                                    rootView.height,
                                    Bitmap.Config.ARGB_8888
                            )

                    // Tentar captura via PixelCopy (funciona melhor com hardware rendering)
                    val success =
                            suspendCancellableCoroutine<Boolean> { continuation ->
                                try {
                                    PixelCopy.request(
                                            requireActivity().window,
                                            bitmap,
                                            { copyResult ->
                                                continuation.resume(copyResult == PixelCopy.SUCCESS)
                                            },
                                            Handler(Looper.getMainLooper())
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Erro ao iniciar PixelCopy da Activity", e)
                                    continuation.resume(false)
                                }
                            }

                    if (success) {
                        Log.d(
                                TAG,
                                "Screenshot fullscreen capturado: ${bitmap.width}x${bitmap.height}"
                        )
                        bitmap
                    } else {
                        Log.w(TAG, "PixelCopy falhou - tentando fallback com Canvas")
                        bitmap.recycle()
                        captureWithCanvas(rootView)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao capturar screenshot fullscreen", e)
                    null
                }
            }

    /** Fallback: tenta capturar usando Canvas (pode não funcionar bem com GLSurfaceView) */
    private fun captureWithCanvas(view: View): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            Log.d(TAG, "Screenshot via Canvas: ${bitmap.width}x${bitmap.height}")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Erro no fallback Canvas", e)
            null
        }
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
