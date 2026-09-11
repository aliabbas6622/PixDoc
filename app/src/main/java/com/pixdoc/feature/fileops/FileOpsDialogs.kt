package com.pixdoc.feature.fileops

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixdoc.core.utils.FileUtils
import com.pixdoc.data.model.FileItem
import java.io.File

@Composable
fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = if (it.trim().isEmpty()) "Name cannot be empty" else null
                    },
                    label = { Text("File Name") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = name.trim()
                    if (clean.isNotEmpty()) onConfirm(clean)
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("rename_confirm_button")
            ) {
                Text("Rename")
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
fun DeleteConfirmDialog(
    items: List<FileItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val message = if (items.size == 1) {
        "Are you sure you want to permanently delete \"${items[0].name}\"?"
    } else {
        "Are you sure you want to permanently delete ${items.size} items?"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = { Text("Delete Confirmation") },
        text = {
            Column {
                Text(message)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("delete_confirm_button")
            ) {
                Text("Delete")
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
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
        title = { Text("New Folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = if (it.trim().isEmpty()) "Folder name cannot be empty" else null
                },
                label = { Text("Folder Name") },
                placeholder = { Text("e.g. Documents") },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_folder_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = name.trim()
                    if (clean.isNotEmpty()) onConfirm(clean)
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("new_folder_confirm_button")
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
fun CreateFileDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, content: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Text File") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("File Name") },
                    placeholder = { Text("notes.txt") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_file_name_input")
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content (Optional)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = name.trim()
                    if (clean.isNotEmpty()) onConfirm(clean, content)
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("new_file_confirm_button")
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
fun ZipDialog(
    itemCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("Archive_${System.currentTimeMillis() / 1000}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Zip Archive") },
        text = {
            Column {
                Text("Compress $itemCount selected item(s) into a ZIP archive.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Archive Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = name.trim()
                    if (clean.isNotEmpty()) onConfirm(clean)
                },
                enabled = name.trim().isNotEmpty()
            ) {
                Text("Compress")
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
fun FolderPickerDialog(
    title: String,
    initialDir: File,
    actionButtonLabel: String,
    onDismiss: () -> Unit,
    onSelectFolder: (File) -> Unit
) {
    var currentDir by remember { mutableStateOf(initialDir) }
    val subfolders = remember(currentDir) {
        currentDir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name.lowercase() } ?: emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentDir.parentFile != null) {
                    IconButton(onClick = {
                        val p = currentDir.parentFile
                        if (p != null) currentDir = p
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Target: ${currentDir.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currentDir.absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                if (subfolders.isEmpty()) {
                    Text(
                        "No subfolders found inside this directory.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(subfolders, key = { it.absolutePath }) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentDir = folder }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelectFolder(currentDir) }) {
                Text(actionButtonLabel)
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
fun FileDetailsDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text("File Information") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow(label = "Name", value = item.name)
                DetailRow(label = "Type", value = item.fileType.displayName)
                if (!item.isDirectory) {
                    DetailRow(label = "Size", value = "${FileUtils.formatFileSize(item.size)} (${item.size} bytes)")
                } else {
                    DetailRow(label = "Items", value = "${item.itemCount ?: 0} items")
                }
                DetailRow(label = "Modified", value = FileUtils.formatDate(item.lastModified))
                DetailRow(label = "Path", value = item.readablePath)
                DetailRow(label = "MIME Type", value = FileUtils.getMimeType(item.file))
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
