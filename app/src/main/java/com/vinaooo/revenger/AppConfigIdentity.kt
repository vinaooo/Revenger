package com.vinaooo.revenger

/**
 * Identity settings, always sourced from `config.json` ([ConfigSources.baseConfig]) regardless of
 * `default_settings`. Split out of [AppConfig] purely to keep that facade under the project's
 * function-count threshold and exposed back on it via Kotlin interface delegation (`by`).
 */
interface AppConfigIdentity {
    /** Configured display name, falling back to `"Revenger"` when empty. */
    fun getName(): String

    /** ROM filename as configured in `config.json`. */
    fun getRomName(): String

    /** Target ABI(s) as configured in `config.json`. */
    fun getTargetAbi(): String

    /** Platform id as configured in `config.json`. */
    fun getPlatformId(): String
}

class AppConfigIdentityImpl(private val sources: ConfigSources) : AppConfigIdentity {
    override fun getName(): String = sources.baseConfig.name.takeIf { it.isNotEmpty() } ?: "Revenger"

    override fun getRomName(): String = sources.baseConfig.rom

    override fun getTargetAbi(): String = sources.baseConfig.target_abi

    override fun getPlatformId(): String = sources.baseConfig.platform
}
