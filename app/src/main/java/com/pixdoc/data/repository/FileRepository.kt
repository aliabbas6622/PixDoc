package com.pixdoc.data.repository

import android.content.Context
import android.os.Environment
import com.pixdoc.core.utils.DocumentConverter
import com.pixdoc.core.utils.FileUtils
import com.pixdoc.data.model.FileItem
import com.pixdoc.data.model.FileType
import com.pixdoc.data.model.SortField
import com.pixdoc.data.model.SortOption
import com.pixdoc.data.model.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

data class StorageVolumeInfo(
    val name: String,
    val path: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val isPrimary: Boolean = false
) {
    val usedPercent: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

interface FileRepository {
    fun getStorageRoots(): List<FileItem>
    fun getStorageInfo(): StorageVolumeInfo
    fun listFiles(directoryPath: String, sortOption: SortOption, showHidden: Boolean): Flow<List<FileItem>>
    suspend fun rename(file: File, newName: String): Result<File>
    suspend fun delete(file: File): Boolean
    suspend fun deleteBatch(files: List<File>): Int
    suspend fun move(source: File, targetDir: File): Boolean
    suspend fun copy(source: File, targetDir: File): Boolean
    suspend fun createFolder(parentDir: File, name: String): Result<File>
    suspend fun createTextFile(parentDir: File, name: String, content: String = ""): Result<File>
    suspend fun zip(files: List<File>, destZip: File): Result<File>
    suspend fun unzip(zipFile: File, targetDir: File): Result<File>
    fun notifyDirectoryChanged()
    fun scanDocuments(category: com.pixdoc.data.model.DocumentCategory, sortOption: SortOption, searchQuery: String): Flow<List<FileItem>>
    fun getRecentDocuments(): Flow<List<FileItem>>
    fun recordRecentDocument(file: File)
    fun getFavoriteDocuments(): Flow<List<FileItem>>
    fun toggleFavorite(file: File): Boolean
    fun isFavorite(path: String): Boolean
    fun getConvertedDocuments(): Flow<List<FileItem>>
}

class FileRepositoryImpl(
    private val context: Context
) : FileRepository {

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        // Prepare sample documents folder on first run
        SampleFileGenerator.generateSampleFiles(context)
    }

    override fun getStorageRoots(): List<FileItem> {
        val list = mutableListOf<FileItem>()

        // 1. Sample documents (guaranteed accessible, rich documents)
        val sampleDir = File(context.filesDir, "Sample_Documents")
        if (sampleDir.exists()) {
            list.add(
                FileItem(
                    file = sampleDir,
                    name = "Sample Documents",
                    fileType = FileType.FOLDER,
                    itemCount = sampleDir.listFiles()?.size ?: 0
                )
            )
        }

        // 2. Primary external storage
        val extStorage = Environment.getExternalStorageDirectory()
        if (extStorage != null && extStorage.exists()) {
            list.add(
                FileItem(
                    file = extStorage,
                    name = "Internal Storage",
                    fileType = FileType.FOLDER,
                    itemCount = extStorage.listFiles()?.size
                )
            )
        }

        // 3. Download folder
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadDir != null && downloadDir.exists()) {
            list.add(
                FileItem(
                    file = downloadDir,
                    name = "Downloads",
                    fileType = FileType.FOLDER,
                    itemCount = downloadDir.listFiles()?.size
                )
            )
        }

        // 4. Documents folder
        val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (docDir != null && docDir.exists()) {
            list.add(
                FileItem(
                    file = docDir,
                    name = "Documents",
                    fileType = FileType.FOLDER,
                    itemCount = docDir.listFiles()?.size
                )
            )
        }

        // 5. Pictures folder
        val picDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (picDir != null && picDir.exists()) {
            list.add(
                FileItem(
                    file = picDir,
                    name = "Pictures",
                    fileType = FileType.FOLDER,
                    itemCount = picDir.listFiles()?.size
                )
            )
        }

        return list
    }

    override fun getStorageInfo(): StorageVolumeInfo {
        val root = Environment.getExternalStorageDirectory() ?: context.filesDir
        val total = root.totalSpace
        val free = root.freeSpace
        val used = (total - free).coerceAtLeast(0L)
        return StorageVolumeInfo(
            name = "Device Storage",
            path = root.absolutePath,
            totalBytes = total,
            freeBytes = free,
            usedBytes = used,
            isPrimary = true
        )
    }

    override fun listFiles(
        directoryPath: String,
        sortOption: SortOption,
        showHidden: Boolean
    ): Flow<List<FileItem>> = flow {
        val target = File(directoryPath)
        if (!target.exists() || !target.isDirectory) {
            emit(emptyList())
            return@flow
        }

        val rawFiles = target.listFiles()?.toList() ?: emptyList()
        val filtered = if (showHidden) rawFiles else rawFiles.filter { !it.name.startsWith(".") && !it.isHidden }

        val items = filtered.map { file ->
            val count = if (file.isDirectory) file.listFiles()?.size else null
            FileItem(
                file = file,
                itemCount = count
            )
        }

        val sorted = sortItems(items, sortOption)
        emit(sorted)
    }.flowOn(Dispatchers.IO)

    private fun sortItems(items: List<FileItem>, sortOption: SortOption): List<FileItem> {
        val comparator: Comparator<FileItem> = when (sortOption.field) {
            SortField.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortField.DATE -> compareBy { it.lastModified }
            SortField.SIZE -> compareBy { it.size }
            SortField.TYPE -> compareBy { it.fileType.name }
        }

        val effectiveComparator = if (sortOption.ascending) comparator else comparator.reversed()

        // Always show folders first, then files
        return items.sortedWith { a, b ->
            if (a.isDirectory && !b.isDirectory) -1
            else if (!a.isDirectory && b.isDirectory) 1
            else effectiveComparator.compare(a, b)
        }
    }

    override suspend fun rename(file: File, newName: String): Result<File> {
        return FileUtils.renameFile(file, newName).also {
            notifyDirectoryChanged()
        }
    }

    override suspend fun delete(file: File): Boolean {
        return FileUtils.deleteFileRecursively(file).also {
            notifyDirectoryChanged()
        }
    }

    override suspend fun deleteBatch(files: List<File>): Int {
        var count = 0
        for (f in files) {
            if (FileUtils.deleteFileRecursively(f)) count++
        }
        notifyDirectoryChanged()
        return count
    }

    override suspend fun move(source: File, targetDir: File): Boolean {
        return FileUtils.moveFileOrDirectory(source, targetDir).also {
            notifyDirectoryChanged()
        }
    }

    override suspend fun copy(source: File, targetDir: File): Boolean {
        return FileUtils.copyFileOrDirectory(source, targetDir).also {
            notifyDirectoryChanged()
        }
    }

    override suspend fun createFolder(parentDir: File, name: String): Result<File> {
        return FileUtils.createFolder(parentDir, name).also {
            notifyDirectoryChanged()
        }
    }

    override suspend fun createTextFile(parentDir: File, name: String, content: String): Result<File> {
        return FileUtils.createTextFile(parentDir, name, content).also {
            notifyDirectoryChanged()
        }
    }

    override suspend fun zip(files: List<File>, destZip: File): Result<File> {
        return FileUtils.zipFiles(files, destZip).also {
            notifyDirectoryChanged()
        }
    }

    override suspend fun unzip(zipFile: File, targetDir: File): Result<File> {
        return FileUtils.unzip(zipFile, targetDir).also {
            notifyDirectoryChanged()
        }
    }

    override fun notifyDirectoryChanged() {
        refreshTrigger.tryEmit(Unit)
    }

    private val prefs = context.getSharedPreferences("office_suite_prefs", Context.MODE_PRIVATE)

    override fun isFavorite(path: String): Boolean {
        val favs = prefs.getStringSet("favorite_paths", emptySet()) ?: emptySet()
        return favs.contains(path)
    }

    override fun toggleFavorite(file: File): Boolean {
        val favs = prefs.getStringSet("favorite_paths", emptySet())?.toMutableSet() ?: mutableSetOf()
        val path = file.absolutePath
        val newState = if (favs.contains(path)) {
            favs.remove(path)
            false
        } else {
            favs.add(path)
            true
        }
        prefs.edit().putStringSet("favorite_paths", favs).apply()
        notifyDirectoryChanged()
        return newState
    }

    override fun recordRecentDocument(file: File) {
        if (!file.exists() || file.isDirectory) return
        val currentRecents = prefs.getString("recent_list", "") ?: ""
        val paths = currentRecents.split(";").filter { it.isNotBlank() && it != file.absolutePath }.toMutableList()
        paths.add(0, file.absolutePath)
        val trimmed = paths.take(20).joinToString(";")
        prefs.edit().putString("recent_list", trimmed).apply()
        notifyDirectoryChanged()
    }

    override fun getRecentDocuments(): Flow<List<FileItem>> = flow {
        val currentRecents = prefs.getString("recent_list", "") ?: ""
        val paths = currentRecents.split(";").filter { it.isNotBlank() }
        val items = mutableListOf<FileItem>()

        for (path in paths) {
            val file = File(path)
            if (file.exists() && file.isFile) {
                items.add(FileItem(file = file))
            }
        }

        // If no recents recorded yet, populate with initial sample documents
        if (items.isEmpty()) {
            val sampleDir = File(context.filesDir, "Sample_Documents")
            if (sampleDir.exists()) {
                val samples = sampleDir.listFiles()?.filter { it.isFile } ?: emptyList()
                for (s in samples.take(5)) {
                    items.add(FileItem(file = s))
                }
            }
        }

        emit(items)
    }.flowOn(Dispatchers.IO)

    override fun getFavoriteDocuments(): Flow<List<FileItem>> = flow {
        val favs = prefs.getStringSet("favorite_paths", emptySet()) ?: emptySet()
        val items = mutableListOf<FileItem>()
        for (path in favs) {
            val file = File(path)
            if (file.exists() && file.isFile) {
                items.add(FileItem(file = file))
            }
        }
        emit(items.sortedByDescending { it.lastModified })
    }.flowOn(Dispatchers.IO)

    override fun getConvertedDocuments(): Flow<List<FileItem>> = flow {
        val convertedDir = DocumentConverter.getConvertedDir(context)
        val files = convertedDir.listFiles()?.filter { it.isFile } ?: emptyList()
        val items: List<FileItem> = files.map { FileItem(file = it) }.sortedByDescending { it.lastModified }
        emit(items)
    }.flowOn(Dispatchers.IO)

    override fun scanDocuments(
        category: com.pixdoc.data.model.DocumentCategory,
        sortOption: SortOption,
        searchQuery: String
    ): Flow<List<FileItem>> = flow {
        val collectedFiles = mutableSetOf<File>()

        // 1. App internal sample documents (always available)
        val sampleDir = File(context.filesDir, "Sample_Documents")
        if (sampleDir.exists()) {
            val sampleFiles = sampleDir.listFiles()?.filter { it.isFile }
            if (sampleFiles != null) {
                collectedFiles.addAll(sampleFiles)
            }
        }

        // 2. Converted documents
        val convertedDir = DocumentConverter.getConvertedDir(context)
        if (convertedDir.exists()) {
            val convFiles = convertedDir.listFiles()?.filter { it.isFile }
            if (convFiles != null) {
                collectedFiles.addAll(convFiles)
            }
        }

        // 3. Public Document, Download, and Picture directories
        val publicDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        )

        for (dir in publicDirs) {
            if (dir != null && dir.exists()) {
                scanDirFast(dir, collectedFiles, maxDepth = 2)
            }
        }

        // 4. Primary external storage root shallow scan
        val extRoot = Environment.getExternalStorageDirectory()
        if (extRoot != null && extRoot.exists()) {
            scanDirFast(extRoot, collectedFiles, maxDepth = 2)
        }

        // Filter by category
        val categoryFiltered = collectedFiles.filter { file: File ->
            category.matches(file.extension)
        }

        // Filter by search query
        val searchFiltered = if (searchQuery.isBlank()) {
            categoryFiltered
        } else {
            val q = searchQuery.trim().lowercase()
            categoryFiltered.filter { it.name.lowercase().contains(q) }
        }

        val items: List<FileItem> = searchFiltered.map { FileItem(file = it) }
        val sorted: List<FileItem> = sortItems(items, sortOption)
        emit(sorted)
    }.flowOn(Dispatchers.IO)

    private fun scanDirFast(directory: File, targetSet: MutableSet<File>, maxDepth: Int, currentDepth: Int = 0) {
        if (currentDepth > maxDepth) return
        val children = directory.listFiles() ?: return
        for (child in children) {
            if (child.name.startsWith(".") || child.isHidden) continue
            // Skip large system dirs like Android data/obb
            if (child.isDirectory) {
                if (child.name.equals("Android", ignoreCase = true) || child.name.equals(".git", ignoreCase = true)) {
                    continue
                }
                scanDirFast(child, targetSet, maxDepth, currentDepth + 1)
            } else if (child.isFile) {
                targetSet.add(child)
            }
        }
    }
}
