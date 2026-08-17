package com.mz.shunji

import org.junit.Assert.assertEquals
import org.junit.Test
import com.mz.shunji.data.model.Attachment
import com.mz.shunji.data.model.Note

class NoteUnitTests {
    @Test
    fun defaultNoteShouldBeEmpty() {
        assertEquals(Note().isEmpty(), true)
    }

    @Test
    fun noteWithAttachmentsShouldNotBeEmpty() {
        assertEquals(Note(attachments = listOf(Attachment())).isEmpty(), false)
    }
}
