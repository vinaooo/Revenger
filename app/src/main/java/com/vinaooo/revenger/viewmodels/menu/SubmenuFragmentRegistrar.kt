package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.MenuStateManager
import com.vinaooo.revenger.ui.retromenu3.MenuSystemState
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import com.vinaooo.revenger.viewmodels.MenuViewModel

/**
 * The register/registerForRotation/unregister surface for the four submenu fragments
 * (Settings/Progress/Exit/About).
 */
interface SubmenuFragmentRegistration {
    fun registerSettingsMenuFragment(fragment: SettingsMenuFragment)
    fun registerSettingsMenuFragmentForRotation(fragment: SettingsMenuFragment)
    fun unregisterSettingsMenuFragment()
    fun registerProgressFragment(fragment: ProgressFragment)
    fun registerProgressFragmentForRotation(fragment: ProgressFragment)
    fun registerExitFragment(fragment: ExitFragment)
    fun registerExitFragmentForRotation(fragment: ExitFragment)
    fun registerAboutFragment(fragment: AboutFragment)
    fun registerAboutFragmentForRotation(fragment: AboutFragment)
}

/**
 * Implementation of [SubmenuFragmentRegistration]. [menuManager] and [menuViewModel] are read
 * lazily via providers, not captured at construction time, because callers may replace those
 * dependencies by reflection (in tests) after this registrar is already built.
 */
class SubmenuFragmentRegistrar(
        private val state: SubmenuFragmentState,
        private val menuManager: () -> MenuManager,
        private val menuStateManager: MenuStateManager,
        private val menuViewModel: () -> MenuViewModel,
        private val isAnyMenuActive: () -> Boolean
) : SubmenuFragmentRegistration {

    /**
     * Shared metadata for the log lines in [registerSubmenuFragment]. The exact combination of
     * [SubmenuRegistrationMeta.methodLabel]/emoji/class name below reproduces each original
     * per-method log text; [registerSubmenuFragment]'s wording additionally depends on whether
     * `activate` is null (see its own doc).
     */
    private data class SubmenuRegistrationMeta(
            val menuState: MenuState,
            val methodLabel: String,
            val emoji: String,
            val fragmentClassName: String
    )

    /**
     * Shared implementation for all eight register/registerForRotation methods below. Each one
     * does some subset of: set the fragment field, notify `menuViewModel`, activate the
     * corresponding menu state, and register with `menuManager` -- always in that order. The
     * exact combination of [notifyMenuViewModel] and [activate] is NOT uniform across fragment
     * types (e.g. Settings' rotation variant passes both as null, while Progress/Exit's still
     * pass a non-null [notifyMenuViewModel]); callers must reproduce their original per-method
     * combination exactly, not "clean it up".
     *
     * The two log lines' wording is driven by whether [activate] is null, matching the original
     * per-method log text: methods that activate a state log "Registering X - isAdded=...,
     * isResumed=..." / "Registration completed - isAnyMenuActive=...", while the `ForRotation`
     * methods (which never activate) log "Registering without state activation" / "Completed
     * (state NOT changed)".
     */
    private fun <F> registerSubmenuFragment(
            fragment: F,
            meta: SubmenuRegistrationMeta,
            setFragmentRef: (F) -> Unit,
            notifyMenuViewModel: (() -> Unit)?,
            activate: (() -> Unit)?
    ) where F : androidx.fragment.app.Fragment, F : com.vinaooo.revenger.ui.retromenu3.MenuFragment {
        if (activate != null) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[REGISTER] ${meta.emoji} ${meta.methodLabel}: Registering ${meta.fragmentClassName} - " +
                            "isAdded=${fragment.isAdded}, isResumed=${fragment.isResumed}"
            )
        } else {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[REGISTER] ${meta.emoji} ${meta.methodLabel}: Registering without state activation"
            )
        }
        setFragmentRef(fragment)
        notifyMenuViewModel?.invoke()
        activate?.invoke()
        menuManager().registerFragment(meta.menuState, fragment)
        if (activate != null) {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[REGISTER] ${meta.emoji} ${meta.methodLabel}: Registration completed - isAnyMenuActive=${isAnyMenuActive()}"
            )
        } else {
            android.util.Log.d(
                    "GameActivityViewModel",
                    "[REGISTER] ${meta.emoji} ${meta.methodLabel}: Completed (state NOT changed)"
            )
        }
    }

    override fun registerSettingsMenuFragment(fragment: SettingsMenuFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        MenuState.SETTINGS_MENU,
                        "registerSettingsMenuFragment",
                        "⚙️",
                        "SettingsMenuFragment"
                ),
                setFragmentRef = { state.settingsMenuFragment = it },
                notifyMenuViewModel = { menuViewModel().registerSettingsMenuFragment(fragment) },
                activate = {
                    menuStateManager.activateMenu(MenuSystemState.MenuType.SETTINGS_MENU)
                }
        )
    }

    override fun unregisterSettingsMenuFragment() {
        android.util.Log.d(
                "GameActivityViewModel",
                "[UNREGISTER] ⚙️ unregisterSettingsMenuFragment: Clearing SettingsMenuFragment reference"
        )
        state.settingsMenuFragment = null
        menuStateManager.deactivateMenu(MenuSystemState.MenuType.SETTINGS_MENU)
        menuManager().unregisterFragment(MenuState.SETTINGS_MENU)
        android.util.Log.d(
                "GameActivityViewModel",
                "[UNREGISTER] ⚙️ unregisterSettingsMenuFragment: Unregistration completed"
        )
    }

    override fun registerSettingsMenuFragmentForRotation(fragment: SettingsMenuFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        MenuState.SETTINGS_MENU,
                        "registerSettingsMenuFragmentForRotation",
                        "⚙️",
                        "SettingsMenuFragment"
                ),
                setFragmentRef = { state.settingsMenuFragment = it },
                notifyMenuViewModel = null,
                activate = null
        )
    }

    override fun registerProgressFragment(fragment: ProgressFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        MenuState.PROGRESS_MENU,
                        "registerProgressFragment",
                        "💾",
                        "ProgressFragment"
                ),
                setFragmentRef = { state.progressFragment = it },
                notifyMenuViewModel = { menuViewModel().registerProgressFragment(fragment) },
                activate = { menuStateManager.activateMenu(MenuSystemState.MenuType.PROGRESS_MENU) }
        )
    }

    override fun registerProgressFragmentForRotation(fragment: ProgressFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        MenuState.PROGRESS_MENU,
                        "registerProgressFragmentForRotation",
                        "💾",
                        "ProgressFragment"
                ),
                setFragmentRef = { state.progressFragment = it },
                notifyMenuViewModel = { menuViewModel().registerProgressFragment(fragment) },
                activate = null
        )
    }

    override fun registerExitFragment(fragment: ExitFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        MenuState.EXIT_MENU,
                        "registerExitFragment",
                        "🚪",
                        "ExitFragment"
                ),
                setFragmentRef = { state.exitFragment = it },
                notifyMenuViewModel = { menuViewModel().registerExitFragment(fragment) },
                activate = { menuStateManager.activateMenu(MenuSystemState.MenuType.EXIT_MENU) }
        )
    }

    override fun registerExitFragmentForRotation(fragment: ExitFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        MenuState.EXIT_MENU,
                        "registerExitFragmentForRotation",
                        "🚪",
                        "ExitFragment"
                ),
                setFragmentRef = { state.exitFragment = it },
                notifyMenuViewModel = { menuViewModel().registerExitFragment(fragment) },
                activate = null
        )
    }

    override fun registerAboutFragment(fragment: AboutFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        MenuState.ABOUT_MENU,
                        "registerAboutFragment",
                        "📋",
                        "AboutFragment"
                ),
                setFragmentRef = { state.aboutFragment = it },
                notifyMenuViewModel = null,
                activate = { menuStateManager.activateMenu(MenuSystemState.MenuType.ABOUT_MENU) }
        )
    }

    override fun registerAboutFragmentForRotation(fragment: AboutFragment) {
        registerSubmenuFragment(
                fragment,
                SubmenuRegistrationMeta(
                        MenuState.ABOUT_MENU,
                        "registerAboutFragmentForRotation",
                        "📋",
                        "AboutFragment"
                ),
                setFragmentRef = { state.aboutFragment = it },
                notifyMenuViewModel = null,
                activate = null
        )
    }
}
