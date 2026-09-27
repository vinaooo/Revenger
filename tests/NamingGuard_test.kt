package com.vinaooo.revenger

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Enforces the naming rule in `CLAUDE.md`: no game, platform, core or brand names in Kotlin
 * sources (`app/src/main/java`, `tests/`, `app/src/androidTest/`) or in the Markdown docs
 * (`docs/`).
 *
 * This file names nothing itself. The denylist is built at runtime from the files where those
 * values are allowed to live: `icons/platforms.json` (platform, core and extension ids),
 * `default_settings.json` (profile ids, cores, extensions, core option prefixes) and
 * `config.json` (`name`, `rom`, `core`). Manufacturer, product and title words are checked as
 * SHA-256 hashes from `icons/tests/brand_denylist_sha256.txt`, shared with the icon scripts' guard.
 *
 * Matching is by whole tokens, case-insensitive, with camelCase split, so an id never matches
 * inside a longer word. Accented letters belong to the word (some docs are in Portuguese). A
 * multi-word id (a title, a core with underscores) matches only as the same token sequence.
 */
class NamingGuard_test {

    companion object {
        private val repoRoot: File =
            generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle").isFile }

        private val SCANNED_ROOTS =
            listOf("app/src/main/java", "tests", "app/src/androidTest", "docs")

        private val SCANNED_EXTENSIONS = setOf("kt", "java", "md")

        /**
         * Ids that are also ordinary words; matching them would flag everyday code (a save-file
         * suffix, the verb in "play the game"). A real name is never on this list.
         */
        private val ALLOWED = setOf("play", "bin")

        /** Doc file names such as `Code.md`: their extension is not a platform id. */
        private val DOC_FILE_NAME = Regex("""[\w-]+\.md\b""")

        fun tokens(text: String): List<String> {
            val split =
                DOC_FILE_NAME.replace(text, " ")
                    .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
                    .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1 $2")
            return Regex("[\\p{L}\\p{N}]+").findAll(split.lowercase()).map { it.value }.toList()
        }

        /** True if [id]'s tokens appear consecutively in [lineTokens]. */
        fun containsId(lineTokens: List<String>, id: List<String>): Boolean =
            id.isNotEmpty() && lineTokens.windowed(id.size).any { it == id }

        fun sha256(word: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(word.toByteArray())
                .joinToString("") { "%02x".format(it) }

        private fun json(path: String) = JsonParser.parseString(File(repoRoot, path).readText())

        private fun JsonArray.strings() = map { it.asString }

        fun configuredIds(): Set<String> {
            val ids = mutableSetOf<String>()

            val platforms = json("icons/platforms.json").asJsonObject
            platforms.getAsJsonObject("extension_to_platform").entrySet().forEach { (ext, p) ->
                ids += ext
                ids += p.asString
            }
            platforms.getAsJsonObject("ambiguous_extensions").entrySet().forEach { (ext, rule) ->
                ids += ext
                ids += rule.asJsonObject.get("default").asString
                rule.asJsonObject.getAsJsonArray("by_core_substring").forEach {
                    ids += it.asJsonArray.strings()
                }
            }
            platforms.getAsJsonArray("core_to_platform").forEach { ids += it.asJsonArray.strings() }
            ids += platforms.getAsJsonObject("console_icons").keySet()
            ids += platforms.getAsJsonObject("igdb_platform_ids").keySet()

            json("app/src/main/assets/default_settings.json").asJsonArray.forEach {
                val profile = it.asJsonObject
                ids += profile.get("platform_id").asString
                ids += profile.get("core").asString
                ids += profile.getAsJsonArray("extensions").strings().map { e -> e.removePrefix(".") }
                // Core options are prefixed with the core's own name ("<prefix>_<option>=value").
                profile.get("variables").asString.split(",").filter { v -> "=" in v }.forEach { v ->
                    ids += v.substringBefore("=").substringBefore("_")
                }
            }

            val config = json("app/src/main/assets/config/config.json").asJsonObject
            listOf("name", "rom", "core").mapNotNull { config.stringOrNull(it) }.forEach { ids += it }
            config.stringOrNull("rom")?.let { ids += it.substringBeforeLast(".") }

            return ids.filter { it.isNotBlank() && it.lowercase() !in ALLOWED }.toSet()
        }

        private fun JsonObject.stringOrNull(key: String): String? =
            get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }

        fun hashedWords(): Set<String> =
            File(repoRoot, "icons/tests/brand_denylist_sha256.txt")
                .readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toSet()

        /** This file holds only made-up ids and the matcher's own fixtures (e.g. the `md` case). */
        private const val SELF = "NamingGuard_test.kt"

        private fun sourceFiles(): List<File> =
            SCANNED_ROOTS.map { File(repoRoot, it) }
                .flatMap { root ->
                    root.walk().filter {
                        it.isFile && it.extension in SCANNED_EXTENSIONS && it.name != SELF
                    }
                }

        /** `path:line` for every line of [files] that holds a denylisted id or hashed word. */
        fun findHits(files: List<File>, ids: Set<String>, hashes: Set<String>): List<String> {
            val idTokens = ids.map { tokens(it) }.filter { it.isNotEmpty() }
            return files.flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val lineTokens = tokens(line)
                    val hit =
                        idTokens.any { containsId(lineTokens, it) } ||
                            lineTokens.any { sha256(it) in hashes }
                    if (hit) "${file.relativeTo(repoRoot)}:${index + 1}" else null
                }
            }
        }
    }

    // --- the real sources ---

    @Test
    fun `no configured game, platform or core id appears in Kotlin sources or docs`() {
        val hits = findHits(sourceFiles(), configuredIds(), emptySet())

        assertEquals("Neutral wording needed (see CLAUDE.md) at: $hits", emptyList<String>(), hits)
    }

    @Test
    fun `no denylisted brand or title word appears in Kotlin sources or docs`() {
        val hits = findHits(sourceFiles(), emptySet(), hashedWords())

        assertEquals("Neutral wording needed (see CLAUDE.md) at: $hits", emptyList<String>(), hits)
    }

    @Test
    fun `the denylist is built from every data file`() {
        val ids = configuredIds()

        assertTrue("expected ids from platforms.json and the profiles", ids.size > 20)
        assertTrue(hashedWords().size >= 36)
        assertTrue(hashedWords().all { it.matches(Regex("[0-9a-f]{64}")) })
        assertTrue("the scanned roots exist", sourceFiles().size > 100)
        assertTrue("the docs are scanned", sourceFiles().any { it.extension == "md" })
    }

    // --- the matcher, on made-up ids ---

    @Test
    fun `tokens are lowercase and split camelCase and punctuation`() {
        assertEquals(
            listOf("load", "zork", "core", "for", "abc", "rom"),
            tokens("loadZorkCore(\"for\") // ABC_rom")
        )
    }

    @Test
    fun `tokens split an acronym from the next word`() {
        assertEquals(listOf("xyz", "parser"), tokens("XYZParser"))
    }

    @Test
    fun `an accented letter stays inside its word`() {
        assertEquals(listOf("genérico", "não"), tokens("Genérico não"))
        assertTrue(!containsId(tokens("um emulador genérico"), tokens("gen")))
    }

    @Test
    fun `an id matches as a whole token, not inside a longer word`() {
        val id = tokens("zor")

        assertTrue(containsId(tokens("the zor core"), id))
        assertTrue(containsId(tokens("ZOR"), id))
        assertTrue(containsId(tokens("useZorCore"), id))
        assertTrue(!containsId(tokens("the zorkmid core"), id))
    }

    @Test
    fun `a multi-word id matches only as the same token sequence`() {
        val id = tokens("Quux Frob Adventure")

        assertTrue(containsId(tokens("load \"quux_frob_adventure.rom\""), id))
        assertTrue(!containsId(tokens("frob the quux adventure"), id))
    }

    @Test
    fun `a doc file name does not count as an extension`() {
        val id = tokens("md")

        assertTrue(!containsId(tokens("see definitions/Code.md"), id))
        assertTrue(containsId(tokens("extension \".md\""), id))
    }

    @Test
    fun `findHits reports the file and line of a planted id and a hashed word`() {
        val dir = createTempDir()
        try {
            val file = File(dir, "Planted.kt")
            file.writeText("val a = 1\n// runs on the zorcore\nval b = \"wibble\"\n")

            val hits = findHits(listOf(file), setOf("zorcore"), setOf(sha256("wibble")))

            assertEquals(2, hits.size)
            assertTrue(hits[0].endsWith("Planted.kt:2"))
            assertTrue(hits[1].endsWith("Planted.kt:3"))
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun createTempDir(): File =
        kotlin.io.path.createTempDirectory("naming-guard").toFile()
}
