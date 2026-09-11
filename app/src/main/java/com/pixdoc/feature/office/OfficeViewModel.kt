package com.pixdoc.feature.office

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixdoc.core.utils.DocumentConverter
import com.pixdoc.core.utils.FileUtils
import com.pixdoc.data.model.DocumentCategory
import com.pixdoc.data.model.FileItem
import com.pixdoc.data.model.FileType
import com.pixdoc.data.model.SortField
import com.pixdoc.data.model.SortOption
import com.pixdoc.data.model.SortOrder
import com.pixdoc.data.model.ViewMode
import com.pixdoc.data.repository.FileRepository
import com.pixdoc.data.repository.FileRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class OfficeNavTab(val label: String) {
    DOCUMENTS("Documents"),
    TOOLS("Tools & Convert"),
    FOLDERS("Folders")
}

sealed interface OfficeDialog {
    object ImageToPdf : OfficeDialog
    object TextToPdf : OfficeDialog
    data class CreateDoc(val initialType: String = "docx") : OfficeDialog
    data class Rename(val file: File) : OfficeDialog
    data class Delete(val file: File) : OfficeDialog
    data class Info(val item: FileItem) : OfficeDialog
    data class Success(val file: File, val message: String) : OfficeDialog
}

data class OfficeUiState(
    val currentTab: OfficeNavTab = OfficeNavTab.DOCUMENTS,
    val selectedCategory: DocumentCategory = DocumentCategory.ALL,
    val showFavoritesOnly: Boolean = false,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption(SortField.DATE, ascending = false),
    val viewMode: ViewMode = ViewMode.LIST,
    val documents: List<FileItem> = emptyList(),
    val categoryCounts: Map<DocumentCategory, Int> = emptyMap(),
    val recentDocuments: List<FileItem> = emptyList(),
    val favoriteDocuments: List<FileItem> = emptyList(),
    val convertedDocuments: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val permissionGranted: Boolean = true,
    val activeDialog: OfficeDialog? = null,
    val snackbarMessage: String? = null,
    // Folder browser state for Folders tab
    val currentFolderPath: String? = null,
    val folderItems: List<FileItem> = emptyList(),
    val folderBreadcrumbs: List<String> = emptyList()
)

class OfficeViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: FileRepository = FileRepositoryImpl(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OfficeUiState())
    val uiState: StateFlow<OfficeUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun setPermissionState(granted: Boolean) {
        _uiState.update { it.copy(permissionGranted = granted) }
        if (granted) {
            loadAllData()
        }
    }

    fun selectTab(tab: OfficeNavTab) {
        _uiState.update { it.copy(currentTab = tab) }
        when (tab) {
            OfficeNavTab.DOCUMENTS -> refreshDocuments()
            OfficeNavTab.TOOLS -> loadConvertedFiles()
            OfficeNavTab.FOLDERS -> {
                if (_uiState.value.currentFolderPath == null) {
                    loadStorageRoots()
                }
            }
        }
    }

    fun selectCategory(category: DocumentCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
        refreshDocuments()
    }

    fun toggleFavoritesFilter() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
        refreshDocuments()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        refreshDocuments()
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(viewMode = if (it.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
        }
    }

    fun setSortOption(sortOption: SortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
        refreshDocuments()
    }

    fun showDialog(dialog: OfficeDialog?) {
        _uiState.update { it.copy(activeDialog = dialog) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(activeDialog = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun recordDocumentOpened(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordRecentDocument(file)
            loadRecents()
        }
    }

    fun toggleFavorite(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val isNowFav = repository.toggleFavorite(file)
            _uiState.update {
                it.copy(
                    snackbarMessage = if (isNowFav) "Added to Favorites" else "Removed from Favorites"
                )
            }
            loadFavoritesAndRecents()
            refreshDocuments()
        }
    }

    fun isFavorite(path: String): Boolean {
        return repository.isFavorite(path)
    }

    fun refreshDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val state = _uiState.value
                val rawDocs = repository.scanDocuments(
                    category = state.selectedCategory,
                    sortOption = state.sortOption,
                    searchQuery = state.searchQuery
                ).first()

                val docs = if (state.showFavoritesOnly) {
                    rawDocs.filter { repository.isFavorite(it.path) }
                } else {
                    rawDocs
                }

                // Also compute counts across categories
                val allDocs = repository.scanDocuments(
                    category = DocumentCategory.ALL,
                    sortOption = state.sortOption,
                    searchQuery = ""
                ).first()

                val counts = mutableMapOf<DocumentCategory, Int>()
                counts[DocumentCategory.ALL] = allDocs.size
                for (cat in DocumentCategory.values()) {
                    if (cat != DocumentCategory.ALL) {
                        counts[cat] = allDocs.count { doc -> cat.matches(doc.extension) }
                    }
                }

                _uiState.update {
                    it.copy(
                        documents = docs,
                        categoryCounts = counts,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadAllData() {
        refreshDocuments()
        loadRecents()
        loadFavoritesAndRecents()
        loadConvertedFiles()
    }

    private fun loadRecents() {
        viewModelScope.launch {
            val recents = repository.getRecentDocuments().first()
            _uiState.update { it.copy(recentDocuments = recents) }
        }
    }

    private fun loadFavoritesAndRecents() {
        viewModelScope.launch {
            val favs = repository.getFavoriteDocuments().first()
            val recents = repository.getRecentDocuments().first()
            _uiState.update { it.copy(favoriteDocuments = favs, recentDocuments = recents) }
        }
    }

    fun loadConvertedFiles() {
        viewModelScope.launch {
            val converted = repository.getConvertedDocuments().first()
            _uiState.update { it.copy(convertedDocuments = converted) }
        }
    }

    // --- Conversion Actions ---

    fun convertImagesToPdf(imageFiles: List<File>, title: String, onComplete: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) {
                DocumentConverter.convertImagesToPdf(
                    context = getApplication(),
                    imageFiles = imageFiles,
                    docTitle = title.ifBlank { "Converted_Image" }
                )
            }
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess { file ->
                repository.notifyDirectoryChanged()
                loadAllData()
                showDialog(OfficeDialog.Success(file, "PDF generated successfully!"))
                onComplete(file)
            }.onFailure { err ->
                _uiState.update { it.copy(snackbarMessage = "Conversion failed: ${err.message}") }
            }
        }
    }

    fun convertTextToPdf(content: String, title: String, onComplete: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) {
                DocumentConverter.convertTextToPdf(
                    context = getApplication(),
                    content = content,
                    docTitle = title.ifBlank { "Notes_Export" }
                )
            }
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess { file ->
                repository.notifyDirectoryChanged()
                loadAllData()
                showDialog(OfficeDialog.Success(file, "PDF created successfully!"))
                onComplete(file)
            }.onFailure { err ->
                _uiState.update { it.copy(snackbarMessage = "Export failed: ${err.message}") }
            }
        }
    }

    fun convertCsvToPdf(csvFile: File, onComplete: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) {
                DocumentConverter.convertCsvToPdf(
                    context = getApplication(),
                    csvFile = csvFile,
                    docTitle = csvFile.nameWithoutExtension
                )
            }
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess { file ->
                repository.notifyDirectoryChanged()
                loadAllData()
                showDialog(OfficeDialog.Success(file, "Table PDF created successfully!"))
                onComplete(file)
            }.onFailure { err ->
                _uiState.update { it.copy(snackbarMessage = "Conversion failed: ${err.message}") }
            }
        }
    }

    fun extractText(file: File, onComplete: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) {
                if (file.extension.equals("docx", ignoreCase = true)) {
                    DocumentConverter.extractTextFromDocx(getApplication(), file)
                } else {
                    // Export text representation
                    DocumentConverter.createBlankNote(getApplication(), "${file.nameWithoutExtension}_text", file.readText())
                }
            }
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess { txtFile ->
                repository.notifyDirectoryChanged()
                loadAllData()
                showDialog(OfficeDialog.Success(txtFile, "Extracted to text successfully!"))
                onComplete(txtFile)
            }.onFailure { err ->
                _uiState.update { it.copy(snackbarMessage = "Extraction failed: ${err.message}") }
            }
        }
    }

    fun createNewDocument(type: String, title: String, onComplete: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) {
                when (type.lowercase()) {
                    "docx", "word" -> DocumentConverter.createBlankWordDoc(getApplication(), title)
                    "csv", "sheet", "xlsx" -> DocumentConverter.createBlankSpreadsheet(getApplication(), title)
                    else -> DocumentConverter.createBlankNote(getApplication(), title)
                }
            }
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess { newFile ->
                repository.notifyDirectoryChanged()
                loadAllData()
                dismissDialog()
                onComplete(newFile)
            }.onFailure { err ->
                _uiState.update { it.copy(snackbarMessage = "Creation failed: ${err.message}") }
            }
        }
    }

    // --- File operations ---

    fun renameDocument(file: File, newName: String) {
        viewModelScope.launch {
            val res = repository.rename(file, newName)
            res.onSuccess {
                dismissDialog()
                loadAllData()
                _uiState.update { it.copy(snackbarMessage = "Renamed successfully") }
            }.onFailure { err ->
                _uiState.update { it.copy(snackbarMessage = "Rename failed: ${err.message}") }
            }
        }
    }

    fun deleteDocument(file: File) {
        viewModelScope.launch {
            val ok = repository.delete(file)
            dismissDialog()
            if (ok) {
                loadAllData()
                _uiState.update { it.copy(snackbarMessage = "Document deleted") }
            } else {
                _uiState.update { it.copy(snackbarMessage = "Could not delete document") }
            }
        }
    }

    // --- Folder Explorer Methods (for Folders tab) ---

    fun loadStorageRoots() {
        viewModelScope.launch {
            val roots = repository.getStorageRoots()
            _uiState.update {
                it.copy(
                    currentFolderPath = null,
                    folderItems = roots,
                    folderBreadcrumbs = listOf("Storage Roots")
                )
            }
        }
    }

    fun openFolder(path: String) {
        viewModelScope.launch {
            val target = File(path)
            if (!target.exists() || !target.isDirectory) return@launch

            val items = repository.listFiles(path, _uiState.value.sortOption, showHidden = false).first()
            val breadcrumbs = path.split("/").filter { it.isNotBlank() }

            _uiState.update {
                it.copy(
                    currentFolderPath = path,
                    folderItems = items,
                    folderBreadcrumbs = breadcrumbs
                )
            }
        }
    }

    fun navigateUpFolder() {
        val current = _uiState.value.currentFolderPath ?: return
        val parent = File(current).parentFile
        if (parent != null && parent.exists() && parent.canRead()) {
            openFolder(parent.absolutePath)
        } else {
            loadStorageRoots()
        }
    }
}
