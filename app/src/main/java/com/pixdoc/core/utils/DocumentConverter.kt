package com.pixdoc.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object DocumentConverter {

    fun getConvertedDir(context: Context): File {
        val dir = File(context.filesDir, "Converted_Documents")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Converts one or multiple images into a multi-page PDF document
     */
    fun convertImagesToPdf(
        context: Context,
        imageFiles: List<File>,
        docTitle: String = "Converted_Image"
    ): Result<File> {
        if (imageFiles.isEmpty()) {
            return Result.failure(IllegalArgumentException("No images selected for conversion"))
        }

        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points

        try {
            var pageNum = 1
            for (imgFile in imageFiles) {
                if (!imgFile.exists()) continue

                // Decode bitmap with downsampling if very large
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(imgFile.absolutePath, options)

                var sampleSize = 1
                while (options.outWidth / sampleSize > 2048 || options.outHeight / sampleSize > 2048) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath, decodeOpts) ?: continue

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Fill white background
                canvas.drawColor(Color.WHITE)

                // Draw header
                val headerPaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 12f
                    isAntiAlias = true
                }
                canvas.drawText(docTitle.replace("_", " "), 40f, 35f, headerPaint)

                // Scale and fit image into bounds (margins: 40 left/right, 50 top, 50 bottom)
                val destWidth = (pageWidth - 80).toFloat()
                val destHeight = (pageHeight - 110).toFloat()

                val scaleX = destWidth / bitmap.width
                val scaleY = destHeight / bitmap.height
                val scale = minOf(scaleX, scaleY)

                val scaledW = bitmap.width * scale
                val scaledH = bitmap.height * scale
                val left = 40f + (destWidth - scaledW) / 2f
                val top = 50f + (destHeight - scaledH) / 2f

                val destRect = Rect(left.toInt(), top.toInt(), (left + scaledW).toInt(), (top + scaledH).toInt())
                val paint = Paint().apply { isFilterBitmap = true }
                canvas.drawBitmap(bitmap, null, destRect, paint)

                // Footer page number
                val footerPaint = Paint().apply {
                    color = Color.GRAY
                    textSize = 10f
                    isAntiAlias = true
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("Page $pageNum of ${imageFiles.size}", (pageWidth - 40).toFloat(), (pageHeight - 25).toFloat(), footerPaint)

                pdfDocument.finishPage(page)
                bitmap.recycle()
                pageNum++
            }

            val sanitizedTitle = docTitle.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetName = if (sanitizedTitle.endsWith(".pdf", ignoreCase = true)) sanitizedTitle else "$sanitizedTitle.pdf"
            val targetFile = File(getConvertedDir(context), targetName)

            FileOutputStream(targetFile).use { out ->
                pdfDocument.writeTo(out)
            }

            return Result.success(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Converts plain text, markdown, or code into a paginated PDF document
     */
    fun convertTextToPdf(
        context: Context,
        content: String,
        docTitle: String = "Exported_Document"
    ): Result<File> {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 50f
        val contentWidth = (pageWidth - margin * 2).toInt()

        try {
            val textPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }

            val titlePaint = TextPaint().apply {
                color = Color.rgb(25, 118, 210) // Office blue
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val headerPaint = TextPaint().apply {
                color = Color.GRAY
                textSize = 10f
                isAntiAlias = true
            }

            // Split content into lines and paragraphs
            val paragraphs = content.lines()
            var currentY = margin + 40f
            var pageNum = 1

            var currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            var currentCanvas: Canvas = currentPage.canvas
            currentCanvas.drawColor(Color.WHITE)

            // Draw title on first page
            currentCanvas.drawText(docTitle.replace("_", " "), margin, margin + 20f, titlePaint)
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            currentCanvas.drawText("Generated on $dateStr", margin, margin + 35f, headerPaint)
            currentCanvas.drawLine(margin, margin + 42f, pageWidth - margin, margin + 42f, headerPaint)

            currentY = margin + 65f

            for (paragraph in paragraphs) {
                val pText = if (paragraph.isEmpty()) " " else paragraph
                @Suppress("DEPRECATION")
                val layout = StaticLayout(
                    pText,
                    textPaint,
                    contentWidth,
                    Layout.Alignment.ALIGN_NORMAL,
                    1.2f,
                    0.0f,
                    false
                )

                if (currentY + layout.height > pageHeight - margin - 30f) {
                    // Draw footer on current page
                    val footerPaint = TextPaint().apply {
                        color = Color.GRAY
                        textSize = 9f
                        textAlign = Paint.Align.RIGHT
                    }
                    currentCanvas.drawText("Page $pageNum", pageWidth - margin, pageHeight - margin + 15f, footerPaint)
                    pdfDocument.finishPage(currentPage)

                    // Start new page
                    pageNum++
                    currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    currentCanvas = currentPage.canvas
                    currentCanvas.drawColor(Color.WHITE)

                    // Header on subsequent pages
                    currentCanvas.drawText(docTitle.replace("_", " "), margin, margin - 10f, headerPaint)
                    currentCanvas.drawLine(margin, margin - 2f, pageWidth - margin, margin - 2f, headerPaint)
                    currentY = margin + 15f
                }

                currentCanvas.save()
                currentCanvas.translate(margin, currentY)
                layout.draw(currentCanvas)
                currentCanvas.restore()

                currentY += layout.height + 4f
            }

            // Draw footer on last page
            val footerPaint = TextPaint().apply {
                color = Color.GRAY
                textSize = 9f
                textAlign = Paint.Align.RIGHT
            }
            currentCanvas.drawText("Page $pageNum", pageWidth - margin, pageHeight - margin + 15f, footerPaint)
            pdfDocument.finishPage(currentPage)

            val sanitizedTitle = docTitle.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetName = if (sanitizedTitle.endsWith(".pdf", ignoreCase = true)) sanitizedTitle else "$sanitizedTitle.pdf"
            val targetFile = File(getConvertedDir(context), targetName)

            FileOutputStream(targetFile).use { out ->
                pdfDocument.writeTo(out)
            }

            return Result.success(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Converts a CSV file into a cleanly bordered PDF table
     */
    fun convertCsvToPdf(
        context: Context,
        csvFile: File,
        docTitle: String = csvFile.nameWithoutExtension
    ): Result<File> {
        val pdfDocument = PdfDocument()
        val pageWidth = 842 // Landscape A4 for tables
        val pageHeight = 595
        val margin = 40f

        try {
            val lines = csvFile.readLines().filter { it.isNotBlank() }
            if (lines.isEmpty()) {
                return Result.failure(IllegalArgumentException("CSV file is empty"))
            }

            val tableData = lines.map { line ->
                FileUtils.parseCsvLine(line)
            }

            val columnCount = tableData.maxOfOrNull { it.size } ?: 1
            val availableWidth = (pageWidth - margin * 2)
            val colWidth = availableWidth / columnCount
            val rowHeight = 24f

            val headerPaint = Paint().apply {
                color = Color.rgb(25, 118, 210)
                isAntiAlias = true
                textSize = 14f
                isFakeBoldText = true
            }

            val cellPaint = Paint().apply {
                color = Color.rgb(33, 33, 33)
                isAntiAlias = true
                textSize = 10f
            }

            val borderPaint = Paint().apply {
                color = Color.LTGRAY
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(227, 242, 253) // light blue
                style = Paint.Style.FILL
            }

            var pageNum = 1
            var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            var canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            // Title
            canvas.drawText(docTitle.replace("_", " "), margin, margin, headerPaint)
            var currentY = margin + 25f

            for ((rowIndex, row) in tableData.withIndex()) {
                if (currentY + rowHeight > pageHeight - margin) {
                    pdfDocument.finishPage(page)
                    pageNum++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    canvas = page.canvas
                    canvas.drawColor(Color.WHITE)
                    currentY = margin + 20f
                }

                val isHeader = (rowIndex == 0)
                if (isHeader) {
                    canvas.drawRect(margin, currentY, margin + availableWidth, currentY + rowHeight, headerBgPaint)
                }

                for (colIndex in 0 until columnCount) {
                    val cellX = margin + colIndex * colWidth
                    val text = row.getOrNull(colIndex) ?: ""

                    // Draw cell text
                    val p = if (isHeader) Paint(cellPaint).apply { isFakeBoldText = true } else cellPaint
                    val truncated = if (text.length > 25) text.take(23) + "…" else text
                    canvas.drawText(truncated, cellX + 6f, currentY + 16f, p)

                    // Draw vertical cell line
                    canvas.drawLine(cellX, currentY, cellX, currentY + rowHeight, borderPaint)
                }
                // End of row vertical line
                canvas.drawLine(margin + availableWidth, currentY, margin + availableWidth, currentY + rowHeight, borderPaint)

                // Horizontal row line
                canvas.drawLine(margin, currentY, margin + availableWidth, currentY, borderPaint)
                canvas.drawLine(margin, currentY + rowHeight, margin + availableWidth, currentY + rowHeight, borderPaint)

                currentY += rowHeight
            }

            pdfDocument.finishPage(page)

            val sanitizedTitle = docTitle.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetName = "${sanitizedTitle}_table.pdf"
            val targetFile = File(getConvertedDir(context), targetName)

            FileOutputStream(targetFile).use { out ->
                pdfDocument.writeTo(out)
            }

            return Result.success(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Extracts text content from DOCX file into clean .txt format
     */
    fun extractTextFromDocx(context: Context, docxFile: File): Result<File> {
        try {
            val stringBuilder = java.lang.StringBuilder()
            ZipInputStream(docxFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val factory = XmlPullParserFactory.newInstance()
                        val parser = factory.newPullParser()
                        parser.setInput(zip, "UTF-8")

                        var eventType = parser.eventType
                        val currentParagraph = StringBuilder()

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    when (parser.name) {
                                        "p" -> currentParagraph.setLength(0)
                                        "br", "cr" -> currentParagraph.append("\n")
                                        "t" -> currentParagraph.append(parser.nextText())
                                    }
                                }
                                XmlPullParser.END_TAG -> {
                                    if (parser.name == "p") {
                                        val text = currentParagraph.toString().trim()
                                        if (text.isNotEmpty()) {
                                            stringBuilder.append(text).append("\n\n")
                                        }
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                        break
                    }
                    entry = zip.nextEntry
                }
            }

            val targetFile = File(getConvertedDir(context), "${docxFile.nameWithoutExtension}_extracted.txt")
            targetFile.writeText(stringBuilder.toString().ifEmpty { "No text content found in document." })
            return Result.success(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }
    }

    /**
     * Creates a new blank Word (.docx) document with valid OOXML package structure
     */
    fun createBlankWordDoc(context: Context, docTitle: String): Result<File> {
        val sanitized = docTitle.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifEmpty { "New_Document" }
        val targetFile = File(getConvertedDir(context), "$sanitized.docx")

        val docXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:p>
                        <w:pPr>
                            <w:pStyle w:val="Heading1"/>
                        </w:pPr>
                        <w:r>
                            <w:rPr>
                                <w:b/>
                                <w:sz w:val="48"/>
                                <w:color w:val="1976D2"/>
                            </w:rPr>
                            <w:t>${sanitized.replace("_", " ")}</w:t>
                        </w:r>
                    </w:p>
                    <w:p>
                        <w:r>
                            <w:rPr>
                                <w:i/>
                                <w:color w:val="757575"/>
                            </w:rPr>
                            <w:t>Created with Office Suite on ${SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())}</w:t>
                        </w:r>
                    </w:p>
                    <w:p>
                        <w:r>
                            <w:t>Type your notes and content here...</w:t>
                        </w:r>
                    </w:p>
                    <w:sectPr/>
                </w:body>
            </w:document>
        """.trimIndent()

        val contentTypes = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()

        val rootRels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
        """.trimIndent()

        try {
            ZipOutputStream(FileOutputStream(targetFile)).use { zos ->
                // 1. [Content_Types].xml
                zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                zos.write(contentTypes.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 2. _rels/.rels
                zos.putNextEntry(ZipEntry("_rels/.rels"))
                zos.write(rootRels.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 3. word/document.xml
                zos.putNextEntry(ZipEntry("word/document.xml"))
                zos.write(docXml.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            return Result.success(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }
    }

    /**
     * Creates a new blank Spreadsheet (.csv)
     */
    fun createBlankSpreadsheet(context: Context, docTitle: String): Result<File> {
        val sanitized = docTitle.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifEmpty { "New_Spreadsheet" }
        val targetFile = File(getConvertedDir(context), "$sanitized.csv")

        try {
            targetFile.writeText(
                """
                Item,Category,Quantity,Price ($),Total ($)
                Item 1,General,10,25.00,250.00
                Item 2,Office,5,15.50,77.50
                Item 3,Supplies,20,4.20,84.00
                """.trimIndent()
            )
            return Result.success(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }
    }

    /**
     * Creates a new blank Text/Markdown Note (.txt)
     */
    fun createBlankNote(context: Context, docTitle: String, content: String = ""): Result<File> {
        val sanitized = docTitle.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifEmpty { "New_Note" }
        val targetFile = File(getConvertedDir(context), "$sanitized.txt")

        try {
            val initialContent = content.ifEmpty {
                "# ${sanitized.replace("_", " ")}\n\nCreated on ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())}\n\nStart typing here..."
            }
            targetFile.writeText(initialContent)
            return Result.success(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }
    }
}
