package com.vinaooo.revenger.input

import android.view.MotionEvent
import androidx.lifecycle.MutableLiveData
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.retroview.RetroView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MotionEventRouter] (reached through [ControllerInput.processMotionEvent]) with the menu open:
 * the DPAD hat navigates past 0.1, the left stick past 0.7, each direction once per push; values
 * exactly on a threshold count as centered and go on to the core.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MotionEventRouter_test {

    private lateinit var input: ControllerInput
    private lateinit var retroView: RetroView
    private lateinit var glRetroView: GLRetroView
    private val fired = mutableListOf<String>()

    @Before
    fun setUp() {
        input = ControllerInput()
        input.shouldInterceptDpadForMenu = { true }
        input.menuNavigateUpCallback = { fired += "up" }
        input.menuNavigateDownCallback = { fired += "down" }
        input.menuNavigateLeftCallback = { fired += "left" }
        input.menuNavigateRightCallback = { fired += "right" }
        glRetroView = mockk(relaxed = true)
        retroView = mockk(relaxed = true)
        every { retroView.view } returns glRetroView
        every { retroView.frameRendered } returns MutableLiveData(true)
    }

    private fun move(hatX: Float = 0f, hatY: Float = 0f, axisX: Float = 0f, axisY: Float = 0f): Boolean? {
        val event = mockk<MotionEvent>(relaxed = true)
        every { event.getAxisValue(MotionEvent.AXIS_HAT_X) } returns hatX
        every { event.getAxisValue(MotionEvent.AXIS_HAT_Y) } returns hatY
        every { event.getAxisValue(MotionEvent.AXIS_X) } returns axisX
        every { event.getAxisValue(MotionEvent.AXIS_Y) } returns axisY
        return input.processMotionEvent(event, retroView)
    }

    private fun sentToCore(times: Int) =
            verify(exactly = times) { glRetroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, any(), any(), any()) }

    @Test
    fun `cada direcao do DPAD navega uma vez por toque`() {
        move(hatY = 1f)
        move()
        move(hatX = -1f)
        move()
        move(hatX = 1f)

        assertEquals(listOf("down", "left", "right"), fired)
        sentToCore(times = 2) // the two centered events
    }

    @Test
    fun `cada direcao do analogico esquerdo navega uma vez por toque`() {
        move(axisY = -1f)
        move()
        move(axisY = 1f)
        move()
        move(axisX = -1f)
        move()
        move(axisX = 1f)

        assertEquals(listOf("up", "down", "left", "right"), fired)
    }

    @Test
    fun `DPAD e analogico juntos navegam pelo DPAD`() {
        move(hatY = -1f, axisY = 1f)

        assertEquals(listOf("up"), fired)
    }

    @Test
    fun `DPAD exatamente no limite conta como centralizado e vai para o jogo`() {
        assertEquals(true, move(hatX = DPAD_THRESHOLD, hatY = -DPAD_THRESHOLD))

        assertTrue(fired.isEmpty())
        sentToCore(times = 1)
    }

    @Test
    fun `DPAD logo acima do limite navega`() {
        move(hatY = DPAD_THRESHOLD + STEP)

        assertEquals(listOf("down"), fired)
        sentToCore(times = 0)
    }

    @Test
    fun `analogico exatamente no limite conta como centralizado e vai para o jogo`() {
        assertEquals(true, move(axisX = -STICK_THRESHOLD, axisY = STICK_THRESHOLD))

        assertTrue(fired.isEmpty())
        sentToCore(times = 1)
    }

    @Test
    fun `analogico logo acima do limite navega`() {
        move(axisX = STICK_THRESHOLD + STEP)

        assertEquals(listOf("right"), fired)
    }

    @Test
    fun `analogico mantido fora da zona morta e consumido sem navegar de novo`() {
        move(axisX = -1f)

        assertEquals(true, move(axisX = -1f))

        assertEquals(listOf("left"), fired)
        sentToCore(times = 0)
    }

    @Test
    fun `analogico pequeno demais para navegar vai para o jogo`() {
        assertEquals(true, move(axisY = 0.5f))

        assertTrue(fired.isEmpty())
        sentToCore(times = 1)
    }

    private companion object {
        const val DPAD_THRESHOLD = 0.1f
        const val STICK_THRESHOLD = 0.7f
        const val STEP = 0.01f
    }
}
