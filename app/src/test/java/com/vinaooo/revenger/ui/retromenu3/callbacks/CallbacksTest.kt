package com.vinaooo.revenger.ui.retromenu3.callbacks

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the RetroMenu3 callback interfaces.
 * Ensures segregated interfaces (ISP) work correctly.
 */
class CallbacksTest {

    @Test
    fun `ExitListener pode ser implementada`() {
        val listener = object : ExitListener {
            override fun onBackToMainMenu() {
                // Implementation
            }
        }
        assertNotNull(listener)
    }

    @Test
    fun `ProgressListener pode ser implementada`() {
        val listener = object : ProgressListener {
            override fun onBackToMainMenu() {
                // Implementation
            }
        }
        assertNotNull(listener)
    }

    @Test
    fun `AboutListener pode ser implementada`() {
        val listener = object : AboutListener {
            override fun onAboutBackToMainMenu() {
                // Implementation
            }
        }
        assertNotNull(listener)
    }

    @Test
    fun `SettingsMenuListener pode ser implementada`() {
        val listener = object : SettingsMenuListener {
            override fun onBackToMainMenu() {
                // Implementation
            }
        }
        assertNotNull(listener)
    }

    @Test
    fun `SaveStateOperations pode ser implementada`() {
        val ops = object : SaveStateOperations {
            override fun onSaveState() {}
            override fun onLoadState() {}
            override fun hasSaveState(): Boolean = false
        }
        assertNotNull(ops)
        assertFalse(ops.hasSaveState())
    }

    @Test
    fun `GameControlOperations pode ser implementada`() {
        val ops = object : GameControlOperations {
            override fun onResetGame() {}
            override fun onFastForward() {}
            override fun getFastForwardState(): Boolean = false
        }
        assertNotNull(ops)
        assertFalse(ops.getFastForwardState())
    }

    @Test
    fun `AudioVideoOperations pode ser implementada`() {
        val ops = object : AudioVideoOperations {
            override fun onToggleAudio() {}
            override fun onToggleShader() {}
            override fun getAudioState(): Boolean = true
            override fun getShaderState(): String = "default"
        }
        assertNotNull(ops)
        assertTrue(ops.getAudioState())
        assertEquals("default", ops.getShaderState())
    }

    @Test
    fun `RetroMenu3Listener herda de todas as interfaces segregadas`() {
        val listener = object : RetroMenu3Listener {
            override fun onSaveState() {}
            override fun onLoadState() {}
            override fun hasSaveState(): Boolean = true
            override fun onResetGame() {}
            override fun onFastForward() {}
            override fun getFastForwardState(): Boolean = false
            override fun onToggleAudio() {}
            override fun onToggleShader() {}
            override fun getAudioState(): Boolean = true
            override fun getShaderState(): String = "crt"
        }
        
        assertNotNull(listener)
        assertTrue(listener.hasSaveState())
        assertTrue(listener.getAudioState())
        assertEquals("crt", listener.getShaderState())
        assertFalse(listener.getFastForwardState())
    }

    @Test
    fun `RetroMenu3Listener pode ser usado como SaveStateOperations`() {
        val listener = object : RetroMenu3Listener {
            override fun onSaveState() {}
            override fun onLoadState() {}
            override fun hasSaveState(): Boolean = true
            override fun onResetGame() {}
            override fun onFastForward() {}
            override fun getFastForwardState(): Boolean = false
            override fun onToggleAudio() {}
            override fun onToggleShader() {}
            override fun getAudioState(): Boolean = true
            override fun getShaderState(): String = "default"
        }
        
        val saveOps: SaveStateOperations = listener
        assertTrue(saveOps.hasSaveState())
    }

    @Test
    fun `RetroMenu3Listener pode ser usado como GameControlOperations`() {
        val listener = object : RetroMenu3Listener {
            override fun onSaveState() {}
            override fun onLoadState() {}
            override fun hasSaveState(): Boolean = false
            override fun onResetGame() {}
            override fun onFastForward() {}
            override fun getFastForwardState(): Boolean = true
            override fun onToggleAudio() {}
            override fun onToggleShader() {}
            override fun getAudioState(): Boolean = true
            override fun getShaderState(): String = "default"
        }
        
        val gameOps: GameControlOperations = listener
        assertTrue(gameOps.getFastForwardState())
    }

    @Test
    fun `RetroMenu3Listener pode ser usado como AudioVideoOperations`() {
        val listener = object : RetroMenu3Listener {
            override fun onSaveState() {}
            override fun onLoadState() {}
            override fun hasSaveState(): Boolean = false
            override fun onResetGame() {}
            override fun onFastForward() {}
            override fun getFastForwardState(): Boolean = false
            override fun onToggleAudio() {}
            override fun onToggleShader() {}
            override fun getAudioState(): Boolean = false
            override fun getShaderState(): String = "scanlines"
        }
        
        val avOps: AudioVideoOperations = listener
        assertFalse(avOps.getAudioState())
        assertEquals("scanlines", avOps.getShaderState())
    }
}
