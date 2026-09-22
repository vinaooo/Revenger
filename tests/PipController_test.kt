package com.vinaooo.revenger.controllers

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.AppConfig
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
 * [PipController] owns the two functions that used to live on `GameActivity` and were flagged by
 * detekt's `ReturnCount` rule (`onUserLeaveHint`, `maybeEnterPictureInPictureAfterMenuClosed`),
 * restructured here to 2 raw `return`s each. These tests pin the branching those functions
 * perform -- SDK S+ auto-enter vs. direct `enterPictureInPictureMode`, the OS-rejects-entry
 * cleanup path, and the deferred-until-menu-closed flow -- so that restructuring cannot silently
 * change behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PipController_test {

    private class FakePipHost(override val activity: FragmentActivity) : PipHost {
        var isInPip = false
        var enterPipCalls = mutableListOf<PictureInPictureParams>()
        var enterPipResult = true
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
            return enterPipResult
        }

        override fun updatePipParams(params: PictureInPictureParams) {
            updatePipParamsCalls++
        }

        override fun registerPipReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
            registeredReceiver = receiver
        }

        override fun unregisterPipReceiver(receiver: BroadcastReceiver) {
            // Mirrors the real Android contract: unregistering a receiver that isn't currently
            // registered throws IllegalArgumentException. PipController.dispose() relies on this
            // to safely no-op when PiP was never entered this session.
            if (registeredReceiver == null) {
                throw IllegalArgumentException("Receiver not registered")
            }
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

        controller = PipController(host, viewModel, appConfig, views)
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
    fun `receiver de PIP_QUICK_SAVE agenda quick save e traz a task para frente`() {
        controller.onPictureInPictureModeChanged(true)
        val receiver = requireNotNull(host.registeredReceiver)

        receiver.onReceive(context(), Intent(PipController.ACTION_PIP_QUICK_SAVE))

        assertEquals(1, host.bringTaskToFrontCalls)

        // onActivityResumed() should now run the deferred quick save; with retroView present it
        // launches a coroutine rather than finishing immediately, so just assert it was consumed
        // (pendingPipQuickSave is private, so this is observed indirectly via no double-trigger).
        every { viewModel.retroView } returns null
        controller.onActivityResumed()

        assertEquals(1, host.finishPipTaskCalls)
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
