package com.mz.shunji.ui.attachments

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import coil.decode.VideoFrameDecoder
import coil.fetch.Fetcher
import coil.load
import io.noties.markwon.Markwon
import com.mz.shunji.R
import com.mz.shunji.data.model.Attachment
import com.mz.shunji.data.model.Note
import com.mz.shunji.databinding.LayoutAttachmentBinding
import com.mz.shunji.ui.editor.markdown.applyTo
import com.mz.shunji.ui.utils.coil.AlbumArtFetcher
import com.mz.shunji.ui.utils.dp

/**
 * Marker based inline attachments (图文模式).
 *
 * When a note is in inline mode, attachments are referenced inside the note content
 * with a marker of the form `[[attachment:path]]`. The content is then rendered as a
 * sequence of text segments and inline attachment views.
 */
object InlineContent {
    const val PREFIX = "[[attachment:"
    const val SUFFIX = "]]"

    val regex = Regex("\\[\\[attachment:([^\\]]+)]]")

    fun markerFor(attachment: Attachment): String = "$PREFIX${attachment.path}$SUFFIX"

    fun containsMarker(content: String, attachment: Attachment): Boolean =
        content.contains(markerFor(attachment))

    fun stripMarkers(content: String): String = content.replace(regex, "")

    fun rearrangeMarkers(content: String, fromPath: String, toPath: String): String {
        val allMarkers = regex.findAll(content).map { it.value }.toList()
        val fromMarker = "$PREFIX$fromPath$SUFFIX"
        val toMarker = "$PREFIX$toPath$SUFFIX"
        val fromIdx = allMarkers.indexOf(fromMarker)
        val toIdx = allMarkers.indexOf(toMarker)
        if (fromIdx < 0 || toIdx < 0 || fromIdx == toIdx) return content

        val reordered = allMarkers.toMutableList()
        reordered.removeAt(fromIdx)
        reordered.add(toIdx, fromMarker)

        var result = content
        for ((i, marker) in allMarkers.withIndex()) {
            result = result.replaceFirst(marker, reordered[i])
        }
        return result
    }

    sealed class Segment {
        data class Text(val text: String) : Segment()
        data class AttachmentRef(val attachment: Attachment) : Segment()
    }

    fun parse(content: String, attachments: List<Attachment>): List<Segment> {
        if (content.isEmpty()) return emptyList()

        val byPath = attachments.associateBy { it.path }
        val segments = mutableListOf<Segment>()
        var lastIndex = 0

        regex.findAll(content).forEach { match ->
            if (match.range.first > lastIndex) {
                segments.add(Segment.Text(content.substring(lastIndex, match.range.first)))
            }
            val path = match.groupValues[1]
            byPath[path]?.let { segments.add(Segment.AttachmentRef(it)) }
            lastIndex = match.range.last + 1
        }

        if (lastIndex < content.length) {
            segments.add(Segment.Text(content.substring(lastIndex)))
        }

        return segments
    }
}

/**
 * Renders a note in inline mode into the given container. Text segments are rendered with
 * markwon (if the note has markdown enabled), attachment segments as inline preview cards.
 *
 * @param onAttachmentClick invoked when an inline attachment is tapped; null means not clickable.
 */
fun renderInlineContent(
    container: LinearLayout,
    context: Context,
    note: Note,
    markwon: Markwon?,
    textSize: Float? = null,
    maxAttachmentHeight: Int = 220,
    onAttachmentClick: ((Attachment) -> Unit)? = null,
    onAttachmentLongClick: ((Attachment) -> Boolean)? = null,
) {
    container.removeAllViews()

    val segments = InlineContent.parse(note.content, note.attachments)
    segments.forEach { segment ->
        when (segment) {
            is InlineContent.Segment.Text -> {
                val textView = AppCompatTextView(context).apply {
                    setTextAppearance(R.style.TextAppearance_MaterialComponents_Body1)
                    textSize?.let { setTextSize(it) }
                    setPadding(0, 0, 0, 0)
                }
                if (note.isMarkdownEnabled && markwon != null) {
                    markwon.applyTo(textView, segment.text)
                } else {
                    textView.text = segment.text
                }
                container.addView(
                    textView,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                )
            }

            is InlineContent.Segment.AttachmentRef -> {
                val binding = LayoutAttachmentBinding.inflate(
                    android.view.LayoutInflater.from(context),
                    container,
                    false
                )
                bindInlineAttachment(binding, context, segment.attachment, maxAttachmentHeight)
                if (onAttachmentClick != null) {
                    binding.root.setOnClickListener { onAttachmentClick(segment.attachment) }
                }
                if (onAttachmentLongClick != null) {
                    binding.root.setOnLongClickListener { onAttachmentLongClick(segment.attachment) }
                }
                container.addView(
                    binding.root,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        maxAttachmentHeight.dp(context)
                    )
                )
            }
        }
    }
}

private fun bindInlineAttachment(
    binding: LayoutAttachmentBinding,
    context: Context,
    attachment: Attachment,
    maxAttachmentHeight: Int,
) {
    binding.indicatorAttachmentType.isVisible = false
    binding.imageView.scaleType = ImageView.ScaleType.CENTER_CROP

    when (attachment.type) {
        Attachment.Type.IMAGE -> {
            binding.imageView.load(attachment.uri(context))
        }

        Attachment.Type.VIDEO -> {
            binding.imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            binding.imageView.load(attachment.uri(context)) {
                decoderFactory(VideoFrameDecoder.Factory())
            }
            setIndicator(binding, context, R.drawable.ic_movie)
        }

        Attachment.Type.AUDIO -> {
            binding.imageView.load(attachment.uri(context)) {
                fetcherFactory(Fetcher.Factory { data, options, _ ->
                    (data as? android.net.Uri)?.let {
                        AlbumArtFetcher(context, it, options)
                    }
                })
            }
            setIndicator(binding, context, R.drawable.ic_music)
        }

        Attachment.Type.GENERIC -> {
            binding.imageView.setColorFilter(Color.WHITE)
            binding.imageView.load(R.drawable.ic_file)
        }
    }

    if (attachment.description.isNotEmpty()) {
        binding.textView.isVisible = true
        binding.textView.text = attachment.description
    }
}

private fun setIndicator(
    binding: LayoutAttachmentBinding,
    context: Context,
    @androidx.annotation.DrawableRes id: Int,
) {
    binding.indicatorAttachmentType.isVisible = true
    binding.indicatorAttachmentType.setImageDrawable(
        androidx.core.content.ContextCompat.getDrawable(context, id)
    )
}
