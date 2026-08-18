package com.mz.shunji.ui.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import android.widget.Toast
import com.mz.shunji.data.model.Attachment
import com.mz.shunji.data.model.Note
import com.mz.shunji.ui.attachments.InlineContent
import com.mz.shunji.ui.attachments.getAttachmentUri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    fun generatePdfDirect(context: Context, note: Note): File? {
        return try {
            val title = note.title.ifEmpty { "未命名笔记" }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeTitle = title.replace(Regex("[^\\w\\u4e00-\\u9fff\\-]"), "_")
            val fileName = "${safeTitle}_${timestamp}.pdf"

            val document = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points
            val leftMargin = 40f
            val rightMargin = 555f
            val contentWidth = rightMargin - leftMargin

            val titlePaint = Paint().apply {
                color = Color.BLACK
                isAntiAlias = true
                textSize = 18f
                isFakeBoldText = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val contentPaint = Paint().apply {
                color = Color.BLACK
                isAntiAlias = true
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT
            }
            val attachmentPaint = Paint().apply {
                color = Color.GRAY
                isAntiAlias = true
                textSize = 11f
                typeface = android.graphics.Typeface.DEFAULT
            }

            var currentPage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            var canvas = currentPage.canvas
            var y = 50f

            fun checkPage(neededHeight: Float) {
                if (y + neededHeight > pageHeight - 50f) {
                    document.finishPage(currentPage)
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create()
                    currentPage = document.startPage(pageInfo)
                    canvas = currentPage.canvas
                    y = 50f
                }
            }

            // Draw title
            val titleLayout = android.text.StaticLayout.Builder.obtain(
                title, 0, title.length,
                android.text.TextPaint(titlePaint), contentWidth.toInt()
            ).setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).build()
            canvas.save()
            canvas.translate(leftMargin, y)
            titleLayout.draw(canvas)
            canvas.restore()
            y += titleLayout.height + 10f

            // Parse content and draw
            val rawContent = if (note.isList) note.taskListToString(withCheckmarks = true) else note.content
            val segments = parseContentForPdf(rawContent, note.attachments)

            for (segment in segments) {
                when (segment) {
                    is PdfSegment.Text -> {
                        val stripped = stripMarkdownForPdf(segment.text)
                        if (stripped.isBlank()) continue
                        val textLayout = android.text.StaticLayout.Builder.obtain(
                            stripped, 0, stripped.length,
                            android.text.TextPaint(contentPaint), contentWidth.toInt()
                        ).setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).build()
                        checkPage(textLayout.height.toFloat())
                        canvas.save()
                        canvas.translate(leftMargin, y)
                        textLayout.draw(canvas)
                        canvas.restore()
                        y += textLayout.height + 4f
                    }
                    is PdfSegment.Image -> {
                        val bitmap = loadBitmapForPdf(context, segment.noteAttachment)
                        if (bitmap != null) {
                            val scale = contentWidth / bitmap.width.toFloat()
                            val scaledHeight = bitmap.height * scale
                            checkPage(scaledHeight + 10f)
                            val destRect = RectF(leftMargin, y, leftMargin + contentWidth, y + scaledHeight)
                            canvas.drawBitmap(bitmap, null, destRect, null)
                            bitmap.recycle()
                            y += scaledHeight + 8f
                        }
                    }
                    is PdfSegment.FileAttachment -> {
                        val label = "[${segment.noteAttachment.type.name}] ${segment.noteAttachment.fileName}"
                        checkPage(20f)
                        canvas.drawText(label, leftMargin, y + 12f, attachmentPaint)
                        y += 20f
                    }
                }
            }

            document.finishPage(currentPage)

            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            if (file.exists() && file.length() > 0) file else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private sealed class PdfSegment {
        data class Text(val text: String) : PdfSegment()
        data class Image(val noteAttachment: Attachment) : PdfSegment()
        data class FileAttachment(val noteAttachment: Attachment) : PdfSegment()
    }

    private fun parseContentForPdf(content: String, attachments: List<Attachment>): List<PdfSegment> {
        val segments = mutableListOf<PdfSegment>()
        val parts = content.split(InlineContent.regex)

        for (i in parts.indices) {
            val text = parts[i].trim()
            if (text.isNotEmpty()) {
                segments.add(PdfSegment.Text(text))
            }
            if (i < parts.size - 1) {
                val match = InlineContent.regex.find(content, if (i == 0) 0 else content.indexOf(parts[i]) + parts[i].length)
                if (match != null) {
                    val path = match.groupValues[1]
                    val attachment = attachments.find { it.path == path }
                    if (attachment != null) {
                        segments.add(
                            if (attachment.type == Attachment.Type.IMAGE) PdfSegment.Image(attachment)
                            else PdfSegment.FileAttachment(attachment)
                        )
                    }
                }
            }
        }
        return segments
    }

    private fun loadBitmapForPdf(context: Context, attachment: Attachment): Bitmap? {
        return try {
            val uri = getAttachmentUri(context, attachment.path) ?: return null
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun stripMarkdownForPdf(text: String): String {
        var result = text
        result = result.replace(Regex("\\*{1,3}([^*]+)\\*{1,3}"), "$1")
        result = result.replace(Regex("~~([^~]+)~~"), "$1")
        result = result.replace(Regex("`([^`]+)`"), "$1")
        result = result.replace(Regex("^>\\s*", RegexOption.MULTILINE), "")
        result = result.replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
        result = result.replace(Regex("^-{3,}$", RegexOption.MULTILINE), "")
        result = result.replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
        result = result.replace(Regex("!\\[([^]]*)]\\([^)]+\\)"), "$1")
        result = result.replace(Regex("\\[attachment:[^\\]]*\\]"), "")
        result = result.replace(Regex("\\n{3,}"), "\n\n")
        return result.trim()
    }

    private fun breakText(canvas: Canvas, text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (paragraph in paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("")
                continue
            }
            val chars = paragraph.toCharArray()
            var start = 0
            while (start < chars.size) {
                val end = paint.breakText(chars, start, chars.size, maxWidth, null)
                lines.add(String(chars, start, end))
                start += end
            }
        }
        return lines
    }

    fun createBitmapFromView(scrollView: NestedScrollView, scale: Float = 1f): Bitmap {
        val totalHeight = scrollView.getChildAt(0).height
        val width = scrollView.width

        val bitmap = Bitmap.createBitmap(
            (width * scale).toInt(),
            (totalHeight * scale).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)

        val bg = scrollView.background
        if (bg != null) {
            bg.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        val prevLayerType = scrollView.layerType
        scrollView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        try {
            scrollView.draw(canvas)
        } finally {
            scrollView.setLayerType(prevLayerType, null)
        }
        return bitmap
    }

    fun saveBitmapToDownloads(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "image/png")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    true
                } ?: false
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun generateScreenshotFileName(note: Note): String {
        val title = note.title.ifEmpty { "未命名笔记" }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeTitle = title.replace(Regex("[^\\w\\u4e00-\\u9fff\\-]"), "_")
        return "${safeTitle}_${timestamp}.png"
    }
}
