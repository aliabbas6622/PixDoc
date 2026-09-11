package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SampleFileGenerator {

    fun generateSampleFiles(context: Context): File {
        val sampleDir = File(context.filesDir, "Sample_Documents")
        if (!sampleDir.exists()) {
            sampleDir.mkdirs()
        }

        // Generate files if empty or not fully populated
        val existingCount = sampleDir.listFiles()?.size ?: 0
        if (existingCount >= 6) {
            return sampleDir
        }

        try {
            // 1. Markdown welcome file
            val mdFile = File(sampleDir, "Welcome_Guide.md")
            if (!mdFile.exists()) {
                mdFile.writeText(
                    """
                    # Welcome to File Manager
                    
                    A fast, elegant file browser and multi-format document viewer designed for Android.
                    
                    ## Key Features
                    - **Smart Browser**: Browse internal storage & folders with search, sort, and grid/list view
                    - **Built-in Document Viewers**:
                      - PDF Viewer with lazy-rendering and zoom
                      - Full-Screen Image Viewer with pan and zoom
                      - Code & Plain Text Viewer with syntax layout and monospace toggle
                      - Office Viewers for DOCX, XLSX, and PPTX
                      - Audio & Media Player
                    - **File Operations**:
                      - Rename, Move, Copy, Delete with safety Undo
                      - Zip & Unzip archive support
                      - Batch operations with multi-select
                      - System Share & Open-With integration
                    
                    ## Getting Started
                    Tap any file in this folder to preview it instantly, or tap the folder icon at the top to navigate to your device's internal storage!
                    """.trimIndent()
                )
            }

            // 2. Real multi-page PDF Document
            val pdfFile = File(sampleDir, "Quarterly_Report.pdf")
            if (!pdfFile.exists()) {
                createSamplePdf(pdfFile)
            }

            // 3. Sample valid DOCX file
            val docxFile = File(sampleDir, "Project_Proposal.docx")
            if (!docxFile.exists()) {
                createSampleDocx(docxFile)
            }

            // 4. Sample valid XLSX file
            val xlsxFile = File(sampleDir, "Financial_Budget.xlsx")
            if (!xlsxFile.exists()) {
                createSampleXlsx(xlsxFile)
            }

            // 5. Sample valid PPTX file
            val pptxFile = File(sampleDir, "Product_Roadmap.pptx")
            if (!pptxFile.exists()) {
                createSamplePptx(pptxFile)
            }

            // 6. Sample CSV file
            val csvFile = File(sampleDir, "Market_Analysis.csv")
            if (!csvFile.exists()) {
                csvFile.writeText(
                    """
                    Quarter,Region,Revenue (${'$'}M),Growth (%),Units Sold
                    Q1 2026,North America,124.5,14.2%,450000
                    Q1 2026,Europe,88.2,11.5%,310000
                    Q1 2026,Asia Pacific,156.8,22.4%,620000
                    Q2 2026,North America,138.0,10.8%,490000
                    Q2 2026,Europe,94.3,6.9%,330000
                    Q2 2026,Asia Pacific,182.4,16.3%,710000
                    """.trimIndent()
                )
            }

            // 7. Sample Source Code file (Kotlin)
            val codeFile = File(sampleDir, "RepositoryEngine.kt")
            if (!codeFile.exists()) {
                codeFile.writeText(
                    """
                    package com.example.filemanager.engine

                    import kotlinx.coroutines.flow.Flow
                    import kotlinx.coroutines.flow.flow
                    import java.io.File

                    class RepositoryEngine(private val rootDir: File) {

                        fun scanDirectory(path: String): Flow<List<File>> = flow {
                            val target = File(path)
                            if (target.exists() && target.isDirectory) {
                                val files = target.listFiles()?.toList() ?: emptyList()
                                emit(files.sortedBy { it.name.lowercase() })
                            } else {
                                emit(emptyList())
                            }
                        }

                        fun calculateTotalSize(directory: File): Long {
                            return directory.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                        }
                    }
                    """.trimIndent()
                )
            }

            // 8. Sample JSON file
            val jsonFile = File(sampleDir, "AppConfig.json")
            if (!jsonFile.exists()) {
                jsonFile.writeText(
                    """
                    {
                      "appName": "File Manager",
                      "version": "1.0.0",
                      "theme": "Dynamic Indigo",
                      "supportedViewers": [
                        "PDF",
                        "Image",
                        "DOCX",
                        "XLSX",
                        "PPTX",
                        "Text",
                        "Audio"
                      ],
                      "features": {
                        "multiSelect": true,
                        "zipSupport": true,
                        "batchOperations": true,
                        "cloudSyncPhase2": false
                      }
                    }
                    """.trimIndent()
                )
            }

            // 9. Sample Image (PNG)
            val imgFile = File(sampleDir, "Infographic_Dashboard.png")
            if (!imgFile.exists()) {
                createSampleImage(imgFile)
            }

            // 10. Sample Audio File (WAV format tone)
            val wavFile = File(sampleDir, "Notification_Chime.wav")
            if (!wavFile.exists()) {
                createSampleWav(wavFile)
            }

            // 11. Sample Subfolder with files
            val subDir = File(sampleDir, "Project_Assets")
            if (!subDir.exists()) {
                subDir.mkdirs()
                File(subDir, "Asset_Manifest.txt").writeText("Manifest v1: Icon assets, style templates, color definitions.")
                File(subDir, "Color_Tokens.json").writeText("{\n  \"primary\": \"#1976D2\",\n  \"accent\": \"#FFB300\"\n}")
            }

            // 12. Sample Zip Archive containing sample assets
            val zipFile = File(sampleDir, "Sample_Archive.zip")
            if (!zipFile.exists()) {
                createSampleZip(zipFile, mdFile, codeFile, jsonFile)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return sampleDir
    }

    private fun createSamplePdf(file: File) {
        val document = PdfDocument()
        val paint = Paint().apply { isAntiAlias = true }

        // Page 1: Cover & Summary
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Header Banner
            paint.color = Color.parseColor("#1976D2")
            canvas.drawRect(0f, 0f, 595f, 140f, paint)

            paint.color = Color.WHITE
            paint.textSize = 26f
            paint.isFakeBoldText = true
            canvas.drawText("Quarterly Performance Report", 40f, 75f, paint)

            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("Executive Summary & Market Analysis — 2026", 40f, 105f, paint)

            // Content text
            paint.color = Color.parseColor("#212121")
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText("1. Key Accomplishments", 40f, 180f, paint)

            paint.textSize = 12f
            paint.isFakeBoldText = false
            val lines = listOf(
                "• Deployed high-speed indexing engine with 40% reduction in memory footprint.",
                "• Added full multi-format document preview support (PDF, DOCX, XLSX, PPTX).",
                "• Expanded customer adoption by 68% across mobile and tablet form factors.",
                "• Achieved 99.98% crash-free sessions across target device baselines."
            )
            var y = 210f
            for (line in lines) {
                canvas.drawText(line, 45f, y, paint)
                y += 24f
            }

            // Metrics Box
            paint.color = Color.parseColor("#E3F2FD")
            canvas.drawRoundRect(40f, 320f, 555f, 440f, 12f, 12f, paint)

            paint.color = Color.parseColor("#0D47A1")
            paint.textSize = 16f
            paint.isFakeBoldText = true
            canvas.drawText("Financial Metrics Overview", 60f, 355f, paint)

            paint.color = Color.parseColor("#37474F")
            paint.textSize = 13f
            paint.isFakeBoldText = false
            canvas.drawText("Total Revenue: $363.7M (+18.4% YoY)", 60f, 385f, paint)
            canvas.drawText("Operating Margin: 24.2% (Target: 22.0%)", 60f, 410f, paint)

            // Footer
            paint.color = Color.parseColor("#9E9E9E")
            paint.textSize = 10f
            canvas.drawText("Page 1 of 2  •  Confidential  •  Generated by File Manager", 40f, 810f, paint)

            document.finishPage(page)
        }

        // Page 2: Roadmap & Strategy
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 2).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Header Banner
            paint.color = Color.parseColor("#0288D1")
            canvas.drawRect(0f, 0f, 595f, 100f, paint)

            paint.color = Color.WHITE
            paint.textSize = 22f
            paint.isFakeBoldText = true
            canvas.drawText("Strategic Roadmap — Next Steps", 40f, 60f, paint)

            paint.color = Color.parseColor("#212121")
            paint.textSize = 16f
            paint.isFakeBoldText = true
            canvas.drawText("2. Product Initiatives", 40f, 140f, paint)

            paint.textSize = 12f
            paint.isFakeBoldText = false
            val lines = listOf(
                "1. Real-time background sync and compression algorithms.",
                "2. Enhanced security: encrypted folder vaults and biometrics.",
                "3. Native deep-search indexing for complex documents.",
                "4. Desktop and DeX mode adaptive UI optimization."
            )
            var y = 170f
            for (line in lines) {
                canvas.drawText(line, 45f, y, paint)
                y += 24f
            }

            // Footer
            paint.color = Color.parseColor("#9E9E9E")
            paint.textSize = 10f
            canvas.drawText("Page 2 of 2  •  Confidential  •  Generated by File Manager", 40f, 810f, paint)

            document.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun createSampleImage(file: File) {
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }

        // Background
        paint.color = Color.parseColor("#1565C0")
        canvas.drawRect(0f, 0f, 800f, 600f, paint)

        // Accent circle
        paint.color = Color.parseColor("#1E88E5")
        canvas.drawCircle(400f, 300f, 220f, paint)

        paint.color = Color.parseColor("#42A5F5")
        canvas.drawCircle(400f, 300f, 150f, paint)

        // Title
        paint.color = Color.WHITE
        paint.textSize = 36f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("File Manager Pro", 400f, 290f, paint)

        paint.textSize = 20f
        paint.isFakeBoldText = false
        canvas.drawText("High Fidelity Document Viewing & Storage", 400f, 335f, paint)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
    }

    private fun createSampleWav(file: File) {
        // Generate a clean 1.5s 44.1kHz stereo/mono sine wave chime
        val sampleRate = 44100
        val durationSeconds = 1.2
        val numSamples = (sampleRate * durationSeconds).toInt()
        val dataSize = numSamples * 2 // 16-bit mono

        val output = ByteArrayOutputStream()
        // RIFF header
        output.write("RIFF".toByteArray())
        val chunkSize = 36 + dataSize
        output.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(chunkSize).array())
        output.write("WAVE".toByteArray())

        // fmt subchunk
        output.write("fmt ".toByteArray())
        output.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16).array()) // Subchunk1Size
        output.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1).array())  // AudioFormat (PCM)
        output.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1).array())  // NumChannels (1 mono)
        output.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate).array())
        output.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate * 2).array()) // ByteRate
        output.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(2).array())  // BlockAlign
        output.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(16).array()) // BitsPerSample

        // data subchunk
        output.write("data".toByteArray())
        output.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dataSize).array())

        // PCM data: pleasing chime (frequencies: 587Hz D5 -> 880Hz A5)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = if (t < 0.4) 587.33 else 880.0
            val envelope = Math.exp(-3.0 * t) // decay
            val sample = (Math.sin(2.0 * Math.PI * freq * t) * envelope * 24000).toInt().coerceIn(-32768, 32767)
            val byte1 = (sample and 0xFF).toByte()
            val byte2 = ((sample shr 8) and 0xFF).toByte()
            output.write(byteArrayOf(byte1, byte2))
        }

        FileOutputStream(file).use { it.write(output.toByteArray()) }
    }

    private fun createSampleZip(zipFile: File, vararg files: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            for (f in files) {
                if (f.exists() && f.isFile) {
                    zos.putNextEntry(ZipEntry(f.name))
                    f.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    // Helper to build a valid OpenXML DOCX file
    private fun createSampleDocx(file: File) {
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>""".trimIndent().toByteArray()
            )
            zos.closeEntry()

            // _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>""".trimIndent().toByteArray()
            )
            zos.closeEntry()

            // word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p>
                      <w:pPr><w:jc w:val="center"/></w:pPr>
                      <w:r><w:rPr><w:b/><w:sz w:val="48"/><w:color w:val="1976D2"/></w:rPr><w:t>Project Architecture Proposal</w:t></w:r>
                    </w:p>
                    <w:p>
                      <w:pPr><w:jc w:val="center"/></w:pPr>
                      <w:r><w:rPr><w:i/><w:sz w:val="24"/><w:color w:val="757575"/></w:rPr><w:t>Confidential Internal Review • Prepared by Lead Engineering</w:t></w:r>
                    </w:p>
                    <w:p><w:r><w:t></w:t></w:r></w:p>
                    <w:p>
                      <w:r><w:rPr><w:b/><w:sz w:val="32"/><w:color w:val="0D47A1"/></w:rPr><w:t>1. Executive Summary</w:t></w:r>
                    </w:p>
                    <w:p>
                      <w:r><w:t>This proposal introduces a modern, high-performance file management system designed for Android. The architecture emphasizes zero-latency scrolling, granular storage permission handling, and responsive previews for documents, images, and spreadsheets.</w:t></w:r>
                    </w:p>
                    <w:p>
                      <w:r><w:rPr><w:b/><w:sz w:val="32"/><w:color w:val="0D47A1"/></w:rPr><w:t>2. Core System Modules</w:t></w:r>
                    </w:p>
                    <w:p>
                      <w:r><w:rPr><w:b/></w:rPr><w:t>• Storage Layer:</w:t></w:r>
                      <w:r><w:t> Flow-driven file scanning, asynchronous thumbnail generation, and cached index metadata.</w:t></w:r>
                    </w:p>
                    <w:p>
                      <w:r><w:rPr><w:b/></w:rPr><w:t>• Document Engine:</w:t></w:r>
                      <w:r><w:t> Multi-format rendering engine supporting native PDF pages, OpenXML document parsing, and interactive spreadsheets.</w:t></w:r>
                    </w:p>
                    <w:p>
                      <w:r><w:rPr><w:b/></w:rPr><w:t>• Operations Engine:</w:t></w:r>
                      <w:r><w:t> Non-blocking file operations including recursive copy, safe rename, zip archive compression, and deep sharing.</w:t></w:r>
                    </w:p>
                    <w:p><w:r><w:t></w:t></w:r></w:p>
                    <w:p>
                      <w:r><w:rPr><w:b/><w:sz w:val="32"/><w:color w:val="0D47A1"/></w:rPr><w:t>3. Conclusion &amp; Next Steps</w:t></w:r>
                    </w:p>
                    <w:p>
                      <w:r><w:t>With full Jetpack Compose integration, the app achieves seamless 60fps rendering even with thousands of files in active directory trees.</w:t></w:r>
                    </w:p>
                  </w:body>
                </w:document>""".trimIndent().toByteArray()
            )
            zos.closeEntry()
        }
    }

    // Helper to build a valid OpenXML XLSX file
    private fun createSampleXlsx(file: File) {
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
                </Types>""".trimIndent().toByteArray()
            )
            zos.closeEntry()

            // _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>""".trimIndent().toByteArray()
            )
            zos.closeEntry()

            // xl/_rels/workbook.xml.rels
            zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
                </Relationships>""".trimIndent().toByteArray()
            )
            zos.closeEntry()

            // xl/workbook.xml
            zos.putNextEntry(ZipEntry("xl/workbook.xml"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheets>
                    <sheet name="Budget 2026" sheetId="1" r:id="rId1" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>
                  </sheets>
                </workbook>""".trimIndent().toByteArray()
            )
            zos.closeEntry()

            // xl/sharedStrings.xml
            val strings = listOf(
                "Category", "Q1 Budget", "Q2 Budget", "Q3 Budget", "Q4 Budget", "Status",
                "Cloud Infrastructure", "On Track",
                "Engineering Payroll", "Active",
                "Marketing & Growth", "On Track",
                "Hardware & Testing", "Review",
                "Security Audits", "Completed",
                "Total Expenditure"
            )
            zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            val ssb = StringBuilder("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            ssb.append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${strings.size}" uniqueCount="${strings.size}">""")
            for (s in strings) {
                ssb.append("<si><t>$s</t></si>")
            }
            ssb.append("</sst>")
            zos.write(ssb.toString().toByteArray())
            zos.closeEntry()

            // xl/worksheets/sheet1.xml
            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    <row r="1">
                      <c r="A1" t="s"><v>0</v></c>
                      <c r="B1" t="s"><v>1</v></c>
                      <c r="C1" t="s"><v>2</v></c>
                      <c r="D1" t="s"><v>3</v></c>
                      <c r="E1" t="s"><v>4</v></c>
                      <c r="F1" t="s"><v>5</v></c>
                    </row>
                    <row r="2">
                      <c r="A2" t="s"><v>6</v></c>
                      <c r="B2"><v>45000</v></c>
                      <c r="C2"><v>48000</v></c>
                      <c r="D2"><v>52000</v></c>
                      <c r="E2"><v>55000</v></c>
                      <c r="F2" t="s"><v>7</v></c>
                    </row>
                    <row r="3">
                      <c r="A3" t="s"><v>8</v></c>
                      <c r="B3"><v>180000</v></c>
                      <c r="C3"><v>195000</v></c>
                      <c r="D3"><v>210000</v></c>
                      <c r="E3"><v>225000</v></c>
                      <c r="F3" t="s"><v>9</v></c>
                    </row>
                    <row r="4">
                      <c r="A4" t="s"><v>10</v></c>
                      <c r="B4"><v>35000</v></c>
                      <c r="C4"><v>42000</v></c>
                      <c r="D4"><v>50000</v></c>
                      <c r="E4"><v>60000</v></c>
                      <c r="F4" t="s"><v>11</v></c>
                    </row>
                    <row r="5">
                      <c r="A5" t="s"><v>12</v></c>
                      <c r="B5"><v>15000</v></c>
                      <c r="C5"><v>12000</v></c>
                      <c r="D5"><v>18000</v></c>
                      <c r="E5"><v>14000</v></c>
                      <c r="F5" t="s"><v>13</v></c>
                    </row>
                    <row r="6">
                      <c r="A6" t="s"><v>14</v></c>
                      <c r="B6"><v>25000</v></c>
                      <c r="C6"><v>5000</v></c>
                      <c r="D6"><v>30000</v></c>
                      <c r="E6"><v>5000</v></c>
                      <c r="F6" t="s"><v>15</v></c>
                    </row>
                  </sheetData>
                </worksheet>""".trimIndent().toByteArray()
            )
            zos.closeEntry()
        }
    }

    // Helper to build a valid OpenXML PPTX file
    private fun createSamplePptx(file: File) {
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
                  <Override PartName="/ppt/slides/slide1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
                  <Override PartName="/ppt/slides/slide2.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
                </Types>""".trimIndent().toByteArray()
            )
            zos.closeEntry()

            // _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
                </Relationships>""".trimIndent().toByteArray()
            )
            zos.closeEntry()

            // ppt/presentation.xml
            zos.putNextEntry(ZipEntry("ppt/presentation.xml"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
                  <p:sldIdLst>
                    <p:sldId id="256" r:id="rId1" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>
                    <p:sldId id="257" r:id="rId2" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>
                  </p:sldIdLst>
                </p:presentation>""".trimIndent().toByteArray()
            )
            zos.closeEntry()

            // ppt/slides/slide1.xml
            zos.putNextEntry(ZipEntry("ppt/slides/slide1.xml"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                  <p:cSld>
                    <p:spTree>
                      <p:sp>
                        <p:txBody>
                          <a:p><a:r><a:t>2026 Product Vision</a:t></a:r></a:p>
                          <a:p><a:r><a:t>Building the premier Android file management experience</a:t></a:r></a:p>
                        </p:txBody>
                      </p:sp>
                    </p:spTree>
                  </p:cSld>
                </p:sld>""".trimIndent().toByteArray()
            )
            zos.closeEntry()

            // ppt/slides/slide2.xml
            zos.putNextEntry(ZipEntry("ppt/slides/slide2.xml"))
            zos.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                  <p:cSld>
                    <p:spTree>
                      <p:sp>
                        <p:txBody>
                          <a:p><a:r><a:t>Key Technical Milestones</a:t></a:r></a:p>
                          <a:p><a:r><a:t>• Ultra-low latency Compose UI rendering</a:t></a:r></a:p>
                          <a:p><a:r><a:t>• Robust OpenXML &amp; PDF document parsing</a:t></a:r></a:p>
                          <a:p><a:r><a:t>• Secure scoped &amp; external storage support</a:t></a:r></a:p>
                        </p:txBody>
                      </p:sp>
                    </p:spTree>
                  </p:cSld>
                </p:sld>""".trimIndent().toByteArray()
            )
            zos.closeEntry()
        }
    }
}
