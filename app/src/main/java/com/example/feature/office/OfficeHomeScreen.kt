package com.example.feature.office

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.utils.FileUtils
import com.example.data.model.DocumentCategory
import com.example.data.model.FileItem
import com.example.data.model.FileType
import com.example.data.model.SortField
import com.example.data.model.SortOption
import com.example.data.model.ViewMode
import com.example.ui.theme.ColorDocx
import com.example.ui.theme.ColorPdf
import com.example.ui.theme.ColorText
import com.example.ui.theme.ColorXlsx
import com.example.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeHomeScreen(
    viewModel: OfficeViewModel,
    onOpenFile: (FileItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSortMenu by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Office Suite",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshDocuments() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                if (uiState.viewMode == ViewMode.LIST) Icons.Filled.GridView else Icons.Filled.ViewList,
                                contentDescription = "Toggle View"
                            )
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Date (Newest)") },
                                    onClick = {
                                        viewModel.setSortOption(SortOption(SortField.DATE, false))
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (A-Z)") },
                                    onClick = {
                                        viewModel.setSortOption(SortOption(SortField.NAME, true))
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Size (Largest)") },
                                    onClick = {
                                        viewModel.setSortOption(SortOption(SortField.SIZE, false))
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                // Clean, compact search field
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search documents...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Category Filter Chips
                if (uiState.currentTab == OfficeNavTab.DOCUMENTS) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "fav_filter_chip") {
                            FilterChip(
                                selected = uiState.showFavoritesOnly,
                                onClick = { viewModel.toggleFavoritesFilter() },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Starred",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                        items(DocumentCategory.values(), key = { it.name }) { cat ->
                            val isSelected = cat == uiState.selectedCategory
                            val count = uiState.categoryCounts[cat] ?: 0
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectCategory(cat) },
                                label = {
                                    Text(
                                        if (count > 0) "${cat.title} ($count)" else cat.title,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == OfficeNavTab.DOCUMENTS,
                    onClick = { viewModel.selectTab(OfficeNavTab.DOCUMENTS) },
                    icon = { Icon(Icons.Filled.Description, contentDescription = null) },
                    label = { Text("Documents") }
                )
                NavigationBarItem(
                    selected = uiState.currentTab == OfficeNavTab.TOOLS,
                    onClick = { viewModel.selectTab(OfficeNavTab.TOOLS) },
                    icon = { Icon(Icons.Filled.Transform, contentDescription = null) },
                    label = { Text("Tools") }
                )
                NavigationBarItem(
                    selected = uiState.currentTab == OfficeNavTab.FOLDERS,
                    onClick = { viewModel.selectTab(OfficeNavTab.FOLDERS) },
                    icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                    label = { Text("Folders") }
                )
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showCreateMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("office_create_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create or Convert")
                }

                DropdownMenu(
                    expanded = showCreateMenu,
                    onDismissRequest = { showCreateMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("New Word Doc (.docx)") },
                        leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null, tint = ColorDocx) },
                        onClick = {
                            showCreateMenu = false
                            viewModel.showDialog(OfficeDialog.CreateDoc("docx"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Sheet (.csv)") },
                        leadingIcon = { Icon(Icons.Filled.TableChart, contentDescription = null, tint = ColorXlsx) },
                        onClick = {
                            showCreateMenu = false
                            viewModel.showDialog(OfficeDialog.CreateDoc("csv"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Note (.txt)") },
                        leadingIcon = { Icon(Icons.Filled.Notes, contentDescription = null, tint = ColorText) },
                        onClick = {
                            showCreateMenu = false
                            viewModel.showDialog(OfficeDialog.CreateDoc("txt"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Image to PDF") },
                        leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null, tint = ColorPdf) },
                        onClick = {
                            showCreateMenu = false
                            viewModel.showDialog(OfficeDialog.ImageToPdf)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Text to PDF") },
                        leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = ColorPdf) },
                        onClick = {
                            showCreateMenu = false
                            viewModel.showDialog(OfficeDialog.TextToPdf)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (uiState.currentTab) {
                OfficeNavTab.DOCUMENTS -> {
                    DocumentsTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenFile = { fileItem ->
                            viewModel.recordDocumentOpened(fileItem.file)
                            onOpenFile(fileItem)
                        }
                    )
                }
                OfficeNavTab.TOOLS -> {
                    ToolsTabContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        onOpenFile = { fileItem ->
                            viewModel.recordDocumentOpened(fileItem.file)
                            onOpenFile(fileItem)
                        }
                    )
                }
                OfficeNavTab.FOLDERS -> {
                    FoldersTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenFile = { fileItem ->
                            if (fileItem.isDirectory) {
                                viewModel.openFolder(fileItem.path)
                            } else {
                                viewModel.recordDocumentOpened(fileItem.file)
                                onOpenFile(fileItem)
                            }
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            }
        }
    }

    OfficeDialogHost(
        viewModel = viewModel,
        uiState = uiState,
        onOpenFile = { file ->
            onOpenFile(FileItem(file = file))
        }
    )
}

@Composable
fun DocumentsTabContent(
    uiState: OfficeUiState,
    viewModel: OfficeViewModel,
    onOpenFile: (FileItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        // Recent documents shelf (only if recents exist and no search is active)
        if (uiState.recentDocuments.isNotEmpty() && uiState.searchQuery.isEmpty()) {
            item(key = "recents_header") {
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp)
                )
            }
            item(key = "recents_carousel") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.recentDocuments.take(5), key = { it.path }) { item ->
                        OutlinedCard(
                            modifier = Modifier
                                .width(130.dp)
                                .clickable { onOpenFile(item) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = item.extension.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = FileUtils.formatFileSize(item.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.documents.isEmpty()) {
            item(key = "empty_view") {
                EmptyStateView(category = uiState.selectedCategory)
            }
        } else {
            item(key = "docs_count") {
                Text(
                    text = "${uiState.documents.size} files",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                )
            }
            items(uiState.documents, key = { it.path }) { fileItem ->
                OptimizedDocumentRow(
                    item = fileItem,
                    isFavorite = viewModel.isFavorite(fileItem.path),
                    onOpen = { onOpenFile(fileItem) },
                    onToggleFavorite = { viewModel.toggleFavorite(fileItem.file) },
                    onConvertPdf = {
                        if (fileItem.fileType == FileType.IMAGE) {
                            viewModel.convertImagesToPdf(listOf(fileItem.file), fileItem.file.nameWithoutExtension) { onOpenFile(FileItem(it)) }
                        } else if (fileItem.extension.equals("csv", ignoreCase = true)) {
                            viewModel.convertCsvToPdf(fileItem.file) { onOpenFile(FileItem(it)) }
                        } else {
                            viewModel.convertTextToPdf(fileItem.file.readText(), fileItem.file.nameWithoutExtension) { onOpenFile(FileItem(it)) }
                        }
                    },
                    onExtractText = { viewModel.extractText(fileItem.file) { onOpenFile(FileItem(it)) } },
                    onRename = { viewModel.showDialog(OfficeDialog.Rename(fileItem.file)) },
                    onDelete = { viewModel.showDialog(OfficeDialog.Delete(fileItem.file)) },
                    onInfo = { viewModel.showDialog(OfficeDialog.Info(fileItem)) }
                )
            }
        }
    }
}

@Composable
fun OptimizedDocumentRow(
    item: FileItem,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onConvertPdf: () -> Unit,
    onExtractText: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clean neutral badge
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.extension.take(3).uppercase().ifEmpty { "DOC" },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${FileUtils.formatFileSize(item.size)} • ${FileUtils.formatDate(item.lastModified)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
            Icon(
                if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "Star",
                tint = if (isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Actions",
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Open") },
                    onClick = {
                        menuExpanded = false
                        onOpen()
                    }
                )
                if (item.fileType != FileType.PDF) {
                    DropdownMenuItem(
                        text = { Text("Convert to PDF") },
                        onClick = {
                            menuExpanded = false
                            onConvertPdf()
                        }
                    )
                }
                if (item.fileType == FileType.DOCX || item.fileType == FileType.PDF) {
                    DropdownMenuItem(
                        text = { Text("Extract Text") },
                        onClick = {
                            menuExpanded = false
                            onExtractText()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = {
                        menuExpanded = false
                        FileUtils.shareFile(context, item.file)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Properties") },
                    onClick = {
                        menuExpanded = false
                        onInfo()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
fun ToolsTabContent(
    viewModel: OfficeViewModel,
    uiState: OfficeUiState,
    onOpenFile: (FileItem) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.showDialog(OfficeDialog.ImageToPdf) }
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Image to PDF", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Combine photos into PDF", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.showDialog(OfficeDialog.TextToPdf) }
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Text to PDF", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Export notes to PDF", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.showDialog(OfficeDialog.CreateDoc("docx")) }
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("New Word Doc", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Create .docx file", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.showDialog(OfficeDialog.CreateDoc("csv")) }
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("New Spreadsheet", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Create .csv table", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (uiState.convertedDocuments.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Converted Files (${uiState.convertedDocuments.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(uiState.convertedDocuments, key = { it.path }) { convItem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenFile(convItem) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(convItem.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(FileUtils.formatFileSize(convItem.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { FileUtils.shareFile(context, convItem.file) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FoldersTabContent(
    uiState: OfficeUiState,
    viewModel: OfficeViewModel,
    onOpenFile: (FileItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.currentFolderPath != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateUpFolder() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = uiState.currentFolderPath?.substringAfterLast('/') ?: "Folders",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HorizontalDivider()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            items(uiState.folderItems, key = { it.path }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenFile(item) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (item.isDirectory) Icons.Filled.Folder else item.fileType.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (item.isDirectory) "${item.itemCount ?: 0} items" else FileUtils.formatFileSize(item.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(category: DocumentCategory) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "No ${category.title} found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
