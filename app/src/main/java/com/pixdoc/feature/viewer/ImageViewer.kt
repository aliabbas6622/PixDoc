package com.pixdoc.feature.viewer

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pixdoc.core.utils.FileUtils
import com.pixdoc.data.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ImageViewer(
    currentFile: File,
    onNavigateSibling: ((File) -> Unit)? = null
) {
    val context = LocalContext.current

    // Sibling image discovery in the same folder
    val siblingImages = remember(currentFile) {
        val parent = currentFile.parentFile
        parent?.listFiles()?.filter {
            !it.isDirectory && FileType.fromExtension(it.extension, false) == FileType.IMAGE
        }?.sortedBy { it.name.lowercase() } ?: listOf(currentFile)
    }

    val currentIndex = remember(currentFile, siblingImages) {
        siblingImages.indexOfFirst { it.absolutePath == currentFile.absolutePath }.coerceAtLeast(0)
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showControls by remember { mutableStateOf(true) }

    // Reset transform when file changes
    LaunchedEffect(currentFile) {
        scale = 1f
        offset = Offset.Zero
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(currentFile) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(currentFile) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(0.8f, 5f)
                    scale = newScale
                    offset = if (newScale > 1f) {
                        Offset(offset.x + pan.x, offset.y + pan.y)
                    } else {
                        Offset.Zero
                    }
                }
            }
    ) {
        // Image content
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(currentFile)
                .crossfade(true)
                .build(),
            contentDescription = currentFile.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .testTag("image_viewer_content")
        )

        // Floating info & zoom badge
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Prev image
                    if (siblingImages.size > 1) {
                        IconButton(
                            onClick = {
                                if (currentIndex > 0) {
                                    onNavigateSibling?.invoke(siblingImages[currentIndex - 1])
                                }
                            },
                            enabled = currentIndex > 0
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Image",
                                tint = if (currentIndex > 0) Color.White else Color.Gray
                            )
                        }
                    }

                    // Index indicator & file size
                    Text(
                        text = if (siblingImages.size > 1) {
                            "${currentIndex + 1} / ${siblingImages.size}  •  ${FileUtils.formatFileSize(currentFile.length())}"
                        } else {
                            FileUtils.formatFileSize(currentFile.length())
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Zoom reset
                    if (scale != 1f) {
                        IconButton(onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        }) {
                            Icon(
                                Icons.Default.ZoomOut,
                                contentDescription = "Reset Zoom",
                                tint = Color.White
                            )
                        }
                    }

                    // Next image
                    if (siblingImages.size > 1) {
                        IconButton(
                            onClick = {
                                if (currentIndex < siblingImages.size - 1) {
                                    onNavigateSibling?.invoke(siblingImages[currentIndex + 1])
                                }
                            },
                            enabled = currentIndex < siblingImages.size - 1
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Image",
                                tint = if (currentIndex < siblingImages.size - 1) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
