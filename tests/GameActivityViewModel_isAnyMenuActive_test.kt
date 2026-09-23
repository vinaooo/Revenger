package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.CoreVariablesFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
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
 * Characterization tests for [GameActivityViewModel.isAnyMenuActive], written against the
 * unmodified implementation before it is restructured to resolve its `LongMethod`/
 * `CyclomaticComplexMethod` detekt findings. They pin the decision the current code makes so a
 * later extraction can be proven behavior-preserving:
 *
 * - When `navigationController` is set, its `isMenuActive()` result is authoritative and the
 *   fragment-tracking fallback below it is never consulted, even if fragment state disagrees.
 * - When `navigationController` is `null` (the fallback path -- a fresh `GameActivityViewModel`'s
 *   natural state before `setupMenuCallback()`/`initializeNavigationControllerIfNeeded` first
 *   runs, and the state every existing unit test constructs), each of the five submenu fragments
 *   (`aboutFragment`, `coreVariablesFragment`, `settingsMenuFragment`, `progressFragment`,
 *   `exitFragment`) independently makes the result `true` when added, with `retroMenu3Fragment`
 *   left `null` -- proving the seemingly-redundant individual terms in the final OR-chain are each
 *   load-bearing on their own, not merely implied by `menuSystemActive`.
 *
 * `forceMainMenuActive` is not exercised in isolation here: it requires `retroMenu3Fragment` to
 * exist and be added, which already makes `retroMenu3Open` (and therefore the result) `true` on
 * its own via the `retroMenu3Fragment` added/active test below -- isolating it would just
 * re-assert that same case under a different name.
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
    // Fast path: navigationController set -> authoritative, fragment fallback never consulted
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
    // Fallback path: navigationController == null (fresh ViewModel's default state)
    // ---------------------------------------------------------------------------------------

    @Test
    fun `nada configurado retorna false`() {
        assertEquals(false, viewModel.isAnyMenuActive())
    }

    @Test
    fun `retroMenu3Fragment adicionado retorna true`() {
        val fragment = mockk<RetroMenu3Fragment>(relaxed = true)
        every { fragment.isAdded } returns true
        setPrivateField(viewModel, "retroMenu3Fragment", fragment)

        assertEquals(true, viewModel.isAnyMenuActive())
    }

    @Test
    fun `aboutFragment adicionado sozinho retorna true`() {
        val fragment = mockk<AboutFragment>(relaxed = true)
        every { fragment.isAdded } returns true
        setPrivateField(viewModel, "aboutFragment", fragment)

        assertEquals(true, viewModel.isAnyMenuActive())
    }

    @Test
    fun `coreVariablesFragment adicionado sozinho retorna true`() {
        val fragment = mockk<CoreVariablesFragment>(relaxed = true)
        every { fragment.isAdded } returns true
        setPrivateField(viewModel, "coreVariablesFragment", fragment)

        assertEquals(true, viewModel.isAnyMenuActive())
    }

    @Test
    fun `settingsMenuFragment adicionado sozinho retorna true`() {
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { fragment.isAdded } returns true
        setPrivateField(viewModel, "settingsMenuFragment", fragment)

        assertEquals(true, viewModel.isAnyMenuActive())
    }

    @Test
    fun `progressFragment adicionado sozinho retorna true`() {
        val fragment = mockk<ProgressFragment>(relaxed = true)
        every { fragment.isAdded } returns true
        setPrivateField(viewModel, "progressFragment", fragment)

        assertEquals(true, viewModel.isAnyMenuActive())
    }

    @Test
    fun `exitFragment adicionado sozinho retorna true`() {
        val fragment = mockk<ExitFragment>(relaxed = true)
        every { fragment.isAdded } returns true
        setPrivateField(viewModel, "exitFragment", fragment)

        assertEquals(true, viewModel.isAnyMenuActive())
    }

    @Test
    fun `fragmento de submenu nao adicionado nao ativa o menu`() {
        val fragment = mockk<SettingsMenuFragment>(relaxed = true)
        every { fragment.isAdded } returns false
        setPrivateField(viewModel, "settingsMenuFragment", fragment)

        assertEquals(false, viewModel.isAnyMenuActive())
    }
}
