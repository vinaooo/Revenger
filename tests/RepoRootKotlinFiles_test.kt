package com.vinaooo.revenger

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards against scratch Kotlin files piling up in the repository root. Nothing compiles the root
 * (the only source sets are `app/src/...` and `tests/`), so a `.kt` file there is dead code that
 * detekt, lint and the compiler never see. The only one that belongs there is `TODO.kt`, the
 * roadmap notes read by the TODO Tree editor extension.
 */
class RepoRootKotlinFiles_test {

    /** Walks up from the test's working directory (the `app` module) to the settings file. */
    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle").isFile || File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        error("Repository root (settings.gradle) not found above ${System.getProperty("user.dir")}")
    }

    @Test
    fun `raiz do repositorio so tem o TODO kt como arquivo Kotlin`() {
        val rootKotlinFiles =
                repoRoot().listFiles { file -> file.isFile && file.extension == "kt" }.orEmpty()
                        .map { it.name }
                        .sorted()

        assertEquals(listOf("TODO.kt"), rootKotlinFiles)
    }
}
