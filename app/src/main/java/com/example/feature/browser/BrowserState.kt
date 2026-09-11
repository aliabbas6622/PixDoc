package com.example.feature.browser

import com.example.data.model.FileItem
import com.example.data.model.SortOption
import com.example.data.repository.StorageVolumeInfo
import java.io.File

data class BreadcrumbItem(
    val name: String,
    val path: String
)

sealed class DialogState {
    object CreateFolder : DialogState()
    object CreateFile : DialogState()
    data class Rename(val item: FileItem) : DialogState()
    data class DeleteConfirm(val items: List<FileItem>) : DialogState()
    data class Move(val items: List<FileItem>) : DialogState()
    data class Copy(val items: List<FileItem>) : DialogState()
    data class Zip(val items: List<FileItem>) : DialogState()
    data class Unzip(val item: FileItem) : DialogState()
    data class Details(val item: FileItem) : DialogState()
}

data class BrowserState(
    val currentPath: String? = null,
    val currentDirName: String = "Files",
    val breadcrumbs: List<BreadcrumbItem> = emptyList(),
    val storageRoots: List<FileItem> = emptyList(),
    val files: List<FileItem> = emptyList(),
    val storageInfo: StorageVolumeInfo? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val sortOption: SortOption = SortOption(),
    val isGridView: Boolean = false,
    val showHidden: Boolean = false,
    val isLoading: Boolean = false,
    val selectionMode: Boolean = false,
    val selectedPaths: Set<String> = emptySet(),
    val hasStoragePermission: Boolean = true,
    val dialogState: DialogState? = null,
    val statusMessage: String? = null
) {
    val displayedFiles: List<FileItem>
        get() {
            val list = if (searchQuery.isBlank()) {
                files
            } else {
                files.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
            }
            return if (selectionMode) {
                list.map { it.copy(isSelected = selectedPaths.contains(it.path)) }
            } else {
                list
            }
        }

    val selectedCount: Int
        get() = selectedPaths.size

    val isRoot: Boolean
        get() = currentPath == null
}
