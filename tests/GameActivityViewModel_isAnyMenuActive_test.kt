package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Characterization tests for [GameActivityViewModel.isAnyMenuActive]:
 *
 * - When `navigationController` is set, its `isMenuActive()` result is authoritative, even when
 *   fragment state disagrees.
 * - When `navigationController` is `null` (a fresh `GameActivityViewModel`'s natural state before
 *   `setupMenuCallback()`/`initializeNavigationControllerIfNeeded` first runs, and the state every
 *   existing unit test constructs), the result is always `false`, regardless of fragment state.
 *   This used to be a fragment-tracking fallback computing activity from `retroMenu3Fragment` and
 *   five submenu fragments; on-device logging (menu open/navigate/close/background/foreground)
 *   confirmed that fallback was never reached in practice -- every real caller of
 *   `isAnyMenuActive()` runs after `setupMenuCallback()` has already set `navigationController` --
 *   so it was replaced with a flat `false`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameActivityViewModel_isAnyMenuActive_test {

    private lateinit var viewModel: GameActivityViewModel

    private fun setRevengerAppConfig(appConfig: AppConfig?) {
        val field = RevengerApplication::class.java.getDeclaredField("appConfig")
        field.isAccessible = true
        field.set(null, appConfig)
    }

    private fun <T> setPrivateField(target: Any, fieldName: String, value: T) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    @Before
    fun setUp() {
        setRevengerAppConfig(mockk(relaxed = true))
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GameActivityViewModel(app)
    }

    @After
    fun tearDown() {
        setRevengerAppConfig(null)
    }

    // ---------------------------------------------------------------------------------------
    // Fast path: navigationController set -> authoritative
    // ---------------------------------------------------------------------------------------

    @Test
    fun `navigationController presente e isMenuActive true retorna true`() {
        val navController = mockk<NavigationController>(relaxed = true)
        every { navController.isMenuActive() } returns true
        viewModel.navigationController = navController

        assertEquals(true, viewModel.isAnyMenuActive())
    }

    @Test
    fun `navigationController presente e isMenuActive false retorna false mesmo com fragmento de submenu ativo`() {
        val navController = mockk<NavigationController>(relaxed = true)
        every { navController.isMenuActive() } returns false
        viewModel.navigationController = navController

        val settingsFragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { settingsFragment.isAdded } returns true
        setPrivateField(viewModel, "settingsMenuFragment", settingsFragment)

        assertEquals(false, viewModel.isAnyMenuActive())
    }

    // ---------------------------------------------------------------------------------------
    // navigationController == null: always false, regardless of fragment state
    // ---------------------------------------------------------------------------------------

    @Test
    fun `nada configurado retorna false`() {
        assertEquals(false, viewModel.isAnyMenuActive())
    }

    @Test
    fun `retroMenu3Fragment adicionado ainda retorna false sem navigationController`() {
        val fragment = mockk<RetroMenu3Fragment>(relaxed = true)
        every { fragment.isAdded } returns true
        setPrivateField(viewModel, "retroMenu3Fragment", fragment)

        assertEquals(false, viewModel.isAnyMenuActive())
    }

    @Test
    fun `fragmento de submenu ativo ainda retorna false sem navigationController`() {
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { fragment.isAdded } returns true
        setPrivateField(viewModel, "settingsMenuFragment", fragment)

        assertEquals(false, viewModel.isAnyMenuActive())
    }
}
