package com.vinaooo.revenger.ui.retromenu3

import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.roborazziSystemPropertyOutputDirectory
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Hosts the menu fragments with a stubbed [GameActivityViewModel]. The fragments look it up
 * through `ViewModelProvider(requireActivity())`, so the stub is handed out by this Activity's
 * default factory; every other ViewModel (InputViewModel) is created as usual.
 */
class ScreenshotHostActivity : FragmentActivity() {
    lateinit var gameViewModel: GameActivityViewModel

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() {
            val base = super.defaultViewModelProviderFactory
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                        if (modelClass == GameActivityViewModel::class.java) modelClass.cast(gameViewModel)!!
                        else base.create(modelClass, extras)
            }
        }
}

/**
 * Screenshot tests for the RetroMenu3 pixel UI (Roborazzi on Robolectric's native graphics).
 * Goldens live in `tests/screenshots/`; `./gradlew recordRoborazziDebug` rewrites them and `check`
 * compares against them, failing on any pixel change.
 *
 * Only screens whose content is fixed by the repo's resources are captured. Everything a menu shows
 * from the emulator or the configured game (audio, speed and shader state, the exit screenshot)
 * comes from the stubbed ViewModel, so the goldens don't change when `config.json` points at
 * another game. Locale, screen size and density are pinned by the qualifiers.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = LANDSCAPE)
class RetroMenu3Screenshot_test {

    private lateinit var viewModel: GameActivityViewModel

    @Before
    fun setUp() {
        viewModel = mockk(relaxed = true)
        every { viewModel.getAudioState() } returns true
        every { viewModel.getFastForwardState() } returns false
        every { viewModel.getShaderDisplayName() } returns "Sharp"
        every { viewModel.getCachedScreenshot() } returns null
    }

    /** Shows [fragment] full screen, runs [before] on it (e.g. navigation), and saves `<name>.png`. */
    private fun <F : Fragment> capture(fragment: F, name: String, before: (F) -> Unit = {}) {
        val controller = Robolectric.buildActivity(ScreenshotHostActivity::class.java)
        val activity = controller.get()
        activity.gameViewModel = viewModel
        controller.create()
        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)
        activity.supportFragmentManager.beginTransaction().add(container.id, fragment).commitNow()
        controller.start().resume().visible()
        before(fragment)
        shadowOf(Looper.getMainLooper()).idle()
        container.captureRoboImage("${roborazziSystemPropertyOutputDirectory()}/$name.png")
    }

    @Test
    fun `menu principal em paisagem`() = capture(RetroMenu3Fragment.newInstance(), "retromenu3_main_land")

    @Test
    @Config(qualifiers = PORTRAIT)
    fun `menu principal em retrato`() = capture(RetroMenu3Fragment.newInstance(), "retromenu3_main_port")

    @Test
    fun `menu principal com a selecao no terceiro item`() =
            capture(RetroMenu3Fragment.newInstance(), "retromenu3_main_selection") {
                it.onNavigateDown()
                it.onNavigateDown()
            }

    @Test
    fun `submenu de settings com audio ligado e velocidade normal`() =
            capture(SettingsMenuFragment.newInstance(), "retromenu3_settings")

    @Test
    fun `submenu de settings com audio desligado e fast forward ativo`() {
        every { viewModel.getAudioState() } returns false
        every { viewModel.getFastForwardState() } returns true
        capture(SettingsMenuFragment.newInstance(), "retromenu3_settings_toggled")
    }

    @Test
    fun `submenu de progress`() = capture(ProgressFragment.newInstance(), "retromenu3_progress")

    @Test
    fun `submenu de exit`() = capture(ExitFragment.newInstance(), "retromenu3_exit")
}

private const val LANDSCAPE = "en-rUS-w891dp-h411dp-land-xxhdpi"
private const val PORTRAIT = "en-rUS-w411dp-h891dp-port-xxhdpi"
