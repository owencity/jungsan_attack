package app.jeongsan.server.receipt

import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStream

/** 촬영 직후의 메모리 바이트와 갤러리 파일은 같은 raw 사진 계약으로 읽는다. */
@Component
class ReceiptPhotoReader {
    fun mediaType(contentType: String?, contentLength: Long): String {
        val mediaType = ReceiptPolicy.mediaType(contentType) ?: throw ReceiptException("IMAGE_FORMAT_UNSUPPORTED", 415)
        requireLength(contentLength)
        return mediaType
    }

    fun read(mediaType: String, source: () -> InputStream): ByteArray {
        val bytes = try { source().use { it.readNBytes(ReceiptPolicy.MAX_BYTES + 1) } }
            catch (_: IOException) { throw ReceiptException("IMAGE_INVALID", 400) }
        requireLength(bytes.size.toLong())
        if (!ReceiptPolicy.matchesSignature(bytes, mediaType)) throw ReceiptException("IMAGE_INVALID", 400)
        return bytes
    }

    private fun requireLength(length: Long) {
        ReceiptPolicy.lengthError(length)?.let { throw ReceiptException(it, if (it == "IMAGE_TOO_LARGE") 413 else 400) }
    }
}
