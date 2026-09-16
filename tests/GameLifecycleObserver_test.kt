package com.vinaooo.revenger.managers

import androidx.fragment.app.FragmentActivity
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.retroview.RetroView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * GameLifecycleObserver concentra a máquina de estados de pausa/retomada durante o modo
 * Picture-in-Picture. Vários bugs de PiP já corrigidos no histórico do projeto (congelamento de
 * frame, transições incompletas) vivem exatamente nessa interação entre
 * [GameLifecycleObserver.onPause]/[GameLifecycleObserver.onResume] e o estado de PiP da Activity.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GameLifecycleObserverTest {

    private lateinit var glRetroView: GLRetroView
    private lateinit var retroView: RetroView
    private lateinit var observer: GameLifecycleObserver
    private lateinit var activity: FragmentActivity

    @Before
    fun setUp() {
        glRetroView = mockk(relaxed = true)
        retroView = mockk(relaxed = true)
        every { retroView.view } returns glRetroView

        observer = GameLifecycleObserver(retroView)
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
    }

    @Test
    fun `onResume fora do PiP retoma a emulacao`() {
        observer.onResume(activity)

        verify { retroView.resume() }
    }

    @Test
    fun `onResume durante o PiP nao retoma a emulacao`() {
        @Suppress("DEPRECATION") activity.enterPictureInPictureMode()

        observer.onResume(activity)

        verify(exactly = 0) { retroView.resume() }
    }

    @Test
    fun `onPause fora do PiP pausa a emulacao`() {
        observer.onPause(activity)

        verify { retroView.pause() }
    }

    @Test
    fun `onPause durante o PiP nao pausa a emulacao`() {
        @Suppress("DEPRECATION") activity.enterPictureInPictureMode()

        observer.onPause(activity)

        verify(exactly = 0) { retroView.pause() }
    }

    @Test
    fun `onPause com transicao de PiP pendente nao pausa a emulacao`() {
        observer.prepareForPipTransition()

        observer.onPause(activity)

        verify(exactly = 0) { retroView.pause() }
    }

    @Test
    fun `clearPendingPipTransition permite pausa normal apos preparar uma transicao`() {
        observer.prepareForPipTransition()
        observer.clearPendingPipTransition()

        observer.onPause(activity)

        verify { retroView.pause() }
    }

    @Test
    fun `onEnteredPictureInPicture congela o frame do core`() {
        observer.onEnteredPictureInPicture()

        verify { glRetroView.frameSpeed = 0 }
    }

    @Test
    fun `onExitedPictureInPicture restaura o frame apos ter entrado em PiP`() {
        observer.onEnteredPictureInPicture()

        observer.onExitedPictureInPicture()

        verifyOrder {
            glRetroView.frameSpeed = 0
            glRetroView.frameSpeed = 1
        }
    }

    @Test
    fun `onExitedPictureInPicture sem ter entrado em PiP antes nao mexe no frameSpeed`() {
        observer.onExitedPictureInPicture()

        verify(exactly = 0) { glRetroView.frameSpeed = any() }
    }

    @Test
    fun `onResume apos sair do PiP restaura o frameSpeed sem chamar resume diretamente`() {
        observer.onEnteredPictureInPicture()

        observer.onResume(activity)

        verify { glRetroView.frameSpeed = 1 }
        verify(exactly = 0) { retroView.resume() }
    }

    @Test
    fun `onDestroy libera os recursos do core`() {
        observer.onDestroy(activity)

        verify { retroView.destroy() }
    }
}
