package com.mz.shunji.ui.attachments.recycler

import com.mz.shunji.databinding.LayoutAttachmentBinding

interface AttachmentRecyclerListener {
    fun onItemClick(position: Int, viewBinding: LayoutAttachmentBinding)
    fun onLongClick(position: Int, viewBinding: LayoutAttachmentBinding): Boolean
}
