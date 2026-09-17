package com.vinaooo.revenger.ui.retromenu3


import com.vinaooo.revenger.ui.retromenu3.callbacks.RetroMenu3Listener
import com.vinaooo.revenger.utils.MenuLogger

/**
 * Imperative menu commands (game actions triggered from the menu). Split out of
 * [MenuCallbackManager] so each half stays under the project's function-count threshold; see
 * [MenuCallbackQueries] for the read-only counterpart.
 */
interface MenuCallbackActions {
    fun onContinueGame()
    fun onResetGame()
    fun onSaveState()
    fun onLoadState()
    fun onToggleAudio()
    fun onFastForward()
    fun onToggleShader()
}

/**
 * Read-only state queries backing the menu's toggle/indicator display. Split out of
 * [MenuCallbackManager]; see [MenuCallbackActions] for the imperative counterpart.
 */
interface MenuCallbackQueries {
    fun getAudioState(): Boolean
    fun getFastForwardState(): Boolean
    fun getShaderState(): String
    fun hasSaveState(): Boolean
}

/**
 * Interface for managing menu callbacks. Centralizes all calls to the menu listener.
 *
 * Composed of [MenuCallbackActions] and [MenuCallbackQueries] (declares no members of its own)
 * so implementers and callers keep using a single unified type.
 */
interface MenuCallbackManager : MenuCallbackActions, MenuCallbackQueries

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
