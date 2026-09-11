package com.example.feature.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.example.core.utils.FileUtils
import com.example.data.model.FileItem
import com.example.data.model.FileType
import com.example.feature.fileops.FileDetailsDialog
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.runtime.rememberCoroutineScope
import com.example.core.utils.DocumentConverter
import com.example.feature.office.ConversionSuccessDialog
import com.example.ui.theme.ColorPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    filePath: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentFile by remember(filePath) { mutableStateOf(File(filePath)) }
    val fileType = remember(currentFile) {
        FileType.fromExtension(currentFile.extension, currentFile.isDirectory)
    }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var convertedPdfFile by remember { mutableStateOf<File?>(null) }

    if (!currentFile.exists()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("File Not Found") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                Text(
                    text = "File does not exist or was moved: ${currentFile.name}",
                    modifier = Modifier.padding(padding)
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentFile.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("viewer_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!currentFile.extension.equals("pdf", ignoreCase = true)) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val res = withContext(Dispatchers.IO) {
                                        if (fileType == FileType.IMAGE) {
                                            DocumentConverter.convertImagesToPdf(context, listOf(currentFile), currentFile.nameWithoutExtension)
                                        } else if (currentFile.extension.equals("csv", ignoreCase = true)) {
                                            DocumentConverter.convertCsvToPdf(context, currentFile, currentFile.nameWithoutExtension)
                                        } else {
                                            DocumentConverter.convertTextToPdf(context, currentFile.readText(), currentFile.nameWithoutExtension)
                                        }
                                    }
                                    res.onSuccess { pdf ->
                                        convertedPdfFile = pdf
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Convert to PDF")
                        }
                    }
                    IconButton(onClick = { FileUtils.shareFile(context, currentFile) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { FileUtils.openWithExternalApp(context, currentFile) }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open with")
                    }
                    IconButton(onClick = { showDetailsDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Info")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (fileType) {
                FileType.IMAGE -> {
                    ImageViewer(
                        currentFile = currentFile,
                        onNavigateSibling = { newFile -> currentFile = newFile }
                    )
                }
                FileType.PDF -> {
                    PdfViewer(file = currentFile)
                }
                FileType.TEXT, FileType.CODE -> {
                    TextViewer(file = currentFile)
                }
                FileType.DOCX -> {
                    DocxViewer(file = currentFile)
                }
                FileType.XLSX -> {
                    XlsxViewer(file = currentFile)
                }
                FileType.PPTX -> {
                    PptxViewer(file = currentFile)
                }
                FileType.AUDIO, FileType.VIDEO -> {
                    MediaViewer(file = currentFile)
                }
                else -> {
                    UnsupportedDocFallback(file = currentFile)
                }
            }
        }
    }

    if (showDetailsDialog) {
        FileDetailsDialog(
            item = FileItem(file = currentFile),
            onDismiss = { showDetailsDialog = false }
        )
    }

    convertedPdfFile?.let { newPdf ->
        ConversionSuccessDialog(
            file = newPdf,
            message = "Converted to PDF successfully!",
            onDismiss = { convertedPdfFile = null },
            onOpen = {
                currentFile = newPdf
                convertedPdfFile = null
            },
            onShare = {
                FileUtils.shareFile(context, newPdf)
            }
        )
    }
}
