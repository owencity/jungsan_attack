package app.jeongsan.server.receipt

import app.jeongsan.server.common.ActiveUserAccess
import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.common.LoginUserArgumentResolver
import app.jeongsan.server.common.UnauthenticatedException
import app.jeongsan.server.user.JwtService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.module.kotlin.jacksonMapperBuilder

class ReceiptIntakeSpec : StringSpec({
    val jwt = JwtService("01234567890123456789012345678901", 30)
    val token = jwt.issue(12, "APP")
    val path = "/api/v1/receipts/extract"
    val unitPath = "/api/v1/gatherings/1/settlement-units/10/receipts/extract"
    val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte())
    var deleted = false
    var deleteDuringCall = false
    var checks = 0
    var unitChecks = 0
    var calls = 0
    var admission = ReceiptAdmission()
    val account = ActiveUserAccess { userId ->
        userId shouldBe 12L
        checks++
        if (deleted) throw UnauthenticatedException()
    }
    val access = ReceiptUnitAccess { _, _, _ -> unitChecks++ }
    val candidate = ReceiptExtractionResponse(2, null, emptyList(), listOf("TOTAL_NOT_FOUND"), true)
    val client = object : ReceiptOcrClient {
        override fun available() = true
        override fun extract(bytes: ByteArray, mediaType: String): ReceiptExtractionResponse {
            calls++
            if (deleteDuringCall) deleted = true
            return candidate
        }
    }
    fun mvc() = ReceiptService(access, client, admission, ReceiptMetrics(SimpleMeterRegistry()), account).let { service ->
        MockMvcBuilders.standaloneSetup(ReceiptIntakeController(service), ReceiptController(service))
            .setCustomArgumentResolvers(LoginUserArgumentResolver(jwt)).setControllerAdvice(GlobalExceptionHandler())
            .setMessageConverters(JacksonJsonHttpMessageConverter(jacksonMapperBuilder().build())).build()
    }
    beforeTest { deleted = false; deleteDuringCall = false; checks = 0; unitChecks = 0; calls = 0; admission = ReceiptAdmission() }

    "간단 정산 OCR은 술자리 없이 계정 확인 전후로 같은 후보를 반환한다" {
        mvc().perform(post(path).header("Authorization", "Bearer $token").contentType("image/jpeg").content(jpeg))
            .andExpect(status().isOk).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.schemaVersion").value(2)).andExpect(jsonPath("$.total").isEmpty)
            .andExpect(jsonPath("$.requiresConfirmation").value(true))
        checks shouldBe 2; unitChecks shouldBe 0; calls shouldBe 1
    }
    "비로그인과 삭제 계정을 거절하고 추론 중 삭제된 계정에도 후보를 전달하지 않는다" {
        mvc().perform(post(path).contentType("image/jpeg").content(jpeg)).andExpect(status().isUnauthorized)
        checks shouldBe 0; calls shouldBe 0
        deleted = true
        mvc().perform(post(path).header("Authorization", "Bearer $token").contentType("image/jpeg").content(jpeg))
            .andExpect(status().isUnauthorized)
        calls shouldBe 0
        deleted = false
        deleteDuringCall = true
        mvc().perform(post(path).header("Authorization", "Bearer $token").contentType("image/jpeg").content(jpeg))
            .andExpect(status().isUnauthorized)
        calls shouldBe 1
    }
    "두 OCR 경로가 사용자 횟수와 동시 실행 한도를 공유한다" {
        val mvc = mvc()
        repeat(5) {
            mvc.perform(post(path).header("Authorization", "Bearer $token").contentType("image/jpeg").content(jpeg)).andExpect(status().isOk)
            mvc.perform(post(unitPath).header("Authorization", "Bearer $token").contentType("image/jpeg").content(jpeg)).andExpect(status().isOk)
        }
        mvc.perform(post(path).header("Authorization", "Bearer $token").contentType("image/jpeg").content(jpeg))
            .andExpect(status().isTooManyRequests).andExpect(header().exists("Retry-After"))
        calls shouldBe 10
        admission = ReceiptAdmission()
        val available = mvc()
        admission.enter(99).use {
            for (busyPath in listOf(path, unitPath)) {
                available.perform(post(busyPath).header("Authorization", "Bearer $token").contentType("image/jpeg").content(jpeg))
                    .andExpect(status().isServiceUnavailable).andExpect(jsonPath("$.code").value("OCR_BUSY"))
                    .andExpect(header().string("Retry-After", "2"))
            }
        }
        calls shouldBe 10
    }
})
