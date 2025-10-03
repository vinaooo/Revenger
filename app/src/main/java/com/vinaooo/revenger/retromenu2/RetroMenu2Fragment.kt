package com.vinaooo.revenger.retromenu2

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
 * Fragment principal do RetroMenu2 - tela de pause com 5 opções:
 * 1. Continue
 * 2. Restart Game
 * 3. Save State
 * 4. Settings (abre submenu)
 * 5. Exit Game (abre submenu)
 *
 * Design Philosophy:
 * - Fragment só cuida de UI (renderização, navegação visual)
 * - ViewModel cuida de lógica de negócio (pause, save, load, etc)
 * - ControllerInput2 cuida de input (detectar teclas, analog, touch)
 */
class RetroMenu2Fragment : Fragment() {

    companion object {
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
    private lateinit var optionSettings: TextView
    private lateinit var optionExit: TextView
    private lateinit var buttonHint: TextView

    /** Lista de TextViews na ordem do menu */
    private val optionViews: MutableList<TextView> = mutableListOf()

    /** Índice da opção atualmente selecionada (0-4) */
    private var selectedOptionIndex = 0

    /** Lista de opções do menu */
    private val menuOptions =
            listOf(
                    MenuOption.CONTINUE,
                    MenuOption.RESTART,
                    MenuOption.SAVE_STATE,
                    MenuOption.SETTINGS,
                    MenuOption.EXIT
            )

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

        // Usar MenuSoundManager compartilhado do ViewModel
        // Ele já foi inicializado no app startup, então os sons estão prontos
        soundManager =
                viewModel.getMenuSoundManager()
                        ?: MenuSoundManager(requireContext()).also { it.initialize() }

        setupControllerCallbacks()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
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
        optionSettings = view.findViewById(R.id.optionSettings)
        optionExit = view.findViewById(R.id.optionExit)
        buttonHint = view.findViewById(R.id.buttonHint)

        // Popular lista de opções na ordem
        optionViews.clear()
        optionViews.add(optionContinue)
        optionViews.add(optionRestart)
        optionViews.add(optionSave)
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

        updateUI()

        // Configurar listener para voltar do submenu
        setupBackStackListener()
    }

    override fun onResume() {
        super.onResume()

        // Notificar ControllerInput que menu está aberto
        controllerInput.menuOpened()

        // NÃO pausar aqui - já foi pausado 300ms antes no callback
        // viewModel.pauseEmulator() foi movido para selectStartPauseCallback

        updateUI()

        // Tocar som de abertura do menu
        // O fragment foi preparado 300ms antes, então SoundPool já carregou os sons
        soundManager.playOpen()
    }

    override fun onPause() {
        super.onPause()

        // Notificar ControllerInput que menu fechou
        controllerInput.menuClosed()
    }

    override fun onDestroy() {
        super.onDestroy()

        // NÃO liberar SoundManager aqui - ele é compartilhado pelo ViewModel
        // e deve permanecer ativo durante toda a vida do app

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
        selectedOptionIndex =
                if (selectedOptionIndex > 0) {
                    selectedOptionIndex - 1
                } else {
                    menuOptions.size - 1 // Wrap para o final
                }

        soundManager.playNavigation() // Som de navegação
        updateUI()
    }

    /** Navega para baixo na lista de opções. */
    private fun navigateDown() {
        selectedOptionIndex =
                if (selectedOptionIndex < menuOptions.size - 1) {
                    selectedOptionIndex + 1
                } else {
                    0 // Wrap para o início
                }

        soundManager.playNavigation() // Som de navegação
        updateUI()
    }

    /** Confirma a seleção atual. */
    private fun confirmSelection() {
        val option = menuOptions[selectedOptionIndex]

        soundManager.playConfirm() // Som de confirmação
        executeOption(option)
    }

    /** Cancela ação atual (equivalente a Continue). */
    private fun cancelAction() {
        soundManager.playCancel() // Som de cancelamento
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
            pendingGameSpeedChange = null
        }

        // PASSO 1: Fechar fragment PRIMEIRO
        parentFragmentManager.beginTransaction().remove(this).commit()

        // PASSO 2: Aguardar 300ms para evitar sobreposição de sons
        Handler(Looper.getMainLooper())
                .postDelayed(
                        {
                            // PASSO 3: Retomar emulador (frameSpeed = 1 ou o valor pendente
                            // aplicado acima)
                            viewModel.resumeEmulator()
                        },
                        300
                )
    }

    /** Reinicia o jogo usando reset(). */
    private fun restartGame() {
        // CRÍTICO: Limpar keyLog ANTES de retomar para evitar reabertura imediata!
        viewModel.clearInputKeyLog()

        // Retomar emulador primeiro (frameSpeed = 1)
        viewModel.resumeEmulator()

        // Executar reset()
        viewModel.resetGame()

        // Fechar menu
        parentFragmentManager.beginTransaction().remove(this).commit()
    }

    /** Salva o estado do jogo. */
    private fun saveState() {
        // CORREÇÃO CRÍTICA: Usar mesmo fluxo do Modern Menu
        // NÃO pausar/despausar manualmente - deixar o callback lidar com tudo
        viewModel.saveStateCentralized { updateUI() }
    }

    /** Abre submenu de Settings. */
    private fun openSettingsSubmenu() {
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
            buttonHint.typeface = arcadaFont
        }
    }

    /** Configura touch listeners para navegação direta (tap = selecionar + confirmar). */
    private fun setupTouchListeners() {
        optionViews.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                val option = menuOptions[index]
                selectedOptionIndex = index
                updateUI()
                executeOption(option)
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
            }
        }
    }
    /** Atualiza a UI para refletir estado atual. */
    private fun updateUI() {
        // Atualizar cores e seta de acordo com seleção e disponibilidade
        optionViews.forEachIndexed { index, textView ->
            val option = menuOptions[index]
            val isSelected = (index == selectedOptionIndex)

            // Adicionar seta '> ' antes da opção selecionada (estilo retrô)
            val baseText = getOptionBaseText(option)
            textView.text = if (isSelected) "> $baseText" else "  $baseText"

            textView.setTextColor(
                    if (isSelected) {
                        config.textSelectedColor
                    } else {
                        config.textColor
                    }
            )
        }
    }

    /** Retorna o texto base da opção (sem seta ou espaçamento). */
    private fun getOptionBaseText(option: MenuOption): String {
        return when (option) {
            MenuOption.CONTINUE -> getString(R.string.retromenu2_option_continue)
            MenuOption.RESTART -> getString(R.string.retromenu2_option_restart)
            MenuOption.SAVE_STATE -> getString(R.string.retromenu2_option_save)
            MenuOption.SETTINGS -> getString(R.string.retromenu2_option_settings)
            MenuOption.EXIT -> getString(R.string.retromenu2_option_exit)
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

                    // Processar efeito em IO thread (processamento pesado)
                    val processedBitmap =
                            withContext(Dispatchers.IO) {
                                val effect = BackgroundEffectFactory.create(effectType)
                                val result = effect.apply(requireContext(), screenshot, intensity)
                                result
                            }

                    // Atualizar UI (já em Main thread)
                    backgroundImage.setImageBitmap(processedBitmap)
                    backgroundImage.visibility = View.VISIBLE
                    backgroundOverlay.visibility = View.GONE // Esconde overlay de fallback

                    // Mostrar menu após efeito aplicado
                    menuContainer.visibility = View.VISIBLE

                    // Liberar bitmap original
                    if (screenshot != processedBitmap) {
                        screenshot.recycle()
                    }
                } else {
                    backgroundImage.visibility = View.GONE
                    backgroundOverlay.visibility = View.VISIBLE
                    menuContainer.visibility = View.VISIBLE // Mostrar menu mesmo sem efeito
                }
            } catch (e: Exception) {
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
                                    continuation.resume(false)
                                }
                            }

                    if (success) {
                        bitmap
                    } else {
                        bitmap.recycle()
                        captureWithCanvas(rootView)
                    }
                } catch (e: Exception) {
                    null
                }
            }

    /** Fallback: tenta capturar usando Canvas (pode não funcionar bem com GLSurfaceView) */
    private fun captureWithCanvas(view: View): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
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
        SETTINGS,
        EXIT
    }
}
