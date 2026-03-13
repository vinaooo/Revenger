package com.vinaooo.revenger.repositories

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.vinaooo.revenger.models.GamepadConfigJson

/**
 * Singleton repository that loads and caches gamepad_config.json from assets.
 */
class GamepadConfigRepository private constructor(context: Context) {

    private val TAG = "GamepadConfigRepo"
    private val config: GamepadConfigJson

    init {
        config = loadConfig(context)
    }

    fun get(): GamepadConfigJson = config

    private fun loadConfig(context: Context): GamepadConfigJson {
        return try {
            val jsonString = context.assets.open("gamepad_config.json")
                .bufferedReader()
                .use { it.readText() }

            val parsed = Gson().fromJson(jsonString, GamepadConfigJson::class.java)
            Log.d(TAG, "gamepad_config.json loaded successfully")
            parsed
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load gamepad_config.json, using defaults", e)
            GamepadConfigJson()
        }
    }

    companion object {
        @Volatile
        private var instance: GamepadConfigRepository? = null

        fun getInstance(context: Context): GamepadConfigRepository {
            return instance ?: synchronized(this) {
                instance ?: GamepadConfigRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }

        fun resetForTesting() {
            instance = null
        }
    }
}
