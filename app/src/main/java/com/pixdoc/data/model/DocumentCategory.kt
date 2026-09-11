package com.pixdoc.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pixdoc.ui.theme.ColorDocx
import com.pixdoc.ui.theme.ColorImage
import com.pixdoc.ui.theme.ColorPdf
import com.pixdoc.ui.theme.ColorPptx
import com.pixdoc.ui.theme.ColorText
import com.pixdoc.ui.theme.ColorXlsx
import com.pixdoc.ui.theme.PrimaryBlue

enum class DocumentCategory(
    val title: String,
    val extensions: List<String>?,
    val icon: ImageVector,
    val badgeColor: Color,
    val shortLabel: String
) {
    ALL(
        title = "All Docs",
        extensions = null,
        icon = Icons.Filled.Description,
        badgeColor = PrimaryBlue,
        shortLabel = "ALL"
    ),
    PDF(
        title = "PDF",
        extensions = listOf("pdf"),
        icon = Icons.Filled.PictureAsPdf,
        badgeColor = ColorPdf,
        shortLabel = "PDF"
    ),
    WORD(
        title = "Word",
        extensions = listOf("docx", "doc"),
        icon = Icons.Filled.Description,
        badgeColor = ColorDocx,
        shortLabel = "DOC"
    ),
    EXCEL(
        title = "Excel",
        extensions = listOf("xlsx", "xls", "csv"),
        icon = Icons.Filled.TableChart,
        badgeColor = ColorXlsx,
        shortLabel = "XLS"
    ),
    PPT(
        title = "PPT",
        extensions = listOf("pptx", "ppt"),
        icon = Icons.Filled.Slideshow,
        badgeColor = ColorPptx,
        shortLabel = "PPT"
    ),
    TXT(
        title = "Text",
        extensions = listOf("txt", "md", "rtf", "log", "json", "kt", "java", "xml", "html"),
        icon = Icons.Filled.Description,
        badgeColor = ColorText,
        shortLabel = "TXT"
    ),
    IMAGE(
        title = "Images",
        extensions = listOf("jpg", "jpeg", "png", "webp", "bmp", "gif"),
        icon = Icons.Filled.Image,
        badgeColor = ColorImage,
        shortLabel = "IMG"
    );

    fun matches(fileExtension: String): Boolean {
        if (extensions == null) {
            // "ALL" matches document formats only — images are excluded so they
            // don't clutter the Documents list (use the Images category for those)
            val allExts = setOf(
                "pdf", "docx", "doc", "xlsx", "xls", "csv",
                "pptx", "ppt", "txt", "md", "rtf", "log", "json",
                "kt", "java"
            )
            return allExts.contains(fileExtension.lowercase())
        }
        return extensions.contains(fileExtension.lowercase())
    }
}
