package com.mz.shunji.ui.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.mz.shunji.R
import com.mz.shunji.data.model.Attachment
import com.mz.shunji.data.model.Note
import com.mz.shunji.ui.attachments.uri

fun shareNote(context: Context, note: Note) {
    val sendIntent: Intent = Intent().apply {
        val textContent = if (note.isList) note.taskListToString(withCheckmarks = true) else note.content
        putExtra(
            Intent.EXTRA_TITLE,
            note.title
        )
        putExtra(
            Intent.EXTRA_TEXT,
            textContent,
        )

        note.attachments
            .map { it.uri(context) }
            .ifEmpty {
                action = Intent.ACTION_SEND
                type = "text/plain"
                null
            }
            ?.let { uris ->
                clipData = ClipData("", arrayOf("*/*"), ClipData.Item(uris.first())).apply {
                    (1 until uris.size).forEach { addItem(ClipData.Item(uris[it])) }
                }

                action = Intent.ACTION_SEND_MULTIPLE
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
    }

    val chooser = Intent.createChooser(sendIntent, null)
    ContextCompat.startActivity(context, chooser, null)
}

fun shareAttachment(context: Context, attachment: Attachment) {
    val sendIntent: Intent = Intent().apply {
        val uri = attachment.uri(context) ?: return

        action = Intent.ACTION_SEND
        data = uri
        type = "*/*"

        clipData = ClipData(
            attachment.description,
            arrayOf("*/*"),
            ClipData.Item(uri)
        )

        putExtra(Intent.EXTRA_TEXT, attachment.description)
        putExtra(Intent.EXTRA_STREAM, uri)

        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(sendIntent, context.getString(R.string.action_share))
    ContextCompat.startActivity(context, chooser, null)
}

fun shareNoteAsText(context: Context, note: Note) {
    val rawContent = if (note.isList) note.taskListToString(withCheckmarks = true) else note.content
    val textContent = stripMarkdown(rawContent)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TITLE, note.title)
        putExtra(Intent.EXTRA_TEXT, if (note.title.isNotEmpty()) "${note.title}\n\n${textContent}" else textContent)
        type = "text/plain"
    }
    val chooser = Intent.createChooser(sendIntent, context.getString(R.string.action_share))
    ContextCompat.startActivity(context, chooser, null)
}

private fun stripMarkdown(text: String): String {
    var result = text
    // Remove inline content markers [attachment:path]
    result = result.replace(Regex("\\[attachment:[^\\]]*\\]"), "")
    // Remove bold/italic markers
    result = result.replace(Regex("\\*{1,3}([^*]+)\\*{1,3}"), "$1")
    // Remove strikethrough
    result = result.replace(Regex("~~([^~]+)~~"), "$1")
    // Remove inline code
    result = result.replace(Regex("`([^`]+)`"), "$1")
    // Remove blockquotes
    result = result.replace(Regex("^>\\s*", RegexOption.MULTILINE), "")
    // Remove headings
    result = result.replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
    // Remove horizontal rules
    result = result.replace(Regex("^-{3,}$", RegexOption.MULTILINE), "")
    // Remove links [text](url) -> text
    result = result.replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
    // Remove images ![alt](url)
    result = result.replace(Regex("!\\[([^]]*)]\\([^)]+\\)"), "$1")
    // Clean up multiple blank lines
    result = result.replace(Regex("\\n{3,}"), "\n\n")
    return result.trim()
}

fun shareNoteAsImages(context: Context, note: Note) {
    val imageAttachments = note.attachments.filter { it.type == Attachment.Type.IMAGE }
    if (imageAttachments.isEmpty()) {
        Toast.makeText(context, "笔记中没有图片", Toast.LENGTH_SHORT).show()
        return
    }
    val uris = imageAttachments.mapNotNull { it.uri(context) }
    if (uris.isEmpty()) return

    val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        putExtra(Intent.EXTRA_TITLE, note.title)
        putExtra(Intent.EXTRA_TEXT, note.title)
        type = "image/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        clipData = ClipData("", arrayOf("image/*"), ClipData.Item(uris.first())).apply {
            (1 until uris.size).forEach { addItem(ClipData.Item(uris[it])) }
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(sendIntent, context.getString(R.string.action_share))
    ContextCompat.startActivity(context, chooser, null)
}

fun shareNoteAsFiles(context: Context, note: Note) {
    if (note.attachments.isEmpty()) {
        Toast.makeText(context, "笔记中没有附件", Toast.LENGTH_SHORT).show()
        return
    }
    val uris = note.attachments.mapNotNull { it.uri(context) }
    if (uris.isEmpty()) return

    val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        putExtra(Intent.EXTRA_TITLE, note.title)
        putExtra(Intent.EXTRA_TEXT, note.title)
        type = "*/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        clipData = ClipData("", arrayOf("*/*"), ClipData.Item(uris.first())).apply {
            (1 until uris.size).forEach { addItem(ClipData.Item(uris[it])) }
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(sendIntent, context.getString(R.string.action_share))
    ContextCompat.startActivity(context, chooser, null)
}
