package com.pixdoc.feature.browser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixdoc.data.model.FileItem
import com.pixdoc.data.model.SortOption
import com.pixdoc.data.repository.FileRepository
import com.pixdoc.data.repository.FileRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class BrowserViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: FileRepository = FileRepositoryImpl(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BrowserState())
    val uiState: StateFlow<BrowserState> = _uiState.asStateFlow()

    init {
        loadRoots()
    }

    fun loadRoots() {
        val roots = repository.getStorageRoots()
        val info = repository.getStorageInfo()
        _uiState.update {
            it.copy(
                currentPath = null,
                currentDirName = "Files",
                breadcrumbs = emptyList(),
                storageRoots = roots,
                files = emptyList(),
                storageInfo = info,
                selectionMode = false,
                selectedPaths = emptySet(),
                isLoading = false
            )
        }
    }

    fun openDirectory(path: String) {
        val file = File(path)
        if (!file.exists() || !file.isDirectory) return

        val breadcrumbs = generateBreadcrumbs(file)
        val name = when {
            path.contains("Sample_Documents") -> "Sample Documents"
            path == "/storage/emulated/0" -> "Internal Storage"
            else -> file.name
        }

        _uiState.update {
            it.copy(
                currentPath = path,
                currentDirName = name,
                breadcrumbs = breadcrumbs,
                isLoading = true,
                selectionMode = false,
                selectedPaths = emptySet()
            )
        }

        loadFilesForCurrentPath()
    }

    fun loadFilesForCurrentPath() {
        val path = _uiState.value.currentPath ?: return
        viewModelScope.launch {
            repository.listFiles(
                directoryPath = path,
                sortOption = _uiState.value.sortOption,
                showHidden = _uiState.value.showHidden
            ).collect { items ->
                _uiState.update {
                    it.copy(
                        files = items,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun navigateUp(): Boolean {
        val current = _uiState.value.currentPath ?: return false
        val currentFile = File(current)
        val parent = currentFile.parentFile

        // Check if we reached root of our storage or top
        if (parent == null || current == "/storage/emulated/0" || currentFile.name == "Sample_Documents") {
            loadRoots()
            return true
        }

        openDirectory(parent.absolutePath)
        return true
    }

    private fun generateBreadcrumbs(file: File): List<BreadcrumbItem> {
        val list = mutableListOf<BreadcrumbItem>()
        var curr: File? = file
        while (curr != null) {
            val name = when {
                curr.absolutePath.contains("Sample_Documents") && curr.name == "Sample_Documents" -> "Sample Documents"
                curr.absolutePath == "/storage/emulated/0" -> "Internal Storage"
                curr.name.isEmpty() -> "Root"
                else -> curr.name
            }
            list.add(0, BreadcrumbItem(name = name, path = curr.absolutePath))

            if (curr.absolutePath == "/storage/emulated/0" || curr.name == "Sample_Documents") {
                break
            }
            curr = curr.parentFile
        }
        return list
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch() {
        _uiState.update {
            val nextState = !it.isSearching
            it.copy(
                isSearching = nextState,
                searchQuery = if (!nextState) "" else it.searchQuery
            )
        }
    }

    fun setSortOption(sortOption: SortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
        loadFilesForCurrentPath()
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun toggleShowHidden() {
        _uiState.update { it.copy(showHidden = !it.showHidden) }
        loadFilesForCurrentPath()
    }

    fun toggleSelection(item: FileItem) {
        _uiState.update { state ->
            val set = state.selectedPaths.toMutableSet()
            if (set.contains(item.path)) {
                set.remove(item.path)
            } else {
                set.add(item.path)
            }
            state.copy(
                selectionMode = set.isNotEmpty(),
                selectedPaths = set
            )
        }
    }

    fun enterSelectionMode(item: FileItem) {
        _uiState.update {
            it.copy(
                selectionMode = true,
                selectedPaths = setOf(item.path)
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allPaths = state.displayedFiles.map { it.path }.toSet()
            state.copy(
                selectionMode = true,
                selectedPaths = allPaths
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectionMode = false,
                selectedPaths = emptySet()
            )
        }
    }

    fun showDialog(dialog: DialogState) {
        _uiState.update { it.copy(dialogState = dialog) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = null) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun setPermissionState(granted: Boolean) {
        _uiState.update { it.copy(hasStoragePermission = granted) }
        if (granted) {
            loadRoots()
        }
    }

    fun renameFile(item: FileItem, newName: String) {
        viewModelScope.launch {
            val result = repository.rename(item.file, newName)
            if (result.isSuccess) {
                _uiState.update { it.copy(statusMessage = "Renamed to $newName", dialogState = null) }
                loadFilesForCurrentPath()
            } else {
                _uiState.update {
                    it.copy(
                        statusMessage = "Rename failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}",
                        dialogState = null
                    )
                }
            }
        }
    }

    fun deleteItems(items: List<FileItem>) {
        viewModelScope.launch {
            val count = repository.deleteBatch(items.map { it.file })
            _uiState.update {
                it.copy(
                    statusMessage = "Deleted $count item(s)",
                    dialogState = null,
                    selectionMode = false,
                    selectedPaths = emptySet()
                )
            }
            loadFilesForCurrentPath()
        }
    }

    fun createFolder(name: String) {
        val path = _uiState.value.currentPath ?: return
        viewModelScope.launch {
            val result = repository.createFolder(File(path), name)
            if (result.isSuccess) {
                _uiState.update { it.copy(statusMessage = "Folder created", dialogState = null) }
                loadFilesForCurrentPath()
            } else {
                _uiState.update {
                    it.copy(
                        statusMessage = "Failed: ${result.exceptionOrNull()?.message}",
                        dialogState = null
                    )
                }
            }
        }
    }

    fun createTextFile(name: String, content: String) {
        val path = _uiState.value.currentPath ?: return
        viewModelScope.launch {
            val result = repository.createTextFile(File(path), name, content)
            if (result.isSuccess) {
                _uiState.update { it.copy(statusMessage = "File created", dialogState = null) }
                loadFilesForCurrentPath()
            } else {
                _uiState.update {
                    it.copy(
                        statusMessage = "Failed: ${result.exceptionOrNull()?.message}",
                        dialogState = null
                    )
                }
            }
        }
    }

    fun moveItems(items: List<FileItem>, targetDir: File) {
        viewModelScope.launch {
            var count = 0
            for (item in items) {
                if (repository.move(item.file, targetDir)) count++
            }
            _uiState.update {
                it.copy(
                    statusMessage = "Moved $count item(s)",
                    dialogState = null,
                    selectionMode = false,
                    selectedPaths = emptySet()
                )
            }
            loadFilesForCurrentPath()
        }
    }

    fun copyItems(items: List<FileItem>, targetDir: File) {
        viewModelScope.launch {
            var count = 0
            for (item in items) {
                if (repository.copy(item.file, targetDir)) count++
            }
            _uiState.update {
                it.copy(
                    statusMessage = "Copied $count item(s)",
                    dialogState = null,
                    selectionMode = false,
                    selectedPaths = emptySet()
                )
            }
            loadFilesForCurrentPath()
        }
    }

    fun zipItems(items: List<FileItem>, zipName: String) {
        val path = _uiState.value.currentPath ?: return
        val cleanName = if (zipName.endsWith(".zip")) zipName else "$zipName.zip"
        val destFile = File(path, cleanName)

        viewModelScope.launch {
            val result = repository.zip(items.map { it.file }, destFile)
            if (result.isSuccess) {
                _uiState.update { it.copy(statusMessage = "Compressed to $cleanName", dialogState = null) }
                loadFilesForCurrentPath()
            } else {
                _uiState.update {
                    it.copy(
                        statusMessage = "Zip failed: ${result.exceptionOrNull()?.message}",
                        dialogState = null
                    )
                }
            }
        }
    }

    fun unzipItem(item: FileItem) {
        val parent = item.file.parentFile ?: return
        val folderName = item.file.nameWithoutExtension
        val targetDir = File(parent, folderName)

        viewModelScope.launch {
            val result = repository.unzip(item.file, targetDir)
            if (result.isSuccess) {
                _uiState.update { it.copy(statusMessage = "Extracted to $folderName/", dialogState = null) }
                loadFilesForCurrentPath()
            } else {
                _uiState.update {
                    it.copy(
                        statusMessage = "Unzip failed: ${result.exceptionOrNull()?.message}",
                        dialogState = null
                    )
                }
            }
        }
    }
}
