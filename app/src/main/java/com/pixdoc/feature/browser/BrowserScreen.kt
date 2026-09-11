package com.pixdoc.feature.browser

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pixdoc.core.utils.FileUtils
import com.pixdoc.data.model.FileItem
import com.pixdoc.data.model.FileType
import com.pixdoc.data.model.SortField
import com.pixdoc.data.model.SortOption
import com.pixdoc.feature.fileops.CreateFileDialog
import com.pixdoc.feature.fileops.CreateFolderDialog
import com.pixdoc.feature.fileops.DeleteConfirmDialog
import com.pixdoc.feature.fileops.FileDetailsDialog
import com.pixdoc.feature.fileops.FolderPickerDialog
import com.pixdoc.feature.fileops.RenameDialog
import com.pixdoc.feature.fileops.ZipDialog
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onOpenFile: (FileItem) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    // Intercept back button to navigate up folder hierarchy
    BackHandler(enabled = !uiState.isRoot || uiState.selectionMode || uiState.isSearching) {
        when {
            uiState.selectionMode -> viewModel.clearSelection()
            uiState.isSearching -> viewModel.toggleSearch()
            !uiState.isRoot -> viewModel.navigateUp()
        }
    }

    // Snackbar notifications
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.selectionMode) {
                // Selection Mode Top Bar
                TopAppBar(
                    title = {
                        Text("${uiState.selectedCount} selected")
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.clearSelection() },
                            modifier = Modifier.testTag("exit_selection_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Selection")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.selectAll() },
                            modifier = Modifier.testTag("select_all_button")
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else if (uiState.isSearching) {
                // Search Top Bar
                TopAppBar(
                    title = {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search files and folders…") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_input")
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Search")
                        }
                    },
                    actions = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    }
                )
            } else {
                // Normal Top Bar
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!uiState.isRoot) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = uiState.currentDirName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        if (!uiState.isRoot) {
                            IconButton(
                                onClick = { viewModel.navigateUp() },
                                modifier = Modifier.testTag("browser_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (!uiState.isRoot) {
                            IconButton(
                                onClick = { viewModel.toggleSearch() },
                                modifier = Modifier.testTag("search_button")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(
                                onClick = { viewModel.toggleViewMode() },
                                modifier = Modifier.testTag("toggle_view_mode_button")
                            ) {
                                Icon(
                                    if (uiState.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle View"
                                )
                            }
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("sort_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                            }
                        }

                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.testTag("more_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }

                        // More dropdown menu
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            if (!uiState.isRoot) {
                                DropdownMenuItem(
                                    text = { Text("New Folder") },
                                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.showDialog(DialogState.CreateFolder)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New File") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.showDialog(DialogState.CreateFile)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (uiState.showHidden) "Hide Hidden Files" else "Show Hidden Files") },
                                    leadingIcon = {
                                        Icon(
                                            if (uiState.showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.toggleShowHidden()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Refresh") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    if (uiState.isRoot) viewModel.loadRoots() else viewModel.loadFilesForCurrentPath()
                                }
                            )
                        }

                        // Sort dropdown menu
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            Text(
                                "Sort By",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            SortField.values().forEach { field ->
                                DropdownMenuItem(
                                    text = { Text(field.label) },
                                    leadingIcon = {
                                        RadioButton(
                                            selected = uiState.sortOption.field == field,
                                            onClick = null
                                        )
                                    },
                                    onClick = {
                                        showSortMenu = false
                                        viewModel.setSortOption(uiState.sortOption.copy(field = field))
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (uiState.sortOption.ascending) "Ascending (A-Z)" else "Descending (Z-A)") },
                                onClick = {
                                    showSortMenu = false
                                    viewModel.setSortOption(uiState.sortOption.toggleOrder())
                                }
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (uiState.selectionMode) {
                val selectedFiles = uiState.displayedFiles.filter { uiState.selectedPaths.contains(it.path) }
                BottomAppBar(
                    actions = {
                        IconButton(onClick = {
                            FileUtils.shareMultipleFiles(context, selectedFiles.map { it.file })
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share selected")
                        }
                        IconButton(onClick = {
                            viewModel.showDialog(DialogState.DeleteConfirm(selectedFiles))
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                        IconButton(onClick = {
                            viewModel.showDialog(DialogState.Move(selectedFiles))
                        }) {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move selected")
                        }
                        IconButton(onClick = {
                            viewModel.showDialog(DialogState.Copy(selectedFiles))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy selected")
                        }
                        IconButton(onClick = {
                            viewModel.showDialog(DialogState.Zip(selectedFiles))
                        }) {
                            Icon(Icons.Default.Archive, contentDescription = "Zip selected")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isRoot && !uiState.selectionMode) {
                FloatingActionButton(
                    onClick = { viewModel.showDialog(DialogState.CreateFolder) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("browser_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add folder")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Breadcrumbs Navigation Strip
            if (!uiState.isRoot && uiState.breadcrumbs.isNotEmpty()) {
                val breadcrumbScroll = rememberScrollState()
                Surface(
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(breadcrumbScroll)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home icon
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { viewModel.loadRoots() }
                        )

                        for (crumb in uiState.breadcrumbs) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                            val isLast = crumb.path == uiState.currentPath
                            Text(
                                text = crumb.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                                color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .clickable {
                                        if (!isLast) viewModel.openDirectory(crumb.path)
                                    }
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            // Main Content Area
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.isRoot) {
                // Storage Overview & Root Folders Screen
                StorageOverviewScreen(
                    roots = uiState.storageRoots,
                    storageInfo = uiState.storageInfo,
                    onOpenFolder = { item -> viewModel.openDirectory(item.path) }
                )
            } else {
                val files = uiState.displayedFiles
                if (files.isEmpty()) {
                    // Empty state
                    EmptyStateScreen(
                        isSearch = uiState.searchQuery.isNotBlank(),
                        searchQuery = uiState.searchQuery,
                        onCreateFolder = { viewModel.showDialog(DialogState.CreateFolder) },
                        onCreateFile = { viewModel.showDialog(DialogState.CreateFile) }
                    )
                } else if (uiState.isGridView) {
                    // Grid View
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("file_grid_view")
                    ) {
                        items(files, key = { it.path }) { item ->
                            FileGridItem(
                                item = item,
                                inSelectionMode = uiState.selectionMode,
                                onClick = {
                                    if (uiState.selectionMode) {
                                        viewModel.toggleSelection(item)
                                    } else if (item.isDirectory) {
                                        viewModel.openDirectory(item.path)
                                    } else {
                                        onOpenFile(item)
                                    }
                                },
                                onLongClick = {
                                    if (!uiState.selectionMode) {
                                        viewModel.enterSelectionMode(item)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // List View (Milestone 2: LazyColumn with key = { it.path })
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("file_list_view")
                    ) {
                        items(files, key = { it.path }) { item ->
                            FileListItem(
                                item = item,
                                inSelectionMode = uiState.selectionMode,
                                onClick = {
                                    if (uiState.selectionMode) {
                                        viewModel.toggleSelection(item)
                                    } else if (item.isDirectory) {
                                        viewModel.openDirectory(item.path)
                                    } else {
                                        onOpenFile(item)
                                    }
                                },
                                onLongClick = {
                                    if (!uiState.selectionMode) {
                                        viewModel.enterSelectionMode(item)
                                    }
                                },
                                onAction = { action ->
                                    handleItemAction(action, item, viewModel, context)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    when (val d = uiState.dialogState) {
        is DialogState.CreateFolder -> {
            CreateFolderDialog(
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name -> viewModel.createFolder(name) }
            )
        }
        is DialogState.CreateFile -> {
            CreateFileDialog(
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name, content -> viewModel.createTextFile(name, content) }
            )
        }
        is DialogState.Rename -> {
            RenameDialog(
                initialName = d.item.name,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { newName -> viewModel.renameFile(d.item, newName) }
            )
        }
        is DialogState.DeleteConfirm -> {
            DeleteConfirmDialog(
                items = d.items,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { viewModel.deleteItems(d.items) }
            )
        }
        is DialogState.Move -> {
            val root = File(uiState.currentPath ?: context.filesDir.absolutePath)
            FolderPickerDialog(
                title = "Move ${d.items.size} item(s)",
                initialDir = root,
                actionButtonLabel = "Move Here",
                onDismiss = { viewModel.dismissDialog() },
                onSelectFolder = { target -> viewModel.moveItems(d.items, target) }
            )
        }
        is DialogState.Copy -> {
            val root = File(uiState.currentPath ?: context.filesDir.absolutePath)
            FolderPickerDialog(
                title = "Copy ${d.items.size} item(s)",
                initialDir = root,
                actionButtonLabel = "Copy Here",
                onDismiss = { viewModel.dismissDialog() },
                onSelectFolder = { target -> viewModel.copyItems(d.items, target) }
            )
        }
        is DialogState.Zip -> {
            ZipDialog(
                itemCount = d.items.size,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { zipName -> viewModel.zipItems(d.items, zipName) }
            )
        }
        is DialogState.Details -> {
            FileDetailsDialog(
                item = d.item,
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        else -> {}
    }
}

// Item actions helper
private fun handleItemAction(
    action: String,
    item: FileItem,
    viewModel: BrowserViewModel,
    context: Context
) {
    when (action) {
        "rename" -> viewModel.showDialog(DialogState.Rename(item))
        "delete" -> viewModel.showDialog(DialogState.DeleteConfirm(listOf(item)))
        "share" -> FileUtils.shareFile(context, item.file)
        "move" -> viewModel.showDialog(DialogState.Move(listOf(item)))
        "copy" -> viewModel.showDialog(DialogState.Copy(listOf(item)))
        "zip" -> viewModel.showDialog(DialogState.Zip(listOf(item)))
        "unzip" -> viewModel.unzipItem(item)
        "info" -> viewModel.showDialog(DialogState.Details(item))
    }
}

// Storage Overview Screen
@Composable
private fun StorageOverviewScreen(
    roots: List<FileItem>,
    storageInfo: com.pixdoc.data.repository.StorageVolumeInfo?,
    onOpenFolder: (FileItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Storage Space Card
        if (storageInfo != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    storageInfo.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                "${(storageInfo.usedPercent * 100).toInt()}% used",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { storageInfo.usedPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Used: ${FileUtils.formatFileSize(storageInfo.usedBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                "Free: ${FileUtils.formatFileSize(storageInfo.freeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                "Locations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Root Folder Cards
        items(roots, key = { it.path }) { rootItem ->
            val isSample = rootItem.name.contains("Sample")
            Card(
                onClick = { onOpenFolder(rootItem) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSample) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isSample) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSample) Icons.Default.FolderOpen else Icons.Default.Folder,
                            contentDescription = null,
                            tint = if (isSample) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rootItem.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSample) "Rich sample files (PDF, DOCX, XLSX, PPTX, Images, Code)" else rootItem.readablePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

// File List Item
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListItem(
    item: FileItem,
    inSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAction: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = if (item.isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox in selection mode or file icon
            if (inSelectionMode) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else {
                FileThumbnail(item = item, size = 42)
                Spacer(modifier = Modifier.width(14.dp))
            }

            // File Name & Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subText = if (item.isDirectory) {
                    val count = item.itemCount
                    if (count != null) "$count items" else "Folder"
                } else {
                    "${FileUtils.formatFileSize(item.size)}  •  ${FileUtils.formatDate(item.lastModified)}"
                }
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // More actions icon
            if (!inSelectionMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "File Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                showMenu = false
                                onAction("rename")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                showMenu = false
                                onAction("share")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move") },
                            onClick = {
                                showMenu = false
                                onAction("move")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy") },
                            onClick = {
                                showMenu = false
                                onAction("copy")
                            }
                        )
                        if (item.fileType == FileType.ARCHIVE) {
                            DropdownMenuItem(
                                text = { Text("Extract / Unzip") },
                                onClick = {
                                    showMenu = false
                                    onAction("unzip")
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Compress to Zip") },
                                onClick = {
                                    showMenu = false
                                    onAction("zip")
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Details") },
                            onClick = {
                                showMenu = false
                                onAction("info")
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onAction("delete")
                            }
                        )
                    }
                }
            }
        }
    }
}

// File Grid Item
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGridItem(
    item: FileItem,
    inSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                FileThumbnail(item = item, size = 50)
                if (inSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                    ) {
                        Checkbox(
                            checked = item.isSelected,
                            onCheckedChange = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// File Thumbnail (Coil AsyncImage for images, or colored vector icon badge)
@Composable
private fun FileThumbnail(item: FileItem, size: Int = 42) {
    if (item.fileType == FileType.IMAGE) {
        AsyncImage(
            model = item.file,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .background(item.fileType.color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.fileType.icon,
                contentDescription = null,
                tint = item.fileType.color,
                modifier = Modifier.size((size * 0.6f).dp)
            )
        }
    }
}

// Empty State Screen
@Composable
private fun EmptyStateScreen(
    isSearch: Boolean,
    searchQuery: String,
    onCreateFolder: () -> Unit,
    onCreateFile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isSearch) Icons.Default.Search else Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )

            Text(
                text = if (isSearch) "No results found" else "This folder is empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isSearch) "No files matching \"$searchQuery\"" else "Create a new folder or file to get started.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!isSearch) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onCreateFolder) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Folder")
                    }
                    OutlinedButton(onClick = onCreateFile) {
                        Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New File")
                    }
                }
            }
        }
    }
}
