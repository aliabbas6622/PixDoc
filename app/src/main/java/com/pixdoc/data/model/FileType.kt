package com.pixdoc.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Archive
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pixdoc.ui.theme.ColorArchive
import com.pixdoc.ui.theme.ColorAudio
import com.pixdoc.ui.theme.ColorCode
import com.pixdoc.ui.theme.ColorDocx
import com.pixdoc.ui.theme.ColorFolder
import com.pixdoc.ui.theme.ColorImage
import com.pixdoc.ui.theme.ColorPdf
import com.pixdoc.ui.theme.ColorPptx
import com.pixdoc.ui.theme.ColorText
import com.pixdoc.ui.theme.ColorUnknown
import com.pixdoc.ui.theme.ColorVideo
import com.pixdoc.ui.theme.ColorXlsx

enum class FileType(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    FOLDER("Folder", Icons.Filled.Folder, ColorFolder),
    IMAGE("Image", Icons.Filled.Image, ColorImage),
    PDF("PDF Document", Icons.Filled.PictureAsPdf, ColorPdf),
    DOCX("Word Document", Icons.Filled.Description, ColorDocx),
    XLSX("Spreadsheet", Icons.Filled.TableChart, ColorXlsx),
    PPTX("Presentation", Icons.Filled.Slideshow, ColorPptx),
    TEXT("Text File", Icons.Filled.Description, ColorText),
    CODE("Source Code", Icons.Filled.Code, ColorCode),
    AUDIO("Audio", Icons.Filled.Audiotrack, ColorAudio),
    VIDEO("Video", Icons.Filled.Movie, ColorVideo),
    ARCHIVE("Archive", Icons.Filled.Archive, ColorArchive),
    OTHER("File", Icons.AutoMirrored.Filled.InsertDriveFile, ColorUnknown);

    companion object {
        fun fromExtension(ext: String, isDir: Boolean): FileType {
            if (isDir) return FOLDER
            val cleanExt = ext.lowercase().trim()
            return when (cleanExt) {
                "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg" -> IMAGE
                "pdf" -> PDF
                "docx", "doc" -> DOCX
                "xlsx", "xls" -> XLSX
                "pptx", "ppt" -> PPTX
                "txt", "md", "log", "rtf", "ini", "cfg", "env" -> TEXT
                "kt", "java", "py", "js", "ts", "json", "xml", "html", "css", "c", "cpp", "h", "sh", "sql", "dart", "swift", "yaml", "yml" -> CODE
                "mp3", "wav", "ogg", "m4a", "flac", "aac", "mid" -> AUDIO
                "mp4", "mkv", "webm", "avi", "mov", "flv", "3gp" -> VIDEO
                "zip", "rar", "7z", "tar", "gz", "bz2" -> ARCHIVE
                else -> OTHER
            }
        }
    }
}
