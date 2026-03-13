package com.vinaooo.revenger.repositories

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.vinaooo.revenger.models.ConfigJson

/**
 * Singleton repository that loads and caches config.json from assets.
 * Replaces the Android XML resource system for application configuration.
 *
 * Usage:
 *   val config = ConfigRepository.getInstance(context)
 *   val name = config.get().identity.name
 */
class ConfigRepository private constructor(context: Context) {

    private val TAG = "ConfigRepository"
    private val config: ConfigJson

    init {
        config = loadConfig(context)
    }

    fun get(): ConfigJson = config

    private fun loadConfig(context: Context): ConfigJson {
        return try {
            val jsonString = context.assets.open("config.json")
                .bufferedReader()
                .use { it.readText() }

            val parsed = Gson().fromJson(jsonString, ConfigJson::class.java)
            Log.d(TAG, "config.json loaded successfully: name=${parsed.identity.name}, core=${parsed.identity.core}")
            parsed
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load config.json, using defaults", e)
            ConfigJson()
        }
    }

    companion object {
        @Volatile
        private var instance: ConfigRepository? = null

        fun getInstance(context: Context): ConfigRepository {
            return instance ?: synchronized(this) {
                instance ?: ConfigRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }

        /**
         * Reset the singleton (useful for testing).
         */
        fun resetForTesting() {
            instance = null
        }
    }
}
