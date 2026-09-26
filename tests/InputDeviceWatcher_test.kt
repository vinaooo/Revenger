package com.vinaooo.revenger.controllers

import android.hardware.input.InputManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/**
 * [InputDeviceWatcher] replaced an anonymous `InputManager.InputDeviceListener` in `GameActivity`
 * that was registered and never unregistered, so the process-wide [InputManager] kept every
 * destroyed `GameActivity` reachable. These tests pin that each device event calls the callback
 * once, and that [InputDeviceWatcher.dispose] unregisters the exact listener that was registered.
 */
class InputDeviceWatcher_test {

    private lateinit var inputManager: InputManager
    private val registered = slot<InputManager.InputDeviceListener>()
    private var callbackCount = 0
    private lateinit var watcher: InputDeviceWatcher

    @Before
    fun setUp() {
        inputManager = mockk(relaxed = true)
        every { inputManager.registerInputDeviceListener(capture(registered), any()) } returns Unit
        callbackCount = 0
        watcher = InputDeviceWatcher(inputManager) { callbackCount++ }
    }

    @Test
    fun `register adds one listener to the input manager`() {
        watcher.register()

        verify(exactly = 1) { inputManager.registerInputDeviceListener(any(), null) }
    }

    @Test
    fun `device added calls the callback once`() {
        watcher.register()

        registered.captured.onInputDeviceAdded(1)

        assertEquals(1, callbackCount)
    }

    @Test
    fun `device removed calls the callback once`() {
        watcher.register()

        registered.captured.onInputDeviceRemoved(1)

        assertEquals(1, callbackCount)
    }

    @Test
    fun `device changed calls the callback once`() {
        watcher.register()

        registered.captured.onInputDeviceChanged(1)

        assertEquals(1, callbackCount)
    }

    @Test
    fun `dispose unregisters the same listener that was registered`() {
        watcher.register()
        val unregistered = slot<InputManager.InputDeviceListener>()
        every { inputManager.unregisterInputDeviceListener(capture(unregistered)) } returns Unit

        watcher.dispose()

        assertSame(registered.captured, unregistered.captured)
    }

    @Test
    fun `dispose twice unregisters only once`() {
        watcher.register()

        watcher.dispose()
        watcher.dispose()

        verify(exactly = 1) { inputManager.unregisterInputDeviceListener(any()) }
    }

    @Test
    fun `dispose without register touches nothing`() {
        watcher.dispose()

        verify(exactly = 0) { inputManager.unregisterInputDeviceListener(any()) }
    }

    @Test
    fun `register twice registers only once`() {
        watcher.register()
        watcher.register()

        verify(exactly = 1) { inputManager.registerInputDeviceListener(any(), any()) }
    }

    @Test
    fun `register after dispose listens again`() {
        watcher.register()
        watcher.dispose()

        watcher.register()
        registered.captured.onInputDeviceAdded(1)

        verify(exactly = 2) { inputManager.registerInputDeviceListener(any(), any()) }
        assertEquals(1, callbackCount)
    }
}
