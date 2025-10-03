package com.vinaooo.revenger.repositories

import android.content.Context
import java.io.File

/** Singleton responsável por fornecer caminhos estáveis para ROM, SRAM e save states. */
class Storage(context: Context) {
    companion object {
        @Volatile private var instance: Storage? = null

        fun getInstance(context: Context): Storage =
                instance
                        ?: synchronized(this) {
                            instance ?: Storage(context).also { instance = it }
                        }
    }

    private val internalFilesDir: File = context.filesDir
    private val externalFilesDir: File? = context.getExternalFilesDir(null)

    val storagePath: String = internalFilesDir.path
    val cachePath: String = (context.externalCacheDir ?: context.cacheDir).path

    val rom = File("$cachePath/rom")
    val sram = File(internalFilesDir, "sram")
    val state = File(internalFilesDir, "state")
    val tempState = File(internalFilesDir, "tempstate")

    init {
        ensureParentDirectories()
        migrateLegacyFile("state", state)
        migrateLegacyFile("tempstate", tempState)
        migrateLegacyFile("sram", sram)
    }

    private fun ensureParentDirectories() {
        listOf(rom.parentFile, sram.parentFile, state.parentFile, tempState.parentFile)
                .filterNotNull()
                .forEach { parent ->
                    if (!parent.exists()) {
                        parent.mkdirs()
                    }
                }
    }

    private fun migrateLegacyFile(fileName: String, target: File) {
        val legacyDir = externalFilesDir ?: return
        val legacyFile = File(legacyDir, fileName)

        if (!legacyFile.exists()) return
        if (target.exists()) return

        try {
            legacyFile.copyTo(target, overwrite = false)
            if (!legacyFile.delete()) {
                // Remoção falhou; arquivo legado será mantido sem afetar o novo estado
            }
        } catch (e: Exception) {
            // Falhas de migração são ignoradas para manter compatibilidade com o comportamento
            // anterior
        }
    }
}
