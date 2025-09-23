package com.vinaooo.revenger.utils

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
        private const val CONNECT_TIMEOUT = 30000 // 30s
        private const val READ_TIMEOUT = 60000 // 60s

        /** Main para ser chamado pelo Gradle */
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size != 3) {
                System.err.println("Uso: LibRetroDownloader <url> <destDir> <coreName>")
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
                println("LibRetroDownloader: Baixando $coreUrl")

                // Criar diretório se não existir
                destinationDir.mkdirs()

                // Baixar arquivo ZIP
                val zipBytes = downloadFile(coreUrl)

                // Extrair e renomear
                extractAndRename(zipBytes, destinationDir, coreName)

                println(
                        "LibRetroDownloader: Sucesso - $coreName extraído para ${destinationDir.absolutePath}"
                )
                true
            } catch (e: Exception) {
                println("LibRetroDownloader: ERRO ao baixar $coreUrl - ${e.message}")
                e.printStackTrace()
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

        private fun extractAndRename(zipBytes: ByteArray, destinationDir: File, coreName: String) {
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zipIn ->
                var entry = zipIn.nextEntry

                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".so")) {
                        // Extrair diretamente como libcore.so
                        val outputFile = File(destinationDir, "libcore.so")

                        FileOutputStream(outputFile).use { output -> zipIn.copyTo(output) }

                        println("LibRetroDownloader: Extraído ${entry.name} -> libcore.so")
                        break
                    }
                    entry = zipIn.nextEntry
                }
            }
        }
    }
}
