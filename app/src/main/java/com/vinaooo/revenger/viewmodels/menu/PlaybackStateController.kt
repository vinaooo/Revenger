package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.viewmodels.AudioViewModel
import com.vinaooo.revenger.viewmodels.ShaderViewModel
import com.vinaooo.revenger.viewmodels.SpeedViewModel

/** Audio/fast-forward/shader state queries and the shader toggle. */
interface PlaybackStateFacade {
    fun getAudioState(): Boolean
    fun getFastForwardState(): Boolean
    fun onToggleShader(): String
    fun getShaderState(): String
    fun getShaderDisplayName(): String
}

/** Implementation of [PlaybackStateFacade]: thin delegation to the specialized ViewModels. */
class PlaybackStateController(
        private val audioViewModel: AudioViewModel,
        private val speedViewModel: SpeedViewModel,
        private val shaderViewModel: ShaderViewModel
) : PlaybackStateFacade {

    override fun getAudioState(): Boolean = audioViewModel.getAudioState()

    override fun getFastForwardState(): Boolean = speedViewModel.getFastForwardState()

    override fun onToggleShader(): String = shaderViewModel.toggleShader()

    override fun getShaderState(): String = shaderViewModel.getShaderState()

    override fun getShaderDisplayName(): String = shaderViewModel.getCurrentShaderDisplayName()
}
