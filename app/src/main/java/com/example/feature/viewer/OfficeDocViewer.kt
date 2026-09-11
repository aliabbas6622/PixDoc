package com.example.feature.viewer

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.utils.FileUtils
import com.example.data.model.FileType
import com.example.ui.theme.ColorDocx
import com.example.ui.theme.ColorPptx
import com.example.ui.theme.ColorXlsx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

// --- Models ---
data class DocParagraph(
    val text: String,
    val isTitle: Boolean = false,
    val isHeading: Boolean = false,
    val isBullet: Boolean = false,
    val isBold: Boolean = false,
    val isItalic: Boolean = false
)

data class SpreadsheetData(
    val title: String,
    val rows: List<List<String>>
)

data class PresentationSlide(
    val slideNumber: Int,
    val title: String,
    val bullets: List<String>
)

// --- DOCX Viewer ---
@Composable
fun DocxViewer(file: File) {
    val context = LocalContext.current
    var paragraphs by remember { mutableStateOf<List<DocParagraph>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                paragraphs = parseDocx(file)
            } catch (e: Exception) {
                error = "Unable to parse Word document: ${e.message}"
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

    if (error != null || paragraphs.isEmpty()) {
        UnsupportedDocFallback(file, message = error ?: "No document content found")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("docx_viewer_content")
    ) {
        // Document Card container (mimics a page)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    for (p in paragraphs) {
                        when {
                            p.isTitle -> {
                                Text(
                                    text = p.text,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorDocx,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            p.isHeading -> {
                                Text(
                                    text = p.text,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            p.isBullet -> {
                                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                    Text(
                                        text = "• ",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = p.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (p.isBold) FontWeight.Bold else FontWeight.Normal,
                                        fontStyle = if (p.isItalic) FontStyle.Italic else FontStyle.Normal
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = p.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp,
                                    fontWeight = if (p.isBold) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = if (p.isItalic) FontStyle.Italic else FontStyle.Normal,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- XLSX / Spreadsheet Viewer ---
@Composable
fun XlsxViewer(file: File) {
    var sheetData by remember { mutableStateOf<SpreadsheetData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                sheetData = if (file.extension.equals("csv", ignoreCase = true)) {
                    parseCsv(file)
                } else {
                    parseXlsx(file)
                }
            } catch (e: Exception) {
                error = "Unable to parse spreadsheet: ${e.message}"
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

    val data = sheetData
    if (error != null || data == null || data.rows.isEmpty()) {
        UnsupportedDocFallback(file, message = error ?: "Spreadsheet is empty")
        return
    }

    val horizontalScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sheet Header Info
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TableChart,
                        contentDescription = null,
                        tint = ColorXlsx,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${data.rows.size} rows",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        // Table Grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScroll)
                .testTag("xlsx_viewer_content")
        ) {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(data.rows) { rowIndex, row ->
                    val isHeader = rowIndex == 0
                    Row(
                        modifier = Modifier
                            .background(
                                when {
                                    isHeader -> ColorXlsx.copy(alpha = 0.15f)
                                    rowIndex % 2 == 1 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        // Row index header cell (1, 2, 3...)
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(38.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isHeader) "#" else "$rowIndex",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Data cells
                        for (cell in row) {
                            Box(
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(38.dp)
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = cell,
                                    style = if (isHeader) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    color = if (isHeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- PPTX / Presentation Viewer ---
@Composable
fun PptxViewer(file: File) {
    var slides by remember { mutableStateOf<List<PresentationSlide>>(emptyList()) }
    var currentSlideIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                slides = parsePptx(file)
            } catch (e: Exception) {
                error = "Unable to parse presentation: ${e.message}"
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

    if (error != null || slides.isEmpty()) {
        UnsupportedDocFallback(file, message = error ?: "No slides found in presentation")
        return
    }

    val slide = slides[currentSlideIndex.coerceIn(0, slides.size - 1)]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF263238))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Slideshow,
                    contentDescription = null,
                    tint = ColorPptx,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    file.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Text(
                "Slide ${currentSlideIndex + 1} of ${slides.size}",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Active Slide Card (16:9 ratio)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .testTag("pptx_slide_card"),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Slide Title Banner
                Text(
                    text = slide.title.ifBlank { "Slide ${slide.slideNumber}" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ColorPptx
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Bullets
                for (bullet in slide.bullets) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            text = bullet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF212121),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Navigation Footer
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = { if (currentSlideIndex > 0) currentSlideIndex-- },
                    enabled = currentSlideIndex > 0
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Slide",
                        tint = if (currentSlideIndex > 0) Color.White else Color.Gray
                    )
                }

                Text(
                    text = "${currentSlideIndex + 1} / ${slides.size}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { if (currentSlideIndex < slides.size - 1) currentSlideIndex++ },
                    enabled = currentSlideIndex < slides.size - 1
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Slide",
                        tint = if (currentSlideIndex < slides.size - 1) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}

// --- Fallback for unsupported / external formats ---
@Composable
fun UnsupportedDocFallback(
    file: File,
    message: String = "No built-in preview available for this format."
) {
    val context = LocalContext.current
    val fileType = FileType.fromExtension(file.extension, file.isDirectory)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = fileType.icon,
                    contentDescription = null,
                    tint = fileType.color,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "${fileType.displayName}  •  ${FileUtils.formatFileSize(file.length())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { FileUtils.openWithExternalApp(context, file) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open with External App")
                }
            }
        }
    }
}

// --- Parsers ---

private fun parseDocx(file: File): List<DocParagraph> {
    val paragraphs = mutableListOf<DocParagraph>()
    ZipInputStream(FileInputStream(file)).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(zis, "UTF-8")

                var eventType = parser.eventType
                var currentText = StringBuilder()
                var isBold = false
                var isItalic = false
                var isTitle = false
                var isHeading = false

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name) {
                                "p" -> {
                                    currentText = StringBuilder()
                                    isBold = false
                                    isItalic = false
                                    isTitle = false
                                    isHeading = false
                                }
                                "b" -> isBold = true
                                "i" -> isItalic = true
                                "sz" -> {
                                    val sizeVal = parser.getAttributeValue(null, "val")?.toIntOrNull() ?: 0
                                    if (sizeVal >= 40) isTitle = true
                                    else if (sizeVal >= 28) isHeading = true
                                }
                                "br", "cr" -> currentText.append("\n")
                                "t" -> {
                                    val text = parser.nextText()
                                    currentText.append(text)
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "p") {
                                val clean = currentText.toString().trim()
                                if (clean.isNotEmpty()) {
                                    val isBullet = clean.startsWith("•") || clean.startsWith("-")
                                    val formattedText = if (isBullet) clean.removePrefix("•").removePrefix("-").trim() else clean
                                    paragraphs.add(
                                        DocParagraph(
                                            text = formattedText,
                                            isTitle = isTitle,
                                            isHeading = isHeading,
                                            isBullet = isBullet,
                                            isBold = isBold,
                                            isItalic = isItalic
                                        )
                                    )
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
                break
            }
            entry = zis.nextEntry
        }
    }
    return paragraphs
}

private fun parseXlsx(file: File): SpreadsheetData {
    val sharedStrings = mutableListOf<String>()
    val rows = mutableListOf<List<String>>()

    // First pass: extract shared strings
    ZipInputStream(FileInputStream(file)).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "xl/sharedStrings.xml") {
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(zis, "UTF-8")
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "t") {
                        sharedStrings.add(parser.nextText())
                    }
                    eventType = parser.next()
                }
            }
            entry = zis.nextEntry
        }
    }

    // Second pass: extract sheet rows
    ZipInputStream(FileInputStream(file)).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name.startsWith("xl/worksheets/sheet") && entry.name.endsWith(".xml")) {
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(zis, "UTF-8")
                var eventType = parser.eventType

                var currentRow = mutableListOf<String>()
                var isCellString = false
                var currentVal = ""

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name) {
                                "row" -> currentRow = mutableListOf()
                                "c" -> {
                                    val type = parser.getAttributeValue(null, "t")
                                    isCellString = (type == "s")
                                }
                                "v" -> {
                                    currentVal = parser.nextText()
                                    val finalVal = if (isCellString) {
                                        val idx = currentVal.toIntOrNull()
                                        if (idx != null && idx < sharedStrings.size) sharedStrings[idx] else currentVal
                                    } else {
                                        currentVal
                                    }
                                    currentRow.add(finalVal)
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "row") {
                                if (currentRow.isNotEmpty()) {
                                    rows.add(currentRow)
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
                break
            }
            entry = zis.nextEntry
        }
    }

    return SpreadsheetData(
        title = file.nameWithoutExtension,
        rows = rows
    )
}

private fun parseCsv(file: File): SpreadsheetData {
    val rows = mutableListOf<List<String>>()
    file.forEachLine { line ->
        if (line.isNotBlank()) {
            rows.add(FileUtils.parseCsvLine(line))
        }
    }
    return SpreadsheetData(
        title = file.nameWithoutExtension,
        rows = rows
    )
}

private fun parsePptx(file: File): List<PresentationSlide> {
    val slides = mutableListOf<PresentationSlide>()
    var slideIndex = 1

    ZipInputStream(FileInputStream(file)).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name.startsWith("ppt/slides/slide") && entry.name.endsWith(".xml")) {
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(zis, "UTF-8")
                var eventType = parser.eventType

                var title = ""
                val bullets = mutableListOf<String>()

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "t") {
                        val text = parser.nextText().trim()
                        if (text.isNotEmpty()) {
                            if (title.isEmpty()) {
                                title = text
                            } else {
                                bullets.add(text)
                            }
                        }
                    }
                    eventType = parser.next()
                }

                slides.add(
                    PresentationSlide(
                        slideNumber = slideIndex++,
                        title = title,
                        bullets = bullets
                    )
                )
            }
            entry = zis.nextEntry
        }
    }
    return slides.sortedBy { it.slideNumber }
}
