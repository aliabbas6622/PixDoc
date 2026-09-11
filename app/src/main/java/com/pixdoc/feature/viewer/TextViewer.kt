package com.pixdoc.feature.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun TextViewer(file: File) {
    val context = LocalContext.current
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var rawText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    var isMonospace by remember { mutableStateOf(true) }
    var showLineNumbers by remember { mutableStateOf(true) }
    var isWordWrap by remember { mutableStateOf(true) }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()

    LaunchedEffect(file) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                // Read up to 2MB to keep performance crisp
                val text = file.bufferedReader().use { it.readText() }
                rawText = text
                lines = text.lines()
            } catch (e: Exception) {
                rawText = "Error reading file: ${e.message}"
                lines = listOf(rawText)
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Toolbar for text viewing options
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Monospace toggle
                        FilterChip(
                            selected = isMonospace,
                            onClick = { isMonospace = !isMonospace },
                            label = { Text("Mono", fontSize = 12.sp) }
                        )

                        // Line numbers toggle
                        FilterChip(
                            selected = showLineNumbers,
                            onClick = { showLineNumbers = !showLineNumbers },
                            label = { Text("Lines", fontSize = 12.sp) }
                        )

                        // Word wrap toggle
                        FilterChip(
                            selected = isWordWrap,
                            onClick = { isWordWrap = !isWordWrap },
                            label = { Text("Wrap", fontSize = 12.sp) }
                        )
                    }

                    Row {
                        IconButton(onClick = { isSearching = !isSearching }) {
                            Icon(Icons.Default.Search, contentDescription = "Search in file")
                        }
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(file.name, rawText))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy all")
                        }
                    }
                }

                // Search Bar
                AnimatedVisibility(visible = isSearching) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Find in document…", fontSize = 13.sp) },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // Content Area
        val textModifier = if (!isWordWrap) {
            Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
        } else {
            Modifier.fillMaxSize()
        }

        val fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default

        LazyColumn(
            state = listState,
            modifier = textModifier
                .testTag("text_viewer_content")
                .padding(vertical = 4.dp)
        ) {
            itemsIndexed(lines, key = { index, _ -> index }) { index, line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (showLineNumbers) {
                        Text(
                            text = "${index + 1}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.width(36.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        VerticalDivider(
                            modifier = Modifier
                                .height(16.dp)
                                .padding(vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Render highlighted text or regular text
                    val displayText = if (searchQuery.isNotBlank() && line.contains(searchQuery, ignoreCase = true)) {
                        buildAnnotatedString {
                            var startIndex = 0
                            val lowerLine = line.lowercase()
                            val lowerQuery = searchQuery.lowercase()
                            while (startIndex < line.length) {
                                val matchIndex = lowerLine.indexOf(lowerQuery, startIndex)
                                if (matchIndex == -1) {
                                    append(line.substring(startIndex))
                                    break
                                }
                                append(line.substring(startIndex, matchIndex))
                                withStyle(
                                    SpanStyle(
                                        background = Color(0xFFFFD54F),
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(line.substring(matchIndex, matchIndex + searchQuery.length))
                                }
                                startIndex = matchIndex + searchQuery.length
                            }
                        }
                    } else {
                        buildAnnotatedString { append(line) }
                    }

                    Text(
                        text = displayText,
                        fontFamily = fontFamily,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}
