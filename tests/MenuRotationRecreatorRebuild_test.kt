package com.vinaooo.revenger.controllers

import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.R
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.CoreVariablesFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.ExitSaveGridFragment
import com.vinaooo.revenger.ui.retromenu3.LoadSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.ManageSavesFragment
import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SaveSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.ScreenshotHostActivity
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import com.vinaooo.revenger.ui.retromenu3.navigation.MenuType
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import com.vinaooo.revenger.views.menu.RotationMenuStateResolver
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * The rebuild half of [MenuRotationRecreator]'s chain (steps 2-4, after the teardown that
 * `MenuRotationRecreator_test` covers), with the real menu fragments. The host is the screenshot
 * tests' [ScreenshotHostActivity], so those fragments get the mocked [GameActivityViewModel] from
 * `ViewModelProvider`.
 *
 * Each test runs the whole chain (its delays add up to 1.1s) and checks the end state plus the
 * order of the ViewModel calls. Intermediate times aren't asserted: committing the real menu
 * fragments advances Robolectric's clock by itself, so "at +100ms" isn't observable.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MenuRotationRecreatorRebuild_test {

    private val viewModel: GameActivityViewModel = mockk(relaxed = true)
    private val navigationController: NavigationController = mockk(relaxed = true)
    private val menuManager: MenuManager = mockk(relaxed = true)
    private lateinit var controller: ActivityController<ScreenshotHostActivity>
    private lateinit var activity: ScreenshotHostActivity
    private lateinit var recreator: MenuRotationRecreator

    @Before
    fun setUp() {
        SaveStateManager.clearInstance()
        SessionSlotTracker.clearInstance()
        every { viewModel.isAnyMenuActive() } returns true
        every { viewModel.navigationController } returns navigationController
        every { viewModel.getMenuManager() } returns menuManager
        every { menuManager.getCurrentState() } returns MenuState.MAIN_MENU
        every { viewModel.retroView } returns null
        every { viewModel.getCachedScreenshot() } returns null

        controller = Robolectric.buildActivity(ScreenshotHostActivity::class.java)
        activity = controller.get()
        activity.gameViewModel = viewModel
        controller.create()
        activity.setContentView(FrameLayout(activity).apply { id = R.id.menu_container })
        controller.start().resume().visible()

        recreator = MenuRotationRecreator(activity, viewModel)
    }

    @After
    fun tearDown() {
        controller.pause().stop().destroy()
        SaveStateManager.clearInstance()
        SessionSlotTracker.clearInstance()
    }

    private val fragmentManager get() = activity.supportFragmentManager

    private fun advance(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    /** Puts a stand-in for the pre-rotation menu in the container, as a submenu when [asSubmenu]. */
    private fun showBeforeRotation(asSubmenu: Boolean): Fragment {
        val before = DummyMenuFragment()
        val transaction = fragmentManager.beginTransaction().add(R.id.menu_container, before, "before")
        if (asSubmenu) transaction.addToBackStack("before")
        transaction.commit()
        fragmentManager.executePendingTransactions()
        return before
    }

    private fun containerFragment(): Fragment? = fragmentManager.findFragmentById(R.id.menu_container)

    @Test
    fun `sem backstack reconstroi so o menu principal e devolve o foco ao primeiro item`() {
        val before = showBeforeRotation(asSubmenu = false)

        recreator.scheduleMenuRecreationAfterRotation(before, hasBackStack = false, currentState = MenuState.MAIN_MENU)
        advance(SETTLE_MS)

        val main = containerFragment()
        assertTrue(main is RetroMenu3Fragment)
        assertEquals("RetroMenu3Fragment", main?.tag)
        assertEquals(0, fragmentManager.backStackEntryCount)
        verify { viewModel.updateRetroMenu3FragmentReference(main as RetroMenu3Fragment) }
        verify(exactly = 0) { menuManager.navigateToState(any()) }

        assertFocused(R.id.menu_continue)
    }

    @Test
    fun `sem backstack mas com estado de submenu desatualizado o menu principal ainda e registrado`() {
        // After a BACK the menu manager can still report the submenu. The rebuilt main menu then
        // doesn't register itself on resume, so the recreator's own registration is the only one.
        every { menuManager.getCurrentState() } returns MenuState.SETTINGS_MENU
        val before = showBeforeRotation(asSubmenu = false)

        recreator.scheduleMenuRecreationAfterRotation(before, hasBackStack = false, currentState = MenuState.SETTINGS_MENU)
        advance(SETTLE_MS)

        val main = containerFragment()
        assertTrue(main is RetroMenu3Fragment)
        verify(exactly = 1) { viewModel.updateRetroMenu3FragmentReference(main as RetroMenu3Fragment) }
    }

    /**
     * Runs the whole submenu branch and checks what every submenu shares: the base main menu was
     * committed and handed to the ViewModel, then the menu manager moved to [state], before the
     * submenu went on top with one backstack entry tagged with its class name.
     */
    private fun rebuildSubmenu(state: MenuState): Fragment? {
        // As on a device: the menu manager still reports the submenu when the base main menu
        // resumes, so that fragment doesn't register itself and the recreator has to.
        every { menuManager.getCurrentState() } returns state
        val before = showBeforeRotation(asSubmenu = true)

        recreator.scheduleMenuRecreationAfterRotation(before, hasBackStack = true, currentState = state)
        advance(SETTLE_MS)

        val submenu = containerFragment()
        assertEquals(1, fragmentManager.backStackEntryCount)
        assertEquals(submenu?.javaClass?.simpleName, submenu?.tag)
        verify(exactly = 1) { viewModel.updateRetroMenu3FragmentReference(any()) }
        verifyOrder {
            viewModel.updateRetroMenu3FragmentReference(any())
            menuManager.navigateToState(state)
            navigationController.syncState(any(), 0, false)
        }
        return submenu
    }

    private fun assertFocused(id: Int) = assertTrue(activity.findViewById<View>(id).isFocused)

    @Test
    fun `Settings e reconstruido sobre o menu principal, registrado e sincronizado`() {
        val submenu = rebuildSubmenu(MenuState.SETTINGS_MENU)

        assertTrue(submenu is SettingsMenuFragment)
        verify { viewModel.registerSettingsMenuFragmentForRotation(submenu as SettingsMenuFragment) }
        verify { navigationController.syncState(MenuType.SETTINGS, 0, false) }

        assertFocused(R.id.settings_sound)
    }

    @Test
    fun `Progress e reconstruido, registrado e sincronizado`() {
        val submenu = rebuildSubmenu(MenuState.PROGRESS_MENU)

        assertTrue(submenu is ProgressFragment)
        verify { viewModel.registerProgressFragmentForRotation(submenu as ProgressFragment) }
        verify { navigationController.syncState(MenuType.PROGRESS, 0, false) }

        assertFocused(R.id.progress_load_state)
    }

    @Test
    fun `About e reconstruido, registrado e sincronizado`() {
        val submenu = rebuildSubmenu(MenuState.ABOUT_MENU)

        assertTrue(submenu is AboutFragment)
        verify { viewModel.registerAboutFragmentForRotation(submenu as AboutFragment) }
        verify { navigationController.syncState(MenuType.ABOUT, 0, false) }

        assertFocused(R.id.about_back)
    }

    @Test
    fun `Exit e reconstruido, registrado e sincronizado`() {
        val submenu = rebuildSubmenu(MenuState.EXIT_MENU)

        assertTrue(submenu is ExitFragment)
        verify { viewModel.registerExitFragmentForRotation(submenu as ExitFragment) }
        verify { navigationController.syncState(MenuType.EXIT, 0, false) }

        assertFocused(R.id.exit_menu_option_a)
    }

    /**
     * Rotates with [grid] open on top of [parentState]'s submenu, as a device has it: the grid
     * opened through the navigation controller, so the menu manager still reports the parent and
     * two submenus are on the backstack. The grid must come back on top of a new parent, with the
     * menu manager left on the parent, navigation synced to the grid and the grid focused.
     */
    private fun rebuildNestedGrid(grid: Fragment, gridState: MenuState, parentState: MenuState): Fragment? {
        every { menuManager.getCurrentState() } returns parentState
        showBeforeRotation(asSubmenu = true)
        showBeforeRotation(asSubmenu = true)

        recreator.scheduleMenuRecreationAfterRotation(grid, hasBackStack = true, currentState = parentState)
        advance(SETTLE_MS)

        val rebuilt = containerFragment()
        assertEquals(grid.javaClass, rebuilt?.javaClass)
        assertEquals(2, fragmentManager.backStackEntryCount)
        verifyOrder {
            viewModel.updateRetroMenu3FragmentReference(any())
            menuManager.navigateToState(parentState)
            navigationController.syncState(RotationMenuStateResolver.resolveNavigationMenuType(gridState), 0, false)
        }
        verify(exactly = 0) { menuManager.navigateToState(gridState) }
        assertTrue(rebuilt?.requireView()?.isFocused == true)

        // Back pops to the parent the rebuild put underneath, the one registered with the ViewModel.
        fragmentManager.popBackStackImmediate()
        return containerFragment()
    }

    @Test
    fun `grade de salvar e reconstruida sobre Progress, e voltar leva a Progress`() {
        val progress = rebuildNestedGrid(SaveSlotsFragment(), MenuState.SAVE_SLOTS_MENU, MenuState.PROGRESS_MENU)

        assertTrue(progress is ProgressFragment)
        verify { viewModel.registerProgressFragmentForRotation(progress as ProgressFragment) }
    }

    @Test
    fun `grade de carregar e reconstruida sobre Progress, e voltar leva a Progress`() {
        val progress = rebuildNestedGrid(LoadSlotsFragment(), MenuState.LOAD_SLOTS_MENU, MenuState.PROGRESS_MENU)

        assertTrue(progress is ProgressFragment)
        verify { viewModel.registerProgressFragmentForRotation(progress as ProgressFragment) }
    }

    @Test
    fun `grade de gerenciar e reconstruida sobre Progress, e voltar leva a Progress`() {
        val progress = rebuildNestedGrid(ManageSavesFragment(), MenuState.MANAGE_SAVES_MENU, MenuState.PROGRESS_MENU)

        assertTrue(progress is ProgressFragment)
        verify { viewModel.registerProgressFragmentForRotation(progress as ProgressFragment) }
    }

    @Test
    fun `grade de salvar ao sair e reconstruida sobre Exit, e voltar leva a Exit`() {
        val exit = rebuildNestedGrid(ExitSaveGridFragment(), MenuState.EXIT_SAVE_SLOTS_MENU, MenuState.EXIT_MENU)

        assertTrue(exit is ExitFragment)
        verify { viewModel.registerExitFragmentForRotation(exit as ExitFragment) }
    }

    @Test
    fun `grade de salvar ao sair aberta sozinha e reconstruida sem Exit por baixo`() {
        // The PiP "Save and Exit" path opens the grid on its own: one backstack entry, and Back
        // from it closes the menu. The rebuild must not put an Exit menu underneath.
        every { menuManager.getCurrentState() } returns MenuState.MAIN_MENU
        showBeforeRotation(asSubmenu = true)

        recreator.scheduleMenuRecreationAfterRotation(
                ExitSaveGridFragment(),
                hasBackStack = true,
                currentState = MenuState.MAIN_MENU
        )
        advance(SETTLE_MS)

        val grid = containerFragment()
        assertTrue(grid is ExitSaveGridFragment)
        assertEquals(1, fragmentManager.backStackEntryCount)
        verify { menuManager.navigateToState(MenuState.EXIT_SAVE_SLOTS_MENU) }
        verify { navigationController.syncState(MenuType.EXIT_SAVE_SLOTS, 0, false) }
        verify(exactly = 0) { viewModel.registerExitFragmentForRotation(any()) }
        verify(exactly = 0) { viewModel.registerProgressFragmentForRotation(any()) }
        assertTrue(grid?.requireView()?.isFocused == true)
    }

    @Test
    fun `Core Variables e reconstruido sobre About, e voltar leva a About`() {
        // As on a device: Core Variables opens from About through the navigation controller, so
        // the menu manager still reports About and two submenus are on the backstack.
        val appConfigField =
                RevengerApplication::class.java.getDeclaredField("appConfig").apply { isAccessible = true }
        val originalAppConfig = appConfigField.get(null)
        appConfigField.set(null, mockk<AppConfig>(relaxed = true) { every { getVariables() } returns "opt_a=1" })
        try {
            every { menuManager.getCurrentState() } returns MenuState.ABOUT_MENU
            showBeforeRotation(asSubmenu = true)
            showBeforeRotation(asSubmenu = true)

            recreator.scheduleMenuRecreationAfterRotation(
                    CoreVariablesFragment(),
                    hasBackStack = true,
                    currentState = MenuState.ABOUT_MENU
            )
            advance(SETTLE_MS)

            assertTrue(containerFragment() is CoreVariablesFragment)
            assertEquals(2, fragmentManager.backStackEntryCount)
            verifyOrder {
                viewModel.updateRetroMenu3FragmentReference(any())
                menuManager.navigateToState(MenuState.ABOUT_MENU)
                navigationController.syncState(MenuType.CORE_VARIABLES, 0, false)
            }
            verify(exactly = 0) { menuManager.navigateToState(MenuState.CORE_VARIABLES_MENU) }
            assertFocused(R.id.variable_back)

            // Back pops to the About that was registered, the one the rebuild put underneath.
            fragmentManager.popBackStackImmediate()
            val about = containerFragment()
            assertTrue(about is AboutFragment)
            verify { viewModel.registerAboutFragmentForRotation(about as AboutFragment) }
        } finally {
            appConfigField.set(null, originalAppConfig)
        }
    }

    private companion object {
        /** Longer than the whole chain (250 + 100 + 150 + 600ms on the submenu branch). */
        const val SETTLE_MS = 3_000L
    }
}
