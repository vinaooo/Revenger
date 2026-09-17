package com.vinaooo.revenger

/**
 * Face-button and start/select toggles (default profile overrides [ConfigSources.manualConfig]
 * when `default_settings=true`). Split out of [AppConfig] purely to keep that facade under the
 * project's function-count threshold and exposed back on it via Kotlin interface delegation
 * (`by`).
 */
interface AppConfigFaceButtons {
    /** Whether the A button is enabled. */
    fun getButtonA(): Boolean

    /** Whether the B button is enabled. */
    fun getButtonB(): Boolean

    /** Whether the X button is enabled. */
    fun getButtonX(): Boolean

    /** Whether the Y button is enabled. */
    fun getButtonY(): Boolean

    /** Whether the Start button is enabled. */
    fun getButtonStart(): Boolean

    /** Whether the Select button is enabled. */
    fun getButtonSelect(): Boolean
}

class AppConfigFaceButtonsImpl(private val sources: ConfigSources) : AppConfigFaceButtons {
    override fun getButtonA(): Boolean = sources.profile?.confButtonA ?: sources.manualConfig.button_a

    override fun getButtonB(): Boolean = sources.profile?.confButtonB ?: sources.manualConfig.button_b

    override fun getButtonX(): Boolean = sources.profile?.confButtonX ?: sources.manualConfig.button_x

    override fun getButtonY(): Boolean = sources.profile?.confButtonY ?: sources.manualConfig.button_y

    override fun getButtonStart(): Boolean =
            sources.profile?.confButtonStart ?: sources.manualConfig.button_start

    override fun getButtonSelect(): Boolean =
            sources.profile?.confButtonSelect ?: sources.manualConfig.button_select
}
