package com.vinaooo.revenger.viewmodels.menu

import android.widget.FrameLayout
import android.widget.LinearLayout
import com.vinaooo.revenger.R
import com.vinaooo.revenger.viewmodels.MenuViewModel

/** Wires the menu/gamepad container views the owning Activity's layout provides. */
interface RetroMenu3ContainerConfigFacade {
    fun setMenuContainer(container: FrameLayout)
    fun getMenuContainerId(): Int
    fun setGamePadContainer(container: LinearLayout)
}

/**
 * Implementation of [RetroMenu3ContainerConfigFacade]. Container references are read/written on
 * the owning ViewModel via lambdas rather than captured, since they're mutated on the owning
 * ViewModel after construction.
 */
class RetroMenu3ContainerConfig(
        private val menuContainerView: () -> FrameLayout?,
        private val setMenuContainerView: (FrameLayout?) -> Unit,
        private val setGamePadContainerView: (LinearLayout?) -> Unit,
        private val menuViewModel: () -> MenuViewModel
) : RetroMenu3ContainerConfigFacade {

    override fun setMenuContainer(container: FrameLayout) {
        setMenuContainerView(container)
        menuViewModel().setMenuContainer(container)
    }

    override fun getMenuContainerId(): Int = menuContainerView()?.id ?: R.id.menu_container

    override fun setGamePadContainer(container: LinearLayout) {
        setGamePadContainerView(container)
    }
}
