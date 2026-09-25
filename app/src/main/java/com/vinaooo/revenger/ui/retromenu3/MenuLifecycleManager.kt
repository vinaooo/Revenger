package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.MenuLogger
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Interface for managing the lifecycle of the RetroMenu3Fragment. Responsible for
 * initialization, setup, and cleanup of the fragment.
 */
interface MenuLifecycleManager {
    fun onCreateView(inflater: LayoutInflater, container: ViewGroup?): View
    fun onViewCreated(view: View, savedInstanceState: Bundle?)
    fun onResume()
    fun onDestroy()
}

/**
 * The specialized managers [MenuLifecycleManagerImpl] delegates to, bundled into one parameter
 * object so the constructor stays under detekt's parameter-count threshold.
 */
data class MenuLifecycleCollaborators(
        val viewInitializer: MenuViewInitializer,
        val animationController: MenuAnimationController,
        val inputHandler: MenuInputHandler,
        val stateController: MenuStateController,
        val menuViewManager: MenuViewManager,
        val actionHandler: MenuActionHandler
)

/**
 * Implementation of MenuLifecycleManager. Coordinates menu initialization and delegates to
 * other specialized managers.
 */
class MenuLifecycleManagerImpl(
        private val fragment: RetroMenu3Fragment,
        private val viewModel: GameActivityViewModel,
        private val collaborators: MenuLifecycleCollaborators
) : MenuLifecycleManager {
    private val viewInitializer get() = collaborators.viewInitializer
    private val animationController get() = collaborators.animationController
    private val inputHandler get() = collaborators.inputHandler
    private val stateController get() = collaborators.stateController
    private val menuViewManager get() = collaborators.menuViewManager
    private val actionHandler get() = collaborators.actionHandler

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?): View {
        MenuLogger.lifecycle("MenuLifecycleManager: onCreateView START")
        return inflater.inflate(R.layout.retro_menu3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        MenuLogger.lifecycle("MenuLifecycleManager.onViewCreated - Starting setup")

        try {
            // NEW: Apply configurable layout proportions (10-80-10, 10-70-20, etc)
            applyConfigurableLayoutProportions(view)
            MenuLogger.lifecycle("MenuLifecycleManager: Layout proportions applied")

            // Use the MenuViewManager instance passed from fragment
            MenuLogger.lifecycle("MenuLifecycleManager: MenuViewManager instance received")

            // Force all views to z=0 to stay below gamepad
            com.vinaooo.revenger.utils.ViewUtils.forceZeroElevationRecursively(view)
            MenuLogger.lifecycle("MenuLifecycleManager: Elevation forced to zero")

            // Initialize views via viewInitializer
            val menuViews = viewInitializer.initializeViews(view)

            // ARMAZENAR menuViews NO FRAGMENT
            (fragment as? RetroMenu3Fragment)?.menuViews = menuViews
            MenuLogger.lifecycle("MenuLifecycleManager: menuViews stored in fragment")

            // Configurar estados iniciais das views
            viewInitializer.configureInitialViewStates(menuViews)

            // Configure dynamic title
            viewInitializer.setupDynamicTitle(menuViews)

            // Setup MenuViewManager views
            menuViewManager.setupViews(view)
            MenuLogger.lifecycle("MenuLifecycleManager: MenuViewManager views setup completed")

            // Configurar animationController com as views
            animationController.setMenuViews(menuViews)

            // Inicializar state controller
            stateController.initializeState(menuViews)

            // Configurar input handler
            inputHandler.setupInputHandling(menuViews)

            // Configurar click listeners
            // PHASE 3.3a: Pass navigationController for touch event routing
            val navigationController = viewModel.navigationController
            viewInitializer.setupClickListeners(menuViews, actionHandler, navigationController)

            // Start menu animation
            animationController.animateMenuIn()
            MenuLogger.lifecycle("MenuLifecycleManager: Menu animation started")

            // Ensure first item is selected after animation
            fragment.setSelectedIndex(0)
            stateController.updateSelectionVisuals()
            MenuLogger.lifecycle("MenuLifecycleManager: Selection visual updated")

            MenuLogger.lifecycle("MenuLifecycleManager.onViewCreated - Setup completed")
            // This is a broad top-level setup boundary spanning ~8 collaborator classes
            // (viewInitializer, menuViewManager, animationController, stateController,
            // inputHandler); it deliberately fails loudly rather than swallowing, logging the
            // failure before rethrowing unchanged, so narrowing to one type would require
            // enumerating every exception each collaborator can throw. Kept via detekt's
            // documented escape hatch instead of @Suppress.
        } catch (expectedSetupFailure: Exception) {
            MenuLogger.lifecycle(
                    "MenuLifecycleManager.onViewCreated - ERROR: ${expectedSetupFailure.message}"
            )
            throw expectedSetupFailure
        }
    }

    override fun onResume() {
        MenuLogger.lifecycle("MenuLifecycleManager: onResume START")

        MenuLogger.lifecycle("MenuLifecycleManager: onResume COMPLETED")
    }

    override fun onDestroy() {
        MenuLogger.lifecycle("MenuLifecycleManager: onDestroy START")

        // Notify ViewModel that fragment is being destroyed
        try {
            val activity = fragment.requireActivity()
            val viewModel =
                    androidx.lifecycle.ViewModelProvider(activity)[
                            com.vinaooo.revenger.viewmodels.GameActivityViewModel::class.java]
            viewModel.onRetroMenu3FragmentDestroyed()
        } catch (e: IllegalStateException) {
            // requireActivity() throws IllegalStateException if the fragment is no longer
            // attached, which is routinely the case by the time onDestroy() runs.
            MenuLogger.e("Error notifying ViewModel of fragment destruction")
            MenuLogger.e("MenuLifecycleManager", e)
        }

        // Clean up back stack change listener to prevent memory leaks
        // Note: This is handled by SubmenuCoordinator

        // Ensure that comboAlreadyTriggered is reset when the fragment is destroyed
        try {
            val activity = fragment.requireActivity()
            val viewModel =
                    androidx.lifecycle.ViewModelProvider(activity)[
                            com.vinaooo.revenger.viewmodels.GameActivityViewModel::class.java]
            // Call clearKeyLog through ViewModel to reset combo state
            viewModel.clearControllerKeyLog()
        } catch (e: IllegalStateException) {
            // requireActivity() throws IllegalStateException if the fragment is no longer
            // attached, which is routinely the case by the time onDestroy() runs.
            Log.w(
                    "MenuLifecycleManager",
                    "Error resetting combo state in onDestroy",
                    e
            )
        }
    }

    /**
     * Applies configurable layout proportions to the menu. Automatically detects if the
     * orientation is portrait or landscape and applies the correct configuration.
     */
    private fun applyConfigurableLayoutProportions(view: View) {
        try {
            // Apply all proportions (horizontal and vertical)
            com.vinaooo.revenger.ui.retromenu3.config.MenuLayoutConfig
                    .applyAllProportionsToMenuLayout(view)
            // applyAllProportionsToMenuLayout already catches its own failures instead of
            // propagating, so nothing reaches this catch in practice; kept as a safety net
            // against a future change to that callee.
        } catch (expectedUnreachable: Exception) {
            Log.e("MenuLifecycleManager", "Error applying layout proportions", expectedUnreachable)
        }
    }
}
