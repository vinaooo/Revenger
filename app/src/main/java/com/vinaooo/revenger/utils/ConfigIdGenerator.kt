package com.vinaooo.revenger.utils

/**
 * Utility for generating unique app identifiers from game name and core.
 * Converts both strings to lowercase, removes special characters,
 * and combines them with an underscore.
 *
 * Example: "Sonic The Hedgehog" + "picodrive" -> "sonic_the_hedgehog_picodrive"
 */
object ConfigIdGenerator {
    fun generate(name: String, core: String): String {
        val cleanName = name
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .replace(Regex("^_+|_+$"), "")

        val cleanCore = core
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .replace(Regex("^_+|_+$"), "")

        return "${cleanName}_${cleanCore}"
    }
}
