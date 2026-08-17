package com.mz.shunji.tests.reminders

import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.mz.shunji.ui.reminders.ReminderManager
import java.time.Instant

class ReminderScheduleTest : KoinComponent {
    private val reminderManager: ReminderManager by inject()

    @Test
    @Throws(Exception::class)
    fun reminderIsScheduledCorrectly() {
        val (reminderId, noteId) = 1L to 1L
        reminderManager.schedule(reminderId, Instant.now().plusSeconds(3600).epochSecond, noteId)
        assertTrue(reminderManager.isReminderSet(reminderId, noteId))
    }
}
