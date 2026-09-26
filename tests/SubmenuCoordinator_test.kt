package com.vinaooo.revenger.ui.retromenu3

import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.ui.retromenu3.callbacks.AboutListener
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * A stand-in for `RetroMenu3Fragment`: a real, attached `Fragment` with a real (attached) view --
 * `SubmenuCoordinator` needs `fragment.view?.post{}` to actually run its posted work -- that also
 * implements [AboutListener], since `showAboutSubmenu` casts its host fragment to it directly.
 */
class SubmenuCoordinatorHostFragment : Fragment(), AboutListener {
    var backToMainMenuCalls = 0
    override fun onAboutBackToMainMenu() {
        backToMainMenuCalls++
    }

    override fun onCreateView(
            inflater: android.view.LayoutInflater,
            container: android.view.ViewGroup?,
            savedInstanceState: android.os.Bundle?
    ): View = FrameLayout(requireContext())
}

/**
 * A host [Fragment] that deliberately does NOT implement [AboutListener], used to force the
 * `fragment as AboutListener` cast in `showAboutSubmenu` to fail with a real
 * `ClassCastException` (regression test below), instead of mocking the exception.
 */
class NonAboutListenerHostFragment : Fragment() {
    override fun onCreateView(
            inflater: android.view.LayoutInflater,
            container: android.view.ViewGroup?,
            savedInstanceState: android.os.Bundle?
    ): View = FrameLayout(requireContext())
}

/**
 * [SubmenuCoordinator] is the navigation coordinator between the RetroMenu3 main menu and its
 * submenus (Progress/Settings/About/Exit): it opens the right submenu fragment, hides the main
 * menu while a submenu is showing, and restores the main menu selection when the submenu closes
 * (detected via a `FragmentManager` back-stack listener). It had zero test coverage before this
 * file.
 *
 * `openSubmenu`'s `show*Submenu` methods add *real* production submenu fragments
 * (`SettingsMenuFragment`, `ProgressFragment`, etc.) via `commitAllowingStateLoss()`. Those
 * fragments read `GameActivityViewModel` via `ViewModelProvider` in `onCreateView`, which needs
 * `RevengerApplication.appConfig` seeded -- heavy setup unrelated to what this class itself does.
 * Rather than actually executing those pending transactions (which would exercise the submenu
 * fragments' own `onCreateView`, not `SubmenuCoordinator`'s logic), these tests deliberately never
 * call `executePendingTransactions()`/idle the looper while such a transaction is pending, and
 * instead assert on `SubmenuCoordinator`'s own synchronous side effects (state navigation,
 * `viewModel.registerXFragment` calls). The back-stack-restoration tests below drive the back
 * stack with a lightweight dummy fragment instead of a real submenu fragment, for the same reason.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SubmenuCoordinator_test {

    private lateinit var activity: FragmentActivity
    private lateinit var hostFragment: SubmenuCoordinatorHostFragment
    private lateinit var viewModel: GameActivityViewModel
    private lateinit var viewManager: MenuViewManager
    private lateinit var animationController: MenuAnimationController
    private lateinit var menuManager: MenuManager
    private lateinit var menuManagerListener: MenuManager.MenuManagerListener

    private var selectedIndexToReport = 0
    private var restoredIndex: Int? = null
    private val showMainMenuCalls = mutableListOf<Boolean>()

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val root = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(root)

        hostFragment = SubmenuCoordinatorHostFragment()
        activity.supportFragmentManager
                .beginTransaction()
                .add(root.id, hostFragment, "host")
                .commitNow()

        viewModel = mockk(relaxed = true)
        viewManager = mockk(relaxed = true)
        animationController = mockk(relaxed = true)
        menuManagerListener = mockk(relaxed = true)
        menuManager = MenuManager(menuManagerListener, MenuStateManager())
    }

    private fun newCoordinator(): SubmenuCoordinator {
        val coordinator =
                SubmenuCoordinator(hostFragment, viewModel, viewManager, menuManager, animationController)
        coordinator.setCallbacks(
                showMainMenuCallback = { showMainMenuCalls.add(it) },
                setSelectedIndexCallback = { restoredIndex = it },
                getCurrentSelectedIndexCallback = { selectedIndexToReport }
        )
        return coordinator
    }

    // restoreMainMenuSelection() chains two nested postDelayed(50ms) calls; advance virtual time
    // far enough for both to fire.
    private fun idle() =
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200))

    // --- openSubmenu: roteamento e efeitos sincronos ---

    @Test
    fun `openSubmenu com PROGRESS_MENU navega o menuManager e registra o ProgressFragment`() {
        val coordinator = newCoordinator()

        coordinator.openSubmenu(MenuState.PROGRESS_MENU)

        assertEquals(MenuState.PROGRESS_MENU, menuManager.getCurrentState())
        verify { viewModel.registerProgressFragment(any()) }
    }

    @Test
    fun `openSubmenu com SETTINGS_MENU navega o menuManager e registra o SettingsMenuFragment`() {
        val coordinator = newCoordinator()

        coordinator.openSubmenu(MenuState.SETTINGS_MENU)

        assertEquals(MenuState.SETTINGS_MENU, menuManager.getCurrentState())
        verify { viewModel.registerSettingsMenuFragment(any()) }
    }

    @Test
    fun `openSubmenu com ABOUT_MENU navega o menuManager e registra o AboutFragment`() {
        val coordinator = newCoordinator()

        coordinator.openSubmenu(MenuState.ABOUT_MENU)

        assertEquals(MenuState.ABOUT_MENU, menuManager.getCurrentState())
        verify { viewModel.registerAboutFragment(any()) }
    }

    @Test
    fun `openSubmenu com EXIT_MENU navega o menuManager e registra o ExitFragment`() {
        val coordinator = newCoordinator()

        coordinator.openSubmenu(MenuState.EXIT_MENU)

        assertEquals(MenuState.EXIT_MENU, menuManager.getCurrentState())
        verify { viewModel.registerExitFragment(any()) }
    }

    @Test
    fun `openSubmenu com MAIN_MENU nao navega nem registra nenhum fragment`() {
        val coordinator = newCoordinator()

        coordinator.openSubmenu(MenuState.MAIN_MENU)

        assertEquals(MenuState.MAIN_MENU, menuManager.getCurrentState())
        verify(inverse = true) { viewModel.registerProgressFragment(any()) }
        verify(inverse = true) { viewModel.registerSettingsMenuFragment(any()) }
        verify(inverse = true) { viewModel.registerAboutFragment(any()) }
        verify(inverse = true) { viewModel.registerExitFragment(any()) }
    }

    @Test
    fun `openSubmenu le o indice atual do menu principal exatamente uma vez, para salva-lo antes de trocar de submenu`() {
        var getIndexCalls = 0
        val coordinator =
                SubmenuCoordinator(hostFragment, viewModel, viewManager, menuManager, animationController)
                        .apply {
                            setCallbacks(
                                    showMainMenuCallback = {},
                                    setSelectedIndexCallback = {},
                                    getCurrentSelectedIndexCallback = {
                                        getIndexCalls++
                                        4
                                    }
                            )
                        }

        coordinator.openSubmenu(MenuState.PROGRESS_MENU)

        // O valor lido (mainMenuSelectedIndexBeforeSubmenu, privado) so e observavel
        // indiretamente, atraves da restauracao -- ver
        // `fechar o submenu via back stack restaura textos do menu principal e o estado MAIN_MENU`,
        // que usa o indice padrao (0), e a documentacao do proprio teste sobre por que nao
        // encadeamos abertura + fechamento real de submenu aqui.
        assertEquals(1, getIndexCalls)
    }

    // Regression test for the narrowed ClassCastException catch in showAboutSubmenu: forces a
    // real cast failure (host fragment doesn't implement AboutListener) instead of mocking the
    // exception. A mis-narrowed catch type would let it propagate and fail this test.
    @Test
    fun `openSubmenu com ABOUT_MENU e host que nao implementa AboutListener nao lanca excecao`() {
        val nonListenerHost = NonAboutListenerHostFragment()
        activity.supportFragmentManager
                .beginTransaction()
                .add(nonListenerHost, "non-listener-host")
                .commitNow()

        val coordinator =
                SubmenuCoordinator(nonListenerHost, viewModel, viewManager, menuManager, animationController)
        coordinator.setCallbacks(
                showMainMenuCallback = {},
                setSelectedIndexCallback = {},
                getCurrentSelectedIndexCallback = { 0 }
        )

        coordinator.openSubmenu(MenuState.ABOUT_MENU)

        // The cast failure happens before the state transition / registration, so neither runs.
        assertEquals(MenuState.MAIN_MENU, menuManager.getCurrentState())
        verify(inverse = true) { viewModel.registerAboutFragment(any()) }
    }

    // --- closeCurrentSubmenu ---

    @Test
    fun `closeCurrentSubmenu sem nenhuma entrada na pilha nao lanca excecao`() {
        val coordinator = newCoordinator()

        coordinator.closeCurrentSubmenu()
    }

    // --- restauracao via listener de back stack ---

    /**
     * Seeds the fragment manager's back stack with a single lightweight dummy entry *before*
     * constructing the coordinator, so its `init` block observes `backStackEntryCount == 1` and
     * sets `hasSubmenuOpen = true` -- equivalent to "a submenu is currently open" without needing
     * to run one of the real submenu fragments.
     */
    private fun seedOpenSubmenuOnBackStack() {
        activity.supportFragmentManager
                .beginTransaction()
                .add(Fragment(), "dummy-submenu")
                .addToBackStack("dummy-submenu")
                .commit()
        activity.supportFragmentManager.executePendingTransactions()
    }

    @Test
    fun `fechar o submenu via back stack restaura textos do menu principal e o estado MAIN_MENU`() {
        seedOpenSubmenuOnBackStack()
        menuManager.navigateToState(MenuState.PROGRESS_MENU)
        val coordinator = newCoordinator()
        coordinator.setupBackStackListener()

        activity.supportFragmentManager.popBackStack()
        activity.supportFragmentManager.executePendingTransactions()

        verify { viewManager.showMainMenuTexts() }
        assertEquals(MenuState.MAIN_MENU, menuManager.getCurrentState())
        assertEquals(0, restoredIndex)
    }

    // Regression test: restoreMainMenuSelection() chains two nested postDelayed(50ms) callbacks
    // with no way to cancel them. cancelPendingRestoration() (called from
    // RetroMenu3Fragment.onDestroyView()) must stop both from firing later against a fragment
    // whose view is already gone -- simulated here by cancelling right after the pop, before
    // idling the looper past either delay.
    @Test
    fun `cancelPendingRestoration impede os callbacks pendentes de restauracao de disparar`() {
        seedOpenSubmenuOnBackStack()
        menuManager.navigateToState(MenuState.PROGRESS_MENU)
        val coordinator = newCoordinator()
        coordinator.setupBackStackListener()

        activity.supportFragmentManager.popBackStack()
        activity.supportFragmentManager.executePendingTransactions()

        coordinator.cancelPendingRestoration()
        idle()

        assertEquals(emptyList<Boolean>(), showMainMenuCalls)
        verify(inverse = true) { animationController.updateSelectionVisual(any()) }
    }

    @Test
    fun `cancelPendingRestoration sem nenhuma restauracao pendente nao lanca excecao`() {
        val coordinator = newCoordinator()

        coordinator.cancelPendingRestoration()
    }

    @Test
    fun `restauracao a partir de SETTINGS_MENU desregistra o SettingsMenuFragment antes de voltar ao MAIN_MENU`() {
        seedOpenSubmenuOnBackStack()
        menuManager.navigateToState(MenuState.SETTINGS_MENU)
        val coordinator = newCoordinator()
        coordinator.setupBackStackListener()

        activity.supportFragmentManager.popBackStack()
        activity.supportFragmentManager.executePendingTransactions()

        verify { viewModel.unregisterSettingsMenuFragment() }
        assertEquals(MenuState.MAIN_MENU, menuManager.getCurrentState())
    }

    @Test
    fun `apos a restauracao sincrona, os callbacks postDelayed mostram o menu e atualizam o visual da selecao`() {
        seedOpenSubmenuOnBackStack()
        menuManager.navigateToState(MenuState.PROGRESS_MENU)
        selectedIndexToReport = 2
        val coordinator = newCoordinator()
        coordinator.setupBackStackListener()

        activity.supportFragmentManager.popBackStack()
        activity.supportFragmentManager.executePendingTransactions()
        idle()

        assertEquals(listOf(true), showMainMenuCalls)
        verify { animationController.updateSelectionVisual(2) }
    }

    @Test
    fun `restauracao e pulada quando isDismissingAllMenus esta ativo, para evitar flicker`() {
        every { viewModel.isDismissingAllMenus() } returns true
        seedOpenSubmenuOnBackStack()
        menuManager.navigateToState(MenuState.PROGRESS_MENU)
        val coordinator = newCoordinator()
        coordinator.setupBackStackListener()

        activity.supportFragmentManager.popBackStack()
        activity.supportFragmentManager.executePendingTransactions()

        verify(inverse = true) { viewManager.showMainMenuTexts() }
        assertEquals(MenuState.PROGRESS_MENU, menuManager.getCurrentState())
    }

    @Test
    fun `listener de back stack ignora mudancas depois que o host fragment e removido`() {
        seedOpenSubmenuOnBackStack()
        menuManager.navigateToState(MenuState.PROGRESS_MENU)
        val coordinator = newCoordinator()
        coordinator.setupBackStackListener()
        activity.supportFragmentManager.beginTransaction().remove(hostFragment).commitNow()

        activity.supportFragmentManager.popBackStack()
        activity.supportFragmentManager.executePendingTransactions()

        verify(inverse = true) { viewManager.showMainMenuTexts() }
    }

    @Test
    fun `closeCurrentSubmenu ainda aciona a restauracao, pois isClosingSubmenuProgrammatically ja voltou a false quando o listener roda`() {
        // CANDIDATE FINDING (see report), not a reproducible user-facing bug: closeCurrentSubmenu()
        // sets isClosingSubmenuProgrammatically = true, then resets it to false in a `finally`
        // block that runs synchronously right after the (asynchronous) popBackStack() call --
        // before the back-stack-changed listener actually observes the change. So the listener's
        // `if (isClosingSubmenuProgrammatically) return` guard is never true in practice; it is
        // dead code. This is harmless today only because every real caller of
        // closeCurrentSubmenu() (RetroMenu3Fragment's performBack/onBackToMainMenu/
        // onAboutBackToMainMenu) *wants* the main menu restored anyway, and hasSubmenuOpen already
        // provides the actual single-shot dedup (it flips false on the first restoration). This
        // test documents that restoreMainMenuSelection() still runs after a programmatic close.
        seedOpenSubmenuOnBackStack()
        menuManager.navigateToState(MenuState.PROGRESS_MENU)
        val coordinator = newCoordinator()
        coordinator.setupBackStackListener()

        coordinator.closeCurrentSubmenu()
        activity.supportFragmentManager.executePendingTransactions()

        verify { viewManager.showMainMenuTexts() }
    }
}
