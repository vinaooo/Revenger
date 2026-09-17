package com.vinaooo.revenger

/**
 * Shoulder/trigger button toggles (default profile overrides [ConfigSources.manualConfig] when
 * `default_settings=true`). Split out of [AppConfig] purely to keep that facade under the
 * project's function-count threshold and exposed back on it via Kotlin interface delegation
 * (`by`).
 */
interface AppConfigShoulderButtons {
    /** Whether the L1 button is enabled. */
    fun getButtonL1(): Boolean

    /** Whether the R1 button is enabled. */
    fun getButtonR1(): Boolean

    /** Whether the L2 trigger is enabled. */
    fun getButtonL2(): Boolean

    /** Whether the R2 trigger is enabled. */
    fun getButtonR2(): Boolean
}

class AppConfigShoulderButtonsImpl(private val sources: ConfigSources) : AppConfigShoulderButtons {
    override fun getButtonL1(): Boolean =
            sources.profile?.confButtonL1 ?: sources.manualConfig.button_l1

    override fun getButtonR1(): Boolean =
            sources.profile?.confButtonR1 ?: sources.manualConfig.button_r1

    override fun getButtonL2(): Boolean =
            sources.profile?.confButtonL2 ?: sources.manualConfig.button_l2

    override fun getButtonR2(): Boolean =
            sources.profile?.confButtonR2 ?: sources.manualConfig.button_r2
}
