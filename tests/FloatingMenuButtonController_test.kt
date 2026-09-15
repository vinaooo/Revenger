package com.vinaooo.revenger.controllers

import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Characterization tests for [FloatingMenuButtonController], extracted verbatim from
 * `GameActivity` (Task 10 of the split-god-classes refactor).
 *
 * Written and run directly against the new class rather than as a pre/post diff against
 * `GameActivity`: like Task 9's `GamePadLayoutAdjuster` extraction, this is a mechanical,
 * self-contained lift (the moved bodies only touch the injected `Button` and `viewModel`), so
 * "written and passing against the new class" is the characterization baseline here.
 *
 * `RevengerApplication.appConfig` is a `lateinit var` on a companion object, only populated by
 * `RevengerApplication.onCreate()` (which never runs for the default Robolectric test
 * Application) -- seeded via reflection the same way `GamePadLayoutAdjuster_test`/
 * `InputViewModel_test` do, since [FloatingMenuButtonController.setup] reads it directly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class FloatingMenuButtonController_test {

    private lateinit var activity: FragmentActivity
    private lateinit var floatingButton: Button
    private lateinit var viewModel: GameActivityViewModel

    private fun setRevengerAppConfig(appConfig: AppConfig?) {
        val field = RevengerApplication::class.java.getDeclaredField("appConfig")
        field.isAccessible = true
        field.set(null, appConfig)
    }

    private fun seedAppConfig(menuModeFab: String = "bottom-right", gamepadEnabled: Boolean = false) {
        val appConfig = mockk<AppConfig>()
        every { appConfig.getMenuModeFab() } returns menuModeFab
        every { appConfig.getGamepad() } returns gamepadEnabled
        setRevengerAppConfig(appConfig)
    }

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        floatingButton =
            Button(activity).apply {
                layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
            }
        viewModel = mockk(relaxed = true)
        seedAppConfig()
    }

    @After
    fun tearDown() {
        setRevengerAppConfig(null)
    }

    private fun newController() = FloatingMenuButtonController(floatingButton, viewModel)

    // --- setup(): gravity per config value ---

    @Test
    fun `setup aplica TOP-START para top-left`() {
        seedAppConfig(menuModeFab = "top-left")
        newController().setup()

        val params = floatingButton.layoutParams as FrameLayout.LayoutParams
        assertEquals(Gravity.TOP or Gravity.START, params.gravity)
    }

    @Test
    fun `setup aplica TOP-END para top-right`() {
        seedAppConfig(menuModeFab = "top-right")
        newController().setup()

        val params = floatingButton.layoutParams as FrameLayout.LayoutParams
        assertEquals(Gravity.TOP or Gravity.END, params.gravity)
    }

    @Test
    fun `setup aplica BOTTOM-START para bottom-left`() {
        seedAppConfig(menuModeFab = "bottom-left")
        newController().setup()

        val params = floatingButton.layoutParams as FrameLayout.LayoutParams
        assertEquals(Gravity.BOTTOM or Gravity.START, params.gravity)
    }

    @Test
    fun `setup aplica BOTTOM-END para bottom-right`() {
        seedAppConfig(menuModeFab = "bottom-right")
        newController().setup()

        val params = floatingButton.layoutParams as FrameLayout.LayoutParams
        assertEquals(Gravity.BOTTOM or Gravity.END, params.gravity)
    }

    @Test
    fun `setup e case-insensitive para o valor de config`() {
        seedAppConfig(menuModeFab = "TOP-LEFT")
        newController().setup()

        val params = floatingButton.layoutParams as FrameLayout.LayoutParams
        assertEquals(Gravity.TOP or Gravity.START, params.gravity)
    }

    @Test
    fun `setup esconde o botao quando configValue e disabled`() {
        seedAppConfig(menuModeFab = "disabled")
        newController().setup()

        assertEquals(View.GONE, floatingButton.visibility)
    }

    @Test
    fun `setup esconde o botao para um valor de config desconhecido`() {
        seedAppConfig(menuModeFab = "sideways")
        newController().setup()

        assertEquals(View.GONE, floatingButton.visibility)
    }

    // --- setup(): visibility follows shouldShowGamePads ---

    @Test
    fun `setup deixa o botao visivel quando o gamepad virtual esta desabilitado no AppConfig`() {
        // appConfig.getGamepad()=false makes GamePad.shouldShowGamePads() return false via its
        // own first guard, without touching PackageManager/Display -- so !shouldShowGamePads is
        // true and the button stays VISIBLE.
        seedAppConfig(menuModeFab = "bottom-right", gamepadEnabled = false)
        newController().setup()

        assertEquals(View.VISIBLE, floatingButton.visibility)
    }

    // Note: the "shouldShowGamePads() == true -> button GONE" branch is not exercised here.
    // GamePad.shouldShowGamePads() calls `activity.display` once past the `getGamepad()` guard,
    // and Robolectric's ContextImpl.getDisplay() shadow throws UnsupportedOperationException for
    // a Robolectric.buildActivity(...)-built FragmentActivity ("Tried to obtain display from a
    // Context not associated with one"), regardless of activity lifecycle state. That branch's
    // own logic is already covered by GamePad_test.kt (which sidesteps this by mocking `Activity`
    // directly instead of using a real one) -- this controller only reads the boolean it returns.

    // --- setup(): click listener ---

    @Test
    fun `setup liga o click listener a viewModel toggleMainMenu`() {
        seedAppConfig(menuModeFab = "bottom-right", gamepadEnabled = false)
        newController().setup()

        floatingButton.performClick()

        verify(exactly = 1) { viewModel.toggleMainMenu() }
    }

    // --- triggerFade(): guards ---

    @Test
    fun `triggerFade nao faz nada quando ha um menu ativo`() {
        seedAppConfig(menuModeFab = "bottom-right", gamepadEnabled = false)
        val controller = newController()
        controller.setup()
        every { viewModel.isAnyMenuActive() } returns true
        floatingButton.alpha = 1.0f

        controller.triggerFade()

        assertEquals(1.0f, floatingButton.alpha)
    }

    @Test
    fun `triggerFade nao faz nada quando o botao nao esta VISIBLE`() {
        seedAppConfig(menuModeFab = "disabled")
        val controller = newController()
        controller.setup() // configValue=disabled -> button is GONE
        every { viewModel.isAnyMenuActive() } returns false
        floatingButton.alpha = 1.0f

        controller.triggerFade()

        assertEquals(1.0f, floatingButton.alpha)
    }

    // --- triggerFade(): fades and schedules a restore ---

    @Test
    fun `triggerFade desce o alpha para 0,3 quando visivel e sem menu ativo`() {
        seedAppConfig(menuModeFab = "bottom-right", gamepadEnabled = false)
        val controller = newController()
        controller.setup()
        every { viewModel.isAnyMenuActive() } returns false

        controller.triggerFade()
        // ViewPropertyAnimator under Robolectric runs on the (paused) main looper's animation
        // handler; idling past the 200ms fade-out duration lets it reach its final value.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))

        assertEquals(0.3f, floatingButton.alpha)
    }

    @Test
    fun `triggerFade agenda uma restauracao para alpha 1,0 em 10s`() {
        seedAppConfig(menuModeFab = "bottom-right", gamepadEnabled = false)
        val controller = newController()
        controller.setup()
        every { viewModel.isAnyMenuActive() } returns false

        controller.triggerFade()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200)) // fade-out anim completes
        assertEquals(0.3f, floatingButton.alpha)

        // Remaining wait until the 10s restore delay elapses, plus its own 500ms fade-in duration.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(9800 + 500))

        assertEquals(1.0f, floatingButton.alpha)
    }

    @Test
    fun `triggerFade cancela um fade pendente anterior antes de reagendar`() {
        seedAppConfig(menuModeFab = "bottom-right", gamepadEnabled = false)
        val controller = newController()
        controller.setup()
        every { viewModel.isAnyMenuActive() } returns false

        controller.triggerFade() // schedules stale restore-runnable at absolute t=10000ms
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200)) // its fade-out anim completes
        assertEquals(0.3f, floatingButton.alpha)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(7800)) // now at t=8000ms
        controller.triggerFade() // must cancel the stale runnable; schedules a fresh one at t=18000ms
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200)) // its own fade-out anim completes
        assertEquals(0.3f, floatingButton.alpha)

        // Advance past the STALE runnable's original fire time (10000ms) plus its 500ms fade-in
        // duration, but well before the fresh one (18000ms) -- if cancellation failed, alpha
        // would already be back at 1.0 here.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(3600)) // now at t=11800ms
        assertEquals(0.3f, floatingButton.alpha)

        // Advance past the fresh runnable's fire time (18000ms) plus its 500ms fade-in duration.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(6900)) // now at t=18700ms
        assertEquals(1.0f, floatingButton.alpha)
    }

    // --- restoreFloatingButtonVisibility(): guard + cancel-then-animate ---

    @Test
    fun `restoreFloatingButtonVisibility nao faz nada quando o botao nao esta VISIBLE`() {
        seedAppConfig(menuModeFab = "disabled")
        val controller = newController()
        controller.setup() // button is GONE
        floatingButton.alpha = 0.3f

        controller.restoreFloatingButtonVisibility()

        assertEquals(0.3f, floatingButton.alpha)
    }

    @Test
    fun `restoreFloatingButtonVisibility anima o alpha de volta para 1,0`() {
        seedAppConfig(menuModeFab = "bottom-right", gamepadEnabled = false)
        val controller = newController()
        controller.setup()
        floatingButton.alpha = 0.3f

        controller.restoreFloatingButtonVisibility()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200)) // its fade-in anim completes

        assertEquals(1.0f, floatingButton.alpha)
    }

    @Test
    fun `restoreFloatingButtonVisibility cancela o fade pendente agendado por triggerFade`() {
        seedAppConfig(menuModeFab = "bottom-right", gamepadEnabled = false)
        val controller = newController()
        controller.setup()
        every { viewModel.isAnyMenuActive() } returns false

        controller.triggerFade() // schedules a stale restore-to-1.0 runnable at absolute t=10000ms
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200)) // its fade-out anim completes
        assertEquals(0.3f, floatingButton.alpha)

        controller.restoreFloatingButtonVisibility() // must cancel the stale runnable
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200)) // its own fade-in anim completes
        assertEquals(1.0f, floatingButton.alpha)

        // Sentinel set only after both real animations above have already settled. Nothing else
        // in this test touches alpha from here on -- if the stale runnable is still pending, it
        // fires its own `.animate().alpha(1.0f)` at t=10000ms and this reading flips back to 1.0.
        floatingButton.alpha = 0.42f
        // Advance well past the stale runnable's fire time (10000ms) plus its 500ms duration.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10500))

        assertEquals(0.42f, floatingButton.alpha)
    }

    // --- fadeFloatingButtonImmediately(): guard + fade-and-reschedule ---

    @Test
    fun `fadeFloatingButtonImmediately nao faz nada quando o botao nao esta VISIBLE`() {
        seedAppConfig(menuModeFab = "disabled")
        val controller = newController()
        controller.setup() // button is GONE
        floatingButton.alpha = 1.0f

        controller.fadeFloatingButtonImmediately()

        assertEquals(1.0f, floatingButton.alpha)
    }

    @Test
    fun `fadeFloatingButtonImmediately desce o alpha para 0,3 e reagenda a restauracao`() {
        seedAppConfig(menuModeFab = "bottom-right", gamepadEnabled = false)
        val controller = newController()
        controller.setup()

        controller.fadeFloatingButtonImmediately()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200)) // fade-out anim completes
        assertEquals(0.3f, floatingButton.alpha)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(9800 + 500))

        assertEquals(1.0f, floatingButton.alpha)
    }

    @Test
    fun `fadeFloatingButtonImmediately cancela um fade pendente anterior antes de reagendar`() {
        seedAppConfig(menuModeFab = "bottom-right", gamepadEnabled = false)
        val controller = newController()
        controller.setup()
        every { viewModel.isAnyMenuActive() } returns false

        controller.triggerFade() // schedules stale restore-runnable at absolute t=10000ms
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200)) // its fade-out anim completes
        assertEquals(0.3f, floatingButton.alpha)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(7800)) // now at t=8000ms
        controller.fadeFloatingButtonImmediately() // must cancel the stale runnable; reschedules at t=18000ms
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200)) // its own fade-out anim completes
        assertEquals(0.3f, floatingButton.alpha)

        // Advance past the STALE runnable's original fire time (10000ms) plus its 500ms fade-in
        // duration, but well before the fresh one (18000ms).
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(3600)) // now at t=11800ms
        assertEquals(0.3f, floatingButton.alpha)

        // Advance past the fresh runnable's fire time (18000ms) plus its 500ms fade-in duration.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(6900)) // now at t=18700ms
        assertEquals(1.0f, floatingButton.alpha)
    }
}
