package com.vinaooo.revenger.gamepad

import android.app.Activity
import android.app.Service
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.radialgamepad.library.config.CrossConfig
import com.swordfish.radialgamepad.library.config.PrimaryDialConfig
import com.swordfish.radialgamepad.library.config.RadialGamePadConfig
import com.swordfish.radialgamepad.library.event.Event
import com.vinaooo.revenger.AppConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [GamePad.shouldShowGamePads] (deve mostrar o gamepad virtual?) e [GamePad.eventHandler], que
 * passa cada evento do pad na tela pelo callback de interceptação e depois para o core. O pad é
 * um `RadialGamePad` real com um layout mínimo (um D-pad); só `subscribe` fica de
 * fora, porque o fluxo de eventos do pad vem de toques na tela.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GamePad_test {

    @After
    fun tearDown() {
        unmockkStatic(InputDevice::class)
    }

    private fun mockActivity(
        gamepadHasTouchScreen: Boolean = true,
        isPresentationDisplay: Boolean = false,
    ): Activity {
        val packageManager = mockk<PackageManager>()
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) } returns
            gamepadHasTouchScreen

        val display = mockk<Display>()
        every { display.displayId } returns if (isPresentationDisplay) 1 else Display.DEFAULT_DISPLAY
        every { display.flags } returns if (isPresentationDisplay) Display.FLAG_PRESENTATION else 0

        val displayManager = mockk<DisplayManager>()
        every { displayManager.getDisplay(any()) } returns display

        val activity = mockk<Activity>()
        every { activity.packageManager } returns packageManager
        every { activity.display } returns display
        every { activity.getSystemService(Service.DISPLAY_SERVICE) } returns displayManager

        return activity
    }

    @Test
    fun `retorna false quando o gamepad virtual esta desabilitado no AppConfig`() {
        val appConfig = mockk<AppConfig>()
        every { appConfig.getGamepad() } returns false
        // Nada além de getGamepad() é consultado neste caminho — activity pode ficar vazio.
        val activity = mockk<Activity>()

        assertFalse(GamePad.shouldShowGamePads(activity, appConfig))
    }

    @Test
    fun `retorna false quando o dispositivo nao tem touchscreen`() {
        val appConfig = mockk<AppConfig>()
        every { appConfig.getGamepad() } returns true
        val activity = mockActivity(gamepadHasTouchScreen = false)

        assertFalse(GamePad.shouldShowGamePads(activity, appConfig))
    }

    @Test
    fun `retorna false quando esta rodando em um display de apresentacao (ex TV)`() {
        val appConfig = mockk<AppConfig>()
        every { appConfig.getGamepad() } returns true
        val activity = mockActivity(isPresentationDisplay = true)

        assertFalse(GamePad.shouldShowGamePads(activity, appConfig))
    }

    @Test
    fun `retorna false quando ha um gamepad fisico externo conectado`() {
        val appConfig = mockk<AppConfig>()
        every { appConfig.getGamepad() } returns true
        val activity = mockActivity()

        val externalGamepad = mockk<InputDevice>()
        every { externalGamepad.isVirtual } returns false
        every { externalGamepad.supportsSource(InputDevice.SOURCE_GAMEPAD) } returns true
        every { externalGamepad.supportsSource(InputDevice.SOURCE_JOYSTICK) } returns false
        every { externalGamepad.name } returns "Controle de teste"
        every { externalGamepad.sources } returns InputDevice.SOURCE_GAMEPAD

        mockkStatic(InputDevice::class)
        every { InputDevice.getDeviceIds() } returns intArrayOf(1)
        every { InputDevice.getDevice(1) } returns externalGamepad

        assertFalse(GamePad.shouldShowGamePads(activity, appConfig))
    }

    @Test
    fun `ignora dispositivos de entrada virtuais ao detectar gamepad fisico`() {
        val appConfig = mockk<AppConfig>()
        every { appConfig.getGamepad() } returns true
        val activity = mockActivity()

        val virtualDevice = mockk<InputDevice>()
        every { virtualDevice.isVirtual } returns true
        every { virtualDevice.supportsSource(any()) } returns true
        every { virtualDevice.name } returns "Virtual"
        every { virtualDevice.sources } returns InputDevice.SOURCE_GAMEPAD

        mockkStatic(InputDevice::class)
        every { InputDevice.getDeviceIds() } returns intArrayOf(1)
        every { InputDevice.getDevice(1) } returns virtualDevice

        assertTrue(GamePad.shouldShowGamePads(activity, appConfig))
    }

    @Test
    fun `retorna true quando nada bloqueia a exibicao do gamepad virtual`() {
        val appConfig = mockk<AppConfig>()
        every { appConfig.getGamepad() } returns true
        val activity = mockActivity()

        mockkStatic(InputDevice::class)
        every { InputDevice.getDeviceIds() } returns intArrayOf()

        assertTrue(GamePad.shouldShowGamePads(activity, appConfig))
    }

    @Test
    fun `um controle fisico que so se anuncia como joystick tambem esconde o gamepad virtual`() {
        val appConfig = mockk<AppConfig>()
        every { appConfig.getGamepad() } returns true
        val joystick = mockk<InputDevice>()
        every { joystick.isVirtual } returns false
        every { joystick.supportsSource(InputDevice.SOURCE_GAMEPAD) } returns false
        every { joystick.supportsSource(InputDevice.SOURCE_JOYSTICK) } returns true
        every { joystick.name } returns "Joystick"
        every { joystick.sources } returns InputDevice.SOURCE_JOYSTICK

        mockkStatic(InputDevice::class)
        every { InputDevice.getDeviceIds() } returns intArrayOf(1, 2)
        every { InputDevice.getDevice(1) } returns null // unplugged while listing
        every { InputDevice.getDevice(2) } returns joystick

        assertFalse(GamePad.shouldShowGamePads(mockActivity(), appConfig))
    }

    @Test
    fun `um dispositivo fisico que nao e controle nao esconde o gamepad virtual`() {
        val appConfig = mockk<AppConfig>()
        every { appConfig.getGamepad() } returns true
        val keyboard = mockk<InputDevice>()
        every { keyboard.isVirtual } returns false
        every { keyboard.supportsSource(any()) } returns false
        every { keyboard.name } returns "Keyboard"
        every { keyboard.sources } returns InputDevice.SOURCE_KEYBOARD

        mockkStatic(InputDevice::class)
        every { InputDevice.getDeviceIds() } returns intArrayOf(1)
        every { InputDevice.getDevice(1) } returns keyboard

        assertTrue(GamePad.shouldShowGamePads(mockActivity(), appConfig))
    }

    // --- eventHandler: on-screen pad events -> interception callback -> core ---

    private val retroView = mockk<GLRetroView>(relaxed = true)
    private val seen = mutableListOf<Event>()

    private fun gamePad(intercept: Boolean?): GamePad {
        // The smallest real layout: one D-pad. The events under test don't depend on it.
        val config = RadialGamePadConfig(
                sockets = 12,
                primaryDial = PrimaryDialConfig.Cross(CrossConfig(GLRetroView.MOTION_SOURCE_DPAD)),
                secondaryDials = emptyList()
        )
        val callback: ((Event) -> Boolean)? = intercept?.let { result -> { event -> seen += event; result } }
        return GamePad(ApplicationProvider.getApplicationContext(), config, callback)
    }

    @Test
    fun `botao nao interceptado vai para o core`() {
        val button = Event.Button(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_DOWN, 0)

        gamePad(intercept = false).eventHandler(button, retroView)

        assertEquals(listOf<Event>(button), seen)
        verify(exactly = 1) { retroView.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A) }
    }

    @Test
    fun `botao interceptado pelo menu nao chega ao core`() {
        gamePad(intercept = true).eventHandler(Event.Button(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.ACTION_UP, 0), retroView)

        verify(exactly = 0) { retroView.sendKeyEvent(any(), any(), any()) }
    }

    @Test
    fun `sem callback todo botao vai para o core`() {
        for (id in listOf(KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_X)) {
            gamePad(intercept = null).eventHandler(Event.Button(id, KeyEvent.ACTION_DOWN, 0), retroView)
            verify(exactly = 1) { retroView.sendKeyEvent(KeyEvent.ACTION_DOWN, id) }
        }
    }

    @Test
    fun `cada direcao vai para a fonte de movimento certa no core`() {
        val pad = gamePad(intercept = false)
        val sources = listOf(
                GLRetroView.MOTION_SOURCE_DPAD,
                GLRetroView.MOTION_SOURCE_ANALOG_LEFT,
                GLRetroView.MOTION_SOURCE_ANALOG_RIGHT
        )

        sources.forEach { pad.eventHandler(Event.Direction(it, 0.25f, -0.5f, 0), retroView) }

        sources.forEach { verify(exactly = 1) { retroView.sendMotionEvent(it, 0.25f, -0.5f) } }
    }

    @Test
    fun `direcao interceptada ou de fonte desconhecida nao chega ao core`() {
        gamePad(intercept = true).eventHandler(Event.Direction(GLRetroView.MOTION_SOURCE_DPAD, 1f, 0f, 0), retroView)
        gamePad(intercept = null).eventHandler(Event.Direction(UNKNOWN_SOURCE, 1f, 0f, 0), retroView)

        verify(exactly = 0) { retroView.sendMotionEvent(any(), any(), any(), any()) }
    }

    private companion object {
        const val UNKNOWN_SOURCE = 99
    }
}
