package app.jeongsan.server.receipt

import org.springframework.stereotype.Service
import java.io.IOException
import java.io.InputStream

interface ReceiptOcrClient {
    fun available(): Boolean
    fun extract(bytes: ByteArray, mediaType: String): ReceiptExtractionResponse
}

@Service
class ReceiptService(private val access: ReceiptUnitAccess, private val client: ReceiptOcrClient, private val admission: ReceiptAdmission) {
    fun extract(gatheringId: Long, settlementUnitId: Long, userId: Long, contentType: String?, contentLength: Long,
                source: () -> InputStream): ReceiptExtractionResponse {
        access.requireHost(gatheringId, settlementUnitId, userId)
        if (!client.available()) throw ReceiptException("OCR_UNAVAILABLE", 503)
        val mediaType = ReceiptPolicy.mediaType(contentType) ?: throw ReceiptException("IMAGE_FORMAT_UNSUPPORTED", 415)
        ReceiptPolicy.lengthError(contentLength)?.let { throw ReceiptException(it, if (it == "IMAGE_TOO_LARGE") 413 else 400) }
        admission.enter(userId).use {
            val bytes = try { source().use { it.readNBytes(ReceiptPolicy.MAX_BYTES + 1) } }
                catch (_: IOException) { throw ReceiptException("IMAGE_INVALID", 400) }
            ReceiptPolicy.lengthError(bytes.size.toLong())?.let { throw ReceiptException(it, if (it == "IMAGE_TOO_LARGE") 413 else 400) }
            if (!ReceiptPolicy.matchesSignature(bytes, mediaType)) throw ReceiptException("IMAGE_INVALID", 400)
            val result = client.extract(bytes, mediaType)
            // 추론 도중 탈퇴·총무 변경·정산 확정이 있었다면 후보도 돌려주지 않는다.
            access.requireHost(gatheringId, settlementUnitId, userId)
            return result
        }
    }
}
