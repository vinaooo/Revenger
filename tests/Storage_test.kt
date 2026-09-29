package com.vinaooo.revenger.repositories

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for [Storage]'s `init` block: the legacy-file migration and the cleanup of the
 * leftover `tempstate` snapshot older versions wrote.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class Storage_test {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `migra arquivo legado do diretorio externo para o diretorio interno`() {
        val legacyDir = context.getExternalFilesDir(null)!!
        File(legacyDir, "state").writeText("legacy-state")

        val storage = Storage(context)

        assertTrue(storage.state.exists())
        assertEquals("legacy-state", storage.state.readText())
        assertFalse(File(legacyDir, "state").exists())
    }

    @Test
    fun `nao sobrescreve estado ja existente no diretorio interno`() {
        val legacyDir = context.getExternalFilesDir(null)!!
        File(legacyDir, "state").writeText("legacy-state")

        val internalState = File(context.filesDir, "state")
        internalState.parentFile?.mkdirs()
        internalState.writeText("current-state")

        val storage = Storage(context)

        assertEquals("current-state", storage.state.readText())
        // Legacy file is left untouched when the target already has a save.
        assertTrue(File(legacyDir, "state").exists())
    }

    // Regression test for the narrowed IOException/SecurityException catch in migrateLegacyFile:
    // the internal files dir is made read-only, so copyTo() fails opening the destination stream
    // (FileNotFoundException -- permission denied -- an IOException subtype) rather than throwing
    // a plain, unenumerable exception. Construction must not crash, and since the copy never
    // succeeds, the legacy file must not be deleted.
    @Test
    fun `diretorio interno bloqueado para escrita nao interrompe a construcao e nao apaga o legado`() {
        val legacyDir = context.getExternalFilesDir(null)!!
        val legacyFile = File(legacyDir, "state")
        legacyFile.writeText("legacy-state")

        context.filesDir.setWritable(false)
        try {
            val storage = Storage(context)

            assertFalse(storage.state.exists())
            assertTrue(legacyFile.exists())
        } finally {
            context.filesDir.setWritable(true)
        }
    }

    @Test
    fun `apaga o tempstate que versoes antigas deixaram, nos dois diretorios`() {
        val leftovers =
                listOf(File(context.filesDir, "tempstate"), File(context.getExternalFilesDir(null)!!, "tempstate"))
        leftovers.forEach { it.writeText("old snapshot") }

        Storage(context)

        leftovers.forEach { assertFalse(it.path, it.exists()) }
    }

    @Test
    fun `sem tempstate antigo a construcao segue normal`() {
        File(context.filesDir, "tempstate").delete()

        val storage = Storage(context)

        assertTrue(storage.sram.parentFile!!.exists())
    }

    @Test
    fun `migra o sram legado do diretorio externo para o diretorio interno`() {
        val legacyDir = context.getExternalFilesDir(null)!!
        File(legacyDir, "sram").writeText("legacy-sram")

        val storage = Storage(context)

        assertEquals("legacy-sram", storage.sram.readText())
        assertFalse(File(legacyDir, "sram").exists())
    }

    @Test
    fun `cria os diretorios de ROM, SRAM e estados que ainda nao existem`() {
        val root = File(context.cacheDir, "storage-dirs-test").apply { deleteRecursively() }
        val filesDir = File(root, "files")
        val cacheDir = File(root, "cache")
        val missingDirsContext = mockk<Context> {
            every { this@mockk.filesDir } returns filesDir
            every { getExternalFilesDir(null) } returns null
            every { externalCacheDir } returns null
            every { this@mockk.cacheDir } returns cacheDir
        }

        try {
            Storage(missingDirsContext)

            assertTrue(filesDir.isDirectory)
            assertTrue(cacheDir.isDirectory)
        } finally {
            root.deleteRecursively()
        }
    }
}
