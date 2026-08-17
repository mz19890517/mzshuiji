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
import android.widget.TextView
import androidx.core.widget.NestedScrollView
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
            val safeTitle = title.replace(Regex("[^\\w\\u4e00-\\u9fff\\-]"), "_")
            val fileName = "${safeTitle}_${timestamp}.pdf"

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = document.startPage(pageInfo)

            val canvas = page.canvas
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

            var y = 50f
            val leftMargin = 40f
            val rightMargin = 555f
            val lineHeight = 20f

            // Draw title using StaticLayout for proper CJK rendering
            val titleLayout = android.text.StaticLayout.Builder.obtain(
                title, 0, title.length,
                android.text.TextPaint(titlePaint),
                (rightMargin - leftMargin).toInt()
            ).setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .build()
            canvas.save()
            canvas.translate(leftMargin, y)
            titleLayout.draw(canvas)
            canvas.restore()
            y += titleLayout.height + 10f

            // Draw content using StaticLayout for proper CJK rendering
            val content = if (note.isList) note.taskListToString(withCheckmarks = true) else note.content
            val contentLayout = android.text.StaticLayout.Builder.obtain(
                content, 0, content.length,
                android.text.TextPaint(contentPaint),
                (rightMargin - leftMargin).toInt()
            ).setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .build()

            var contentOffset = 0
            val pageHeight = 790f
            while (contentOffset < content.length) {
                canvas.save()
                canvas.translate(leftMargin, y)
                val partialLayout = android.text.StaticLayout.Builder.obtain(
                    content, contentOffset, content.length,
                    android.text.TextPaint(contentPaint),
                    (rightMargin - leftMargin).toInt()
                ).setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1f)
                    .build()

                // Find how many lines fit on this page
                var linesUsed = 0
                for (i in 0 until partialLayout.lineCount) {
                    val lineBottom = partialLayout.getLineBottom(i)
                    if (lineBottom > pageHeight - y + 50f) break
                    linesUsed++
                }
                if (linesUsed == 0) linesUsed = 1

                val visibleLayout = android.text.StaticLayout.Builder.obtain(
                    content, contentOffset, content.length,
                    android.text.TextPaint(contentPaint),
                    (rightMargin - leftMargin).toInt()
                ).setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1f)
                    .build()

                visibleLayout.draw(canvas)
                canvas.restore()

                contentOffset = visibleLayout.getLineEnd(linesUsed - 1)
                if (contentOffset >= content.length) break

                document.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                val newPage = document.startPage(newPageInfo)
                y = 50f
            }

            document.finishPage(page)

            // Save to cache
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
