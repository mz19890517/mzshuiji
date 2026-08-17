package com.mz.shunji.ui.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.mz.shunji.data.model.Note
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
            val fileName = "${title}_${timestamp}.pdf"

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = document.startPage(pageInfo)

            val canvas = page.canvas
            val paint = Paint().apply {
                color = Color.BLACK
                isAntiAlias = true
            }

            var y = 50f
            val leftMargin = 40f
            val rightMargin = 555f
            val lineHeight = 20f

            // Draw title
            paint.textSize = 18f
            paint.isFakeBoldText = true
            val titleLines = breakText(canvas, title, paint, rightMargin - leftMargin)
            for (line in titleLines) {
                canvas.drawText(line, leftMargin, y, paint)
                y += lineHeight + 8
            }
            y += 10

            // Draw content
            paint.textSize = 12f
            paint.isFakeBoldText = false
            val content = if (note.isList) note.taskListToString(withCheckmarks = true) else note.content
            val contentLines = breakText(canvas, content, paint, rightMargin - leftMargin)
            for (line in contentLines) {
                if (y > 800f) {
                    document.finishPage(page)
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                    val newPage = document.startPage(newPageInfo)
                    y = 50f
                }
                canvas.drawText(line, leftMargin, y, paint)
                y += lineHeight
            }

            document.finishPage(page)

            // Save to cache first
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun breakText(canvas: Canvas, text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (paragraph in paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("")
                continue
            }
            var start = 0
            while (start < paragraph.length) {
                val end = paint.breakText(paragraph, start, paragraph.length, maxWidth, null)
                lines.add(paragraph.substring(start, start + end))
                start += end
            }
        }
        return lines
    }

    fun createBitmapFromView scrollView: ScrollView, scale: Float = 1f: Bitmap {
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

        scrollView.draw(canvas)
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
        return "${title}_${timestamp}.png"
    }
}
