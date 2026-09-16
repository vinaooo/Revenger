package com.vinaooo.revenger.repositories

import android.content.Context
import android.util.Log
import com.vinaooo.revenger.models.PipConfigProfile
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Repository for accessing PiP aspect ratio configurations per platform.
 * Loads configurations from assets/pip_config.json.
 */
object PipConfigRepository {
    private const val TAG = "PipConfigRepository"
    private const val ASSET_FILE = "pip_config.json"

    private var platformsConfig: Map<String, PipConfigProfile>? = null
    var defaultConfig: PipConfigProfile? = null
        private set

    /**
     * Initialize and load configurations from assets.
     */
    fun initialize(context: Context) {
        if (platformsConfig != null) {
            return
        }

        try {
            val jsonString = loadJsonFromAssets(context)
            val jsonObject = JSONObject(jsonString)
            
            val defaultObj = jsonObject.getJSONObject("default")
            defaultConfig = PipConfigProfile.fromJson("default", defaultObj)

            val platformsObj = jsonObject.getJSONObject("platforms")
            val map = mutableMapOf<String, PipConfigProfile>()
            
            val keys = platformsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = PipConfigProfile.fromJson(key, platformsObj.getJSONObject(key))
            }
            
            platformsConfig = map
            Log.d(TAG, "Loaded ${map.size} PiP configurations")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load PiP configurations", e)
            platformsConfig = emptyMap()
            defaultConfig = PipConfigProfile("default", 4, 3)
        }
    }

    /**
     * Find a PiP ratio for the given platform ID.
     */
    fun getProfile(platformId: String?): PipConfigProfile {
        if (platformId.isNullOrEmpty()) {
            return defaultConfig ?: PipConfigProfile("default", 4, 3)
        }
        
        return platformsConfig?.get(platformId.lowercase()) ?: defaultConfig ?: PipConfigProfile("default", 4, 3)
    }

    private fun loadJsonFromAssets(context: Context): String {
        val inputStream = context.assets.open(ASSET_FILE)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?
        
        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line)
        }
        
        reader.close()
        inputStream.close()
        
        return stringBuilder.toString()
    }
}
