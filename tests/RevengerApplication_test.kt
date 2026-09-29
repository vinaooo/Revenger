package com.vinaooo.revenger

import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.repositories.DefaultSettingsRepository
import com.vinaooo.revenger.repositories.PipConfigRepository
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [RevengerApplication.onCreate] loads both asset-backed repositories before anything reads them.
 * Both are process-wide singletons that load once, so the test empties them by reflection first;
 * `onCreate` then leaves them loaded again for the tests that follow.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RevengerApplication_test {

    private fun setField(owner: Any, name: String, value: Any?) {
        owner.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(owner, value)
    }

    private fun getField(owner: Any, name: String): Any? =
            owner.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(owner)

    @Test
    fun `onCreate carrega os perfis padrao e as proporcoes de PiP dos assets`() {
        val app = ApplicationProvider.getApplicationContext<RevengerApplication>()
        setField(DefaultSettingsRepository, "profiles", null)
        setField(PipConfigRepository, "platformsConfig", null)

        app.onCreate()

        assertTrue(DefaultSettingsRepository.getAvailablePlatforms().isNotEmpty())
        assertTrue((getField(PipConfigRepository, "platformsConfig") as Map<*, *>).isNotEmpty())
    }
}
