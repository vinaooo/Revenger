package com.vinaooo.revenger.views

import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import com.vinaooo.revenger.performance.AdvancedPerformanceProfiler
import com.vinaooo.revenger.privacy.EnhancedPrivacyManager
import com.vinaooo.revenger.utils.AndroidCompatibility
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/** Main game activity for the emulator Phase 9.4: Enhanced with SDK 36 features */
class GameActivity : FragmentActivity() {

        companion object {
                private const val TAG = "GameActivity"
        }
        private lateinit var leftContainer: FrameLayout
        private lateinit var rightContainer: FrameLayout
        private lateinit var retroviewContainer: FrameLayout
        private lateinit var menuContainer: FrameLayout
        private val viewModel: GameActivityViewModel by viewModels()

        // Performance monitoring
        private var frameStartTime = 0L

        // GamePad container reference for orientation changes
        private lateinit var gamePadContainer: android.widget.LinearLayout

        // BroadcastReceiver para monitorar mudanças de auto-rotate
        private var rotationSettingsReceiver: android.content.BroadcastReceiver? = null

        // Modern permission launcher (replaces deprecated onRequestPermissionsResult)
        private val permissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                        permissions ->
                        val allGranted = permissions.all { it.value }
                        val grantResults =
                                if (allGranted) {
                                        IntArray(permissions.size).apply {
                                                fill(PackageManager.PERMISSION_GRANTED)
                                        }
                                } else {
                                        IntArray(permissions.size).apply {
                                                fill(PackageManager.PERMISSION_DENIED)
                                        }
                                }
                        EnhancedPrivacyManager.handlePermissionResult(grantResults) { _ -> }
                }

        override fun onCreate(savedInstanceState: Bundle?) {
                android.util.Log.e(
                        "GAME_ACTIVITY",
                        "🚨🚨🚨🚨🚨 GAME_ACTIVITY ONCREATE CALLED - NEW APK VERSION 🚨🚨🚨🚨🚨"
                )
                android.util.Log.e("GAME_ACTIVITY", "📅 TIMESTAMP: ${java.util.Date()}")
                android.util.Log.e(
                        "GAME_ACTIVITY",
                        "🔧 APK VERSION: DEBUG WITH EXTENSIVE LOGGING - REV ${System.currentTimeMillis()}"
                )

                super.onCreate(savedInstanceState)

                // Apply conditional features based on Android version
                AndroidCompatibility.applyConditionalFeatures()

                // Phase 9.4: Initialize SDK 36 features
                initializeSdk36Features()

                setContentView(R.layout.activity_game)

                // Configure status/navigation bars based on current theme
                configureSystemBarsForTheme()

                // Initialize views
                leftContainer = findViewById(R.id.left_container)
                rightContainer = findViewById(R.id.right_container)
                retroviewContainer = findViewById(R.id.retroview_container)
                menuContainer = findViewById(R.id.menu_container)

                // Get gamepad container reference
                val gamepadContainers = findViewById<android.widget.LinearLayout>(R.id.containers)
                gamePadContainer = gamepadContainers

                // Pass gamepad container reference to ViewModel
                viewModel.setGamePadContainer(gamepadContainers)

                /* Use immersive mode when we change the window insets */
                window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
                        view.post { viewModel.immersive(window) }
                        return@setOnApplyWindowInsetsListener windowInsets
                }

                registerInputListener()
                viewModel.setConfigOrientation(this)
                registerRotationSettingsListener() // Adicionar listener para mudanças de
                // auto-rotate
                viewModel.updateGamePadVisibility(this, leftContainer, rightContainer)
                viewModel.setupRetroView(this, retroviewContainer)
                viewModel.setupGamePads(this, leftContainer, rightContainer)

                // Force gamepad positioning based on orientation
                adjustGamePadPositionForOrientation(gamepadContainers)

                viewModel.prepareRetroMenu3(this)
                viewModel.setupMenuCallback(this)
                viewModel.setMenuContainer(menuContainer)
        }

        /**
         * Registra um listener para monitorar mudanças na configuração de auto-rotate do sistema.
         * Quando o usuário muda a preferência de auto-rotate nas configurações do sistema, a
         * orientação da app é reajustada automaticamente.
         */
        private fun registerRotationSettingsListener() {
                rotationSettingsReceiver =
                        object : android.content.BroadcastReceiver() {
                                override fun onReceive(
                                        context: android.content.Context?,
                                        intent: android.content.Intent?
                                ) {
                                        if (intent?.action ==
                                                        android.content.Intent
                                                                .ACTION_CONFIGURATION_CHANGED
                                        ) {
                                                // Configuração mudou (pode ser auto-rotate)
                                                Log.d(
                                                        TAG,
                                                        "[ROTATION_LISTENER] Configuração do sistema mudou - verificando auto-rotate"
                                                )
                                                reapplyOrientation()
                                        }
                                }
                        }

                // Criar IntentFilter para detectar mudanças de configuração
                val intentFilter = android.content.IntentFilter()
                intentFilter.addAction(android.content.Intent.ACTION_CONFIGURATION_CHANGED)

                // Registrar receiver com permissão apropriada
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        registerReceiver(
                                rotationSettingsReceiver,
                                intentFilter,
                                android.content.Context.RECEIVER_EXPORTED
                        )
                } else {
                        registerReceiver(rotationSettingsReceiver, intentFilter)
                }

                Log.d(
                        TAG,
                        "[ROTATION_LISTENER] BroadcastReceiver registrado para monitorar mudanças"
                )
        }

        /**
         * Reaplica a configuração de orientação quando a preferência de auto-rotate muda. Isso
         * permite que a app responda dinamicamente às mudanças do sistema.
         */
        private fun reapplyOrientation() {
                try {
                        val wasAutoRotate =
                                try {
                                        android.provider.Settings.System.getInt(
                                                contentResolver,
                                                android.provider.Settings.System
                                                        .ACCELEROMETER_ROTATION,
                                                0
                                        ) == 1
                                } catch (e: Exception) {
                                        false
                                }

                        Log.d(TAG, "[ROTATION_REAPPLY] Auto-rotate do sistema: $wasAutoRotate")

                        // Reaplica a configuração de orientação baseado no novo estado
                        viewModel.setConfigOrientation(this)

                        Log.d(TAG, "[ROTATION_REAPPLY] Orientação reajustada com sucesso")
                } catch (e: Exception) {
                        Log.e(
                                TAG,
                                "[ROTATION_REAPPLY] Erro ao reajustar orientação: ${e.message}",
                                e
                        )
                }
        }

        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
                super.onConfigurationChanged(newConfig)
                Log.d(TAG, "Configuration changed - orientation=${newConfig.orientation}")

                // Verificar se devemos reprocessar orientação
                // NÃO reprocessar quando config=3 e auto-rotate=OFF (para permitir botão manual)
                val configOrientation = resources.getInteger(R.integer.config_orientation)
                val autoRotateEnabled =
                        try {
                                android.provider.Settings.System.getInt(
                                        contentResolver,
                                        android.provider.Settings.System.ACCELEROMETER_ROTATION,
                                        0
                                ) == 1
                        } catch (e: Exception) {
                                false
                        }

                // Só reaplica orientação se config=1 ou 2 (forçadas) ou se config=3 com auto-rotate
                // ON
                if (configOrientation != 3 || autoRotateEnabled) {
                        reapplyOrientation()
                } else {
                        Log.d(
                                TAG,
                                "[ROTATION] config=3 + auto-rotate OFF - não reaplica (permite botão manual)"
                        )
                }

                adjustGamePadPositionForOrientation(gamePadContainer)

                // CRITICAL FIX: Re-register menu callbacks after rotation to prevent back button
                // issues
                viewModel.setupMenuCallback(this)
                Log.d(TAG, "[ROTATION_FIX] Menu callbacks re-registered after rotation")

                // --- SOLUÇÃO: Recriar fragments após mudança de orientação ---
                Log.d(TAG, "[ORIENTATION] ====== CHECKING FOR MENU AFTER ROTATION ======")

                val menuManager = viewModel.getMenuManager()
                val currentState =
                        menuManager.getCurrentState() // CRÍTICO: Verificar o backstack REAL para
                // detectar se estamos num submenu
                // O currentState pode estar desatualizado após operações de BACK
                val fragmentManager = supportFragmentManager
                val hasBackStack = fragmentManager.backStackEntryCount > 0
                val visibleFragment = fragmentManager.findFragmentById(R.id.menu_container)

                Log.d(TAG, "[ORIENTATION] Estado do menu: $currentState")
                Log.d(TAG, "[ORIENTATION] Backstack count: ${fragmentManager.backStackEntryCount}")
                Log.d(
                        TAG,
                        "[ORIENTATION] Visible fragment: ${visibleFragment?.javaClass?.simpleName}"
                )

                // CRITICAL: Only recreate fragments if menu is actually open
                if (visibleFragment == null ||
                                visibleFragment !is com.vinaooo.revenger.ui.retromenu3.MenuFragment
                ) {
                        Log.d(TAG, "[ORIENTATION] ⏭️ No menu fragment visible, skipping recreation")
                        Log.d(TAG, "[ORIENTATION] ====== ORIENTATION CHECK COMPLETED ======")
                        return
                }

                Log.d(TAG, "[ORIENTATION] ✅ Menu fragment found, proceeding with recreation")

                // Aguardar sistema completar rotação
                android.os.Handler(android.os.Looper.getMainLooper())
                        .postDelayed(
                                {
                                        Log.d(
                                                TAG,
                                                "[ORIENTATION] 🔄 Inside postDelayed - starting fragment recreation"
                                        )

                                        // CRITICAL FIX: Re-check backstack INSIDE postDelayed
                                        // O backstack pode ter mudado entre a verificação inicial e
                                        // a execução
                                        // do postDelayed
                                        val currentBackStackCount =
                                                fragmentManager.backStackEntryCount
                                        val hasBackStackNow = currentBackStackCount > 0

                                        Log.d(
                                                TAG,
                                                "[ORIENTATION] ⚠️ RE-CHECKING backstack: initial=$hasBackStack, now=$hasBackStackNow"
                                        )

                                        // CRITICAL FIX: Priorizar backstack ao invés do Fragment
                                        // visível
                                        // Se o backstack está vazio, SEMPRE usar MAIN_MENU
                                        // O Fragment visível pode estar temporariamente
                                        // desatualizado após BACK
                                        val effectiveState =
                                                if (hasBackStackNow) {
                                                        // Há backstack: verificar qual submenu está
                                                        // ativo
                                                        // Detectar estado REAL baseado no Fragment
                                                        // visível
                                                        val actualIsSubmenu =
                                                                when (visibleFragment) {
                                                                        is com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment,
                                                                        is com.vinaooo.revenger.ui.retromenu3.ProgressFragment,
                                                                        is com.vinaooo.revenger.ui.retromenu3.AboutFragment,
                                                                        is com.vinaooo.revenger.ui.retromenu3.ExitFragment ->
                                                                                true
                                                                        else -> false
                                                                }

                                                        Log.d(
                                                                TAG,
                                                                "[ORIENTATION] Backstack presente - detectando submenu: $actualIsSubmenu"
                                                        )

                                                        // Usar o estado baseado no Fragment visível
                                                        // se houver
                                                        // submenu
                                                        if (actualIsSubmenu) {
                                                                when (visibleFragment) {
                                                                        is com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment ->
                                                                                com.vinaooo.revenger
                                                                                        .ui
                                                                                        .retromenu3
                                                                                        .MenuState
                                                                                        .SETTINGS_MENU
                                                                        is com.vinaooo.revenger.ui.retromenu3.ProgressFragment ->
                                                                                com.vinaooo.revenger
                                                                                        .ui
                                                                                        .retromenu3
                                                                                        .MenuState
                                                                                        .PROGRESS_MENU
                                                                        is com.vinaooo.revenger.ui.retromenu3.AboutFragment ->
                                                                                com.vinaooo.revenger
                                                                                        .ui
                                                                                        .retromenu3
                                                                                        .MenuState
                                                                                        .ABOUT_MENU
                                                                        is com.vinaooo.revenger.ui.retromenu3.ExitFragment ->
                                                                                com.vinaooo.revenger
                                                                                        .ui
                                                                                        .retromenu3
                                                                                        .MenuState
                                                                                        .EXIT_MENU
                                                                        else -> currentState
                                                                }
                                                        } else {
                                                                currentState
                                                        }
                                                } else {
                                                        // Backstack vazio: SEMPRE usar MAIN_MENU
                                                        Log.d(
                                                                TAG,
                                                                "[ORIENTATION] ⚠️ Backstack vazio - forçando MAIN_MENU (currentState era: $currentState)"
                                                        )
                                                        com.vinaooo.revenger.ui.retromenu3.MenuState
                                                                .MAIN_MENU
                                                }

                                        Log.d(
                                                TAG,
                                                "[ORIENTATION] Estado efetivo: $effectiveState (original: $currentState)"
                                        )

                                        // Criar instância do Fragment correto baseado no estado
                                        // efetivo
                                        val newFragment =
                                                when (effectiveState) {
                                                        com.vinaooo.revenger.ui.retromenu3.MenuState
                                                                .MAIN_MENU -> {
                                                                Log.d(
                                                                        TAG,
                                                                        "[ORIENTATION] 📋 Menu principal ativo"
                                                                )
                                                                com.vinaooo.revenger.ui.retromenu3
                                                                        .RetroMenu3Fragment()
                                                        }
                                                        com.vinaooo.revenger.ui.retromenu3.MenuState
                                                                .SETTINGS_MENU -> {
                                                                Log.d(
                                                                        TAG,
                                                                        "[ORIENTATION] 📋 Submenu ativo: SETTINGS"
                                                                )
                                                                com.vinaooo.revenger.ui.retromenu3
                                                                        .SettingsMenuFragment()
                                                        }
                                                        com.vinaooo.revenger.ui.retromenu3.MenuState
                                                                .PROGRESS_MENU -> {
                                                                Log.d(
                                                                        TAG,
                                                                        "[ORIENTATION] 📋 Submenu ativo: PROGRESS"
                                                                )
                                                                com.vinaooo.revenger.ui.retromenu3
                                                                        .ProgressFragment()
                                                        }
                                                        com.vinaooo.revenger.ui.retromenu3.MenuState
                                                                .ABOUT_MENU -> {
                                                                Log.d(
                                                                        TAG,
                                                                        "[ORIENTATION] 📋 Submenu ativo: ABOUT"
                                                                )
                                                                com.vinaooo.revenger.ui.retromenu3
                                                                        .AboutFragment()
                                                        }
                                                        com.vinaooo.revenger.ui.retromenu3.MenuState
                                                                .EXIT_MENU -> {
                                                                Log.d(
                                                                        TAG,
                                                                        "[ORIENTATION] 📋 Submenu ativo: EXIT"
                                                                )
                                                                com.vinaooo.revenger.ui.retromenu3
                                                                        .ExitFragment()
                                                        }
                                                        else -> {
                                                                Log.e(
                                                                        TAG,
                                                                        "[ORIENTATION] ❌ Estado desconhecido: $effectiveState"
                                                                )
                                                                null
                                                        }
                                                }

                                        if (newFragment != null) {
                                                // NOTA: syncState do NavigationController será
                                                // chamado APÓS todos os fragments
                                                // serem criados e registrados (no postDelayed após
                                                // registrar submenu).
                                                // Isso evita que registerFragment() sobrescreva o
                                                // estado.

                                                val isMainMenu =
                                                        effectiveState ==
                                                                com.vinaooo.revenger.ui.retromenu3
                                                                        .MenuState.MAIN_MENU

                                                // Limpar COMPLETAMENTE o backstack antes de recriar
                                                Log.d(
                                                        TAG,
                                                        "[ORIENTATION] 🗑️ Limpando backstack (count=${fragmentManager.backStackEntryCount})"
                                                )
                                                fragmentManager.popBackStack(
                                                        null,
                                                        androidx.fragment.app.FragmentManager
                                                                .POP_BACK_STACK_INCLUSIVE
                                                )

                                                // Remover qualquer Fragment que esteja no container
                                                fragmentManager.findFragmentById(
                                                                R.id.menu_container
                                                        )
                                                        ?.let { existingFragment ->
                                                                Log.d(
                                                                        TAG,
                                                                        "[ORIENTATION] 🗑️ Removendo fragment existente: ${existingFragment::class.java.simpleName}"
                                                                )
                                                                fragmentManager
                                                                        .beginTransaction()
                                                                        .remove(existingFragment)
                                                                        .commitNowAllowingStateLoss()
                                                        }

                                                // Aguardar limpeza completa
                                                android.os.Handler(
                                                                android.os.Looper.getMainLooper()
                                                        )
                                                        .postDelayed(
                                                                {
                                                                        // Use the backstack state
                                                                        // from BEFORE cleanup
                                                                        // (hasBackStackNow)
                                                                        // We cleared the backstack
                                                                        // above, so checking
                                                                        // it now would always be 0
                                                                        Log.d(
                                                                                TAG,
                                                                                "[ORIENTATION] 📋 Recriando hierarquia: isMainMenu=$isMainMenu"
                                                                        )

                                                                        if (isMainMenu) {
                                                                                // MAIN_MENU
                                                                                // sozinho:
                                                                                // adicionar sem
                                                                                // backstack
                                                                                // Create NEW
                                                                                // RetroMenu3Fragment
                                                                                val mainMenuFragment =
                                                                                        com.vinaooo
                                                                                                .revenger
                                                                                                .ui
                                                                                                .retromenu3
                                                                                                .RetroMenu3Fragment()

                                                                                Log.d(
                                                                                        TAG,
                                                                                        "[ORIENTATION] ➕ Adicionando RetroMenu3Fragment"
                                                                                )
                                                                                val transaction =
                                                                                        fragmentManager
                                                                                                .beginTransaction()
                                                                                                .replace(
                                                                                                        R.id.menu_container,
                                                                                                        mainMenuFragment,
                                                                                                        "RetroMenu3Fragment"
                                                                                                )

                                                                                transaction
                                                                                        .runOnCommit {
                                                                                                Log.d(
                                                                                                        TAG,
                                                                                                        "[ORIENTATION] 🔄 Main menu committed, updating reference"
                                                                                                )

                                                                                                // Atualizar referência do
                                                                                                // RetroMenu3Fragment no ViewModel
                                                                                                viewModel
                                                                                                        .updateRetroMenu3FragmentReference(
                                                                                                                mainMenuFragment
                                                                                                        )
                                                                                                Log.d(
                                                                                                        TAG,
                                                                                                        "[ORIENTATION] 📋 RetroMenu3Fragment reference updated"
                                                                                                )

                                                                                                // Restaurar foco
                                                                                                android.os
                                                                                                        .Handler(
                                                                                                                android.os
                                                                                                                        .Looper
                                                                                                                        .getMainLooper()
                                                                                                        )
                                                                                                        .postDelayed(
                                                                                                                {
                                                                                                                        val firstItem =
                                                                                                                                findViewById<
                                                                                                                                        android.view.View>(
                                                                                                                                        R.id.menu_continue
                                                                                                                                )
                                                                                                                        if (firstItem !=
                                                                                                                                        null &&
                                                                                                                                        firstItem
                                                                                                                                                .isFocusable
                                                                                                                        ) {
                                                                                                                                firstItem
                                                                                                                                        .requestFocus()
                                                                                                                                Log.d(
                                                                                                                                        TAG,
                                                                                                                                        "[ORIENTATION] 🎮 Foco restaurado no menu principal"
                                                                                                                                )
                                                                                                                        }
                                                                                                                },
                                                                                                                500
                                                                                                        )
                                                                                        }

                                                                                transaction.commit()
                                                                        } else {
                                                                                // SUBMENU:
                                                                                // precisamos
                                                                                // recriar TODA a
                                                                                // pilha
                                                                                // (base + topo)
                                                                                Log.d(
                                                                                        TAG,
                                                                                        "[ORIENTATION] ➕ Recriando pilha: RetroMenu3 (base) + Submenu (topo)"
                                                                                )

                                                                                // 1. Adicionar
                                                                                // RetroMenu3Fragment na base
                                                                                // (sem backstack)
                                                                                val retroMenu3 =
                                                                                        com.vinaooo
                                                                                                .revenger
                                                                                                .ui
                                                                                                .retromenu3
                                                                                                .RetroMenu3Fragment()
                                                                                fragmentManager
                                                                                        .beginTransaction()
                                                                                        .replace(
                                                                                                R.id.menu_container,
                                                                                                retroMenu3,
                                                                                                "RetroMenu3Fragment"
                                                                                        )
                                                                                        .commitNowAllowingStateLoss()

                                                                                // Atualizar
                                                                                // referência no
                                                                                // ViewModel
                                                                                viewModel
                                                                                        .updateRetroMenu3FragmentReference(
                                                                                                retroMenu3
                                                                                        )
                                                                                Log.d(
                                                                                        TAG,
                                                                                        "[ORIENTATION] 📋 RetroMenu3Fragment base criado e registrado"
                                                                                )

                                                                                // CRITICAL: Update
                                                                                // MenuStateManager
                                                                                // to the
                                                                                // submenu state
                                                                                // This ensures
                                                                                // getCurrentFragment() returns
                                                                                // the correct
                                                                                // Fragment
                                                                                val menuManager =
                                                                                        viewModel
                                                                                                .getMenuManager()
                                                                                menuManager
                                                                                        .navigateToState(
                                                                                                effectiveState
                                                                                        )
                                                                                Log.d(
                                                                                        TAG,
                                                                                        "[ORIENTATION] 🎯 MenuStateManager updated to state: $effectiveState"
                                                                                )

                                                                                // 2. CRITICAL:
                                                                                // Configure
                                                                                // listener BEFORE
                                                                                // adding Fragment
                                                                                // This ensures
                                                                                // click listeners
                                                                                // work
                                                                                // correctly after
                                                                                // rotation
                                                                                when (effectiveState
                                                                                ) {
                                                                                        com.vinaooo
                                                                                                .revenger
                                                                                                .ui
                                                                                                .retromenu3
                                                                                                .MenuState
                                                                                                .SETTINGS_MENU -> {
                                                                                                val settingsFragment =
                                                                                                        newFragment as
                                                                                                                com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
                                                                                                // Configure listener BEFORE adding
                                                                                                // to FragmentManager
                                                                                                settingsFragment
                                                                                                        .setSettingsListener(
                                                                                                                retroMenu3 as
                                                                                                                        com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment.SettingsMenuListener
                                                                                                        )
                                                                                                Log.d(
                                                                                                        TAG,
                                                                                                        "[ORIENTATION] 🔧 SettingsMenuFragment listener configured BEFORE adding"
                                                                                                )
                                                                                        }
                                                                                        else -> {
                                                                                                // Other fragments don't need
                                                                                                // pre-configuration
                                                                                        }
                                                                                }

                                                                                // 3. Aguardar e
                                                                                // adicionar submenu
                                                                                // no topo
                                                                                // (COM backstack)
                                                                                android.os.Handler(
                                                                                                android.os
                                                                                                        .Looper
                                                                                                        .getMainLooper()
                                                                                        )
                                                                                        .postDelayed(
                                                                                                {
                                                                                                        val submenuTag =
                                                                                                                newFragment::class
                                                                                                                        .java
                                                                                                                        .simpleName
                                                                                                        Log.d(
                                                                                                                TAG,
                                                                                                                "[ORIENTATION] ➕ Adicionando submenu no topo: $submenuTag"
                                                                                                        )

                                                                                                        fragmentManager
                                                                                                                .beginTransaction()
                                                                                                                .replace(
                                                                                                                        R.id.menu_container,
                                                                                                                        newFragment,
                                                                                                                        submenuTag
                                                                                                                )
                                                                                                                .addToBackStack(
                                                                                                                        submenuTag
                                                                                                                )
                                                                                                                .commit()

                                                                                                        // CRÍTICO: Registrar
                                                                                                        // submenu no ViewModel
                                                                                                        // (listener já foi
                                                                                                        // configurado)
                                                                                                        android.os
                                                                                                                .Handler(
                                                                                                                        android.os
                                                                                                                                .Looper
                                                                                                                                .getMainLooper()
                                                                                                                )
                                                                                                                .postDelayed(
                                                                                                                        {
                                                                                                                                when (effectiveState
                                                                                                                                ) {
                                                                                                                                        com.vinaooo
                                                                                                                                                .revenger
                                                                                                                                                .ui
                                                                                                                                                .retromenu3
                                                                                                                                                .MenuState
                                                                                                                                                .SETTINGS_MENU -> {
                                                                                                                                                val settingsFragment =
                                                                                                                                                        newFragment as
                                                                                                                                                                com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
                                                                                                                                                // Use lightweight registration for rotation (doesn't activate state)
                                                                                                                                                viewModel
                                                                                                                                                        .registerSettingsMenuFragmentForRotation(
                                                                                                                                                                settingsFragment
                                                                                                                                                        )
                                                                                                                                                Log.d(
                                                                                                                                                        TAG,
                                                                                                                                                        "[ORIENTATION] 📋 SettingsMenuFragment registrado (rotation)"
                                                                                                                                                )
                                                                                                                                        }
                                                                                                                                        com.vinaooo
                                                                                                                                                .revenger
                                                                                                                                                .ui
                                                                                                                                                .retromenu3
                                                                                                                                                .MenuState
                                                                                                                                                .PROGRESS_MENU -> {
                                                                                                                                                viewModel
                                                                                                                                                        .registerProgressFragmentForRotation(
                                                                                                                                                                newFragment as
                                                                                                                                                                        com.vinaooo.revenger.ui.retromenu3.ProgressFragment
                                                                                                                                                        )
                                                                                                                                                Log.d(
                                                                                                                                                        TAG,
                                                                                                                                                        "[ORIENTATION] 📋 ProgressFragment registrado (rotation)"
                                                                                                                                                )
                                                                                                                                        }
                                                                                                                                        com.vinaooo
                                                                                                                                                .revenger
                                                                                                                                                .ui
                                                                                                                                                .retromenu3
                                                                                                                                                .MenuState
                                                                                                                                                .ABOUT_MENU -> {
                                                                                                                                                viewModel
                                                                                                                                                        .registerAboutFragmentForRotation(
                                                                                                                                                                newFragment as
                                                                                                                                                                        com.vinaooo.revenger.ui.retromenu3.AboutFragment
                                                                                                                                                        )
                                                                                                                                                Log.d(
                                                                                                                                                        TAG,
                                                                                                                                                        "[ORIENTATION] 📋 AboutFragment registrado (rotation)"
                                                                                                                                                )
                                                                                                                                        }
                                                                                                                                        com.vinaooo
                                                                                                                                                .revenger
                                                                                                                                                .ui
                                                                                                                                                .retromenu3
                                                                                                                                                .MenuState
                                                                                                                                                .EXIT_MENU -> {
                                                                                                                                                viewModel
                                                                                                                                                        .registerExitFragmentForRotation(
                                                                                                                                                                newFragment as
                                                                                                                                                                        com.vinaooo.revenger.ui.retromenu3.ExitFragment
                                                                                                                                                        )
                                                                                                                                                Log.d(
                                                                                                                                                        TAG,
                                                                                                                                                        "[ORIENTATION] 📋 ExitFragment registrado (rotation)"
                                                                                                                                                )
                                                                                                                                        }
                                                                                                                                        else -> {
                                                                                                                                                Log.w(
                                                                                                                                                        TAG,
                                                                                                                                                        "[ORIENTATION] ⚠️ Estado desconhecido, submenu não registrado"
                                                                                                                                                )
                                                                                                                                        }
                                                                                                                                }

                                                                                                                                // CRITICAL: Sincronizar estado do NavigationController APÓS todos os fragments
                                                                                                                                // serem criados e registrados. Isso previne que registerFragment() sobrescreva o estado.
                                                                                                                                val navMenuTypeForSync =
                                                                                                                                        when (effectiveState
                                                                                                                                        ) {
                                                                                                                                                com.vinaooo
                                                                                                                                                        .revenger
                                                                                                                                                        .ui
                                                                                                                                                        .retromenu3
                                                                                                                                                        .MenuState
                                                                                                                                                        .MAIN_MENU ->
                                                                                                                                                        com.vinaooo
                                                                                                                                                                .revenger
                                                                                                                                                                .ui
                                                                                                                                                                .retromenu3
                                                                                                                                                                .navigation
                                                                                                                                                                .MenuType
                                                                                                                                                                .MAIN
                                                                                                                                                com.vinaooo
                                                                                                                                                        .revenger
                                                                                                                                                        .ui
                                                                                                                                                        .retromenu3
                                                                                                                                                        .MenuState
                                                                                                                                                        .SETTINGS_MENU ->
                                                                                                                                                        com.vinaooo
                                                                                                                                                                .revenger
                                                                                                                                                                .ui
                                                                                                                                                                .retromenu3
                                                                                                                                                                .navigation
                                                                                                                                                                .MenuType
                                                                                                                                                                .SETTINGS
                                                                                                                                                com.vinaooo
                                                                                                                                                        .revenger
                                                                                                                                                        .ui
                                                                                                                                                        .retromenu3
                                                                                                                                                        .MenuState
                                                                                                                                                        .PROGRESS_MENU ->
                                                                                                                                                        com.vinaooo
                                                                                                                                                                .revenger
                                                                                                                                                                .ui
                                                                                                                                                                .retromenu3
                                                                                                                                                                .navigation
                                                                                                                                                                .MenuType
                                                                                                                                                                .PROGRESS
                                                                                                                                                com.vinaooo
                                                                                                                                                        .revenger
                                                                                                                                                        .ui
                                                                                                                                                        .retromenu3
                                                                                                                                                        .MenuState
                                                                                                                                                        .ABOUT_MENU ->
                                                                                                                                                        com.vinaooo
                                                                                                                                                                .revenger
                                                                                                                                                                .ui
                                                                                                                                                                .retromenu3
                                                                                                                                                                .navigation
                                                                                                                                                                .MenuType
                                                                                                                                                                .ABOUT
                                                                                                                                                com.vinaooo
                                                                                                                                                        .revenger
                                                                                                                                                        .ui
                                                                                                                                                        .retromenu3
                                                                                                                                                        .MenuState
                                                                                                                                                        .EXIT_MENU ->
                                                                                                                                                        com.vinaooo
                                                                                                                                                                .revenger
                                                                                                                                                                .ui
                                                                                                                                                                .retromenu3
                                                                                                                                                                .navigation
                                                                                                                                                                .MenuType
                                                                                                                                                                .EXIT
                                                                                                                                                else ->
                                                                                                                                                        com.vinaooo
                                                                                                                                                                .revenger
                                                                                                                                                                .ui
                                                                                                                                                                .retromenu3
                                                                                                                                                                .navigation
                                                                                                                                                                .MenuType
                                                                                                                                                                .MAIN
                                                                                                                                        }
                                                                                                                                viewModel
                                                                                                                                        .navigationController
                                                                                                                                        ?.syncState(
                                                                                                                                                menuType =
                                                                                                                                                        navMenuTypeForSync,
                                                                                                                                                selectedIndex =
                                                                                                                                                        0,
                                                                                                                                                clearStack =
                                                                                                                                                        false // Não limpar stack pois backstack já foi reconstruído
                                                                                                                                        )
                                                                                                                                Log.d(
                                                                                                                                        TAG,
                                                                                                                                        "[ORIENTATION] 🔄 NavigationController syncState chamado: $navMenuTypeForSync"
                                                                                                                                )
                                                                                                                        },
                                                                                                                        100
                                                                                                                ) // Aguardar
                                                                                                        // Fragment
                                                                                                        // ser
                                                                                                        // adicionado
                                                                                                        // antes de
                                                                                                        // registrar

                                                                                                        // Restaurar foco no
                                                                                                        // submenu
                                                                                                        android.os
                                                                                                                .Handler(
                                                                                                                        android.os
                                                                                                                                .Looper
                                                                                                                                .getMainLooper()
                                                                                                                )
                                                                                                                .postDelayed(
                                                                                                                        {
                                                                                                                                val firstFocusableId =
                                                                                                                                        when (effectiveState
                                                                                                                                        ) {
                                                                                                                                                com.vinaooo
                                                                                                                                                        .revenger
                                                                                                                                                        .ui
                                                                                                                                                        .retromenu3
                                                                                                                                                        .MenuState
                                                                                                                                                        .SETTINGS_MENU ->
                                                                                                                                                        R.id.settings_sound
                                                                                                                                                com.vinaooo
                                                                                                                                                        .revenger
                                                                                                                                                        .ui
                                                                                                                                                        .retromenu3
                                                                                                                                                        .MenuState
                                                                                                                                                        .PROGRESS_MENU ->
                                                                                                                                                        R.id.progress_load_state
                                                                                                                                                com.vinaooo
                                                                                                                                                        .revenger
                                                                                                                                                        .ui
                                                                                                                                                        .retromenu3
                                                                                                                                                        .MenuState
                                                                                                                                                        .ABOUT_MENU ->
                                                                                                                                                        R.id.about_back
                                                                                                                                                com.vinaooo
                                                                                                                                                        .revenger
                                                                                                                                                        .ui
                                                                                                                                                        .retromenu3
                                                                                                                                                        .MenuState
                                                                                                                                                        .EXIT_MENU ->
                                                                                                                                                        R.id.exit_menu_option_a
                                                                                                                                                else ->
                                                                                                                                                        null
                                                                                                                                        }

                                                                                                                                if (firstFocusableId !=
                                                                                                                                                null
                                                                                                                                ) {
                                                                                                                                        val firstItem =
                                                                                                                                                findViewById<
                                                                                                                                                        android.view.View>(
                                                                                                                                                        firstFocusableId
                                                                                                                                                )
                                                                                                                                        if (firstItem !=
                                                                                                                                                        null &&
                                                                                                                                                        firstItem
                                                                                                                                                                .isFocusable
                                                                                                                                        ) {
                                                                                                                                                firstItem
                                                                                                                                                        .requestFocus()
                                                                                                                                                Log.d(
                                                                                                                                                        TAG,
                                                                                                                                                        "[ORIENTATION] 🎮 Foco restaurado no submenu"
                                                                                                                                                )
                                                                                                                                        }
                                                                                                                                }
                                                                                                                        },
                                                                                                                        600
                                                                                                                )
                                                                                                },
                                                                                                150
                                                                                        ) // Delay
                                                                                // para
                                                                                // garantir que
                                                                                // RetroMenu3 foi
                                                                                // completamente
                                                                                // adicionado
                                                                        }
                                                                },
                                                                100
                                                        )
                                        }

                                        Log.d(
                                                TAG,
                                                "[ORIENTATION] ====== ORIENTATION CHECK COMPLETED ======"
                                        )
                                },
                                250
                        ) // Delay para garantir que sistema terminou de processar rotação
        }

        /**
         * Initialize SDK 36 features with backward compatibility Phase 9.4: Target SDK 36 Features
         */
        private fun initializeSdk36Features() {
                // Dynamic theming is now handled automatically by Material 3 theme inheritance

                // Initialize enhanced privacy controls
                EnhancedPrivacyManager.initializePrivacyControls(this)

                // Start performance profiling
                AdvancedPerformanceProfiler.startProfiling(this)

                // Show debug overlay after layout is ready
                window.decorView.post {
                        AdvancedPerformanceProfiler.showDebugOverlay(this@GameActivity)
                }
        }

        /** Configure status/navigation bars based on current theme for optimal visibility */
        private fun configureSystemBarsForTheme() {
                // Detect if we're using dark theme
                val isDarkTheme =
                        resources.configuration.uiMode and
                                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                                android.content.res.Configuration.UI_MODE_NIGHT_YES

                // In dark theme: use light icons (true) for better visibility on dark backgrounds
                // In light theme: use dark icons (false) for better visibility on light backgrounds
                val lightIcons = isDarkTheme

                // Apply the configuration
                window.decorView.windowInsetsController?.setSystemBarsAppearance(
                        if (lightIcons)
                                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        else 0,
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )

                // Also set for navigation bar if supported
                window.decorView.windowInsetsController?.setSystemBarsAppearance(
                        if (lightIcons)
                                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                        else 0,
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
        }

        /** Listen for new controller additions and removals */
        private fun registerInputListener() {
                val inputManager = getSystemService(INPUT_SERVICE) as InputManager
                inputManager.registerInputDeviceListener(
                        object : InputManager.InputDeviceListener {
                                override fun onInputDeviceAdded(deviceId: Int) {
                                        viewModel.updateGamePadVisibility(
                                                this@GameActivity,
                                                leftContainer,
                                                rightContainer
                                        )
                                }
                                override fun onInputDeviceRemoved(deviceId: Int) {
                                        viewModel.updateGamePadVisibility(
                                                this@GameActivity,
                                                leftContainer,
                                                rightContainer
                                        )
                                }
                                override fun onInputDeviceChanged(deviceId: Int) {
                                        viewModel.updateGamePadVisibility(
                                                this@GameActivity,
                                                leftContainer,
                                                rightContainer
                                        )
                                }
                        },
                        null
                )

                /* Setup back pressed handling - check menu state and mode */
                onBackPressedDispatcher.addCallback(
                        this,
                        object : OnBackPressedCallback(true) {
                                override fun handleOnBackPressed() {
                                        // PHASE 3.4a: Route Android system back through
                                        // NavigationController (permanently enabled)
                                        Log.d(
                                                TAG,
                                                "[BACK] PHASE 3: Routing Android back through NavigationController"
                                        )

                                        // If menu is open, navigate back through controller
                                        if (viewModel.isAnyMenuActive()) {
                                                viewModel.navigationController
                                                        ?.handleNavigationEvent(
                                                                com.vinaooo.revenger.ui.retromenu3
                                                                        .navigation.NavigationEvent
                                                                        .NavigateBack(
                                                                                inputSource =
                                                                                        com.vinaooo
                                                                                                .revenger
                                                                                                .ui
                                                                                                .retromenu3
                                                                                                .navigation
                                                                                                .InputSource
                                                                                                .SYSTEM_BACK,
                                                                                keyCode =
                                                                                        android.view
                                                                                                .KeyEvent
                                                                                                .KEYCODE_BACK
                                                                        )
                                                        )
                                        }
                                        // If menu is not open, check if back should open
                                        // menu
                                        else if (viewModel.shouldHandleBackButton()) {
                                                viewModel.navigationController
                                                        ?.handleNavigationEvent(
                                                                com.vinaooo.revenger.ui.retromenu3
                                                                        .navigation.NavigationEvent
                                                                        .OpenMenu(
                                                                                inputSource =
                                                                                        com.vinaooo
                                                                                                .revenger
                                                                                                .ui
                                                                                                .retromenu3
                                                                                                .navigation
                                                                                                .InputSource
                                                                                                .SYSTEM_BACK
                                                                        )
                                                        )
                                        } else {
                                                // Use default back button behavior
                                                isEnabled = false
                                                onBackPressedDispatcher.onBackPressed()
                                        }
                                }
                        }
                )
        }

        // PHASE 3: Save/restore navigation state during rotation (permanently enabled)
        override fun onSaveInstanceState(outState: Bundle) {
                super.onSaveInstanceState(outState)
                viewModel.navigationController?.saveState(outState)
                android.util.Log.d("GameActivity", "[ROTATION] Navigation state saved")
        }

        override fun onRestoreInstanceState(savedInstanceState: Bundle) {
                super.onRestoreInstanceState(savedInstanceState)
                viewModel.navigationController?.restoreState(savedInstanceState)
                android.util.Log.d("GameActivity", "[ROTATION] Navigation state restored")
        }

        override fun onDestroy() {
                // Remover listener de mudanças de auto-rotate
                rotationSettingsReceiver?.let {
                        try {
                                unregisterReceiver(it)
                                Log.d(TAG, "[ROTATION_LISTENER] BroadcastReceiver desregistrado")
                        } catch (e: Exception) {
                                Log.e(
                                        TAG,
                                        "[ROTATION_LISTENER] Erro ao desregistrar receiver: ${e.message}"
                                )
                        }
                }

                // Stop performance profiling
                AdvancedPerformanceProfiler.stopProfiling()

                // Hide debug overlay
                AdvancedPerformanceProfiler.hideDebugOverlay()

                // Clean up view model
                viewModel.dispose()
                viewModel.detachRetroView(this)
                super.onDestroy()
        }

        override fun onPause() {
                viewModel.preserveState()
                super.onPause()
        }

        override fun onResume() {
                super.onResume()
                frameStartTime = System.nanoTime()
        }

        override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
                // Record frame time for performance monitoring
                recordFrameTime()

                return viewModel.processKeyEvent(keyCode, event) ?: super.onKeyDown(keyCode, event)
        }

        /** Adjust gamepad position based on screen orientation */
        private fun adjustGamePadPositionForOrientation(
                gamepadContainer: android.widget.LinearLayout
        ) {
                val layoutParams = gamepadContainer.layoutParams as FrameLayout.LayoutParams

                // Check current orientation
                val isPortrait =
                        resources.configuration.orientation ==
                                android.content.res.Configuration.ORIENTATION_PORTRAIT

                Log.d(TAG, "Current orientation: ${if (isPortrait) "PORTRAIT" else "LANDSCAPE"}")
                Log.d(TAG, "Current layout gravity before: ${layoutParams.gravity}")

                if (isPortrait) {
                        // Force bottom positioning in portrait
                        layoutParams.gravity = android.view.Gravity.BOTTOM
                        Log.d(TAG, "GamePad positioned at BOTTOM for portrait mode")

                        // Increase gamepad sizes for portrait (40% each instead of 25%)
                        adjustGamePadSizes(gamepadContainer, 0.40f, 0.2f)
                } else {
                        // Keep top positioning in landscape
                        layoutParams.gravity = android.view.Gravity.TOP
                        Log.d(TAG, "GamePad positioned at TOP for landscape mode")

                        // Keep original sizes for landscape (25% each)
                        adjustGamePadSizes(gamepadContainer, 0.25f, 0.5f)
                }

                Log.d(TAG, "Final layout gravity: ${layoutParams.gravity}")
                gamepadContainer.layoutParams = layoutParams
                gamepadContainer.requestLayout()
        }

        /** Adjust gamepad container sizes programmatically */
        private fun adjustGamePadSizes(
                container: android.widget.LinearLayout,
                gamePadWeight: Float,
                centerWeight: Float
        ) {
                // Find the child views
                val leftContainer =
                        container.findViewById<android.widget.FrameLayout>(R.id.left_container)
                val rightContainer =
                        container.findViewById<android.widget.FrameLayout>(R.id.right_container)
                val centerView = container.getChildAt(1) // The View in the middle

                // Adjust weights
                val leftParams =
                        leftContainer.layoutParams as android.widget.LinearLayout.LayoutParams
                leftParams.weight = gamePadWeight
                leftContainer.layoutParams = leftParams

                val rightParams =
                        rightContainer.layoutParams as android.widget.LinearLayout.LayoutParams
                rightParams.weight = gamePadWeight
                rightContainer.layoutParams = rightParams

                if (centerView != null) {
                        val centerParams =
                                centerView.layoutParams as android.widget.LinearLayout.LayoutParams
                        centerParams.weight = centerWeight
                        centerView.layoutParams = centerParams
                }

                Log.d(
                        TAG,
                        "GamePad sizes adjusted - GamePads: $gamePadWeight, Center: $centerWeight"
                )
        }

        override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
                return viewModel.processKeyEvent(keyCode, event) ?: super.onKeyUp(keyCode, event)
        }

        override fun onGenericMotionEvent(event: MotionEvent): Boolean {
                // Record frame time for performance monitoring
                recordFrameTime()

                return viewModel.processMotionEvent(event) ?: super.onGenericMotionEvent(event)
        }

        /** Record frame time for performance monitoring */
        private fun recordFrameTime() {
                val currentTime = System.nanoTime()
                if (frameStartTime > 0) {
                        val frameTime = currentTime - frameStartTime
                        AdvancedPerformanceProfiler.recordFrameTime(frameTime)
                }
                frameStartTime = currentTime
        }

        /**
         * Method to request permissions using modern API Call this instead of deprecated
         * ActivityCompat.requestPermissions
         */
        fun requestPermissionsModern(permissions: Array<String>) {
                permissionLauncher.launch(permissions)
        }
}
