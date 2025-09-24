package com.vinaooo.revenger.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.vinaooo.revenger.R
import com.vinaooo.revenger.repositories.Storage
import com.vinaooo.revenger.retroview.RetroView

class RetroViewUtils(private val activity: Activity) {
    companion object {
        private const val TAG = "RetroViewUtils"
    }

    private val storage = Storage.getInstance(activity)
    private val sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
    private val fastForwardSpeed =
            activity.resources.getInteger(R.integer.config_fast_forward_multiplier)

    fun restoreEmulatorState(retroView: RetroView) {
        Log.i(TAG, "🔄 RESTORING emulator state...")

        try {
            Log.d(TAG, "⚡ Setting frame speed...")
            val savedFrameSpeed =
                    sharedPreferences.getInt(activity.getString(R.string.pref_frame_speed), 1)
            retroView.view.frameSpeed = savedFrameSpeed
            Log.d(TAG, "✅ Frame speed set to: $savedFrameSpeed")

            Log.d(TAG, "🔊 Setting audio enabled...")
            val savedAudioEnabled =
                    sharedPreferences.getBoolean(
                            activity.getString(R.string.pref_audio_enabled),
                            true
                    )
            retroView.view.audioEnabled = savedAudioEnabled
            Log.d(TAG, "✅ Audio enabled set to: $savedAudioEnabled")

            Log.d(TAG, "💾 Loading temp state...")
            loadTempState(retroView)
            Log.d(TAG, "✅ Temp state loaded")

            Log.i(TAG, "🎯 Emulator state RESTORED successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "💥 ERROR restoring emulator state: ${e.message}", e)
            throw e
        }
    }

    fun preserveEmulatorState(retroView: RetroView) {
        saveSRAM(retroView)
        saveTempState(retroView)

        with(sharedPreferences.edit()) {
            putInt(activity.getString(R.string.pref_frame_speed), retroView.view.frameSpeed)
            putBoolean(activity.getString(R.string.pref_audio_enabled), retroView.view.audioEnabled)
            apply()
        }
    }

    fun saveSRAM(retroView: RetroView) {
        storage.sram.outputStream().use { it.write(retroView.view.serializeSRAM()) }
    }

    fun loadState(retroView: RetroView) {
        if (!storage.state.exists()) return

        val stateBytes = storage.state.inputStream().use { it.readBytes() }

        if (stateBytes.isEmpty()) return

        retroView.view.unserializeState(stateBytes)
    }

    fun loadTempState(retroView: RetroView) {
        Log.d(TAG, "🔄 Loading temp state...")

        val tempStateFile = storage.tempState
        Log.d(TAG, "Temp state file: ${tempStateFile.absolutePath}")
        Log.d(TAG, "Temp state exists: ${tempStateFile.exists()}")

        if (!tempStateFile.exists()) {
            Log.d(TAG, "⚠️ No temp state file found, starting fresh")
            return
        }

        try {
            Log.d(TAG, "📥 Reading temp state bytes...")
            val stateBytes = tempStateFile.inputStream().use { it.readBytes() }
            Log.d(TAG, "✅ Temp state bytes read: ${stateBytes.size} bytes")

            if (stateBytes.isEmpty()) {
                Log.w(TAG, "⚠️ Temp state file is empty")
                return
            }

            Log.d(TAG, "📤 Unserializing temp state to RetroView...")
            retroView.view.unserializeState(stateBytes)
            Log.d(TAG, "✅ Temp state unserialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "💥 ERROR loading temp state: ${e.message}", e)
        }
    }

    fun saveState(retroView: RetroView) {
        storage.state.outputStream().use { it.write(retroView.view.serializeState()) }
    }

    fun saveTempState(retroView: RetroView) {
        storage.tempState.outputStream().use { it.write(retroView.view.serializeState()) }
    }

    fun fastForward(retroView: RetroView) {
        retroView.view.frameSpeed = if (retroView.view.frameSpeed == 1) fastForwardSpeed else 1
    }
}
