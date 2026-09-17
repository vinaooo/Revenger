package com.vinaooo.revenger

/**
 * Display/rendering and performance-related settings (default profile overrides
 * [ConfigSources.manualConfig] when `default_settings=true`), split out of [AppConfig] purely to
 * keep that facade under the project's function-count threshold and exposed back on it via
 * Kotlin interface delegation (`by`).
 */
interface AppConfigDisplay {
    /** Whether the emulator screen should run fullscreen. */
    fun getFullscreen(): Boolean

    /** Whether Picture-in-Picture is enabled for this configuration. */
    fun isPipEnabled(): Boolean

    /** Requested screen orientation (`"landscape"` / `"portrait"`). */
    fun getOrientation(): String

    /** Configured shader (`disabled` / `sharp` / `crt` / `lcd` / `upscale1`). */
    fun getShader(): String

    /** Whether the on-screen performance overlay is enabled. */
    fun getPerformanceOverlay(): Boolean

    /** Fast-forward speed multiplier. */
    fun getFastForwardMultiplier(): Int
}

class AppConfigDisplayImpl(private val sources: ConfigSources) : AppConfigDisplay {
    override fun getFullscreen(): Boolean =
            sources.profile?.confFullscreen ?: sources.manualConfig.fullscreen

    override fun isPipEnabled(): Boolean =
            sources.profile?.confEnablePip ?: sources.manualConfig.enable_pip

    override fun getOrientation(): String =
            sources.profile?.confOrientation ?: sources.manualConfig.orientation

    override fun getShader(): String = sources.profile?.confShader ?: sources.manualConfig.shader

    override fun getPerformanceOverlay(): Boolean =
            sources.profile?.confPerformanceOverlay ?: sources.manualConfig.performance_overlay

    override fun getFastForwardMultiplier(): Int =
            sources.profile?.confFastForwardMultiplier ?: sources.manualConfig.fast_forward_multiplier
}
