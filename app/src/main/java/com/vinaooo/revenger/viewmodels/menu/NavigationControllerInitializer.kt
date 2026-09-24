package com.vinaooo.revenger.viewmodels.menu

import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.ui.retromenu3.navigation.KeyboardInputAdapter
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController

/**
 * One-time creation and wiring of the [NavigationController] and [KeyboardInputAdapter].
 * [navigationController] is read via a provider and written via [setNavigationController] rather
 * than a captured mutable reference, since it's mutated on the owning ViewModel after
 * construction. [onMenuOpened]/[onMenuClosed] delegate to the classes that own that behavior.
 */
class NavigationControllerInitializer(
        private val navigationController: () -> NavigationController?,
        private val setNavigationController: (NavigationController?) -> Unit,
        private val setKeyboardInputAdapter: (KeyboardInputAdapter?) -> Unit,
        private val isAnyMenuActive: () -> Boolean,
        private val onMenuOpened: (FragmentActivity) -> Unit,
        private val onMenuClosed: (FragmentActivity, Int?) -> Unit
) {

    fun initializeNavigationControllerIfNeeded(activity: FragmentActivity) {
        // PHASE 3.1a: Initialize NavigationController (permanently enabled after Phase 4
        // validation)
        if (navigationController() != null) return

        val newNavigationController = NavigationController(activity)
        setNavigationController(newNavigationController)

        // PHASE 4.1c: Initialize KeyboardInputAdapter
        setKeyboardInputAdapter(
                KeyboardInputAdapter(newNavigationController) { isAnyMenuActive() }
        )

        // PHASE 3.2b: Configurar callbacks para pausar/resumir o jogo
        newNavigationController.onMenuOpenedCallback = { onMenuOpened(activity) }

        newNavigationController.onMenuClosedCallback = { closingButton: Int? ->
            onMenuClosed(activity, closingButton)
        }
    }
}
