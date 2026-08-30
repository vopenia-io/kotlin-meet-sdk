package io.vopenia.sdk.room

import platform.Foundation.NSDate
import platform.Foundation.NSISO8601DateFormatOptions
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.NSISO8601DateFormatWithFractionalSeconds
import platform.Foundation.NSISO8601DateFormatWithInternetDateTime
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

internal actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970() * 1000.0).toLong()

private val isoFormatter: NSISO8601DateFormatter = NSISO8601DateFormatter().apply {
    formatOptions = NSISO8601DateFormatWithInternetDateTime or
        NSISO8601DateFormatWithFractionalSeconds
}

internal actual fun currentTimeMillisToIso(): String =
    isoFormatter.stringFromDate(NSDate())

internal actual fun newUuid(): String = NSUUID().UUIDString()
