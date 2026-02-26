package com.vinaooo.revenger.ui.retromenu3


import com.vinaooo.revenger.ui.retromenu3.callbacks.RetroMenu3Listener
import com.vinaooo.revenger.utils.MenuLogger

/**
 * Interface for managing menu callbacks. Centralizes all calls to the menu listener.
 */
interface MenuCallbackManager {
    fun onContinueGame()
    fun onResetGame()
    fun onSaveState()
    fun onLoadState()
    fun onToggleAudio()
    fun onFastForward()
    fun onToggleShader()
    fun getAudioState(): Boolean
    fun getFastForwardState(): Boolean
    fun getShaderState(): String
    fun hasSaveState(): Boolean
}

/** Implementation of MenuCallbackManager. Delegates calls to the RetroMenu3Listener. */
class MenuCallbackManagerImpl(private val listener: RetroMenu3Listener?) :
        MenuCallbackManager {

    override fun onContinueGame() {
        MenuLogger.action(
                "MenuCallbackManager: onContinueGame - No callback needed, handled by fragment"
        )
        // Continue game is handled directly by the fragment, no callback needed
    }

    override fun onResetGame() {
        MenuLogger.action("MenuCallbackManager: onResetGame called")
        listener?.onResetGame()
    }

    override fun onSaveState() {
        MenuLogger.action("MenuCallbackManager: onSaveState called")
        listener?.onSaveState()
    }

    override fun onLoadState() {
        MenuLogger.action("MenuCallbackManager: onLoadState called")
        listener?.onLoadState()
    }

    override fun onToggleAudio() {
        MenuLogger.action("MenuCallbackManager: onToggleAudio called")
        listener?.onToggleAudio()
    }

    override fun onFastForward() {
        MenuLogger.action("MenuCallbackManager: onFastForward called")
        listener?.onFastForward()
    }

    override fun onToggleShader() {
        MenuLogger.action("MenuCallbackManager: onToggleShader called")
        listener?.onToggleShader()
    }

    override fun getAudioState(): Boolean {
        val state = listener?.getAudioState() ?: true
        MenuLogger.state("MenuCallbackManager: getAudioState = $state")
        return state
    }

    override fun getFastForwardState(): Boolean {
        val state = listener?.getFastForwardState() ?: false
        MenuLogger.state("MenuCallbackManager: getFastForwardState = $state")
        return state
    }

    override fun getShaderState(): String {
        val state = listener?.getShaderState() ?: "default"
        MenuLogger.state("MenuCallbackManager: getShaderState = $state")
        return state
    }

    override fun hasSaveState(): Boolean {
        val hasState = listener?.hasSaveState() ?: false
        MenuLogger.state("MenuCallbackManager: hasSaveState = $hasState")
        return hasState
    }
}

/** Version of MenuCallbackManager without logging for tests */
class MenuCallbackManagerTestImpl(private val listener: RetroMenu3Listener?) :
        MenuCallbackManager {

    override fun onContinueGame() {
        // Continue game is handled directly by fragment, no callback needed
    }

    override fun onResetGame() {
        listener?.onResetGame()
    }

    override fun onSaveState() {
        listener?.onSaveState()
    }

    override fun onLoadState() {
        listener?.onLoadState()
    }

    override fun onToggleAudio() {
        listener?.onToggleAudio()
    }

    override fun onFastForward() {
        listener?.onFastForward()
    }

    override fun onToggleShader() {
        listener?.onToggleShader()
    }

    override fun getAudioState(): Boolean {
        return listener?.getAudioState() ?: true
    }

    override fun getFastForwardState(): Boolean {
        return listener?.getFastForwardState() ?: false
    }

    override fun getShaderState(): String {
        return listener?.getShaderState() ?: "default"
    }

    override fun hasSaveState(): Boolean {
        return listener?.hasSaveState() ?: false
    }
}
