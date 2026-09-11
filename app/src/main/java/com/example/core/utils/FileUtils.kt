package com.example.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtils {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return if (index == 0) {
            String.format(Locale.US, "%d %s", bytes, units[index])
        } else {
            String.format(Locale.US, "%.1f %s", value, units[index])
        }
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val formatter = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: when (extension) {
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "kt" -> "text/x-kotlin"
            "md" -> "text/markdown"
            "json" -> "application/json"
            else -> "*/*"
        }
    }

    fun renameFile(source: File, newName: String): Result<File> {
        val cleanName = newName.trim()
        if (cleanName.isEmpty() || cleanName.contains("/") || cleanName.contains("\\")) {
            return Result.failure(IllegalArgumentException("Invalid file name"))
        }
        val target = File(source.parentFile, cleanName)
        if (target.exists()) {
            return Result.failure(IllegalStateException("A file with this name already exists"))
        }
        val success = source.renameTo(target)
        return if (success) Result.success(target) else Result.failure(Exception("Failed to rename file"))
    }

    fun deleteFileRecursively(file: File): Boolean {
        return try {
            file.deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }

    fun copyFileOrDirectory(source: File, targetDir: File): Boolean {
        return try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val dest = File(targetDir, source.name)
            if (source.isDirectory) {
                source.copyRecursively(dest, overwrite = true)
            } else {
                source.copyTo(dest, overwrite = true)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun moveFileOrDirectory(source: File, targetDir: File): Boolean {
        return try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val dest = File(targetDir, source.name)
            if (source.renameTo(dest)) {
                true
            } else {
                // Fallback copy then delete
                val copied = if (source.isDirectory) {
                    source.copyRecursively(dest, overwrite = true)
                } else {
                    source.copyTo(dest, overwrite = true)
                    true
                }
                if (copied) {
                    source.deleteRecursively()
                    true
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun createFolder(parentDir: File, name: String): Result<File> {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || cleanName.contains("/") || cleanName.contains("\\")) {
            return Result.failure(IllegalArgumentException("Invalid folder name"))
        }
        val newFolder = File(parentDir, cleanName)
        if (newFolder.exists()) {
            return Result.failure(IllegalStateException("Folder already exists"))
        }
        val created = newFolder.mkdirs()
        return if (created) Result.success(newFolder) else Result.failure(Exception("Could not create folder"))
    }

    fun createTextFile(parentDir: File, name: String, content: String = ""): Result<File> {
        val cleanName = if (name.contains(".")) name.trim() else "${name.trim()}.txt"
        if (cleanName.isEmpty() || cleanName.contains("/") || cleanName.contains("\\")) {
            return Result.failure(IllegalArgumentException("Invalid file name"))
        }
        val newFile = File(parentDir, cleanName)
        if (newFile.exists()) {
            return Result.failure(IllegalStateException("File already exists"))
        }
        return try {
            newFile.writeText(content)
            Result.success(newFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun zipFiles(sourceFiles: List<File>, destZipFile: File): Result<File> {
        return try {
            ZipOutputStream(FileOutputStream(destZipFile)).use { zos ->
                for (file in sourceFiles) {
                    addFileToZip("", file, zos)
                }
            }
            Result.success(destZipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addFileToZip(path: String, file: File, zos: ZipOutputStream) {
        val entryName = if (path.isEmpty()) file.name else "$path/${file.name}"
        if (file.isDirectory) {
            val children = file.listFiles() ?: return
            for (child in children) {
                addFileToZip(entryName, child, zos)
            }
        } else {
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                zos.putNextEntry(ZipEntry(entryName))
                var length: Int
                while (fis.read(buffer).also { length = it } > 0) {
                    zos.write(buffer, 0, length)
                }
                zos.closeEntry()
            }
        }
    }

    fun unzip(zipFile: File, targetDir: File): Result<File> {
        return try {
            if (!targetDir.exists()) targetDir.mkdirs()
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(targetDir, entry.name)
                    // Security check: Zip Slip vulnerability prevention
                    val canonicalDest = targetDir.canonicalPath
                    val canonicalNewFile = newFile.canonicalPath
                    if (!canonicalNewFile.startsWith(canonicalDest)) {
                        throw SecurityException("Zip entry is outside of target dir: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(targetDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareFile(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val mimeType = getMimeType(file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ${file.name}"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareMultipleFiles(context: Context, files: List<File>) {
        if (files.isEmpty()) return
        if (files.size == 1) {
            shareFile(context, files.first())
            return
        }
        try {
            val authority = "${context.packageName}.fileprovider"
            val uris = ArrayList<Uri>()
            for (f in files) {
                if (!f.isDirectory) {
                    uris.add(FileProvider.getUriForFile(context, authority, f))
                }
            }
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ${files.size} files"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openWithExternalApp(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val mimeType = getMimeType(file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with…"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Minimal RFC 4180 CSV line parser supporting quoted commas and escaped quotes
     */
    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }
}
