package app.jeongsan.server.receipt

import app.jeongsan.server.common.ApiException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.http.HttpStatus

class ReceiptMetricsSpec : StringSpec({
    "성공과 거절과 모델 장애를 고정 코드 및 처리 시간으로 집계한다" {
        val registry = SimpleMeterRegistry()
        val metrics = ReceiptMetrics(registry)
        metrics.observe { "candidate" } shouldBe "candidate"
        shouldThrow<ReceiptException> { metrics.observe<Unit> { throw ReceiptException("OCR_BUSY", 503, 2) } }
        shouldThrow<ReceiptException> { metrics.observe<Unit> { throw ReceiptException("OCR_TIMEOUT", 504) } }
        registry.get("receipt.ocr.requests").tags("outcome", "success", "code", "NONE").timer().count() shouldBe 1L
        registry.get("receipt.ocr.requests").tags("outcome", "rejected", "code", "OCR_BUSY").timer().count() shouldBe 1L
        registry.get("receipt.ocr.requests").tags("outcome", "error", "code", "OCR_TIMEOUT").timer().count() shouldBe 1L
    }
    "예외 문자열과 외부 오류 코드는 지표 태그로 내보내지 않는다" {
        val registry = SimpleMeterRegistry()
        val metrics = ReceiptMetrics(registry)
        shouldThrow<ApiException> {
            metrics.observe<Unit> { throw ApiException("private-receipt-product", HttpStatus.BAD_GATEWAY, "private-key-user-image") }
        }
        val id = registry.meters.single().id
        id.tags.associate { it.key to it.value } shouldBe mapOf("code" to "INTERNAL_ERROR", "outcome" to "error")
        id.name shouldBe "receipt.ocr.requests"
    }
})
