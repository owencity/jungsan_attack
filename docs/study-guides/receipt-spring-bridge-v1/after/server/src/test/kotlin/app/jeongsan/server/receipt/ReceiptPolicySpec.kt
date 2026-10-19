package app.jeongsan.server.receipt

import app.jeongsan.server.receipt.javaimpl.ReceiptPolicy as JavaPolicy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReceiptPolicySpec : StringSpec({
    "권한 결정은 양 언어가 실제 기대 오류와 같다" {
        val cases = listOf(
            arrayOf<Any?>(false, false, false, "OPEN", "SETTLEMENT_UNIT_NOT_FOUND"),
            arrayOf<Any?>(true, false, false, "OPEN", "NOT_PARTICIPANT"),
            arrayOf<Any?>(true, true, false, "OPEN", "NOT_SETTLEMENT_UNIT_HOST"),
            arrayOf<Any?>(true, true, true, "SETTLING", "SETTLEMENT_UNIT_NOT_OPEN"),
            arrayOf<Any?>(true, true, true, "COMPLETED", "SETTLEMENT_UNIT_NOT_OPEN"),
            arrayOf<Any?>(true, true, true, "OPEN", null))
        cases.forEach { case ->
            ReceiptPolicy.access(case[0] as Boolean, case[1] as Boolean, case[2] as Boolean, case[3] as String) shouldBe case[4]
            JavaPolicy.access(case[0] as Boolean, case[1] as Boolean, case[2] as Boolean, case[3] as String) shouldBe case[4]
        }
    }
    "매체 타입과 길이 경계는 양 언어에서 동일하다" {
        listOf(null to null, "image/png" to "image/png", " IMAGE/JPEG ; charset=x" to "image/jpeg", "application/pdf" to null,
            "multipart/form-data" to null, "image/jpg" to null).forEach { (header, expected) ->
            ReceiptPolicy.mediaType(header) shouldBe expected
            JavaPolicy.mediaType(header) shouldBe expected
        }
        listOf(-2L to "IMAGE_SIZE_INVALID", -1L to null, 0L to "IMAGE_SIZE_INVALID", 1L to null,
            ReceiptPolicy.MAX_BYTES.toLong() to null, ReceiptPolicy.MAX_BYTES + 1L to "IMAGE_TOO_LARGE").forEach { (length, expected) ->
            ReceiptPolicy.lengthError(length) shouldBe expected
            JavaPolicy.lengthError(length) shouldBe expected
        }
    }
    "파일 시그니처는 JPEG PNG의 실제 앞부분과 일치해야 한다" {
        val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte())
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        for ((bytes, mediaType, expected) in listOf(Triple(jpeg, "image/jpeg", true), Triple(png, "image/png", true),
            Triple(jpeg, "image/png", false), Triple(png, "image/jpeg", false), Triple(byteArrayOf(), "image/png", false))) {
            ReceiptPolicy.matchesSignature(bytes, mediaType) shouldBe expected
            JavaPolicy.matchesSignature(bytes, mediaType) shouldBe expected
        }
    }
    "요청 제한은 60초 경계와 남은 초 올림을 양 언어로 검증한다" {
        listOf(Triple(2, 0L, 0L), Triple(3, 0L, 60L), Triple(3, 1L, 60L), Triple(3, 59_000_000_001L, 1L),
            Triple(3, 60_000_000_000L, 0L)).forEach { (usedCount, elapsed, expected) ->
            ReceiptPolicy.retryAfter(usedCount, elapsed) shouldBe expected
            JavaPolicy.retryAfter(usedCount, elapsed) shouldBe expected
        }
    }
    "대기열 없는 busy는 횟수를 소비하지 않고 lease는 한번만 반환한다" {
        val admission = ReceiptAdmission()
        val lease = admission.enter(1, 0)
        shouldThrow<ReceiptException> { admission.enter(2, 0) }.code shouldBe "OCR_BUSY"
        lease.close(); lease.close()
        admission.enter(2, 0).use {
            shouldThrow<ReceiptException> { admission.enter(3, 0) }.code shouldBe "OCR_BUSY"
        }
        repeat(2) { admission.enter(2, 0).close() }
        val limited = shouldThrow<ReceiptException> { admission.enter(2, 1) }
        limited.code shouldBe "OCR_REQUEST_LIMIT"
        limited.retryAfter shouldBe 60L
        admission.enter(2, ReceiptPolicy.WINDOW_NANOS).close()
    }
    "사용자 창은 만 개까지만 보관하며 만료 후 새 요청을 허용한다" {
        val admission = ReceiptAdmission()
        repeat(10_000) { admission.enter(it.toLong(), 0).close() }
        shouldThrow<ReceiptException> { admission.enter(10_001, 1) }.code shouldBe "OCR_BUSY"
        admission.enter(10_001, ReceiptPolicy.WINDOW_NANOS).close()
    }
})
