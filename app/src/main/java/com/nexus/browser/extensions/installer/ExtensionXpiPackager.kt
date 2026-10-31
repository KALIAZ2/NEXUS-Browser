package com.nexus.browser.extensions.installer

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Empaqueteur d'archives WebExtension (.xpi) conformes à la spécification Mozilla.
 */
@Singleton
class ExtensionXpiPackager @Inject constructor() {

    /**
     * Crée une archive .xpi valide à partir d'une table de fichiers (chemin relatif -> contenu UTF-8).
     */
    fun createXpiArchive(
        outputFile: File,
        files: Map<String, String>
    ): Result<File> {
        return runCatching {
            require(files.containsKey("manifest.json")) {
                "Une archive XPI doit obligatoirement contenir un fichier manifest.json à sa racine."
            }
            outputFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                files.forEach { (relativePath, content) ->
                    val safeEntryName = relativePath.trimStart('/', '\\').replace('\\', '/')
                    require(!safeEntryName.contains("..")) {
                        "Chemin de fichier non sécurisé détecté : $safeEntryName"
                    }
                    val entry = ZipEntry(safeEntryName)
                    zos.putNextEntry(entry)
                    val bytes = content.toByteArray(Charsets.UTF_8)
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
            outputFile
        }
    }
}
