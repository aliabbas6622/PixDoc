package com.pixdoc.feature.office

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixdoc.core.utils.FileUtils
import com.pixdoc.data.model.FileItem
import com.pixdoc.data.model.FileType
import com.pixdoc.ui.theme.ColorDocx
import com.pixdoc.ui.theme.ColorPdf
import com.pixdoc.ui.theme.ColorText
import com.pixdoc.ui.theme.ColorXlsx
import com.pixdoc.ui.theme.PrimaryBlue
import java.io.File

@Composable
fun OfficeDialogHost(
    viewModel: OfficeViewModel,
    uiState: OfficeUiState,
    onOpenFile: (File) -> Unit
) {
    val context = LocalContext.current

    when (val dialog = uiState.activeDialog) {
        is OfficeDialog.ImageToPdf -> {
            ImageToPdfDialog(
                allImages = uiState.documents.filter { it.fileType == FileType.IMAGE }.map { it.file },
                onDismiss = { viewModel.dismissDialog() },
                onConvert = { selectedImages, title ->
                    viewModel.convertImagesToPdf(selectedImages, title) { newFile ->
                        onOpenFile(newFile)
                    }
                }
            )
        }
        is OfficeDialog.TextToPdf -> {
            TextToPdfDialog(
                onDismiss = { viewModel.dismissDialog() },
                onConvert = { text, title ->
                    viewModel.convertTextToPdf(text, title) { newFile ->
                        onOpenFile(newFile)
                    }
                }
            )
        }
        is OfficeDialog.CreateDoc -> {
            CreateDocumentDialog(
                initialType = dialog.initialType,
                onDismiss = { viewModel.dismissDialog() },
                onCreate = { type, title ->
                    viewModel.createNewDocument(type, title) { newFile ->
                        onOpenFile(newFile)
                    }
                }
            )
        }
        is OfficeDialog.Rename -> {
            RenameDocDialog(
                file = dialog.file,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { newName ->
                    viewModel.renameDocument(dialog.file, newName)
                }
            )
        }
        is OfficeDialog.Delete -> {
            DeleteDocDialog(
                file = dialog.file,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = {
                    viewModel.deleteDocument(dialog.file)
                }
            )
        }
        is OfficeDialog.Info -> {
            DocInfoDialog(
                item = dialog.item,
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is OfficeDialog.Success -> {
            ConversionSuccessDialog(
                file = dialog.file,
                message = dialog.message,
                onDismiss = { viewModel.dismissDialog() },
                onOpen = {
                    viewModel.dismissDialog()
                    onOpenFile(dialog.file)
                },
                onShare = {
                    FileUtils.shareFile(context, dialog.file)
                }
            )
        }
        null -> {}
    }
}

@Composable
fun ImageToPdfDialog(
    allImages: List<File>,
    onDismiss: () -> Unit,
    onConvert: (List<File>, String) -> Unit
) {
    var title by remember { mutableStateOf("Photos_Doc") }
    val selectedImages = remember { mutableStateListOf<File>().apply { addAll(allImages.take(3)) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Images to PDF", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("PDF Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "Select Images (${selectedImages.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))

                if (allImages.isEmpty()) {
                    Text(
                        "No images found on device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                    ) {
                        items(allImages) { img ->
                            val isSelected = selectedImages.contains(img)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isSelected) selectedImages.remove(img) else selectedImages.add(img)
                                    }
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = img.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConvert(selectedImages.toList(), title) },
                enabled = selectedImages.isNotEmpty()
            ) {
                Text("Convert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TextToPdfDialog(
    onDismiss: () -> Unit,
    onConvert: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("Notes_Doc") }
    var textContent by remember {
        mutableStateOf(
            "Project Notes:\n1. Office Document Suite\n2. Built-in converter"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Notes to PDF", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = textContent,
                    onValueChange = { textContent = it },
                    label = { Text("Content") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConvert(textContent, title) },
                enabled = textContent.isNotBlank()
            ) {
                Text("Convert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateDocumentDialog(
    initialType: String = "docx",
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var docName by remember {
        mutableStateOf(
            when (initialType) {
                "docx" -> "Meeting_Notes"
                "csv" -> "Quarterly_Budget"
                else -> "Daily_Ideas"
            }
        )
    }

    val typeOptions = listOf(
        Triple("docx", "Word Doc (.docx)", ColorDocx),
        Triple("csv", "Spreadsheet (.csv)", ColorXlsx),
        Triple("txt", "Text Note (.txt)", ColorText)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("New Document", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((typeKey, label) in listOf("docx" to "Word (.docx)", "csv" to "Sheet (.csv)", "txt" to "Note (.txt)")) {
                        val isSelected = selectedType == typeKey
                        androidx.compose.material3.FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = typeKey },
                            label = { Text(typeKey.uppercase(), style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = docName,
                    onValueChange = { docName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(selectedType, docName) },
                enabled = docName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConversionSuccessDialog(
    file: File,
    message: String,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text("Success!", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Size: ${FileUtils.formatFileSize(file.length())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpen,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Open Document")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share")
            }
        }
    )
}

@Composable
fun RenameDocDialog(
    file: File,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(file.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Document") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New file name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank() && newName != file.name
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DeleteDocDialog(
    file: File,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Document") },
        text = { Text("Are you sure you want to delete \"${file.name}\"? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DocInfoDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Document Properties") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Name: ${item.name}", fontWeight = FontWeight.Bold)
                Text("Type: ${item.fileType.displayName}")
                Text("Size: ${FileUtils.formatFileSize(item.size)}")
                Text("Modified: ${FileUtils.formatDate(item.lastModified)}")
                Text("Path: ${item.path}", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
