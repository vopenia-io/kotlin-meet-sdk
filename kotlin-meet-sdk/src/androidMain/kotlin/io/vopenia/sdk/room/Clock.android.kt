package io.vopenia.sdk.room

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

private val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

internal actual fun currentTimeMillisToIso(): String = isoFormatter.format(Date())
