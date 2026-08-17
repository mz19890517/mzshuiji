package com.mz.shunji.ui.attachments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.DragEvent
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import coil.decode.VideoFrameDecoder
import coil.fetch.Fetcher
import coil.load
import io.noties.markwon.Markwon
import com.mz.shunji.R
import com.mz.shunji.data.model.Attachment
import com.mz.shunji.data.model.Note
import com.mz.shunji.databinding.LayoutAttachmentBinding
import com.mz.shunji.ui.editor.markdown.applyTo
import com.mz.shunji.ui.media.MediaActivity
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

    fun insertMarkersAt(content: String, markers: String, atBeginning: Boolean): String {
        val newContent = content.trim()
        return if (atBeginning) {
            markers + newContent
        } else {
            newContent + markers
        }
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
 * Gesture support:
 * - Single tap: show attachment menu
 * - Double tap: open/preview attachment
 * - Long press + drag: reorder attachments
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
    onAttachmentMenuClick: ((Attachment) -> Unit)? = null,
    onAttachmentsReordered: ((List<Attachment>) -> Unit)? = null,
    isEditMode: Boolean = false,
) {
    container.removeAllViews()

    val segments = InlineContent.parse(note.content, note.attachments)

    val segmentToAttIndex = mutableMapOf<Int, Int>()
    var attCount = 0
    segments.forEachIndexed { idx, seg ->
        if (seg is InlineContent.Segment.AttachmentRef) {
            segmentToAttIndex[idx] = attCount++
        }
    }

    var dragAttIndex = -1
    var lastDragScreenY = 0f
    val autoScrollHandler = Handler(Looper.getMainLooper())
    var isAutoScrolling = false

    fun findNestedScrollView(): NestedScrollView? {
        var p: ViewParent? = container.parent
        while (p != null) {
            if (p is NestedScrollView) return p
            p = p.parent
        }
        return null
    }

    val autoScrollRunnable = object : Runnable {
        override fun run() {
            val sv = findNestedScrollView() ?: return
            val screenHeight = sv.height
            val edgeZone = (120 * context.resources.displayMetrics.density).toInt()
            val maxSpeed = (30 * context.resources.displayMetrics.density).toInt()

            val scrollAmount = when {
                lastDragScreenY < edgeZone && lastDragScreenY > 0 -> {
                    val ratio = 1f - (lastDragScreenY / edgeZone.toFloat())
                    -(maxSpeed * ratio * ratio).toInt()
                }
                lastDragScreenY > screenHeight - edgeZone && lastDragScreenY < screenHeight -> {
                    val ratio = 1f - ((screenHeight - lastDragScreenY) / edgeZone.toFloat())
                    (maxSpeed * ratio * ratio).toInt()
                }
                else -> 0
            }

            if (scrollAmount != 0) {
                sv.smoothScrollBy(0, scrollAmount)
            }
            autoScrollHandler.postDelayed(this, 40L)
        }
    }

    fun startAutoScroll() {
        if (!isAutoScrolling) {
            isAutoScrolling = true
            autoScrollHandler.post(autoScrollRunnable)
        }
    }

    fun stopAutoScroll() {
        isAutoScrolling = false
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }

    segments.forEachIndexed { index, segment ->
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

                if (!isEditMode) {
                    val attachmentIndex = segmentToAttIndex[index] ?: -1

                    val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                            onAttachmentMenuClick?.invoke(segment.attachment)
                            return true
                        }

                        override fun onDoubleTap(e: MotionEvent): Boolean {
                            onAttachmentClick?.invoke(segment.attachment)
                            return true
                        }

                        override fun onLongPress(e: MotionEvent) {
                            onAttachmentLongClick?.invoke(segment.attachment)
                        }
                    })

                    binding.root.setOnTouchListener { view, event ->
                        gestureDetector.onTouchEvent(event)
                    }

                    binding.root.setOnDragListener { v, dragEvent ->
                        when (dragEvent.action) {
                            DragEvent.ACTION_DRAG_STARTED -> {
                                dragAttIndex = dragEvent.clipData?.getItemAt(0)?.text?.toString()?.toIntOrNull() ?: -1
                                v.alpha = 0.5f
                                true
                            }
                            DragEvent.ACTION_DRAG_ENTERED -> {
                                v.alpha = 0.8f
                                true
                            }
                            DragEvent.ACTION_DRAG_LOCATION -> {
                                val loc = IntArray(2)
                                v.getLocationOnScreen(loc)
                                lastDragScreenY = loc[1] + dragEvent.y
                                true
                            }
                            DragEvent.ACTION_DRAG_EXITED -> {
                                v.alpha = 0.5f
                                true
                            }
                            DragEvent.ACTION_DROP -> {
                                val targetAttIndex = attachmentIndex
                                if (dragAttIndex >= 0 && dragAttIndex != targetAttIndex && onAttachmentsReordered != null) {
                                    val attachments = note.attachments.toMutableList()
                                    if (dragAttIndex < attachments.size && targetAttIndex < attachments.size) {
                                        val item = attachments.removeAt(dragAttIndex)
                                        attachments.add(targetAttIndex, item)
                                        onAttachmentsReordered(attachments)
                                    }
                                }
                                v.alpha = 1f
                                true
                            }
                            DragEvent.ACTION_DRAG_ENDED -> {
                                v.alpha = 1f
                                dragAttIndex = -1
                                stopAutoScroll()
                                true
                            }
                            else -> false
                        }
                    }

                    binding.root.setOnLongClickListener { view ->
                        val clipData = android.content.ClipData.newPlainText("att_index", attachmentIndex.toString())
                        val shadow = View.DragShadowBuilder(view)
                        startAutoScroll()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            view.startDragAndDrop(clipData, shadow, null, 0)
                        } else {
                            @Suppress("DEPRECATION")
                            view.startDrag(clipData, shadow, null, 0)
                        }
                        true
                    }
                } else {
                    if (onAttachmentClick != null) {
                        binding.root.setOnClickListener { onAttachmentClick(segment.attachment) }
                    }
                    if (onAttachmentLongClick != null) {
                        binding.root.setOnLongClickListener { onAttachmentLongClick(segment.attachment) }
                    }
                }

                val heightDp = when (segment.attachment.type) {
                    Attachment.Type.AUDIO -> 36
                    Attachment.Type.GENERIC -> 36
                    else -> maxAttachmentHeight
                }

                container.addView(
                    binding.root,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        heightDp.dp(context)
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
            binding.imageView.isVisible = false
            binding.indicatorAttachmentType.isVisible = false
            binding.textView.isVisible = false

            val container = binding.root as? ViewGroup ?: return
            container.removeAllViews()

            val bar = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(8.dp(context), 0, 8.dp(context), 0)
                setBackgroundColor(Color.parseColor("#1A000000"))
            }

            val playBtn = android.widget.ImageButton(context).apply {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_play))
                background = null
                layoutParams = LinearLayout.LayoutParams(32.dp(context), 32.dp(context))
                setOnClickListener {
                    try {
                        val intent = Intent(context, MediaActivity::class.java).apply {
                            putExtra(MediaActivity.ATTACHMENT, attachment)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
            bar.addView(playBtn)

            val fileName = TextView(context).apply {
                text = attachment.description.ifEmpty { attachment.fileName }
                setTextColor(Color.parseColor("#333333"))
                textSize = 13f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 8.dp(context)
                    marginEnd = 8.dp(context)
                }
            }
            bar.addView(fileName)

            container.addView(bar, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        Attachment.Type.GENERIC -> {
            binding.imageView.isVisible = false
            binding.indicatorAttachmentType.isVisible = false
            binding.textView.isVisible = false

            val container = binding.root as? ViewGroup ?: return
            container.removeAllViews()

            val bar = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(8.dp(context), 0, 8.dp(context), 0)
                setBackgroundColor(Color.parseColor("#1A000000"))
            }

            val icon = android.widget.ImageView(context).apply {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_file))
                setColorFilter(Color.parseColor("#666666"))
                layoutParams = LinearLayout.LayoutParams(24.dp(context), 24.dp(context))
            }
            bar.addView(icon)

            val fileNameText = attachment.description.ifEmpty { attachment.fileName }
            val fileNameView = TextView(context).apply {
                text = fileNameText
                setTextColor(Color.parseColor("#333333"))
                textSize = 13f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 8.dp(context)
                    marginEnd = 8.dp(context)
                }
            }
            bar.addView(fileNameView)

            val fileUri = attachment.uri(context)
            val fileSize = if (fileUri != null) try {
                context.contentResolver.openInputStream(fileUri)?.use { it.available().toLong() } ?: 0L
            } catch (_: Exception) { 0L } else 0L
            val sizeText = formatFileSize(fileSize)
            val sizeView = TextView(context).apply {
                text = sizeText
                setTextColor(Color.parseColor("#999999"))
                textSize = 12f
            }
            bar.addView(sizeView)

            container.addView(bar, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }

    if (attachment.type != Attachment.Type.AUDIO && attachment.type != Attachment.Type.GENERIC) {
        if (attachment.description.isNotEmpty()) {
            binding.textView.isVisible = true
            binding.textView.text = attachment.description
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
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
