package com.vinaooo.revenger.views

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.media.AudioManager
import com.vinaooo.revenger.controllers.FloatingMenuButtonController
import com.vinaooo.revenger.gamepad.GamePadLayoutAdjuster
import com.vinaooo.revenger.performance.AdvancedPerformanceProfiler
import com.vinaooo.revenger.utils.OrientationManager
import com.vinaooo.revenger.utils.RetroViewUtils
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import com.vinaooo.revenger.utils.SystemBarsAppearance
import com.vinaooo.revenger.viewmodels.menu.GamePadInputController
import android.os.Looper
import android.os.SystemClock
import android.util.Rational
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.controllers.PipController
import com.vinaooo.revenger.controllers.RotationController
import com.vinaooo.revenger.gamepad.GamePad
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import com.vinaooo.revenger.viewmodels.menu.KeyMotionInputRouter
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
 * [GameActivity] under Robolectric. The ViewModel is fetched through [ViewModelProvider] before
 * `create()` so its `retroViewFactory` can return a mock [RetroView] (the real one loads the native
 * core), and [GamePad.shouldShowGamePads] is stubbed because the activity has no display during
 * `onCreate`. Where the activity only delegates, the collaborator ([PipController],
 * [RotationController], the ViewModel's key/motion router, the [NavigationController]) is swapped
 * for a mock after `create()` and the delegation is verified; the collaborators have their own
 * tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameActivity_test {

    private lateinit var controller: ActivityController<GameActivity>
    private lateinit var activity: GameActivity
    private lateinit var viewModel: GameActivityViewModel
    private lateinit var glRetroView: GLRetroView
    private lateinit var retroView: RetroView
    private val frameRendered = MutableLiveData(false)

    @Before
    fun setUp() {
        mockkObject(GamePad.Companion)
        every { GamePad.shouldShowGamePads(any(), any()) } returns false

        glRetroView = mockk(relaxed = true)
        every { glRetroView.parent } returns null
        every { glRetroView.findViewById<View>(any()) } returns null
        every { glRetroView.layoutParams } returns
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        retroView = mockk(relaxed = true)
        every { retroView.view } returns glRetroView
        every { retroView.frameRendered } returns frameRendered

        controller = Robolectric.buildActivity(GameActivity::class.java)
        activity = controller.get()
        viewModel = ViewModelProvider(activity)[GameActivityViewModel::class.java]
        viewModel.retroViewFactory = { retroView }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun getField(owner: Any, name: String): Any? =
            owner.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(owner)

    private fun setField(owner: Any, name: String, value: Any?) {
        owner.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(owner, value)
    }

    /** Creates the activity and swaps its PiP controller for a relaxed mock. */
    private fun createWithMockPip(): PipController {
        controller.create()
        val pip = mockk<PipController>(relaxed = true)
        setField(activity, "pipController", pip)
        return pip
    }

    private fun mockKeyMotionRouter(): KeyMotionInputRouter {
        val router = mockk<KeyMotionInputRouter>()
        setField(viewModel, "keyMotionInputRouter", router)
        return router
    }

    private fun keyEvent(action: Int, keyCode: Int) = KeyEvent(action, keyCode)

    private fun motionEvent(): MotionEvent =
            MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, 0f, 0f, 0)

    // --- Ciclo de vida ---

    @Test
    fun `ciclo completo de create a destroy`() {
        controller.create().start().resume().pause().stop().destroy()

        assertTrue(activity.isDestroyed)
        assertNull(viewModel.retroView)
    }

    @Test
    fun `onCreate poe a view do emulador na tela e registra os listeners de frame`() {
        controller.create()

        assertSame(retroView, viewModel.retroView)
        verify { retroView.registerFrameRenderedListener() }
        verify { retroView.registerFrameCallback() }
        val container = activity.findViewById<FrameLayout>(R.id.retroview_container)
        assertEquals(1, container.childCount)
        assertNotNull(activity.gameLifecycleObserverOrNull())
    }

    @Test
    fun `sem RetroView nao ha observador de ciclo de vida`() {
        assertNull(activity.gameLifecycleObserverOrNull())
    }

    @Test
    fun `onCreate pede o foco de audio, aplica a orientacao e liga captura e profiler`() {
        mockkObject(OrientationManager, ScreenshotCaptureUtil, AdvancedPerformanceProfiler)

        controller.create().start().resume().visible()
        idle()

        val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        assertNotNull(shadowOf(audioManager).lastAudioFocusRequest)
        verify { OrientationManager.applyConfigOrientation(activity, any()) }
        verify { ScreenshotCaptureUtil.setContext(activity) }
        verify { AdvancedPerformanceProfiler.startProfiling(activity) }
        verify { AdvancedPerformanceProfiler.showDebugOverlay(activity) }
    }

    @Test
    fun `onCreate entrega gamepads, containers e o menu ao ViewModel`() {
        val gamePads = mockk<GamePadInputController>(relaxed = true)
        setField(viewModel, "gamePadInputController", gamePads)

        controller.create()

        val left = activity.findViewById<FrameLayout>(R.id.left_container)
        val right = activity.findViewById<FrameLayout>(R.id.right_container)
        verify { gamePads.setupGamePads(activity, left, right) }
        assertNotNull(getField(viewModel, "retroMenu3Fragment"))
        assertSame(activity.findViewById<View>(R.id.menu_container), (getField(viewModel, "menuContainerViewRef") as java.lang.ref.WeakReference<*>).get())
        assertSame(activity.findViewById<View>(R.id.containers), (getField(viewModel, "gamePadContainerViewRef") as java.lang.ref.WeakReference<*>).get())
        assertNotNull(viewModel.navigationController?.onMenuClosedCallback)
    }

    @Test
    fun `onCreate registra o listener de rotacao automatica`() {
        controller.create()

        val rotation = getField(activity, "rotationController") as RotationController
        val receiver = getField(rotation, "rotationSettingsReceiver")
        assertTrue(shadowOf(activity.application).registeredReceivers.any { it.broadcastReceiver === receiver })
    }

    @Test
    fun `onCreate e cada mudanca de configuracao posicionam o gamepad e o tema das barras`() {
        val adjuster = mockk<GamePadLayoutAdjuster>(relaxed = true)
        val fab = mockk<FloatingMenuButtonController>(relaxed = true)
        setField(activity, "gamePadLayoutAdjuster", adjuster)
        setField(activity, "floatingMenuButtonController\$delegate", lazyOf(fab))
        mockkObject(SystemBarsAppearance)

        createWithMockPip()
        val gamePads = activity.findViewById<android.widget.LinearLayout>(R.id.containers)
        verify(exactly = 1) { adjuster.adjustPositionForOrientation(gamePads) }
        verify(exactly = 1) { fab.setup() }

        setField(activity, "rotationController", mockk<RotationController>(relaxed = true))
        val newConfig = Configuration(activity.resources.configuration)
        newConfig.uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
        activity.onConfigurationChanged(newConfig)

        verify(exactly = 2) { adjuster.adjustPositionForOrientation(gamePads) }
        verify { SystemBarsAppearance.apply(any(), newConfig.uiMode) }
    }

    @Test
    fun `pausar a activity pausa o emulador e guarda o estado depois do primeiro frame`() {
        createWithMockPip()
        controller.start().resume()
        val retroViewUtils = mockk<RetroViewUtils>(relaxed = true)
        setField(viewModel, "retroViewUtils", retroViewUtils)
        frameRendered.value = true

        controller.pause()

        verify { retroView.pause() }
        verify(exactly = 1) { retroViewUtils.preserveEmulatorState(retroView) }
    }

    @Test
    fun `a primeira entrada depois de retomar ja mede o tempo de frame`() {
        mockkObject(AdvancedPerformanceProfiler)
        createWithMockPip()
        controller.start().resume()

        mockKeyMotionRouter().also { every { it.processKeyEvent(any(), any()) } returns true }

        activity.onKeyDown(KeyEvent.KEYCODE_BUTTON_A, keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A))

        verify(exactly = 1) { AdvancedPerformanceProfiler.recordFrameTime(any()) }
    }

    @Test
    fun `os gamepads aparecem depois do primeiro frame de layout`() {
        controller.create().start().resume().visible()
        val gamePads = activity.findViewById<View>(R.id.containers)

        idle()

        assertEquals(View.VISIBLE, gamePads.visibility)
    }

    @Test
    fun `no primeiro frame renderizado prepara o still e os parametros de PiP`() {
        val pip = createWithMockPip()
        controller.start()

        frameRendered.value = true
        idle()

        verify { pip.maybeCapturePipFrame(force = true) }
        verify { pip.updatePictureInPictureParams() }
    }

    @Test
    fun `um frameRendered false nao mexe no PiP`() {
        val pip = createWithMockPip()
        controller.start()

        frameRendered.value = false
        idle()

        verify(exactly = 0) { pip.updatePictureInPictureParams() }
    }

    @Test
    fun `onPause e onResume avisam o PipController`() {
        val pip = createWithMockPip()

        controller.start().resume()
        controller.pause()

        verify(exactly = 1) { pip.onActivityResumed() }
        verify(exactly = 1) { pip.onActivityPaused() }
    }

    @Test
    fun `onUserLeaveHint avisa o PipController`() {
        val pip = createWithMockPip()
        controller.start().resume()

        controller.userLeaving()

        verify { pip.onUserLeaveHint() }
    }

    @Test
    fun `onPictureInPictureModeChanged repassa o modo para o PipController`() {
        val pip = createWithMockPip()

        activity.onPictureInPictureModeChanged(true, activity.resources.configuration)
        activity.onPictureInPictureModeChanged(false, activity.resources.configuration)

        verify { pip.onPictureInPictureModeChanged(true) }
        verify { pip.onPictureInPictureModeChanged(false) }
    }

    @Test
    fun `onDestroy libera o PipController`() {
        val pip = createWithMockPip()

        controller.destroy()

        verify { pip.dispose() }
    }

    @Test
    fun `o preview de load aparece com um bitmap e some sem ele`() {
        controller.create()
        val overlay = activity.findViewById<android.widget.ImageView>(R.id.load_preview_overlay)
        val bitmap = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888)

        viewModel.loadPreviewCallback?.invoke(bitmap)
        assertEquals(View.VISIBLE, overlay.visibility)

        viewModel.loadPreviewCallback?.invoke(null)
        assertEquals(View.GONE, overlay.visibility)
    }

    @Test
    fun `conectar ou remover um controle reavalia a visibilidade dos gamepads`() {
        controller.create()
        val watcher = activity.javaClass.getDeclaredField("inputDeviceWatcher").apply { isAccessible = true }.get(activity)
        val listener =
                watcher.javaClass.getDeclaredField("listener").apply { isAccessible = true }.get(watcher)
                        as android.hardware.input.InputManager.InputDeviceListener
        io.mockk.clearMocks(GamePad.Companion, answers = false)

        listener.onInputDeviceAdded(1)
        listener.onInputDeviceRemoved(1)
        listener.onInputDeviceChanged(1)

        verify(exactly = 3) { GamePad.shouldShowGamePads(activity, any()) }
    }

    // --- Rotacao ---

    @Test
    fun `onConfigurationChanged reaplica a orientacao, atualiza o PiP e recria o menu`() {
        val pip = createWithMockPip()
        val rotation = mockk<RotationController>(relaxed = true)
        setField(activity, "rotationController", rotation)
        val newConfig = Configuration(activity.resources.configuration)
        newConfig.orientation = Configuration.ORIENTATION_LANDSCAPE

        activity.onConfigurationChanged(newConfig)

        verify { rotation.reapplyOrientationIfNeeded(newConfig) }
        verify { pip.updatePictureInPictureParams() }
        verify { rotation.maybeRecreateMenuAfterRotation() }
    }

    @Test
    fun `em PiP onConfigurationChanged nao atualiza os parametros de PiP`() {
        val pip = createWithMockPip()
        setField(activity, "rotationController", mockk<RotationController>(relaxed = true))
        activity.enterPip(PictureInPictureParams.Builder().setAspectRatio(Rational(4, 3)).build())

        activity.onConfigurationChanged(Configuration(activity.resources.configuration))

        verify(exactly = 0) { pip.updatePictureInPictureParams() }
    }

    // --- Estado salvo ---

    @Test
    fun `onSaveInstanceState e onRestoreInstanceState passam pelo NavigationController`() {
        controller.create().start()
        val navigation = mockk<NavigationController>(relaxed = true)
        viewModel.navigationController = navigation
        val bundle = Bundle()

        controller.saveInstanceState(bundle)
        controller.stop()
        val restored = Robolectric.buildActivity(GameActivity::class.java)
        val restoredViewModel = ViewModelProvider(restored.get())[GameActivityViewModel::class.java]
        restoredViewModel.retroViewFactory = { retroView }
        restored.create(bundle)
        restoredViewModel.navigationController = navigation
        restored.start().restoreInstanceState(bundle)

        verify { navigation.saveState(bundle) }
        verify { navigation.restoreState(bundle) }
    }

    @Test
    fun `sem NavigationController salvar e restaurar o estado nao falha`() {
        controller.create().start()
        viewModel.navigationController = null
        val bundle = Bundle()

        controller.saveInstanceState(bundle).restoreInstanceState(bundle)

        assertFalse(activity.isFinishing)
    }

    // --- Entrada ---

    @Test
    fun `onKeyDown usa a resposta do ViewModel e captura um frame para o PiP`() {
        val pip = createWithMockPip()
        val router = mockKeyMotionRouter()
        every { router.processKeyEvent(KeyEvent.KEYCODE_BUTTON_A, any()) } returns true

        val handled = activity.onKeyDown(KeyEvent.KEYCODE_BUTTON_A, keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A))

        assertTrue(handled)
        verify { pip.maybeCapturePipFrame() }
    }

    @Test
    fun `onKeyDown sem resposta do ViewModel cai no comportamento padrao`() {
        createWithMockPip()
        val router = mockKeyMotionRouter()
        every { router.processKeyEvent(any(), any()) } returns null

        val handled = activity.onKeyDown(KeyEvent.KEYCODE_A, keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A))

        assertFalse(handled)
    }

    @Test
    fun `onKeyUp usa a resposta do ViewModel ou o comportamento padrao`() {
        createWithMockPip()
        val router = mockKeyMotionRouter()
        every { router.processKeyEvent(KeyEvent.KEYCODE_BUTTON_A, any()) } returns true
        every { router.processKeyEvent(KeyEvent.KEYCODE_A, any()) } returns null

        assertTrue(activity.onKeyUp(KeyEvent.KEYCODE_BUTTON_A, keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A)))
        assertFalse(activity.onKeyUp(KeyEvent.KEYCODE_A, keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A)))
    }

    @Test
    fun `onGenericMotionEvent usa a resposta do ViewModel ou o comportamento padrao`() {
        val pip = createWithMockPip()
        val router = mockKeyMotionRouter()
        val event = motionEvent()
        every { router.processMotionEvent(event) } returnsMany listOf(true, null)

        assertTrue(activity.onGenericMotionEvent(event))
        assertFalse(activity.onGenericMotionEvent(event))
        verify(exactly = 2) { pip.maybeCapturePipFrame() }
    }

    @Test
    fun `dispatchTouchEvent captura um frame para o PiP e segue para a view`() {
        val pip = createWithMockPip()
        controller.start().resume().visible()
        val event = motionEvent()

        activity.dispatchTouchEvent(event)

        verify { pip.maybeCapturePipFrame() }
    }

    // --- Botao voltar ---

    @Test
    fun `voltar com o menu aberto envia um evento de navegacao`() {
        controller.create().start()
        val navigation = mockk<NavigationController>(relaxed = true)
        every { navigation.isMenuActive() } returns true
        viewModel.navigationController = navigation

        activity.onBackPressedDispatcher.onBackPressed()

        verify { navigation.handleNavigationEvent(any<NavigationEvent>()) }
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `voltar sem menu e com back no menu_mode abre o menu`() {
        controller.create().start()
        val navigation = mockk<NavigationController>(relaxed = true)
        every { navigation.isMenuActive() } returns false
        viewModel.navigationController = navigation
        every { mockKeyMotionRouter().shouldHandleBackButton() } returns true

        activity.onBackPressedDispatcher.onBackPressed()

        verify { navigation.handleNavigationEvent(any<NavigationEvent.OpenMenu>()) }
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `voltar sem menu e sem back no menu_mode segue o comportamento da plataforma`() {
        controller.create().start()
        val navigation = mockk<NavigationController>(relaxed = true)
        every { navigation.isMenuActive() } returns false
        viewModel.navigationController = navigation
        every { mockKeyMotionRouter().shouldHandleBackButton() } returns false

        activity.onBackPressedDispatcher.onBackPressed()

        verify(exactly = 0) { navigation.handleNavigationEvent(any()) }
        assertTrue(activity.isFinishing)
    }

    // --- Janela ---

    @Test
    fun `aplicar insets na janela volta a esconder as barras do sistema`() {
        controller.create()

        activity.window.decorView.dispatchApplyWindowInsets(WindowInsets.Builder().build())
        idle()

        assertFalse(activity.isFinishing)
    }

    // --- PipHost ---

    @Test
    fun `activity e isCurrentlyInPip refletem a propria Activity`() {
        controller.create()

        assertSame(activity, activity.activity)
        assertFalse(activity.isCurrentlyInPip())
        assertTrue(activity.enterPip(PictureInPictureParams.Builder().build()))
        assertTrue(activity.isCurrentlyInPip())
    }

    @Test
    fun `updatePipParams aplica os parametros sem falhar`() {
        controller.create()

        activity.updatePipParams(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())

        assertFalse(activity.isFinishing)
    }

    @Test
    fun `registerPipReceiver e unregisterPipReceiver registram e removem o receiver`() {
        controller.create()
        val receiver = mockk<BroadcastReceiver>(relaxed = true)
        val app = shadowOf(activity.application)
        val before = app.registeredReceivers.size

        activity.registerPipReceiver(receiver, IntentFilter("test.ACTION"))
        assertEquals(before + 1, app.registeredReceivers.size)

        activity.unregisterPipReceiver(receiver)
        assertEquals(before, app.registeredReceivers.size)
    }

    @Test
    @Config(sdk = [33])
    fun `a partir do Android 13 o receiver de PiP e registrado como nao exportado`() {
        controller.create()
        val receiver = mockk<BroadcastReceiver>(relaxed = true)

        activity.registerPipReceiver(receiver, IntentFilter("test.ACTION"))

        val wrapper = shadowOf(activity.application).registeredReceivers.last { it.broadcastReceiver === receiver }
        assertTrue(wrapper.flags and Context.RECEIVER_NOT_EXPORTED != 0)
        activity.unregisterPipReceiver(receiver)
    }

    @Test
    fun `bringTaskToFront reabre a Activity trazendo a tarefa para frente`() {
        controller.create()

        activity.bringTaskToFront()

        val started = shadowOf(activity).nextStartedActivity
        assertEquals(GameActivity::class.java.name, started.component?.className)
        assertTrue(started.flags and Intent.FLAG_ACTIVITY_REORDER_TO_FRONT != 0)
    }

    @Test
    fun `finishPipTask encerra a tarefa`() {
        controller.create()

        activity.finishPipTask()

        assertTrue(activity.isFinishing)
    }

    @Test
    fun `postToUiThread executa a acao na thread de UI`() {
        controller.create()
        var ran = false

        activity.postToUiThread { ran = true }
        idle()

        assertTrue(ran)
    }

    // --- Botao flutuante ---

    @Test
    fun `fadeFloatingButtonImmediately e restoreFloatingButtonVisibility mudam o alpha do botao`() {
        controller.create()
        val button = activity.findViewById<View>(R.id.floating_menu_button)
        button.visibility = View.VISIBLE

        activity.fadeFloatingButtonImmediately()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        val dimmed = button.alpha
        activity.restoreFloatingButtonVisibility()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))

        assertTrue(dimmed < 1f)
        assertEquals(1f, button.alpha)
    }

    // --- Animacao de desligar ---

    @Test
    fun `startShutdownAnimation chama o callback ao terminar`() {
        controller.create()
        var completed = false

        activity.startShutdownAnimation { completed = true }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))

        assertTrue(completed)
    }
}
