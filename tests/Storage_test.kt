package com.vinaooo.revenger.repositories

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for [Storage], focused on the legacy-file migration performed in its `init` block.
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
}
