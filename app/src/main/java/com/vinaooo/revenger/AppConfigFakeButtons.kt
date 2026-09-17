package com.vinaooo.revenger

/**
 * Fake-button toggles. Unlike the other button/gamepad settings, these always come from
 * [ConfigSources.manualConfig] -- they have no equivalent field in a default-settings profile.
 * Split out of [AppConfig] purely to keep that facade under the project's function-count
 * threshold and exposed back on it via Kotlin interface delegation (`by`).
 */
interface AppConfigFakeButtons {
    fun getFakeButton0(): Boolean

    fun getFakeButton1(): Boolean

    fun getFakeButton5(): Boolean

    fun getFakeButton6(): Boolean

    fun getFakeButton7(): Boolean

    fun getFakeButton9(): Boolean

    fun getFakeButton10(): Boolean

    fun getFakeButton11(): Boolean
}

class AppConfigFakeButtonsImpl(private val sources: ConfigSources) : AppConfigFakeButtons {
    override fun getFakeButton0(): Boolean = sources.manualConfig.fake_button_0

    override fun getFakeButton1(): Boolean = sources.manualConfig.fake_button_1

    override fun getFakeButton5(): Boolean = sources.manualConfig.fake_button_5

    override fun getFakeButton6(): Boolean = sources.manualConfig.fake_button_6

    override fun getFakeButton7(): Boolean = sources.manualConfig.fake_button_7

    override fun getFakeButton9(): Boolean = sources.manualConfig.fake_button_9

    override fun getFakeButton10(): Boolean = sources.manualConfig.fake_button_10

    override fun getFakeButton11(): Boolean = sources.manualConfig.fake_button_11
}
