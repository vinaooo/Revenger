package com.vinaooo.revenger.utils

import android.util.Log
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Utilitário moderno para download e extração de arquivos LibRetro Substitui o plugin
 * de.undercouch.download por implementação nativa
 */
class LibRetroDownloader {

    companion object {
        private const val TAG = "LibRetroDownloader"
        private const val CONNECT_TIMEOUT = 30000 // 30s
        private const val READ_TIMEOUT = 60000 // 60s

        /** Main para ser chamado pelo Gradle */
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size != 3) {
                Log.e(TAG, "Uso: LibRetroDownloader <url> <destDir> <coreName>")
                System.exit(1)
            }

            val success = downloadAndExtractCore(args[0], File(args[1]), args[2])
            System.exit(if (success) 0 else 1)
        }

        /** Baixa e extrai um core LibRetro */
        @JvmStatic
        fun downloadAndExtractCore(
                coreUrl: String,
                destinationDir: File,
                coreName: String
        ): Boolean {
            return try {
                Log.i(TAG, "Baixando $coreUrl")

                // Criar diretório se não existir
                destinationDir.mkdirs()

                // Baixar arquivo ZIP
                val zipBytes = downloadFile(coreUrl)

                // Extrair e renomear
                extractAndRename(zipBytes, destinationDir)

                Log.i(TAG, "Sucesso - $coreName extraído para ${destinationDir.absolutePath}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "ERRO ao baixar $coreUrl - ${e.message}", e)
                false
            }
        }

        private fun downloadFile(urlString: String): ByteArray {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "LibRetroDownloader/1.0")

            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${connection.responseCode}: ${connection.responseMessage}")
            }

            return connection.inputStream.use { input ->
                ByteArrayOutputStream().use { output ->
                    input.copyTo(output)
                    output.toByteArray()
                }
            }
        }

        private fun extractAndRename(zipBytes: ByteArray, destinationDir: File) {
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zipIn ->
                var entry = zipIn.nextEntry

                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".so")) {
                        // Extrair diretamente como libcore.so
                        val outputFile = File(destinationDir, "libcore.so")

                        FileOutputStream(outputFile).use { output -> zipIn.copyTo(output) }

                        Log.i(TAG, "Extraído ${entry.name} -> libcore.so")
                        break
                    }
                    entry = zipIn.nextEntry
                }
            }
        }
    }
}
