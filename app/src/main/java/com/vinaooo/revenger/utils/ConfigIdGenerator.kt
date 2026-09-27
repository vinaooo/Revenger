package com.vinaooo.revenger.utils

/**
 * Utility for generating unique app identifiers from game name and core.
 * Converts both strings to lowercase, removes special characters,
 * and combines them with an underscore.
 *
 * Example: "My Game: Part 2" + "some_core" -> "my_game_part_2_some_core"
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
