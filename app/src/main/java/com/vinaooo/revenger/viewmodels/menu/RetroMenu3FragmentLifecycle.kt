package com.vinaooo.revenger.viewmodels.menu

import android.util.Log
import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment

/** Create/replace/query the single RetroMenu3 overlay fragment instance. */
interface RetroMenu3FragmentLifecycleFacade {
    fun prepareRetroMenu3()
    fun recreateRetroMenu3()
    fun updateRetroMenu3FragmentReference(fragment: RetroMenu3Fragment)
    fun onRetroMenu3FragmentDestroyed()
    fun isRetroMenu3Open(): Boolean
}

/**
 * Implementation of [RetroMenu3FragmentLifecycleFacade]. [retroMenu3Fragment]/[setRetroMenu3Fragment]
 * read and write the owning ViewModel's field via lambdas rather than a captured value, since
 * it's mutated on the owning ViewModel after construction.
 */
class RetroMenu3FragmentLifecycle(
        private val retroMenu3Fragment: () -> RetroMenu3Fragment?,
        private val setRetroMenu3Fragment: (RetroMenu3Fragment?) -> Unit,
        private val menuManager: () -> MenuManager
) : RetroMenu3FragmentLifecycleFacade {

    override fun prepareRetroMenu3() {
        if (retroMenu3Fragment() != null) return

        val fragment = RetroMenu3Fragment.newInstance()
        setRetroMenu3Fragment(fragment)
        menuManager().registerFragment(MenuState.MAIN_MENU, fragment)
    }

    override fun recreateRetroMenu3() {
        setRetroMenu3Fragment(null)
        prepareRetroMenu3()
    }

    override fun updateRetroMenu3FragmentReference(fragment: RetroMenu3Fragment) {
        setRetroMenu3Fragment(fragment)
        menuManager().registerFragment(MenuState.MAIN_MENU, fragment)
    }

    override fun onRetroMenu3FragmentDestroyed() {
        Log.d(
                "GameActivityViewModel",
                "[FRAGMENT_DESTROYED] onRetroMenu3FragmentDestroyed: Clearing retroMenu3Fragment reference"
        )
        setRetroMenu3Fragment(null)
    }

    override fun isRetroMenu3Open(): Boolean = retroMenu3Fragment()?.isAdded == true
}
