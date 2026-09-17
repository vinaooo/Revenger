package com.vinaooo.revenger.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Modern utility for downloading and extracting LibRetro files. Replaces the plugin
 * de.undercouch.download with native implementation
 */
class LibRetroDownloader private constructor() {

    companion object {
        private const val CONNECT_TIMEOUT = 30000 // 30s
        private const val READ_TIMEOUT = 60000 // 60s

        /** Main to be called by Gradle */
        @JvmStatic
        fun main(args: Array<String>) {
            require(args.size >= 2) { "Usage: LibRetroDownloader <url> <destDir>" }

            val success = downloadAndExtractCore(args[0], File(args[1]))
            System.exit(if (success) 0 else 1)
        }

        /** Downloads and extracts a LibRetro core */
        @JvmStatic
        fun downloadAndExtractCore(
                coreUrl: String,
                destinationDir: File
        ): Boolean {
            return try {
                // Create directory if it doesn't exist
                destinationDir.mkdirs()

                // Download ZIP file
                val zipBytes = downloadFile(coreUrl)

                // Extract and rename
                extractAndRename(zipBytes, destinationDir)
                true
                // Every failure in downloadFile()/extractAndRename() (URL parsing, HTTP
                // connection, ZIP extraction, file writes) surfaces as IOException. Not logged:
                // this class runs on the plain JVM against android.jar stubs during the Gradle
                // build (see main() below), where android.util.Log throws "Stub!" -- the name
                // escape hatch documents the intentional silence instead.
            } catch (ignoredDownloadFailure: IOException) {
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
