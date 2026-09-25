package com.vinaooo.revenger.viewmodels

import android.app.Application
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [GameActivityViewModel.immersive]. The [Window] is a mock so its `insetsController`
 * can be forced to null, which a real Robolectric window never returns.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameActivityViewModel_immersive_test {

    private lateinit var appConfig: AppConfig
    private lateinit var viewModel: GameActivityViewModel

    private fun setRevengerAppConfig(appConfig: AppConfig?) {
        val field = RevengerApplication::class.java.getDeclaredField("appConfig")
        field.isAccessible = true
        field.set(null, appConfig)
    }

    @Before
    fun setUp() {
        appConfig = mockk(relaxed = true)
        setRevengerAppConfig(appConfig)
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GameActivityViewModel(app)
    }

    @After
    fun tearDown() {
        setRevengerAppConfig(null)
    }

    @Test
    fun `immersive com fullscreen desativado nao toca no insetsController`() {
        every { appConfig.getFullscreen() } returns false
        val window = mockk<Window>(relaxed = true)

        viewModel.immersive(window)

        verify(exactly = 0) { window.insetsController }
    }

    @Test
    fun `immersive com fullscreen esconde as system bars`() {
        every { appConfig.getFullscreen() } returns true
        val insetsController = mockk<WindowInsetsController>(relaxed = true)
        val window = mockk<Window>(relaxed = true)
        every { window.insetsController } returns insetsController

        viewModel.immersive(window)

        verify { insetsController.hide(WindowInsets.Type.systemBars()) }
        verify {
            insetsController.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Regression: this used `window.insetsController!!`, which threw a NullPointerException when
    // the window had no insets controller yet.
    @Test
    fun `immersive com insetsController nulo nao lanca excecao`() {
        every { appConfig.getFullscreen() } returns true
        val window = mockk<Window>(relaxed = true)
        every { window.insetsController } returns null

        viewModel.immersive(window)
    }
}
