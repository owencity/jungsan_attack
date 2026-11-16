package app.jeongsan.server.receipt

import app.jeongsan.server.common.ActiveUserAccess
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.io.InputStream

interface ReceiptOcrClient {
    fun available(): Boolean
    fun extract(bytes: ByteArray, mediaType: String): ReceiptExtractionResponse
}

@Service
// 상위 트랜잭션을 일시 중단해도 DB 잠금은 남는다. 그런 호출은 추론 전에 거절한다.
@Transactional(propagation = Propagation.NEVER)
class ReceiptService(private val access: ReceiptUnitAccess, private val client: ReceiptOcrClient,
                     private val admission: ReceiptAdmission, private val metrics: ReceiptMetrics,
                     private val users: ActiveUserAccess) {
    fun extract(gatheringId: Long, settlementUnitId: Long, userId: Long, contentType: String?, contentLength: Long,
                source: () -> InputStream): ReceiptExtractionResponse =
        extractPhoto(userId, contentType, contentLength, source) { access.requireHost(gatheringId, settlementUnitId, userId) }

    fun extract(userId: Long, contentType: String?, contentLength: Long, source: () -> InputStream): ReceiptExtractionResponse =
        extractPhoto(userId, contentType, contentLength, source) { users.requireUser(userId) }

    private fun extractPhoto(userId: Long, contentType: String?, contentLength: Long, source: () -> InputStream,
                             requireAccess: () -> Unit): ReceiptExtractionResponse = metrics.observe {
        requireAccess()
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
            requireAccess()
            result
        }
    }
}
