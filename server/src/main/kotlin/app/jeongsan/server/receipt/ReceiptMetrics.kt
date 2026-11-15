package app.jeongsan.server.receipt

import app.jeongsan.server.common.ApiException
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component

/** 사진·상품명·사용자·술자리 ID 없이 고정된 결과 코드와 처리 시간만 집계한다. */
@Component
class ReceiptMetrics(private val registry: MeterRegistry) {
    private val rejectionCodes = setOf("UNAUTHENTICATED", "SETTLEMENT_UNIT_NOT_FOUND", "NOT_PARTICIPANT",
        "NOT_SETTLEMENT_UNIT_HOST", "SETTLEMENT_UNIT_NOT_OPEN", "IMAGE_FORMAT_UNSUPPORTED", "IMAGE_TOO_LARGE",
        "IMAGE_SIZE_INVALID", "IMAGE_INVALID", "IMAGE_DIMENSIONS_TOO_LARGE", "OCR_REQUEST_LIMIT", "OCR_BUSY")
    private val failureCodes = setOf("OCR_TIMEOUT", "OCR_UNAVAILABLE", "OCR_BAD_RESPONSE")

    fun <T> observe(block: () -> T): T {
        val sample = Timer.start(registry)
        var outcome = "success"
        var code = "NONE"
        try {
            return block()
        } catch (failure: Exception) {
            val suppliedCode = (failure as? ApiException)?.code
            code = suppliedCode?.takeIf { it in rejectionCodes || it in failureCodes } ?: "INTERNAL_ERROR"
            outcome = if (code in rejectionCodes) "rejected" else "error"
            throw failure
        } finally {
            sample.stop(Timer.builder("receipt.ocr.requests")
                .description("Receipt extraction attempts and duration, without receipt or account data")
                .tags("outcome", outcome, "code", code).register(registry))
        }
    }
}
