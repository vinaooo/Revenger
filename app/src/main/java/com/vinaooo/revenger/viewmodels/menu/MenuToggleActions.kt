package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.controllers.SpeedController
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.viewmodels.AudioViewModel
import com.vinaooo.revenger.viewmodels.ShaderViewModel

/**
 * The TOGGLE_AUDIO/TOGGLE_SPEED/TOGGLE_SHADER menu-action handlers. [retroView] and
 * [speedController] are read via provider lambdas rather than captured, since both are mutated on
 * the owning ViewModel after construction.
 */
class MenuToggleActions(
        private val retroView: () -> RetroView?,
        private val audioViewModel: AudioViewModel,
        private val speedController: () -> SpeedController?,
        private val shaderViewModel: ShaderViewModel
) {

    fun toggleAudio() {
        retroView()?.let { audioViewModel.toggleAudio(it.view) }
    }

    fun toggleSpeed() {
        retroView()?.let { speedController()?.toggleFastForward(it.view) }
    }

    fun toggleShader() {
        shaderViewModel.toggleShader()
    }
}
