package com.vinaooo.revenger.viewmodels

import android.app.Application
import android.os.Looper
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.OrientationManager
import com.vinaooo.revenger.utils.RetroViewUtils
import com.vinaooo.revenger.viewmodels.menu.GamePadInputController
import com.vinaooo.revenger.viewmodels.menu.SaveLoadOrchestrator
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The emulator view's lifecycle in [GameActivityViewModel]: [GameActivityViewModel.setupRetroView]
 * (through `retroViewFactory`, since the real view loads the native core), the first-frame
 * setup of speed, audio and shader, [GameActivityViewModel.preserveState],
 * [GameActivityViewModel.detachRetroView], [GameActivityViewModel.setConfigOrientation],
 * [GameActivityViewModel.dispose] and `onCleared`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameActivityViewModel_retroViewLifecycle_test {

    private lateinit var appConfig: AppConfig
    private lateinit var viewModel: GameActivityViewModel
    private lateinit var activity: ComponentActivity
    private lateinit var container: FrameLayout
    private lateinit var retroView: RetroView
    private lateinit var glRetroView: GLRetroView
    private val frameRendered = MutableLiveData(false)

    private fun setRevengerAppConfig(config: AppConfig?) {
        val field = RevengerApplication::class.java.getDeclaredField("appConfig")
        field.isAccessible = true
        field.set(null, config)
    }

    private fun <T> getPrivateField(name: String): T {
        val field = GameActivityViewModel::class.java.getDeclaredField(name)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST") return field.get(viewModel) as T
    }

    private fun setPrivateField(name: String, value: Any?) {
        val field = GameActivityViewModel::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(viewModel, value)
    }

    @Before
    fun setUp() {
        appConfig = mockk(relaxed = true)
        every { appConfig.getOrientation() } returns "3"
        setRevengerAppConfig(appConfig)
        viewModel = GameActivityViewModel(ApplicationProvider.getApplicationContext<Application>())
        activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        container = mockk(relaxed = true)

        glRetroView = mockk(relaxed = true)
        retroView = mockk(relaxed = true)
        every { retroView.view } returns glRetroView
        every { retroView.frameRendered } returns frameRendered
        viewModel.retroViewFactory = { retroView }
    }

    @After
    fun tearDown() {
        setRevengerAppConfig(null)
    }

    // --- setupRetroView ---

    @Test
    fun `setupRetroView poe a view do emulador no container e registra os listeners`() {
        viewModel.setupRetroView(activity, container)

        assertSame(retroView, viewModel.retroView)
        verify(exactly = 1) { container.addView(glRetroView) }
        verify { retroView.registerFrameRenderedListener() }
        verify { retroView.registerFrameCallback() }
    }

    @Test
    fun `a view do emulador segue o ciclo de vida da activity`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()

        viewModel.setupRetroView(controller.get(), container)
        controller.pause().stop().destroy()

        verify(exactly = 1) { glRetroView.onDestroy() }
    }

    @Test
    fun `setupRetroView cria os controllers de audio, velocidade e shader`() {
        viewModel.setupRetroView(activity, container)

        assertNotNull(getPrivateField("audioController"))
        assertNotNull(getPrivateField("speedController"))
        assertNotNull(getPrivateField("shaderController"))
        assertNotNull(getPrivateField("retroViewUtils"))
    }

    @Test
    fun `antes do primeiro frame nada e aplicado ao emulador`() {
        viewModel.setupRetroView(activity, container)
        shadowOf(Looper.getMainLooper()).idle()

        verify(exactly = 0) { glRetroView.frameSpeed = any() }
        verify(exactly = 0) { glRetroView.audioEnabled = any() }
    }

    @Test
    fun `no primeiro frame velocidade, audio e shader sao aplicados sem carregar save`() {
        viewModel.setupRetroView(activity, container)

        frameRendered.value = true
        shadowOf(Looper.getMainLooper()).idle()

        verify { glRetroView.frameSpeed = 1 }
        verify { glRetroView.audioEnabled = any() }
        verify { retroView.dynamicShader = any() }
        verify(exactly = 0) { glRetroView.unserializeState(any<ByteArray>()) }
    }

    // --- preserveState ---

    @Test
    fun `preserveState guarda o estado quando o jogo ja desenhou um frame`() {
        val utils = mockk<RetroViewUtils>(relaxed = true)
        viewModel.retroView = retroView
        setPrivateField("retroViewUtils", utils)
        frameRendered.value = true

        viewModel.preserveState()

        verify(exactly = 1) { utils.preserveEmulatorState(retroView) }
    }

    @Test
    fun `preserveState antes do primeiro frame nao guarda nada`() {
        val utils = mockk<RetroViewUtils>(relaxed = true)
        viewModel.retroView = retroView
        setPrivateField("retroViewUtils", utils)

        viewModel.preserveState()

        verify(exactly = 0) { utils.preserveEmulatorState(any()) }
    }

    @Test
    fun `preserveState sem emulador nao faz nada`() {
        viewModel.retroView = null

        viewModel.preserveState()
    }

    // --- detachRetroView / setConfigOrientation / dispose ---

    @Test
    fun `detachRetroView solta a view do emulador`() {
        viewModel.setupRetroView(activity, container)

        viewModel.detachRetroView(activity)

        assertNull(viewModel.retroView)
    }

    @Test
    fun `detachRetroView sem emulador nao faz nada`() {
        viewModel.detachRetroView(activity)

        assertNull(viewModel.retroView)
    }

    @Test
    fun `setConfigOrientation aplica a orientacao do config`() {
        mockkObject(OrientationManager)
        try {
            every { OrientationManager.applyConfigOrientation(any(), any()) } just runs

            viewModel.setConfigOrientation(activity)

            verify { OrientationManager.applyConfigOrientation(activity, "3") }
        } finally {
            unmockkObject(OrientationManager)
        }
    }

    @Test
    fun `dispose descarta as inscricoes e deixa uma nova pronta`() {
        val before = getPrivateField<CompositeDisposable>("compositeDisposable")

        viewModel.dispose()

        val after = getPrivateField<CompositeDisposable>("compositeDisposable")
        assertTrue(before.isDisposed)
        assertNotSame(before, after)
        assertFalse(after.isDisposed)
    }

    // --- onCleared ---

    @Test
    fun `onCleared cancela o save pendente e solta tudo`() {
        val orchestrator = mockk<SaveLoadOrchestrator>(relaxed = true)
        val gamePads = mockk<GamePadInputController>(relaxed = true)
        setPrivateField("saveLoadOrchestrator", orchestrator)
        setPrivateField("gamePadInputController", gamePads)
        viewModel.setupRetroView(activity, container)
        val disposable = getPrivateField<CompositeDisposable>("compositeDisposable")
        val submenus = getPrivateField<com.vinaooo.revenger.viewmodels.menu.SubmenuFragmentState>("submenuFragmentState")
        submenus.aboutFragment = mockk(relaxed = true)

        val onCleared = GameActivityViewModel::class.java.getDeclaredMethod("onCleared")
        onCleared.isAccessible = true
        onCleared.invoke(viewModel)

        verify(exactly = 1) { orchestrator.cancelPendingSave() }
        verify(exactly = 1) { gamePads.clear() }
        assertTrue(disposable.isDisposed)
        assertNull(viewModel.retroView)
        assertNull(getPrivateField("retroViewUtils"))
        assertNull(getPrivateField("audioController"))
        assertNull(getPrivateField("speedController"))
        assertNull(getPrivateField("shaderController"))
        assertNull(getPrivateField("sharedPreferences"))
        assertEquals(null, getPrivateField<Any?>("retroMenu3Fragment"))
        assertNull(submenus.aboutFragment)
    }

    @Test
    fun `setupGamePads liga os controles na tela aos containers`() {
        val gamePads = mockk<GamePadInputController>(relaxed = true)
        setPrivateField("gamePadInputController", gamePads)
        val left = mockk<FrameLayout>(relaxed = true)
        val right = mockk<FrameLayout>(relaxed = true)

        viewModel.setupGamePads(activity, left, right)

        verify(exactly = 1) { gamePads.setupGamePads(activity, left, right) }
    }
}
