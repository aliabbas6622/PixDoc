package com.example.data.model

import java.io.File

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val extension: String = if (file.isDirectory) "" else file.extension,
    val fileType: FileType = FileType.fromExtension(if (file.isDirectory) "" else file.extension, file.isDirectory),
    val itemCount: Int? = null,
    val isHidden: Boolean = file.isHidden || file.name.startsWith("."),
    val isSelected: Boolean = false
) {
    val readablePath: String
        get() = path.replace("/storage/emulated/0", "Internal Storage")
}
