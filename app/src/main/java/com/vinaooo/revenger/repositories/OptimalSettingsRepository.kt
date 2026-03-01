package com.vinaooo.revenger.repositories

import android.content.Context
import android.util.Log
import com.vinaooo.revenger.models.OptimalSettingsProfile
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Repository for accessing optimal platform configuration profiles.
 * Loads and caches platform profiles from assets/optimal_settings.json.
 * Follows the singleton pattern consistent with Storage.kt.
 */
object OptimalSettingsRepository {
    private const val TAG = "OptimalSettingsRepo"
    private const val ASSET_FILE = "optimal_settings.json"

    private var profiles: List<OptimalSettingsProfile>? = null

    /**
     * Initialize and load profiles from assets.
     * Should be called once during application startup.
     */
    fun initialize(context: Context) {
        if (profiles != null) {
            Log.d(TAG, "Profiles already loaded, skipping initialization")
            return
        }

        try {
            val jsonString = loadJsonFromAssets(context)
            val jsonArray = JSONArray(jsonString)
            profiles = OptimalSettingsProfile.parseProfiles(jsonArray)
            Log.d(TAG, "Loaded ${profiles!!.size} platform profiles")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load optimal settings", e)
            profiles = emptyList()
        }
    }

    /**
     * Find a profile matching the given platform ID or extension.
     * Resolution order:
     * 1. By platformId if non-empty
     * 2. By extension match
     * 
     * @param platformId Explicit platform identifier (e.g., "sms", "snes")
     * @param extension ROM file extension with dot (e.g., ".sms", ".sfc")
     * @return Matching profile or null if not found
     */
    fun findProfile(platformId: String?, extension: String): OptimalSettingsProfile? {
        val profileList = profiles ?: run {
            Log.w(TAG, "Profiles not initialized, call initialize() first")
            return null
        }

        // Try explicit platform ID first
        if (!platformId.isNullOrEmpty()) {
            val profile = profileList.find { it.platformId == platformId }
            if (profile != null) {
                Log.d(TAG, "Found profile by platformId: $platformId")
                return profile
            }
            Log.w(TAG, "No profile found for platformId: $platformId")
        }

        // Fall back to extension matching
        val normalizedExtension = if (extension.startsWith(".")) extension else ".$extension"
        val profile = profileList.find { it.extensions.contains(normalizedExtension.lowercase()) }
        
        if (profile != null) {
            Log.d(TAG, "Found profile by extension: $normalizedExtension -> ${profile.platformId}")
        } else {
            Log.w(TAG, "No profile found for extension: $normalizedExtension")
        }
        
        return profile
    }

    /**
     * Get all available platform IDs.
     * Useful for validation and debugging.
     */
    fun getAvailablePlatforms(): List<String> {
        return profiles?.map { it.platformId } ?: emptyList()
    }

    /**
     * Load JSON content from assets folder
     */
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
