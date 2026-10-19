package app.jeongsan.server.receipt

import java.util.Locale
import kotlin.math.max

/** OCR 후보는 저장 권한이 있는 OPEN 단위의 총무에게만 제공한다. */
object ReceiptPolicy {
    const val MAX_BYTES = 8 * 1024 * 1024
    const val WINDOW_NANOS = 60_000_000_000L

    fun access(exists: Boolean, activeMember: Boolean, host: Boolean, state: String): String? = when {
        !exists -> "SETTLEMENT_UNIT_NOT_FOUND"
        !activeMember -> "NOT_PARTICIPANT"
        !host -> "NOT_SETTLEMENT_UNIT_HOST"
        state != "OPEN" -> "SETTLEMENT_UNIT_NOT_OPEN"
        else -> null
    }

    fun mediaType(header: String?): String? = header?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
        ?.takeIf { it == "image/jpeg" || it == "image/png" }

    fun lengthError(length: Long): String? = when {
        length > MAX_BYTES -> "IMAGE_TOO_LARGE"
        length == 0L || length < -1L -> "IMAGE_SIZE_INVALID"
        else -> null
    }

    fun matchesSignature(bytes: ByteArray, mediaType: String): Boolean = when (mediaType) {
        "image/jpeg" -> bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte()
        "image/png" -> bytes.size >= 8 && bytes.take(8).toByteArray().contentEquals(
            byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a))
        else -> false
    }

    fun retryAfter(usedCount: Int, elapsedNanos: Long): Long =
        if (usedCount < 3 || elapsedNanos >= WINDOW_NANOS) 0 else max(1, (WINDOW_NANOS - elapsedNanos + 999_999_999L) / 1_000_000_000L)
}
