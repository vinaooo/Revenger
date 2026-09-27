package com.vinaooo.revenger.controllers

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.GameLifecycleObserver
import com.vinaooo.revenger.retroview.RetroView
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [PipController] owns Picture-in-Picture for `GameActivity`. These tests pin its decisions:
 * when PiP may be entered (first frame rendered, PiP enabled, menu closed), SDK S+ auto-enter vs.
 * direct `enterPictureInPictureMode`, the cleanup when entry is rejected or fails, what entering
 * and leaving PiP hides and restores, the two PiP action buttons (Quick Save runs once the
 * activity resumes, Save and Exit opens the save menu once PiP is left), and the stuck-overlay
 * cleanup on resume. The quick-save executor itself is covered by `PipQuickSaveExecutor_test`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PipController_test {

    private class FakePipHost(override val activity: FragmentActivity) : PipHost {
        var isInPip = false
        var enterPipCalls = mutableListOf<PictureInPictureParams>()
        var enterPipResult = true
        var enterPipError: RuntimeException? = null
        var updatePipParamsError: RuntimeException? = null
        var updatePipParamsCalls = 0
        var registeredReceiver: BroadcastReceiver? = null
        var unregisteredReceiver: BroadcastReceiver? = null
        var bringTaskToFrontCalls = 0
        var finishPipTaskCalls = 0
        var restoreFloatingButtonVisibilityCalls = 0
        var gameLifecycleObserver: GameLifecycleObserver? = null

        override fun isCurrentlyInPip(): Boolean = isInPip

        override fun enterPip(params: PictureInPictureParams): Boolean {
            enterPipCalls.add(params)
            enterPipError?.let { throw it }
            return enterPipResult
        }

        override fun updatePipParams(params: PictureInPictureParams) {
            updatePipParamsCalls++
            updatePipParamsError?.let { throw it }
        }

        override fun registerPipReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
            registeredReceiver = receiver
        }

        override fun unregisterPipReceiver(receiver: BroadcastReceiver) {
            // Mirrors the real Android contract: unregistering a receiver that isn't currently
            // registered throws IllegalArgumentException. PipController.dispose() relies on this
            // to safely no-op when PiP was never entered this session.
            require(registeredReceiver != null) { "Receiver not registered" }
            unregisteredReceiver = receiver
            registeredReceiver = null
        }

        override fun bringTaskToFront() {
            bringTaskToFrontCalls++
        }

        override fun finishPipTask() {
            finishPipTaskCalls++
        }

        override fun postToUiThread(action: () -> Unit) = action()

        override fun restoreFloatingButtonVisibility() {
            restoreFloatingButtonVisibilityCalls++
        }

        override fun gameLifecycleObserverOrNull(): GameLifecycleObserver? = gameLifecycleObserver
    }

    private lateinit var host: FakePipHost
    private lateinit var viewModel: GameActivityViewModel
    private lateinit var appConfig: AppConfig
    private lateinit var retroView: RetroView
    private lateinit var glRetroView: GLRetroView
    private lateinit var views: PipViews
    private lateinit var quickSaveExecutor: PipQuickSaveExecutor
    private lateinit var controller: PipController

    @Before
    fun setUp() {
        mockkObject(ScreenshotCaptureUtil)
        every { ScreenshotCaptureUtil.getPipFrame() } returns null
        every { ScreenshotCaptureUtil.getCachedFullScreenshot() } returns null
        every { ScreenshotCaptureUtil.getCachedScreenshot() } returns null
        every { ScreenshotCaptureUtil.capturePipFrame(any(), any()) } returns Unit

        val context = ApplicationProvider.getApplicationContext<Context>()
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        host = FakePipHost(activity)

        glRetroView = mockk(relaxed = true)
        retroView = mockk(relaxed = true)
        every { retroView.view } returns glRetroView
        every { retroView.frameRendered } returns MutableLiveData(true)

        viewModel = mockk(relaxed = true)
        every { viewModel.retroView } returns retroView
        every { viewModel.isAnyMenuActive() } returns false

        appConfig = mockk(relaxed = true)
        every { appConfig.isPipEnabled() } returns true

        views = PipViews(
                retroviewContainer = FrameLayout(context),
                pipOverlay = ImageView(context),
                menuContainer = FrameLayout(context),
                leftContainer = FrameLayout(context),
                rightContainer = FrameLayout(context)
        )

        quickSaveExecutor = mockk(relaxed = true)
        controller = PipController(host, viewModel, appConfig, views, quickSaveExecutor)
    }

    @After
    fun tearDown() {
        unmockkObject(ScreenshotCaptureUtil)
    }

    @Test
    fun `onUserLeaveHint antes do primeiro frame nao entra em PiP`() {
        every { retroView.frameRendered } returns MutableLiveData(false)

        controller.onUserLeaveHint()

        assertTrue(host.enterPipCalls.isEmpty())
        assertEquals(0, host.updatePipParamsCalls)
    }

    @Test
    fun `onUserLeaveHint com PiP desabilitado nao entra em PiP`() {
        every { appConfig.isPipEnabled() } returns false

        controller.onUserLeaveHint()

        assertTrue(host.enterPipCalls.isEmpty())
        assertEquals(0, host.updatePipParamsCalls)
    }

    @Test
    fun `onUserLeaveHint com menu ativo so entra em PiP depois do menu fechar`() {
        every { viewModel.isAnyMenuActive() } returns true
        val onAnimationEnd = slotCallback()

        controller.onUserLeaveHint()

        assertTrue("nao deveria entrar em PiP antes do callback de fechamento", host.enterPipCalls.isEmpty())

        // The menu has now actually closed by the time dismissRetroMenu3's callback fires.
        every { viewModel.isAnyMenuActive() } returns false
        onAnimationEnd()

        assertEquals(1, host.enterPipCalls.size)
    }

    // Captures the onAnimationEnd lambda passed to viewModel.dismissRetroMenu3 and returns it.
    private fun slotCallback(): () -> Unit {
        var captured: (() -> Unit)? = null
        every { viewModel.dismissRetroMenu3(any()) } answers {
            captured = firstArg()
        }
        return { captured?.invoke() }
    }

    @Test
    @Config(sdk = [31])
    fun `onUserLeaveHint no Android S+ arma o auto-enter em vez de chamar enterPictureInPictureMode`() {
        controller.onUserLeaveHint()

        assertTrue(host.enterPipCalls.isEmpty())
        assertEquals(1, host.updatePipParamsCalls)
    }

    @Test
    @Config(sdk = [30])
    fun `onUserLeaveHint antes do Android S entra diretamente em PiP`() {
        controller.onUserLeaveHint()

        assertEquals(1, host.enterPipCalls.size)
    }

    @Test
    fun `quando o SO rejeita a entrada em PiP o overlay e limpo e a transicao pendente e cancelada`() {
        host.enterPipResult = false
        val observer = mockk<GameLifecycleObserver>(relaxed = true)
        host.gameLifecycleObserver = observer
        views.pipOverlay.visibility = View.VISIBLE

        controller.onUserLeaveHint()

        assertEquals(View.GONE, views.pipOverlay.visibility)
        verify { observer.clearPendingPipTransition() }
    }

    @Test
    fun `quando o SO aceita a entrada em PiP a transicao e preparada e nao cancelada`() {
        val observer = mockk<GameLifecycleObserver>(relaxed = true)
        host.gameLifecycleObserver = observer

        controller.onUserLeaveHint()

        verify { observer.prepareForPipTransition() }
        verify(exactly = 0) { observer.clearPendingPipTransition() }
    }

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `onPictureInPictureModeChanged true registra o receiver e esconde o menu`() {
        controller.onPictureInPictureModeChanged(true)

        assertEquals(View.GONE, views.menuContainer.visibility)
        assertTrue(host.registeredReceiver != null)
    }

    @Test
    fun `Save and Exit abre o menu de save-slots apenas ao sair do PiP, uma unica vez`() {
        val navigationController = mockk<com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController>(relaxed = true)
        every { viewModel.navigationController } returns navigationController

        controller.onPictureInPictureModeChanged(true)
        val receiver = requireNotNull(host.registeredReceiver)
        receiver.onReceive(context(), Intent(PipController.ACTION_PIP_SAVE))

        controller.onPictureInPictureModeChanged(false)

        // OpenMenu carries a `timestamp` defaulted to the construction time, so an exact-value
        // match would flake on the millisecond -- match on the fields that matter instead.
        verify(exactly = 1) {
            navigationController.handleNavigationEvent(
                    match<com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent> {
                        it is com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent.OpenMenu &&
                                it.inputSource == com.vinaooo.revenger.ui.retromenu3.navigation.InputSource.TOUCH &&
                                it.targetMenu == com.vinaooo.revenger.ui.retromenu3.navigation.MenuType.EXIT_SAVE_SLOTS
                    }
            )
        }

        // A second PiP enter/exit cycle without a new ACTION_PIP_SAVE must not reopen the menu.
        controller.onPictureInPictureModeChanged(true)
        controller.onPictureInPictureModeChanged(false)

        verify(exactly = 1) { navigationController.handleNavigationEvent(any()) }
    }

    @Test
    fun `onPictureInPictureModeChanged false restaura o menu e desregistra o receiver`() {
        controller.onPictureInPictureModeChanged(true)
        val receiver = host.registeredReceiver

        controller.onPictureInPictureModeChanged(false)

        assertEquals(View.VISIBLE, views.menuContainer.visibility)
        assertEquals(receiver, host.unregisteredReceiver)
        assertEquals(1, host.restoreFloatingButtonVisibilityCalls)
    }

    @Test
    fun `receiver de PIP_QUICK_SAVE traz a task para frente e o save roda uma vez no resume`() {
        controller.onPictureInPictureModeChanged(true)
        val receiver = requireNotNull(host.registeredReceiver)

        receiver.onReceive(context(), Intent(PipController.ACTION_PIP_QUICK_SAVE))

        assertEquals(1, host.bringTaskToFrontCalls)
        verify(exactly = 0) { quickSaveExecutor.execute() }

        controller.onActivityResumed()
        controller.onActivityResumed()

        verify(exactly = 1) { quickSaveExecutor.execute() }
    }

    @Test
    fun `resume sem Quick Save pendente nao salva`() {
        controller.onActivityResumed()

        verify(exactly = 0) { quickSaveExecutor.execute() }
    }

    @Test
    fun `receiver de PIP_SAVE com frame guarda o frame como screenshot do save e suprime a proxima captura`() {
        val frame = Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888)
        every { ScreenshotCaptureUtil.getPipFrame() } returns frame
        every { ScreenshotCaptureUtil.setManualScreenshots(any(), any()) } returns Unit
        controller.onPictureInPictureModeChanged(true)

        requireNotNull(host.registeredReceiver).onReceive(context(), Intent(PipController.ACTION_PIP_SAVE))

        verify {
            ScreenshotCaptureUtil.setManualScreenshots(
                    match { it !== frame && it.width == 4 && it.height == 3 },
                    any()
            )
        }
        verify { viewModel.suppressNextScreenshotCapture = true }
        assertEquals(1, host.bringTaskToFrontCalls)
    }

    @Test
    fun `receiver de PIP_SAVE sem frame nao troca os screenshots`() {
        controller.onPictureInPictureModeChanged(true)

        requireNotNull(host.registeredReceiver).onReceive(context(), Intent(PipController.ACTION_PIP_SAVE))

        verify(exactly = 0) { ScreenshotCaptureUtil.setManualScreenshots(any(), any()) }
        verify(exactly = 0) { viewModel.suppressNextScreenshotCapture = any() }
        assertEquals(1, host.bringTaskToFrontCalls)
    }

    @Test
    fun `receiver ignora acoes desconhecidas`() {
        controller.onPictureInPictureModeChanged(true)

        requireNotNull(host.registeredReceiver).onReceive(context(), Intent("some.other.ACTION"))
        controller.onActivityResumed()

        assertEquals(0, host.bringTaskToFrontCalls)
        verify(exactly = 0) { quickSaveExecutor.execute() }
    }

    @Test
    fun `resume fora do PiP limpa um overlay que ficou visivel`() {
        views.pipOverlay.visibility = View.VISIBLE

        controller.onActivityResumed()

        assertEquals(View.GONE, views.pipOverlay.visibility)
    }

    @Test
    fun `resume ainda em PiP mantem o overlay`() {
        host.isInPip = true
        views.pipOverlay.visibility = View.VISIBLE

        controller.onActivityResumed()

        assertEquals(View.VISIBLE, views.pipOverlay.visibility)
    }

    @Test
    fun `entrar em PiP mostra o ultimo frame no overlay`() {
        val frame = Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888)
        every { ScreenshotCaptureUtil.getPipFrame() } returns frame

        controller.onPictureInPictureModeChanged(true)

        assertEquals(View.VISIBLE, views.pipOverlay.visibility)
    }

    @Test
    fun `sem nenhum frame disponivel o overlay continua escondido`() {
        views.pipOverlay.visibility = View.GONE

        controller.onPictureInPictureModeChanged(true)

        assertEquals(View.GONE, views.pipOverlay.visibility)
    }

    @Test
    fun `o gamepad visivel e escondido no PiP e volta ao sair`() {
        val (containers, floatingButton) = installActivityViews(containersVisibility = View.VISIBLE)

        controller.onPictureInPictureModeChanged(true)

        assertEquals(View.INVISIBLE, containers.visibility)
        assertEquals(View.GONE, floatingButton.visibility)

        controller.onPictureInPictureModeChanged(false)

        assertEquals(View.VISIBLE, containers.visibility)
    }

    @Test
    fun `o gamepad escondido antes do PiP continua escondido ao sair`() {
        val (containers, _) = installActivityViews(containersVisibility = View.GONE)

        controller.onPictureInPictureModeChanged(true)
        controller.onPictureInPictureModeChanged(false)

        assertEquals(View.GONE, containers.visibility)
    }

    @Test
    fun `sair do PiP sem receiver registrado nao lanca e restaura o menu`() {
        controller.onPictureInPictureModeChanged(false)

        assertEquals(View.VISIBLE, views.menuContainer.visibility)
        assertEquals(1, host.restoreFloatingButtonVisibilityCalls)
    }

    @Test
    fun `se entrar em PiP lanca o overlay e limpo e a transicao pendente e cancelada`() {
        host.enterPipError = IllegalStateException("activity not eligible")
        val observer = mockk<GameLifecycleObserver>(relaxed = true)
        host.gameLifecycleObserver = observer
        every { ScreenshotCaptureUtil.getPipFrame() } returns Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888)

        controller.onUserLeaveHint()

        assertEquals(View.GONE, views.pipOverlay.visibility)
        verify { observer.clearPendingPipTransition() }
    }

    @Test
    fun `se o primeiro frame some enquanto o menu fecha a entrada em PiP e abortada`() {
        every { viewModel.isAnyMenuActive() } returns true
        val onMenuClosed = slotCallback()
        controller.onUserLeaveHint()

        every { viewModel.isAnyMenuActive() } returns false
        every { retroView.frameRendered } returns MutableLiveData(false)
        onMenuClosed()

        assertTrue(host.enterPipCalls.isEmpty())
    }

    @Test
    fun `se o menu reabre antes do callback a entrada em PiP e abortada`() {
        every { viewModel.isAnyMenuActive() } returns true
        val onMenuClosed = slotCallback()
        controller.onUserLeaveHint()

        onMenuClosed()

        assertTrue(host.enterPipCalls.isEmpty())
    }

    @Test
    @Config(sdk = [31])
    fun `updatePictureInPictureParams antes do primeiro frame nao atualiza`() {
        every { retroView.frameRendered } returns MutableLiveData(false)

        controller.updatePictureInPictureParams()

        assertEquals(0, host.updatePipParamsCalls)
    }

    @Test
    @Config(sdk = [31])
    fun `updatePictureInPictureParams engole as falhas documentadas do SO`() {
        host.updatePipParamsError = IllegalStateException("activity not visible")
        controller.updatePictureInPictureParams()

        host.updatePipParamsError = IllegalArgumentException("invalid aspect ratio")
        controller.updatePictureInPictureParams()

        assertEquals(2, host.updatePipParamsCalls)
    }

    @Test
    fun `onActivityPaused forca a captura do frame do PiP`() {
        controller.onActivityPaused()

        verify { ScreenshotCaptureUtil.capturePipFrame(glRetroView, force = true) }
    }

    @Test
    fun `maybeCapturePipFrame nao captura antes do primeiro frame`() {
        every { retroView.frameRendered } returns MutableLiveData(false)

        controller.maybeCapturePipFrame(force = true)

        verify(exactly = 0) { ScreenshotCaptureUtil.capturePipFrame(any(), any()) }
    }

    @Test
    fun `maybeCapturePipFrame nao captura com PiP desabilitado`() {
        every { appConfig.isPipEnabled() } returns false

        controller.maybeCapturePipFrame(force = true)

        verify(exactly = 0) { ScreenshotCaptureUtil.capturePipFrame(any(), any()) }
    }

    // Gives the activity the two views PipController looks up by id, and returns them.
    private fun installActivityViews(containersVisibility: Int): Pair<View, View> {
        val activity = host.activity
        val containers = FrameLayout(activity).apply {
            id = R.id.containers
            visibility = containersVisibility
        }
        val floatingButton = View(activity).apply { id = R.id.floating_menu_button }
        activity.setContentView(FrameLayout(activity).apply {
            addView(containers)
            addView(floatingButton)
        })
        return containers to floatingButton
    }

    @Test
    fun `maybeCapturePipFrame nao captura quando ja esta em PiP`() {
        host.isInPip = true

        controller.maybeCapturePipFrame(force = true)

        verify(exactly = 0) { ScreenshotCaptureUtil.capturePipFrame(any(), any()) }
    }

    @Test
    fun `maybeCapturePipFrame captura quando fora do PiP e com PiP habilitado`() {
        controller.maybeCapturePipFrame(force = true)

        verify { ScreenshotCaptureUtil.capturePipFrame(glRetroView, force = true) }
    }

    @Test
    fun `dispose desregistra o receiver de PiP e limpa o overlay`() {
        controller.onPictureInPictureModeChanged(true)
        val receiver = requireNotNull(host.registeredReceiver)

        controller.dispose()

        assertEquals(receiver, host.unregisteredReceiver)
        assertEquals(View.GONE, views.pipOverlay.visibility)
    }

    @Test
    fun `dispose nao lanca excecao quando o receiver nunca foi registrado`() {
        controller.dispose()

        assertEquals(View.GONE, views.pipOverlay.visibility)
    }
}
