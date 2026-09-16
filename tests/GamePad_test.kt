package com.vinaooo.revenger.gamepad

import android.app.Activity
import android.app.Service
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.InputDevice
import com.vinaooo.revenger.AppConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Cobre apenas [GamePad.shouldShowGamePads] — a lógica pura de "deve mostrar o gamepad virtual?".
 * O resto da classe (`pad`, `eventHandler`, `subscribe`) constrói um `RadialGamePad` real no
 * construtor (um View da lib externa), caro demais para unit test direto sem Robolectric
 * renderizando a view de verdade.
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
}
