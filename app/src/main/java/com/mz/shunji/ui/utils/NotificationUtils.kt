package com.mz.shunji.ui.utils

import android.app.NotificationManager
import java.time.Instant

fun NotificationManager.generateId(): Int = Instant.now().toEpochMilli().toInt()
