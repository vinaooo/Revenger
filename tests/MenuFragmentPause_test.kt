package com.vinaooo.revenger.ui.retromenu3

import android.app.Application
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.input.ControllerInput
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * Pausing a menu fragment leaves the real [ControllerInput] alone. `MenuFragmentBase.onPause()`
 * used to clear pending input on an orphan instance that no input ever reached, so it did
 * nothing. It was removed rather than pointed at the real instance: that clear also resets the
 * post-close grace period, which keeps the button that closed the menu from reaching the game
 * when it is released, and a closing menu pauses right after that period starts.
 *
 * The fragment runs against a real [GameActivityViewModel], whose own [ControllerInput] is the
 * one that handles real input.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MenuFragmentPause_test {

    private val appConfigField =
            RevengerApplication::class.java.getDeclaredField("appConfig").apply { isAccessible = true }
    private var originalAppConfig: AppConfig? = null
    private lateinit var controller: ActivityController<ScreenshotHostActivity>
    private lateinit var viewModel: GameActivityViewModel

    @Before
    fun setUp() {
        originalAppConfig = appConfigField.get(null) as AppConfig?
        appConfigField.set(null, mockk<AppConfig>(relaxed = true))
        viewModel = GameActivityViewModel(ApplicationProvider.getApplicationContext<Application>())

        controller = Robolectric.buildActivity(ScreenshotHostActivity::class.java)
        controller.get().gameViewModel = viewModel
        controller.create()
    }

    @After
    fun tearDown() {
        if (!controller.get().isDestroyed) controller.pause().stop().destroy()
        appConfigField.set(null, originalAppConfig)
    }

    private fun realControllerInput(): ControllerInput =
            GameActivityViewModel::class.java.getDeclaredField("controllerInput").apply { isAccessible = true }
                    .get(viewModel) as ControllerInput

    @Test
    fun `pausar um menu mantem o periodo de graca do botao que o fechou`() {
        val activity = controller.get()
        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)
        val fragment = AboutFragment.newInstance()
        activity.supportFragmentManager.beginTransaction().add(container.id, fragment, "menu").commitNow()
        controller.start().resume().visible()

        val debouncer = realControllerInput().callbackDebouncer
        debouncer.keepInterceptingButtons(closingButton = KeyEvent.KEYCODE_BUTTON_B)

        activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()

        assertTrue(debouncer.shouldInterceptSpecificButton(KeyEvent.KEYCODE_BUTTON_B))
    }
}
