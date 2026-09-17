package com.vinaooo.revenger

/**
 * General gamepad behavior settings (default profile overrides [ConfigSources.manualConfig] when
 * `default_settings=true`) -- everything about *how* input behaves, as opposed to which physical
 * buttons are enabled (see [AppConfigFaceButtons] / [AppConfigShoulderButtons]). Split out of
 * [AppConfig] purely to keep that facade under the project's function-count threshold and exposed
 * back on it via Kotlin interface delegation (`by`).
 */
interface AppConfigInput {
    /** Whether the virtual touchscreen gamepad is shown. */
    fun getGamepad(): Boolean

    /** Whether gamepad button presses trigger haptic feedback. */
    fun getGpHaptic(): Boolean

    /** Whether a button press action can be triggered by multiple simultaneous presses. */
    fun getButtonAllowMultiplePressesAction(): Boolean

    /** Whether the left analog stick is enabled. */
    fun getLeftAnalog(): Boolean
}

class AppConfigInputImpl(private val sources: ConfigSources) : AppConfigInput {
    override fun getGamepad(): Boolean = sources.profile?.confGamepad ?: sources.manualConfig.gamepad

    override fun getGpHaptic(): Boolean =
            sources.profile?.confGpHaptic ?: sources.manualConfig.gp_haptic

    override fun getButtonAllowMultiplePressesAction(): Boolean =
            sources.profile?.confButtonAllowMultiplePressesAction
                    ?: sources.manualConfig.button_allow_multiple_presses_action

    override fun getLeftAnalog(): Boolean =
            sources.profile?.confLeftAnalog ?: sources.manualConfig.left_analog
}
