package com.mz.shunji.data.sync.nextcloud.model

import com.mz.shunji.data.model.Note
import com.mz.shunji.data.sync.nextcloud.NextcloudNote

fun Note.asNextcloudNote(id: Long, category: String): NextcloudNote = NextcloudNote(
    id = id,
    title = title,
    content = toStorableContent(),
    category = category,
    favorite = isPinned,
    modified = modifiedDate
)
