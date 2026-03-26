package com.vinaooo.revenger.views

import android.content.pm.PackageManager
import android.hardware.input.InputManager
import com.vinaooo.revenger.managers.GameLifecycleObserver
import com.vinaooo.revenger.managers.AudioRoutingManager
import android.media.AudioManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.gamepad.GamePadAlignmentManager
import com.vinaooo.revenger.performance.AdvancedPerformanceProfiler
import com.vinaooo.revenger.privacy.EnhancedPrivacyManager
import com.vinaooo.revenger.ui.retromenu3.callbacks.SettingsMenuListener
import com.vinaooo.revenger.utils.AndroidCompatibility
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
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
        private lateinit var loadPreviewOverlay: android.widget.ImageView
        private lateinit var audioRoutingManager: AudioRoutingManager
        private lateinit var gameLifecycleObserver: GameLifecycleObserver
        private val viewModel: GameActivityViewModel by viewModels()
        private val appConfig by lazy { RevengerApplication.appConfig }

        // GamePad alignment manager for vertical offset
        private lateinit var alignmentManager: GamePadAlignmentManager

        // Performance monitoring
        private var frameStartTime = 0L
        private var pipDiagnosticSession = 0

        // GamePad container reference for orientation changes
        private lateinit var gamePadContainer: android.widget.LinearLayout

        // BroadcastReceiver to monitor auto-rotate changes
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
                val startTime = System.currentTimeMillis()
                android.util.Log.e(
                        "GAME_ACTIVITY",
                        "🚨🚨🚨🚨🚨 GAME_ACTIVITY ONCREATE CALLED - NEW APK VERSION 🚨🚨🚨🚨🚨"
                )
                android.util.Log.e("GAME_ACTIVITY", "📅 TIMESTAMP: ${java.util.Date()}")
                android.util.Log.e(
                        "GAME_ACTIVITY",
                        "🔧 APK VERSION: DEBUG WITH EXTENSIVE LOGGING - REV ${System.currentTimeMillis()}"
                )
                android.util.Log.e("STARTUP_TIMING", "⏱️ [T+0ms] GameActivity.onCreate() START")

                // CRITICAL: Apply orientation in TWO steps to eliminate flash:
                // 1. Force Configuration BEFORE super.onCreate() (chooses correct layout)
                // 2. Apply requestedOrientation for persistence
                val configOrientation = appConfig.getOrientation()
                com.vinaooo.revenger.utils.OrientationManager.forceConfigurationBeforeSetContent(this, configOrientation)

                super.onCreate(savedInstanceState)

                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioRoutingManager = AudioRoutingManager(audioManager)
                audioRoutingManager.requestFocus()
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] forceConfiguration() completed"
                )

                viewModel.setConfigOrientation(this)
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] setConfigOrientation() completed"
                )

                // Initialize ScreenshotCaptureUtil with context for aspect ratio detection
                ScreenshotCaptureUtil.setContext(this)
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] ScreenshotCaptureUtil.setContext() completed"
                )

                // Apply conditional features based on Android version
                AndroidCompatibility.applyConditionalFeatures()

                // Phase 9.4: Initialize SDK 36 features
                initializeSdk36Features()

                setContentView(R.layout.activity_game)
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] setContentView() completed"
                )

                // Configure status/navigation bars based on current theme
                configureSystemBarsForTheme()

                // Initialize views
                leftContainer = findViewById(R.id.left_container)
                rightContainer = findViewById(R.id.right_container)
                retroviewContainer = findViewById(R.id.retroview_container)
                retroviewContainer.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                        updatePictureInPictureParams()
                }
                menuContainer = findViewById(R.id.menu_container)
                loadPreviewOverlay = findViewById(R.id.load_preview_overlay)

                // Setup load preview overlay callback
                viewModel.loadPreviewCallback = { bitmap ->
                        if (bitmap != null) {
                                loadPreviewOverlay.setImageBitmap(bitmap)
                                loadPreviewOverlay.visibility = android.view.View.VISIBLE
                        } else {
                                loadPreviewOverlay.visibility = android.view.View.GONE
                                loadPreviewOverlay.setImageDrawable(null)
                        }
                }

                // Get gamepad container reference
                val gamepadContainers = findViewById<android.widget.LinearLayout>(R.id.containers)
                gamePadContainer = gamepadContainers

                // Initialize GamePad alignment manager
                alignmentManager = GamePadAlignmentManager(appConfig)
                val (offsetsValid, errorMsg) = alignmentManager.validateOffsets()
                if (!offsetsValid) {
                        Log.w(TAG, "GamePad offset validation error: $errorMsg")
                } else {
                        Log.d(TAG, "GamePad offsets validated successfully")
                }

                // Pass gamepad container reference to ViewModel
                viewModel.setGamePadContainer(gamepadContainers)

                /* Use immersive mode when we change the window insets */
                window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
                        view.post { viewModel.immersive(window) }
                        return@setOnApplyWindowInsetsListener windowInsets
                }

                registerInputListener()
                registerRotationSettingsListener() // Add listener for auto-rotate changes
                viewModel.updateGamePadVisibility(
                        this,
                        leftContainer,
                        rightContainer,
                        findViewById(R.id.floating_menu_button)
                )
                viewModel.setupRetroView(this, retroviewContainer)
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] setupRetroView() completed"
                )
                attachPiPDiagnosticLayoutListener()
                viewModel.setupGamePads(this, leftContainer, rightContainer)
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] setupGamePads() completed"
                )

                // Force gamepad positioning based on orientation
                adjustGamePadPositionForOrientation(gamepadContainers)

                // Setup Floating Menu Button
                setupFloatingMenuButton()

                // Reveal gamepads after next frame (when orientation has settled)
                // This eliminates flash of gamepads in wrong orientation
                gamePadContainer.post {
                        gamePadContainer.visibility = android.view.View.VISIBLE
                        android.util.Log.d(TAG, "GamePads revealed after orientation settled")
                }

                viewModel.prepareRetroMenu3()
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] prepareRetroMenu3() completed"
                )
                viewModel.setupMenuCallback(this)
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] setupMenuCallback() completed"
                )
                viewModel.setMenuContainer(menuContainer)
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] onCreate() COMPLETE - Total: ${System.currentTimeMillis() - startTime}ms"
                )
        }

        /**
         * Registers a listener to monitor changes in the system auto-rotate setting. When the user
         * toggles auto-rotate in system settings, the app's orientation is automatically reapplied.
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
                                                // Configuration changed (could be auto-rotate)
                                                Log.d(
                                                        TAG,
                                                        "[ROTATION_LISTENER] System configuration changed - checking auto-rotate"
                                                )
                                                reapplyOrientation()
                                        }
                                }
                        }

                // Create IntentFilter to detect configuration changes
                val intentFilter = android.content.IntentFilter()
                intentFilter.addAction(android.content.Intent.ACTION_CONFIGURATION_CHANGED)

                // Register receiver with appropriate permission
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        registerReceiver(
                                rotationSettingsReceiver,
                                intentFilter,
                                android.content.Context.RECEIVER_EXPORTED
                        )
                } else {
                        registerReceiver(rotationSettingsReceiver, intentFilter)
                }

                Log.d(TAG, "[ROTATION_LISTENER] BroadcastReceiver registered to monitor changes")
        }

        /**
         * Reapplies orientation configuration when the auto-rotate preference changes. This allows
         * the app to dynamically respond to system changes.
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

                        Log.d(TAG, "[ROTATION_REAPPLY] System auto-rotate: $wasAutoRotate")

                        // Reapply orientation configuration based on new state
                        viewModel.setConfigOrientation(this)

                        Log.d(TAG, "[ROTATION_REAPPLY] Orientation successfully reapplied")
                } catch (e: Exception) {
                        Log.e(
                                TAG,
                                "[ROTATION_REAPPLY] Error reapplying orientation: ${e.message}",
                                e
                        )
                }
        }

        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
                super.onConfigurationChanged(newConfig)
                Log.d(TAG, "Configuration changed - orientation=${newConfig.orientation}")
                logPiPState("onConfigurationChanged")

                // Check if we should reprocess orientation
                // DO NOT reprocess when config=3 and auto-rotate=OFF (to allow manual button)
                val configOrientation = appConfig.getOrientation()
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

                // Only reapply orientation if config=1 or 2 (forced) or if config=3 with
                // auto-rotate
                // ON
                if (configOrientation != "auto" || autoRotateEnabled) {
                        reapplyOrientation()
                } else {
                        Log.d(
                                TAG,
                                "[ROTATION] config=3 + auto-rotate OFF - not reapplying (allows manual button)"
                        )
                }

                adjustGamePadPositionForOrientation(gamePadContainer)

                // CRITICAL FIX: Re-register menu callbacks after rotation to prevent back button
                // issues
                viewModel.setupMenuCallback(this)
                Log.d(TAG, "[ROTATION_FIX] Menu callbacks re-registered after rotation")

                // --- SOLUTION: Recreate fragments after orientation change ---
                Log.d(TAG, "[ORIENTATION] ====== CHECKING FOR MENU AFTER ROTATION ======")

                val menuManager = viewModel.getMenuManager()
                val currentState =
                        menuManager.getCurrentState() // CRITICAL: Check the TRUE backstack to
                // detect if we are in a submenu
                // currentState may be outdated after BACK operations
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

                // Wait for system to complete rotation
                android.os.Handler(android.os.Looper.getMainLooper())
                        .postDelayed(
                                {
                                        Log.d(
                                                TAG,
                                                "[ORIENTATION] 🔄 Inside postDelayed - starting fragment recreation"
                                        )

                                        // CRITICAL FIX: Re-check backstack INSIDE postDelayed
                                        // The backstack may have changed between the initial check
                                        // and
                                        // execution
                                        // do postDelayed
                                        val currentBackStackCount =
                                                fragmentManager.backStackEntryCount
                                        val hasBackStackNow = currentBackStackCount > 0

                                        Log.d(
                                                TAG,
                                                "[ORIENTATION] ⚠️ RE-CHECKING backstack: initial=$hasBackStack, now=$hasBackStackNow"
                                        )

                                        // CRITICAL FIX: Prioritize backstack over the visible
                                        // Fragment
                                        // If backstack is empty, ALWAYS use MAIN_MENU
                                        // The visible Fragment may be temporarily
                                        // outdated after BACK
                                        val effectiveState =
                                                if (hasBackStackNow) {
                                                        // There is backstack: check which submenu
                                                        // is
                                                        // ativo
                                                        // Detectar estado REAL baseado no Fragment
                                                        // visible
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

                                                        // Use state based on the visible Fragment
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
                                                                "[ORIENTATION] ⚠️ Backstack empty - forcing MAIN_MENU (currentState was: $currentState)"
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
                                                }

                                        // NOTE: NavigationController syncState will be
                                        // called AFTER all fragments
                                        // to be created and registered (in postDelayed after
                                        // registrar submenu).
                                        // Isso evita que registerFragment() sobrescreva o
                                        // estado.

                                        val isMainMenu =
                                                effectiveState ==
                                                        com.vinaooo.revenger.ui.retromenu3.MenuState
                                                                .MAIN_MENU

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
                                        fragmentManager.findFragmentById(R.id.menu_container)
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
                                        android.os.Handler(android.os.Looper.getMainLooper())
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
                                                                                com.vinaooo.revenger
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

                                                                        transaction.runOnCommit {
                                                                                Log.d(
                                                                                        TAG,
                                                                                        "[ORIENTATION] 🔄 Main menu committed, updating reference"
                                                                                )

                                                                                // Atualizar
                                                                                // referência do
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
                                                                                android.os.Handler(
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
                                                                        // RetroMenu3Fragment na
                                                                        // base
                                                                        // (sem backstack)
                                                                        val retroMenu3 =
                                                                                com.vinaooo.revenger
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
                                                                                "[ORIENTATION] 📋 Base RetroMenu3Fragment created and registered"
                                                                        )

                                                                        // CRITICAL: Update
                                                                        // MenuStateManager
                                                                        // to the
                                                                        // submenu state
                                                                        // This ensures
                                                                        // getCurrentFragment()
                                                                        // returns
                                                                        // the correct
                                                                        // Fragment
                                                                        viewModel
                                                                                .getMenuManager()
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
                                                                        when (effectiveState) {
                                                                                com.vinaooo.revenger
                                                                                        .ui
                                                                                        .retromenu3
                                                                                        .MenuState
                                                                                        .SETTINGS_MENU -> {
                                                                                        val settingsFragment =
                                                                                                newFragment as
                                                                                                        com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
                                                                                        // Configure
                                                                                        // listener
                                                                                        // BEFORE
                                                                                        // adding
                                                                                        // to
                                                                                        // FragmentManager
                                                                                        settingsFragment
                                                                                                .setSettingsListener(
                                                                                                        retroMenu3 as
                                                                                                                SettingsMenuListener
                                                                                                )
                                                                                        Log.d(
                                                                                                TAG,
                                                                                                "[ORIENTATION] 🔧 SettingsMenuFragment listener configured BEFORE adding"
                                                                                        )
                                                                                }
                                                                                else -> {
                                                                                        // Other
                                                                                        // fragments
                                                                                        // don't
                                                                                        // need
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
                                                                                                        "[ORIENTATION] ➕ Adding submenu on top: $submenuTag"
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

                                                                                                // CRITICAL: Register
                                                                                                // submenu in ViewModel
                                                                                                // (listener already
                                                                                                // configured)
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
                                                                                                                                                "[ORIENTATION] 📋 SettingsMenuFragment registered (rotation)"
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
                                                                                                                                                "[ORIENTATION] 📋 ProgressFragment registered (rotation)"
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
                                                                                                                                                "[ORIENTATION] 📋 AboutFragment registered (rotation)"
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
                                                                                                                                                "[ORIENTATION] 📋 ExitFragment registered (rotation)"
                                                                                                                                        )
                                                                                                                                }
                                                                                                                                else -> {
                                                                                                                                        Log.w(
                                                                                                                                                TAG,
                                                                                                                                                "[ORIENTATION] ⚠️ Unknown state, submenu not registered"
                                                                                                                                        )
                                                                                                                                }
                                                                                                                        }

                                                                                                                        // CRITICAL: Synchronize NavigationController state AFTER all fragments
                                                                                                                        // to be created and registered. This prevents registerFragment() from overwriting state.
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
                                                                                                                                }
                                                                                                                        viewModel
                                                                                                                                .navigationController
                                                                                                                                ?.syncState(
                                                                                                                                        menuType =
                                                                                                                                                navMenuTypeForSync,
                                                                                                                                        selectedIndex =
                                                                                                                                                0,
                                                                                                                                        clearStack =
                                                                                                                                                false // Do not clear stack because backstack has already been rebuilt
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

                                        Log.d(
                                                TAG,
                                                "[ORIENTATION] ====== ORIENTATION CHECK COMPLETED ======"
                                        )
                                },
                                250
                        ) // Delay to ensure the system finished processing rotation
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
                                                rightContainer,
                                                findViewById(R.id.floating_menu_button)
                                        )
                                }
                                override fun onInputDeviceRemoved(deviceId: Int) {
                                        viewModel.updateGamePadVisibility(
                                                this@GameActivity,
                                                leftContainer,
                                                rightContainer,
                                                findViewById(R.id.floating_menu_button)
                                        )
                                }
                                override fun onInputDeviceChanged(deviceId: Int) {
                                        viewModel.updateGamePadVisibility(
                                                this@GameActivity,
                                                leftContainer,
                                                rightContainer,
                                                findViewById(R.id.floating_menu_button)
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
                                                // Trigger PiP mode if supported
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                        try {
                                                                enterPiPMode()
                                                        } catch (e: Exception) {
                                                                Log.e(TAG, "Failed to enter PiP mode from back button", e)
                                                                isEnabled = false
                                                                onBackPressedDispatcher.onBackPressed()
                                                        }
                                                } else {
                                                        // Use default back button behavior
                                                        isEnabled = false
                                                        onBackPressedDispatcher.onBackPressed()
                                                }
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
                // Remove auto-rotate change listener
                rotationSettingsReceiver?.let {
                        try {
                                unregisterReceiver(it)
                                Log.d(TAG, "[ROTATION_LISTENER] BroadcastReceiver unregistered")
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
                if (::audioRoutingManager.isInitialized) audioRoutingManager.abandonFocus()
                super.onDestroy()
        }

        override fun onPause() {
                val frameSpeed = viewModel.retroView?.view?.frameSpeed
                Log.d(
                        TAG,
                        "[PIP_DIAG][lifecycle] onPause isInPiP=$isInPictureInPictureMode frameSpeed=$frameSpeed"
                )
                viewModel.preserveState()
                super.onPause()
        }

        override fun onStart() {
                super.onStart()
                val frameSpeed = viewModel.retroView?.view?.frameSpeed
                Log.d(
                        TAG,
                        "[PIP_DIAG][lifecycle] onStart isInPiP=$isInPictureInPictureMode frameSpeed=$frameSpeed"
                )
        }

        override fun onResume() {
                super.onResume()
                val frameSpeed = viewModel.retroView?.view?.frameSpeed
                Log.d(
                        TAG,
                        "[PIP_DIAG][lifecycle] onResume isInPiP=$isInPictureInPictureMode frameSpeed=$frameSpeed"
                )
                frameStartTime = System.nanoTime()
        }

        override fun onStop() {
                val frameSpeed = viewModel.retroView?.view?.frameSpeed
                Log.d(
                        TAG,
                        "[PIP_DIAG][lifecycle] onStop isInPiP=$isInPictureInPictureMode frameSpeed=$frameSpeed"
                )
                super.onStop()
        }

        override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
                // Record frame time for performance monitoring
                recordFrameTime()
                triggerFloatingButtonFade()

                return viewModel.processKeyEvent(keyCode, event) ?: super.onKeyDown(keyCode, event)
        }

        /** Adjust gamepad position based on screen orientation */
        private fun adjustGamePadPositionForOrientation(
                gamepadContainer: android.widget.LinearLayout
        ) {
                val layoutParams = gamepadContainer.layoutParams as FrameLayout.LayoutParams

                // Reset margins and minimumHeight from previous orientation
                layoutParams.topMargin = 0
                layoutParams.bottomMargin = 0

                // Reset minimumHeight of child containers
                val leftContainer =
                        gamepadContainer.findViewById<android.widget.FrameLayout>(
                                R.id.left_container
                        )
                val rightContainer =
                        gamepadContainer.findViewById<android.widget.FrameLayout>(
                                R.id.right_container
                        )
                leftContainer?.minimumHeight = 0
                rightContainer?.minimumHeight = 0

                Log.d(TAG, "Reset margins and minimumHeight for orientation change")

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

                        // Equalize heights and apply portrait offset
                        gamepadContainer.post {
                                equalizeGamePadHeights(gamepadContainer)
                                applyPortraitOffset(gamepadContainer)
                        }
                } else {
                        // Keep top positioning in landscape
                        layoutParams.gravity = android.view.Gravity.TOP
                        Log.d(TAG, "GamePad positioned at TOP for landscape mode")

                        // Keep original sizes for landscape (25% each)
                        adjustGamePadSizes(gamepadContainer, 0.25f, 0.5f)

                        // Equalize heights in landscape to fix alignment issues
                        gamepadContainer.post {
                                equalizeGamePadHeights(gamepadContainer)
                                applyLandscapeOffset(gamepadContainer)
                        }
                }

                Log.d(TAG, "Final layout gravity: ${layoutParams.gravity}")
                gamepadContainer.layoutParams = layoutParams
                gamepadContainer.requestLayout()
        }

        /**
         * Apply portrait offset via bottomMargin based on XML configuration. 100% = base at bottom
         * edge, lower % = higher position
         */
        private fun applyPortraitOffset(container: android.widget.LinearLayout) {
                try {
                        val offsetPercent = appConfig.gamePadConfigModel.gp_offset_portrait

                        // Use parent height (FrameLayout) minus container height to
                        // calculate available space
                        val parent = container.parent as? android.view.View
                        val availableHeight = parent?.height ?: 0
                        val containerHeight = container.height

                        if (availableHeight <= 0 || containerHeight <= 0) {
                                Log.w(
                                        TAG,
                                        "Portrait: Heights not available yet (available=$availableHeight, container=$containerHeight)"
                                )
                                return
                        }

                        // Maximum space to move the gamepad (available height - container height)
                        val maxMovement = availableHeight - containerHeight

                        // Calculate margin: offset 100% = 0px (bottom edge), offset 0% =
                        // maxMovement (top)
                        val bottomMargin = (maxMovement * (100 - offsetPercent) / 100.0).toInt()

                        val layoutParams = container.layoutParams as FrameLayout.LayoutParams
                        layoutParams.bottomMargin = bottomMargin
                        container.layoutParams = layoutParams

                        Log.d(
                                TAG,
                                "Portrait offset: $offsetPercent% → availableHeight=$availableHeight, containerHeight=$containerHeight, maxMovement=$maxMovement, bottomMargin=$bottomMargin px"
                        )
                } catch (e: Exception) {
                        Log.e(TAG, "Error applying portrait offset", e)
                }
        }

        /**
         * Apply landscape offset via topMargin based on XML configuration. 100% = base at bottom,
         * 0% = base at top
         */
        private fun applyLandscapeOffset(container: android.widget.LinearLayout) {
                try {
                        val offsetPercent = appConfig.gamePadConfigModel.gp_offset_landscape

                        // Use parent height (FrameLayout) minus container height to
                        // calculate available space
                        val parent = container.parent as? android.view.View
                        val availableHeight = parent?.height ?: 0
                        val containerHeight = container.height

                        if (availableHeight <= 0 || containerHeight <= 0) {
                                Log.w(
                                        TAG,
                                        "Landscape: Heights not available yet (available=$availableHeight, container=$containerHeight)"
                                )
                                return
                        }

                        // Maximum space to move the gamepad
                        val maxMovement = availableHeight - containerHeight

                        // Calculate margin: offset 0% = 0px (top), offset 100% = maxMovement
                        // (bottom)
                        val topMargin = (maxMovement * offsetPercent / 100.0).toInt()

                        val layoutParams = container.layoutParams as FrameLayout.LayoutParams
                        layoutParams.topMargin = topMargin
                        container.layoutParams = layoutParams

                        Log.d(
                                TAG,
                                "Landscape offset: $offsetPercent% → availableHeight=$availableHeight, containerHeight=$containerHeight, maxMovement=$maxMovement, topMargin=$topMargin px"
                        )
                } catch (e: Exception) {
                        Log.e(TAG, "Error applying landscape offset", e)
                }
        }

        /**
         * Equalizes the height of left and right gamepad containers in landscape mode. This ensures
         * that centers remain aligned even when one side has different secondary dial
         * configurations (e.g., MENU button on right only).
         */
        private fun equalizeGamePadHeights(container: android.widget.LinearLayout) {
                val leftContainer =
                        container.findViewById<android.widget.FrameLayout>(R.id.left_container)
                val rightContainer =
                        container.findViewById<android.widget.FrameLayout>(R.id.right_container)

                if (leftContainer != null && rightContainer != null) {
                        val leftHeight = leftContainer.height
                        val rightHeight = rightContainer.height
                        val maxHeight = maxOf(leftHeight, rightHeight)

                        // Debug: measure actual positions and sizes
                        val leftPos = IntArray(2)
                        val rightPos = IntArray(2)
                        leftContainer.getLocationOnScreen(leftPos)
                        rightContainer.getLocationOnScreen(rightPos)
                        val screenWidth = resources.displayMetrics.widthPixels
                        Log.d(TAG, "=== GAMEPAD ALIGNMENT DEBUG ===")
                        Log.d(TAG, "Screen width: $screenWidth")
                        Log.d(
                                TAG,
                                "LEFT container: x=${leftPos[0]}, width=${leftContainer.width}, height=$leftHeight"
                        )
                        Log.d(
                                TAG,
                                "RIGHT container: x=${rightPos[0]}, width=${rightContainer.width}, height=$rightHeight"
                        )
                        Log.d(TAG, "LEFT margin from left edge: ${leftPos[0]}px")
                        Log.d(
                                TAG,
                                "RIGHT margin from right edge: ${screenWidth - rightPos[0] - rightContainer.width}px"
                        )
                        // Check RadialGamePad view sizes
                        if (leftContainer.childCount > 0) {
                                val leftPad = leftContainer.getChildAt(0)
                                Log.d(
                                        TAG,
                                        "LEFT pad: width=${leftPad.width}, height=${leftPad.height}, x=${leftPad.x}"
                                )
                        }
                        if (rightContainer.childCount > 0) {
                                val rightPad = rightContainer.getChildAt(0)
                                Log.d(
                                        TAG,
                                        "RIGHT pad: width=${rightPad.width}, height=${rightPad.height}, x=${rightPad.x}"
                                )
                        }
                        Log.d(TAG, "=== END ALIGNMENT DEBUG ===")

                        if (maxHeight > 0) {
                                Log.d(
                                        TAG,
                                        "GamePad heights - LEFT: $leftHeight, RIGHT: $rightHeight, MAX: $maxHeight"
                                )

                                // Set both to the same height
                                if (leftHeight != maxHeight) {
                                        leftContainer.minimumHeight = maxHeight
                                        Log.d(TAG, "LEFT container minHeight set to $maxHeight")
                                }
                                if (rightHeight != maxHeight) {
                                        rightContainer.minimumHeight = maxHeight
                                        Log.d(TAG, "RIGHT container minHeight set to $maxHeight")
                                }
                        }
                }
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

        private var floatingButtonFadeHandler: android.os.Handler? = null
        private var floatingButtonFadeRunnable: Runnable? = null

        /** Set up the floating menu button config and listener */
        private fun setupFloatingMenuButton() {
                val floatingButton = findViewById<android.widget.Button>(R.id.floating_menu_button)
                val configValue = appConfig.getMenuModeFab().lowercase()

                if (configValue == "disabled") {
                        floatingButton.visibility = android.view.View.GONE
                        return
                }

                val layoutParams = floatingButton.layoutParams as FrameLayout.LayoutParams

                when (configValue) {
                        "top-left" ->
                                layoutParams.gravity =
                                        android.view.Gravity.TOP or android.view.Gravity.START
                        "top-right" ->
                                layoutParams.gravity =
                                        android.view.Gravity.TOP or android.view.Gravity.END
                        "bottom-left" ->
                                layoutParams.gravity =
                                        android.view.Gravity.BOTTOM or android.view.Gravity.START
                        "bottom-right" ->
                                layoutParams.gravity =
                                        android.view.Gravity.BOTTOM or android.view.Gravity.END
                        else -> {
                                Log.w(
                                        TAG,
                                        "Unknown floating menu button config: $configValue. Disabling floating button."
                                )
                                floatingButton.visibility = android.view.View.GONE
                                return
                        }
                }

                floatingButton.layoutParams = layoutParams
                val shouldShowGamePads =
                        com.vinaooo.revenger.gamepad.GamePad.shouldShowGamePads(this, appConfig)
                floatingButton.visibility =
                        if (!shouldShowGamePads) android.view.View.VISIBLE
                        else android.view.View.GONE

                floatingButton.setOnClickListener {
                        Log.d(TAG, "Floating menu button clicked.")
                        viewModel.toggleMainMenu()
                }

                // Setup fade handler
                floatingButtonFadeHandler = android.os.Handler(android.os.Looper.getMainLooper())
                floatingButtonFadeRunnable = Runnable {
                        floatingButton.animate().alpha(1.0f).setDuration(500).start()
                }
        }

        private fun triggerFloatingButtonFade() {
                if (viewModel.isAnyMenuActive()) return

                val floatingButton =
                        findViewById<android.widget.Button>(R.id.floating_menu_button) ?: return
                if (floatingButton.visibility != android.view.View.VISIBLE) return

                // Fade button to 30% alpha
                floatingButton.animate().alpha(0.3f).setDuration(200).start()

                // Cancel any pending restorative fades, and schedule a new one in 10s
                floatingButtonFadeRunnable?.let { runnable ->
                        floatingButtonFadeHandler?.removeCallbacks(runnable)
                        floatingButtonFadeHandler?.postDelayed(runnable, 10000)
                }
        }

        fun restoreFloatingButtonVisibility() {
                val floatingButton =
                        findViewById<android.widget.Button>(R.id.floating_menu_button) ?: return
                if (floatingButton.visibility != android.view.View.VISIBLE) return

                floatingButtonFadeRunnable?.let { runnable ->
                        floatingButtonFadeHandler?.removeCallbacks(runnable)
                }
                floatingButton.animate().alpha(1.0f).setDuration(200).start()
        }

        fun fadeFloatingButtonImmediately() {
                val floatingButton =
                        findViewById<android.widget.Button>(R.id.floating_menu_button) ?: return
                if (floatingButton.visibility != android.view.View.VISIBLE) return

                floatingButton.animate().alpha(0.3f).setDuration(200).start()
                floatingButtonFadeRunnable?.let { runnable ->
                        floatingButtonFadeHandler?.removeCallbacks(runnable)
                        floatingButtonFadeHandler?.postDelayed(runnable, 10000)
                }
        }

        override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
                return viewModel.processKeyEvent(keyCode, event) ?: super.onKeyUp(keyCode, event)
        }

        override fun onGenericMotionEvent(event: MotionEvent): Boolean {
                // Record frame time for performance monitoring
                recordFrameTime()
                triggerFloatingButtonFade()

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

        /** Starts reverse CRT animation (shutdown) and invokes a callback when finished */
        // Picture-in-Picture Mode
        private fun getExpectedGameAspectRatio(): Float {
                return when (appConfig.getCore().lowercase()) {
                        "gambatte",
                        "sameboy",
                        "gearboy" -> 10f / 9f
                        "gpsp", "vba-m", "meteor" -> 3f / 2f
                        else -> 4f / 3f
                }
        }

        private fun getLayoutSizeString(view: android.view.View?): String {
                if (view == null) return "null"
                return "${view.width}x${view.height}"
        }

        private fun getLocationString(view: android.view.View?): String {
                if (view == null) return "null"
                val location = IntArray(2)
                view.getLocationInWindow(location)
                return "(${location[0]},${location[1]})"
        }

        private fun logPiPState(stage: String) {
                val retroView = viewModel.retroView?.view
                val retroParams = retroView?.layoutParams as? FrameLayout.LayoutParams
                val bounds = getGameBounds()
                val ratio = getGameAspectRatio()
                val frameSpeed = retroView?.frameSpeed

                Log.d(
                        TAG,
                        "[PIP_DIAG][$pipDiagnosticSession][$stage] isInPiP=$isInPictureInPictureMode " +
                                "orientation=${resources.configuration.orientation} " +
                                "containerSize=${getLayoutSizeString(retroviewContainer)} containerLoc=${getLocationString(retroviewContainer)} " +
                                "retroSize=${getLayoutSizeString(retroView)} retroLoc=${getLocationString(retroView)} " +
                                "retroLp=${retroParams?.width}x${retroParams?.height} gravity=${retroParams?.gravity} " +
                                "aspect=${ratio.numerator}:${ratio.denominator}(${ratio.toFloat()}) expected=${getExpectedGameAspectRatio()} " +
                                "bounds=$bounds frameSpeed=$frameSpeed"
                )
        }

        private fun schedulePiPPostEntryProbes() {
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                val delays = listOf(60L, 200L, 500L, 1000L)
                for (delay in delays) {
                        handler.postDelayed({ logPiPState("post-enter-${delay}ms") }, delay)
                }
        }

        private fun attachPiPDiagnosticLayoutListener() {
                val retroView = viewModel.retroView?.view ?: return
                retroView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                        if (!isInPictureInPictureMode) return@addOnLayoutChangeListener
                        Log.d(
                                TAG,
                                "[PIP_DIAG][$pipDiagnosticSession][retro-layout] new=${right - left}x${bottom - top} old=${oldRight - oldLeft}x${oldBottom - oldTop}"
                        )
                        logPiPState("retro-layout-change")
                }
        }

        private fun getGameAspectRatio(): android.util.Rational {
                return try {
                        val expectedAspectRatio = getExpectedGameAspectRatio()
                        val retroView = viewModel.retroView?.view
                        val width = retroView?.width?.takeIf { it > 0 } ?: retroviewContainer.width
                        val height = retroView?.height?.takeIf { it > 0 } ?: retroviewContainer.height
                        
                        if (width > 0 && height > 0) {
                                val measuredAspectRatio = width.toFloat() / height.toFloat()
                                val targetAspectRatio =
                                        if (kotlin.math.abs(measuredAspectRatio - expectedAspectRatio) <= 0.25f) {
                                                measuredAspectRatio
                                        } else {
                                                expectedAspectRatio
                                        }
                                val scaledNumerator = (targetAspectRatio * 10_000).toInt().coerceAtLeast(1)
                                val rational = android.util.Rational(scaledNumerator, 10_000)
                                val floatValue = rational.toFloat()
                                when {
                                        floatValue > 2.39f -> android.util.Rational(239, 100)
                                        floatValue < 1 / 2.39f -> android.util.Rational(100, 239)
                                        else -> rational
                                }
                        } else {
                                val scaledNumerator = (expectedAspectRatio * 10_000).toInt().coerceAtLeast(1)
                                android.util.Rational(scaledNumerator, 10_000)
                        }
                } catch (e: Exception) {
                        android.util.Rational(4, 3)
                }
        }

        private fun getGameBounds(): android.graphics.Rect? {
                val rect = android.graphics.Rect()
                if (!retroviewContainer.getGlobalVisibleRect(rect) || rect.isEmpty) return null

                val containerWidth = rect.width()
                val containerHeight = rect.height()
                if (containerWidth <= 0 || containerHeight <= 0) return null

                val targetAspect = getGameAspectRatio().toFloat()
                val containerAspect = containerWidth.toFloat() / containerHeight.toFloat()

                val gameWidth: Int
                val gameHeight: Int
                if (targetAspect > containerAspect) {
                        gameWidth = containerWidth
                        gameHeight = (containerWidth / targetAspect).toInt().coerceAtLeast(1)
                } else {
                        gameHeight = containerHeight
                        gameWidth = (containerHeight * targetAspect).toInt().coerceAtLeast(1)
                }

                val offsetX = (containerWidth - gameWidth) / 2
                val offsetY = (containerHeight - gameHeight) / 2

                rect.set(
                        rect.left + offsetX,
                        rect.top + offsetY,
                        rect.left + offsetX + gameWidth,
                        rect.top + offsetY + gameHeight,
                )

                return if (rect.isEmpty) null else rect
        }

        private fun updateRetroViewLayoutForPiP(isInPiP: Boolean) {
                val retroView = viewModel.retroView?.view ?: return
                logPiPState("layout-before-$isInPiP")
                val currentParams = retroView.layoutParams as? FrameLayout.LayoutParams
                val params = currentParams ?: FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                )

                if (isInPiP) {
                        params.width = ViewGroup.LayoutParams.MATCH_PARENT
                        params.height = ViewGroup.LayoutParams.MATCH_PARENT
                } else {
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                params.gravity = android.view.Gravity.CENTER

                retroView.layoutParams = params
                retroView.requestLayout()
                logPiPState("layout-after-$isInPiP")
        }

        private fun enterPiPMode() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        logPiPState("enter-request")
                        val builder = android.app.PictureInPictureParams.Builder()
                                .setAspectRatio(getGameAspectRatio())
                        
                        getGameBounds()?.let { builder.setSourceRectHint(it) }

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                builder.setAutoEnterEnabled(false)
                        }
                        val entered = enterPictureInPictureMode(builder.build())
                        Log.d(TAG, "[PIP_DIAG][$pipDiagnosticSession][enter-result] success=$entered")
                }
        }

        private fun updatePictureInPictureParams() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        if (isInPictureInPictureMode) return
                        logPiPState("params-update-request")
                        
                        val builder = android.app.PictureInPictureParams.Builder()
                                .setAspectRatio(getGameAspectRatio())
                                .setAutoEnterEnabled(true)
                                
                        getGameBounds()?.let { builder.setSourceRectHint(it) }
                        
                        setPictureInPictureParams(builder.build())
                }
        }

        override fun onUserLeaveHint() {
                super.onUserLeaveHint()
                logPiPState("onUserLeaveHint")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                        try {
                                enterPiPMode()
                        } catch (e: Exception) {
                                Log.e(TAG, "Failed to enter PiP mode", e)
                        }
                }
        }

        override fun onPictureInPictureModeChanged(
                isInPictureInPictureMode: Boolean,
                newConfig: android.content.res.Configuration
        ) {
                super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
                pipDiagnosticSession += 1
                logPiPState("mode-changed-start=$isInPictureInPictureMode")
                
                if (isInPictureInPictureMode) {
                        updateRetroViewLayoutForPiP(true)

                        // Hide UI
                        leftContainer.visibility = android.view.View.GONE
                        rightContainer.visibility = android.view.View.GONE
                        menuContainer.visibility = android.view.View.GONE

                        viewModel.retroView?.view?.frameSpeed = 1
                        Log.d(TAG, "[PIP_DIAG] PiP diagnostic mode: pause disabled, forcing frameSpeed=1")
                        logPiPState("mode-changed-after-pause")
                        schedulePiPPostEntryProbes()
                } else {
                        updateRetroViewLayoutForPiP(false)

                        // Show UI
                        leftContainer.visibility = android.view.View.VISIBLE
                        rightContainer.visibility = android.view.View.VISIBLE
                        menuContainer.visibility = android.view.View.VISIBLE

                        viewModel.retroView?.view?.frameSpeed = 1
                        Log.d(TAG, "[PIP_DIAG] PiP diagnostic mode: resume bypassed, frameSpeed kept at 1")
                        logPiPState("mode-changed-after-resume")
                }
        }

        fun startShutdownAnimation(onComplete: () -> Unit) {
                Log.d(TAG, "Starting shutdown animation")

                val crtShutdownView =
                        findViewById<com.vinaooo.revenger.ui.splash.CRTBootView>(
                                R.id.crt_shutdown_view
                        )

                // Make overlay visible
                crtShutdownView.visibility = android.view.View.VISIBLE

                // Set callback for when animation finishes
                crtShutdownView.onAnimationEndListener = {
                        Log.d(TAG, "Shutdown animation completed")
                        onComplete()
                }

                // Start reverse animation
                crtShutdownView.startReverseAnimation()
        }
}
