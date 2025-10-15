package com.vinaooo.revenger.utils

import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Modern utility for downloading and extracting LibRetro files. Replaces the plugin
 * de.undercouch.download with native implementation
 */
class LibRetroDownloader {

    companion object {
        private const val CONNECT_TIMEOUT = 30000 // 30s
        private const val READ_TIMEOUT = 60000 // 60s

        /** Main para ser chamado pelo Gradle */
        @JvmStatic
        fun main(args: Array<String>) {
            require(args.size == 3) { "Uso: LibRetroDownloader <url> <destDir> <coreName>" }

            val success = downloadAndExtractCore(args[0], File(args[1]), args[2])
            System.exit(if (success) 0 else 1)
        }

        /** Baixa e extrai um core LibRetro */
        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun downloadAndExtractCore(
                coreUrl: String,
                destinationDir: File,
                coreName: String
        ): Boolean {
            return try {
                // Create directory if it doesn't exist
                destinationDir.mkdirs()

                // Download ZIP file
                val zipBytes = downloadFile(coreUrl)

                // Extract and rename
                extractAndRename(zipBytes, destinationDir)
                true
            } catch (e: Exception) {
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
                        // Extract directly as libcore.so
                        val outputFile = File(destinationDir, "libcore.so")

                        FileOutputStream(outputFile).use { output -> zipIn.copyTo(output) }
                        break
                    }
                    entry = zipIn.nextEntry
                }
            }
        }
    }
}
