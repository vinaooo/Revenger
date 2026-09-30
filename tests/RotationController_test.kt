package com.vinaooo.revenger.controllers

import android.os.Looper
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.MenuFragment
import com.vinaooo.revenger.ui.retromenu3.MenuItem
import com.vinaooo.revenger.ui.retromenu3.MenuManager
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * A minimal stand-in for a submenu fragment, implementing [MenuFragment] directly rather than
 * extending `MenuFragmentBase` -- `RotationController.maybeRecreateMenuAfterRotation`'s
 * visible-menu check only cares about the [MenuFragment] interface, not any particular concrete
 * subclass, and a bare stand-in avoids needing a `GameActivityViewModel` reachable via
 * `ViewModelProvider` (see `SubmenuCoordinator_test`'s comment on the same tradeoff). Shared
 * (package-visible) with `MenuRotationRecreator_test`, which needs the same stand-in.
 */
class DummyMenuFragment : Fragment(), MenuFragment {
    override fun getMenuItems(): List<MenuItem> = emptyList()
    override fun onNavigateUp(): Boolean = false
    override fun onNavigateDown(): Boolean = false
    override fun onConfirm(): Boolean = false
    override fun onBack(): Boolean = false
    override fun getCurrentSelectedIndex(): Int = 0
    override fun setSelectedIndex(index: Int) = Unit
}

/**
 * [RotationController] owns the auto-rotate `BroadcastReceiver` lifecycle and the
 * orientation-reapply decision that used to live inline in `GameActivity`, extracted (alongside
 * [MenuRotationRecreator] and `RotationFragmentFactory`) to keep `GameActivity` within detekt's
 * `TooManyFunctions` threshold. These tests pin: the receiver register/dispose lifecycle, the
 * auto-rotate-reapply branching in [RotationController.reapplyOrientationIfNeeded], and
 * [RotationController.maybeRecreateMenuAfterRotation]'s directly-observable effects (menu callback
 * re-registration, and the skip when no menu fragment is visible). Full end-to-end fragment
 * recreation is covered by `MenuRotationRecreator_test` up to the point it hands off to a real
 * production submenu/main-menu fragment's `onCreateView` -- see that file's header comment for why
 * this suite stops there too.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RotationController_test {

    private lateinit var activity: FragmentActivity
    private lateinit var viewModel: GameActivityViewModel
    private lateinit var appConfig: AppConfig
    private lateinit var menuManager: MenuManager
    private lateinit var controller: RotationController

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val root = FrameLayout(activity).apply { id = R.id.menu_container }
        activity.setContentView(root)

        menuManager = mockk(relaxed = true)
        viewModel = mockk(relaxed = true)
        every { viewModel.getMenuManager() } returns menuManager
        every { viewModel.isAnyMenuActive() } returns true

        appConfig = mockk(relaxed = true)

        controller = RotationController(activity, viewModel, appConfig)
    }

    /** The orientation the activity started in, which the controller records as the menu's. */
    private val initial get() = activity.resources.configuration.orientation

    /** The other orientation: a real rotation. */
    private val rotated
        get() =
                if (initial == Configuration.ORIENTATION_LANDSCAPE) Configuration.ORIENTATION_PORTRAIT
                else Configuration.ORIENTATION_LANDSCAPE

    /** Puts a menu fragment in the container as a submenu, with one backstack entry. */
    private fun showSubmenu() {
        val fragmentManager = activity.supportFragmentManager
        fragmentManager
                .beginTransaction()
                .add(R.id.menu_container, DummyMenuFragment(), "dummy")
                .addToBackStack("dummy")
                .commit()
        fragmentManager.executePendingTransactions()
    }

    /** Past step 1 of the recreation chain, which pops the backstack. */
    private fun idlePastSettle() = shadowOf(activity.mainLooper).idleFor(Duration.ofMillis(250))

    // ---------------------------------------------------------------------------------------
    // register() / dispose(): BroadcastReceiver lifecycle
    // ---------------------------------------------------------------------------------------

    @Test
    fun `register entao dispose nao lanca excecao`() {
        controller.register()
        controller.dispose()
    }

    @Test
    fun `dispose sem register antes nao lanca excecao`() {
        // unregisterReceiver() on a never-registered receiver throws IllegalArgumentException;
        // dispose() must swallow it (mirrors the original inline try/catch in GameActivity).
        controller.dispose()
    }

    @Test
    fun `dispose chamado duas vezes nao lanca excecao na segunda vez`() {
        controller.register()
        controller.dispose()
        controller.dispose()
    }

    private fun broadcast(action: String) {
        activity.sendBroadcast(Intent(action))
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `receiver registrado reaplica a orientacao quando a configuracao do sistema muda`() {
        controller.register()

        broadcast(Intent.ACTION_CONFIGURATION_CHANGED)

        verify(exactly = 1) { viewModel.setConfigOrientation(activity) }
        controller.dispose()
    }

    @Test
    fun `receiver ignora outras acoes e para de ouvir depois do dispose`() {
        controller.register()
        broadcast(Intent.ACTION_SCREEN_ON)

        controller.dispose()
        broadcast(Intent.ACTION_CONFIGURATION_CHANGED)

        verify(exactly = 0) { viewModel.setConfigOrientation(any()) }
    }

    @Test
    fun `falha ao reaplicar a orientacao nao derruba o app`() {
        every { viewModel.setConfigOrientation(any()) } throws IllegalStateException("boom")
        controller.register()

        broadcast(Intent.ACTION_CONFIGURATION_CHANGED)

        verify(exactly = 1) { viewModel.setConfigOrientation(activity) }
        controller.dispose()
    }

    // ---------------------------------------------------------------------------------------
    // reapplyOrientationIfNeeded(): auto-rotate reapply decision
    // ---------------------------------------------------------------------------------------

    @Test
    fun `config auto com auto-rotate do sistema ligado reaplica orientacao`() {
        every { appConfig.getOrientation() } returns "auto"
        Settings.System.putInt(activity.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)

        controller.reapplyOrientationIfNeeded(Configuration())

        verify { viewModel.setConfigOrientation(activity) }
    }

    @Test
    fun `config auto com auto-rotate do sistema desligado nao reaplica orientacao`() {
        every { appConfig.getOrientation() } returns "auto"
        Settings.System.putInt(activity.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)

        controller.reapplyOrientationIfNeeded(Configuration())

        verify(exactly = 0) { viewModel.setConfigOrientation(activity) }
    }

    @Test
    fun `config forcada reaplica orientacao mesmo com auto-rotate do sistema desligado`() {
        every { appConfig.getOrientation() } returns "landscape"
        Settings.System.putInt(activity.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)

        controller.reapplyOrientationIfNeeded(Configuration())

        verify { viewModel.setConfigOrientation(activity) }
    }

    // ---------------------------------------------------------------------------------------
    // maybeRecreateMenuAfterRotation(): directly-observable effects
    // ---------------------------------------------------------------------------------------

    @Test
    fun `maybeRecreateMenuAfterRotation sempre re-registra os callbacks do menu`() {
        controller.maybeRecreateMenuAfterRotation(rotated)

        verify { viewModel.setupMenuCallback(activity) }
    }

    @Test
    fun `maybeRecreateMenuAfterRotation sem fragment de menu visivel nao lanca excecao`() {
        // menu_container is empty -- the abort branch (findFragmentById returns null) must be a
        // clean no-op, not a crash.
        controller.maybeRecreateMenuAfterRotation(rotated)

        verify { viewModel.setupMenuCallback(activity) }
    }

    @Test
    fun `maybeRecreateMenuAfterRotation com fragment que nao implementa MenuFragment nao lanca excecao`() {
        activity.supportFragmentManager
                .beginTransaction()
                .add(R.id.menu_container, Fragment(), "not-a-menu")
                .commitNow()

        controller.maybeRecreateMenuAfterRotation(rotated)

        verify { viewModel.setupMenuCallback(activity) }
    }

    @Test
    fun `maybeRecreateMenuAfterRotation com menu visivel agenda a limpeza do backstack`() {
        val fragmentManager = activity.supportFragmentManager
        val dummy = DummyMenuFragment()
        fragmentManager
                .beginTransaction()
                .add(R.id.menu_container, dummy, "dummy")
                .addToBackStack("dummy")
                .commit()
        fragmentManager.executePendingTransactions()

        controller.maybeRecreateMenuAfterRotation(rotated)

        // Step 0/1 of MenuRotationRecreator's chain fire after SYSTEM_SETTLE_DELAY_MS (250ms) and
        // pop the backstack -- confirms RotationController really handed off to it, without
        // advancing far enough to trigger the real-fragment rebuild step (see this file's header
        // comment).
        shadowOf(activity.mainLooper).idleFor(Duration.ofMillis(250))

        assertEquals(0, fragmentManager.backStackEntryCount)
    }

    @Test
    fun `config auto com falha ao ler o auto-rotate trata como desligado e nao reaplica`() {
        every { appConfig.getOrientation() } returns "auto"
        val failingActivity = spyk(activity)
        every { failingActivity.contentResolver } throws SecurityException("denied")

        RotationController(failingActivity, viewModel, appConfig).reapplyOrientationIfNeeded(Configuration())

        verify(exactly = 0) { viewModel.setConfigOrientation(any()) }
    }

    @Test
    fun `maybeRecreateMenuAfterRotation com fragment que nao e de menu nao mexe no backstack`() {
        val fragmentManager = activity.supportFragmentManager
        fragmentManager
                .beginTransaction()
                .add(R.id.menu_container, Fragment(), "not-a-menu")
                .addToBackStack("not-a-menu")
                .commit()
        fragmentManager.executePendingTransactions()

        controller.maybeRecreateMenuAfterRotation(rotated)
        shadowOf(activity.mainLooper).idleFor(Duration.ofMillis(250))

        assertEquals(1, fragmentManager.backStackEntryCount)
    }

    // ---------------------------------------------------------------------------------------
    // maybeRecreateMenuAfterRotation(): only a change of orientation rebuilds the menu
    // ---------------------------------------------------------------------------------------

    @Test
    fun `configuracao que mantem a orientacao nao recria o menu`() {
        // A second callback for the same rotation, or a uiMode/screen size change, carries the
        // orientation the menu already has. It used to start a second rebuild chain.
        showSubmenu()

        controller.maybeRecreateMenuAfterRotation(initial)
        idlePastSettle()

        assertEquals(1, activity.supportFragmentManager.backStackEntryCount)
        verify { viewModel.setupMenuCallback(activity) }
    }

    @Test
    fun `segunda chamada da mesma rotacao nao recria o menu de novo`() {
        // The first call's chain stops at its "menu dismissed" check, so the backstack it
        // leaves shows only whether the second call started a chain of its own.
        showSubmenu()
        every { viewModel.isAnyMenuActive() } returns false
        controller.maybeRecreateMenuAfterRotation(rotated)
        idlePastSettle()
        assertEquals(1, activity.supportFragmentManager.backStackEntryCount)

        every { viewModel.isAnyMenuActive() } returns true
        controller.maybeRecreateMenuAfterRotation(rotated)
        idlePastSettle()

        assertEquals(1, activity.supportFragmentManager.backStackEntryCount)
    }

    @Test
    fun `rotacao sem menu aberto ainda atualiza a orientacao do menu`() {
        // Rotated with no menu open, then the menu opened in the new orientation: rotating back
        // is a real rotation and must rebuild it; repeating the first one is not.
        controller.maybeRecreateMenuAfterRotation(rotated)
        showSubmenu()

        controller.maybeRecreateMenuAfterRotation(rotated)
        idlePastSettle()
        assertEquals(1, activity.supportFragmentManager.backStackEntryCount)

        controller.maybeRecreateMenuAfterRotation(initial)
        idlePastSettle()
        assertEquals(0, activity.supportFragmentManager.backStackEntryCount)
    }
}
