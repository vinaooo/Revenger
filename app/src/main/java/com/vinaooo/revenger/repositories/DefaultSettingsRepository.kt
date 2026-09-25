package com.vinaooo.revenger.repositories

import android.content.Context
import android.util.Log
import com.vinaooo.revenger.models.DefaultSettingsProfile
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Repository for accessing optimal platform configuration profiles.
 * Loads and caches platform profiles from assets/default_settings.json.
 * Follows the singleton pattern consistent with Storage.kt.
 */
object DefaultSettingsRepository {
    private const val TAG = "DefaultSettingsRepo"
    private const val ASSET_FILE = "default_settings.json"

    private var profiles: List<DefaultSettingsProfile>? = null

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
            val loadedProfiles = DefaultSettingsProfile.parseProfiles(jsonArray)
            profiles = loadedProfiles
            Log.d(TAG, "Loaded ${loadedProfiles.size} platform profiles")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load default settings", e)
            profiles = emptyList()
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to load default settings", e)
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
    fun findProfile(platformId: String?, extension: String): DefaultSettingsProfile? {
        val profileList = profiles
        if (profileList == null) {
            Log.w(TAG, "Profiles not initialized, call initialize() first")
            return null
        }

        return findByPlatformId(profileList, platformId) ?: findByExtension(profileList, extension)
    }

    /** Try an explicit platform ID match first. */
    private fun findByPlatformId(
        profileList: List<DefaultSettingsProfile>,
        platformId: String?
    ): DefaultSettingsProfile? {
        val id = platformId ?: return null
        if (id.isEmpty()) return null

        val profile = profileList.find { it.platformId == id }
        if (profile != null) {
            Log.d(TAG, "Found profile by platformId: $id")
        } else {
            Log.w(TAG, "No profile found for platformId: $id")
        }
        return profile
    }

    /** Fall back to matching by the ROM's file extension. */
    private fun findByExtension(
        profileList: List<DefaultSettingsProfile>,
        extension: String
    ): DefaultSettingsProfile? {
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
