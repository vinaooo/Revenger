package com.vinaooo.revenger

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Enforces the "no Material Design / Material Components" rule in `CLAUDE.md`: the UI uses
 * `RetroCardView` and plain views on purpose. Checks the main sources, resources, manifest and Gradle
 * files for the library's package and themes, and the app's runtime classpath (which the unit
 * tests share) for its classes, so a transitive dependency is caught too.
 */
class NoMaterialComponents_test {

    companion object {
        private val repoRoot: File =
            generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle").isFile }

        private val FORBIDDEN =
            listOf("com.google.android.material", "Theme.MaterialComponents", "Theme.Material3")

        private val SCANNED_EXTENSIONS = setOf("kt", "java", "xml", "gradle", "kts", "toml")

        /** `path:line` for every line of [files] that mentions a forbidden package or theme. */
        fun findHits(files: List<File>): List<String> =
            files.flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (FORBIDDEN.any { it in line }) "${file.name}:${index + 1}" else null
                }
            }

        private fun scannedFiles(): List<File> {
            val gradleFiles =
                listOf("build.gradle", "settings.gradle", "app/build.gradle", "gradle.properties")
                    .map { File(repoRoot, it) }
                    .filter { it.isFile }
            val gradleDir =
                File(repoRoot, "gradle").walk().filter { it.isFile && it.extension == "toml" }
            val main =
                File(repoRoot, "app/src/main").walk().filter {
                    it.isFile && it.extension in SCANNED_EXTENSIONS
                }
            return gradleFiles + gradleDir + main
        }
    }

    @Test
    fun `main sources, resources and Gradle files never mention Material Components`() {
        val files = scannedFiles()
        assertTrue("the scan should cover the app and Gradle files", files.size > 100)

        val hits = findHits(files)

        assertEquals("Material Components are not used (see CLAUDE.md): $hits", emptyList<String>(), hits)
    }

    @Test
    fun `no Material Components class is on the app's runtime classpath`() {
        val probes =
            listOf(
                "com.google.android.material.card.MaterialCardView",
                "com.google.android.material.button.MaterialButton",
                "com.google.android.material.R"
            )
        for (name in probes) {
            try {
                Class.forName(name, false, javaClass.classLoader)
                fail("$name is on the classpath: a dependency brings in Material Components")
            } catch (expected: ClassNotFoundException) {
                // Not on the classpath, as intended.
            }
        }
    }

    @Test
    fun `findHits reports a planted dependency and theme`() {
        val dir = kotlin.io.path.createTempDirectory("no-material").toFile()
        try {
            val gradle = File(dir, "build.gradle")
            gradle.writeText("dependencies {\n    implementation 'com.google.android.material:x:1'\n}\n")
            val themes = File(dir, "themes.xml")
            themes.writeText("<style name=\"A\" parent=\"Theme.Material3.DayNight\" />\n")
            val clean = File(dir, "styles.xml")
            clean.writeText("<style name=\"B\" parent=\"android:Theme.DeviceDefault\" />\n")

            assertEquals(
                listOf("build.gradle:2", "themes.xml:1"),
                findHits(listOf(gradle, themes, clean))
            )
        } finally {
            dir.deleteRecursively()
        }
    }
}
