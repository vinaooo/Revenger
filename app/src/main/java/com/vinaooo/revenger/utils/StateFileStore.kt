package com.vinaooo.revenger.utils

import android.util.Log
import com.vinaooo.revenger.repositories.Storage
import com.vinaooo.revenger.retroview.RetroView
import java.io.IOException

/**
 * Raw save-state/temp-state/SRAM file I/O for [RetroViewUtils], split out purely to keep that
 * class under the project's function-count threshold. Exposed back on [RetroViewUtils] via Kotlin
 * interface delegation (`by`) so its public surface stays identical -- [loadState] and [saveState]
 * are called from outside that file (e.g. `SaveLoadOrchestrator`); the rest are currently only
 * ever called from within [RetroViewUtils] itself but are kept on the same interface so the whole
 * group of storage-file operations moves together.
 */
interface StateFileOperations {
    /** Returns the path of the save-state file in use, or null if it could not be resolved. */
    fun getSaveStatePath(): String?

    fun loadState(retroView: RetroView)

    fun loadTempState(retroView: RetroView)

    fun saveState(retroView: RetroView)

    fun saveTempState(retroView: RetroView)

    fun saveSRAM(retroView: RetroView)
}

class StateFileStore(private val storage: Storage) : StateFileOperations {

    companion object {
        private const val TAG = "StateFileStore"
    }

    override fun getSaveStatePath(): String? {
        return try {
            storage.state.absolutePath
            // File.getAbsolutePath() documents no throwable condition here: storage.state is
            // already derived from Context.filesDir (always absolute), and Android has no
            // SecurityManager, so there is no narrower reachable type.
        } catch (expectedUnreachable: Exception) {
            Log.w(TAG, "Could not resolve save state path", expectedUnreachable)
            null
        }
    }

    override fun loadState(retroView: RetroView) {
        if (!storage.state.exists()) {
            return
        }

        val stateBytes = storage.state.inputStream().use { it.readBytes() }

        if (stateBytes.isEmpty()) {
            return
        }

        retroView.view.unserializeState(stateBytes)
    }

    override fun loadTempState(retroView: RetroView) {
        if (!storage.tempState.exists()) {
            return
        }

        val stateBytes = storage.tempState.inputStream().use { it.readBytes() }

        if (stateBytes.isEmpty()) {
            return
        }

        retroView.view.unserializeState(stateBytes)
    }

    override fun saveState(retroView: RetroView) {
        try {
            val stateBytes = retroView.view.serializeState()

            storage.state.outputStream().use { it.write(stateBytes) }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write save state file", e)
            // serializeState() runs on LibretroDroid's GL thread via a blocking CountDownLatch; a
            // failure inside the native call there deadlocks the latch rather than propagating an
            // exception back to this thread, and the only checked failure mode reaching here (the
            // library unboxing a null native result) surfaces as a plain NullPointerException,
            // which this project's detekt config still treats as "too generic" -- so there is no
            // narrower reachable type to catch. Kept as a safety net via detekt's documented
            // escape hatch instead of @Suppress.
        } catch (expectedNativeCallFailure: Exception) {
            Log.e(TAG, "Failed to save state", expectedNativeCallFailure)
        }
    }

    override fun saveTempState(retroView: RetroView) {
        val stateBytes = retroView.view.serializeState()

        storage.tempState.outputStream().use { it.write(stateBytes) }
    }

    override fun saveSRAM(retroView: RetroView) {
        storage.sram.outputStream().use { it.write(retroView.view.serializeSRAM()) }
    }
}
