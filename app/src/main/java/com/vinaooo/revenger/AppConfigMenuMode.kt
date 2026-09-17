package com.vinaooo.revenger

/**
 * Parses the `menu_mode` comma-string (e.g. `combo,gamepad,back,fab=bottom-right`) into the
 * individual flags/values consumers care about. Split out of [AppConfig] purely to keep that
 * facade under the project's function-count threshold and exposed back on it via Kotlin interface
 * delegation (`by`).
 */
interface AppConfigMenuMode {
    /** The `fab=<value>` token's value, or `""` when absent. */
    fun getMenuModeFab(): String

    /** Whether `menu_mode` includes the `gamepad` token. */
    fun getMenuModeGamepad(): Boolean

    /** Whether `menu_mode` includes the `back` token. */
    fun getMenuModeBack(): Boolean

    /** Whether `menu_mode` includes the `combo` token. */
    fun getMenuModeCombo(): Boolean
}

class AppConfigMenuModeImpl(private val sources: ConfigSources) : AppConfigMenuMode {
    private fun resolvedMenuMode(): String =
            sources.profile?.confMenuMode ?: sources.manualConfig.menu_mode

    private fun tokens(): List<String> = resolvedMenuMode().split(",").map { it.trim() }

    override fun getMenuModeFab(): String {
        val match = Regex("fab=([\\w-]+)").find(resolvedMenuMode())
        return match?.groups?.get(1)?.value ?: ""
    }

    override fun getMenuModeGamepad(): Boolean = tokens().contains("gamepad")

    override fun getMenuModeBack(): Boolean = tokens().contains("back")

    override fun getMenuModeCombo(): Boolean = tokens().contains("combo")
}
