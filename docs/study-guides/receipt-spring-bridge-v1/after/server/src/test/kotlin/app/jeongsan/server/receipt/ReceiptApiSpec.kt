package app.jeongsan.server.receipt

import app.jeongsan.server.common.ApiException
import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.common.LoginUserArgumentResolver
import app.jeongsan.server.user.AuthController
import app.jeongsan.server.user.JwtService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import org.springframework.http.HttpStatus
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.io.ByteArrayInputStream

class ReceiptApiSpec : StringSpec({
    val mapper = jacksonMapperBuilder().build()
    val fixture = ReceiptResponseReader(mapper).read(javaClass.getResourceAsStream("/receipt/response-v2.json")!!.readBytes())
    val jwt = JwtService("01234567890123456789012345678901", 30)
    val cookie = Cookie(AuthController.COOKIE_NAME, jwt.issue(12))
    val path = "/api/v1/gatherings/1/settlement-units/10/receipts/extract"
    val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0)
    var calls = 0
    var checks = 0
    var error: ApiException? = null
    var afterCallError: ApiException? = null
    var enabled = true
    var failure: ReceiptException? = null
    var admission = ReceiptAdmission()
    val access = ReceiptUnitAccess { gatheringId, settlementUnitId, userId ->
        listOf(gatheringId, settlementUnitId, userId) shouldBe listOf(1L, 10L, 12L)
        checks++
        error?.let { throw it }
        if (checks > 1) afterCallError?.let { throw it }
    }
    val client = object : ReceiptOcrClient {
        override fun available() = enabled
        override fun extract(bytes: ByteArray, mediaType: String): ReceiptExtractionResponse {
            calls++
            mediaType shouldBe "image/jpeg"
            failure?.let { throw it }
            return fixture
        }
    }
    fun service() = ReceiptService(access, client, admission)
    fun mvc() = MockMvcBuilders.standaloneSetup(ReceiptController(service()))
        .setCustomArgumentResolvers(LoginUserArgumentResolver(jwt)).setControllerAdvice(GlobalExceptionHandler())
        .setMessageConverters(JacksonJsonHttpMessageConverter(mapper)).build()
    beforeTest { calls = 0; checks = 0; error = null; afterCallError = null; enabled = true; failure = null; admission = ReceiptAdmission() }

    "총무에게만 저장하지 않은 후보를 no-store로 반환한다" {
        mvc().perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg))
            .andExpect(status().isOk).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.schemaVersion").value(2)).andExpect(jsonPath("$.requiresConfirmation").value(true))
            .andExpect(jsonPath("$.drinks[0].unit").value("UNKNOWN")).andExpect(jsonPath("$.drinks[0].bottleCount").isEmpty)
        calls shouldBe 1; checks shouldBe 2
    }
    "로그인 없는 사진 요청은 본문 처리에 진입하지 않는다" {
        mvc().perform(post(path).contentType("application/pdf").content(jpeg)).andExpect(status().isUnauthorized)
        calls shouldBe 0; checks shouldBe 0
    }
    "권한 검사가 실패하면 사진 스트림을 열지 않는다" {
        error = ApiException("NOT_SETTLEMENT_UNIT_HOST", HttpStatus.FORBIDDEN, "총무만 가능")
        var opened = false
        shouldThrow<ApiException> { service().extract(1, 10, 12, "image/jpeg", -1) { opened = true; ByteArrayInputStream(jpeg) } }.code shouldBe "NOT_SETTLEMENT_UNIT_HOST"
        opened shouldBe false; calls shouldBe 0
        mvc().perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg)).andExpect(status().isForbidden)
    }
    "OPEN이 아닌 단위는 409이며 추출하지 않는다" {
        error = ApiException("SETTLEMENT_UNIT_NOT_OPEN", HttpStatus.CONFLICT, "OPEN 필요")
        mvc().perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg)).andExpect(status().isConflict)
        calls shouldBe 0
    }
    "미설정은 503이고 매체 검증 전에도 다른 기능의 설정을 요구하지 않는다" {
        enabled = false
        mvc().perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg))
            .andExpect(status().isServiceUnavailable).andExpect(jsonPath("$.code").value("OCR_UNAVAILABLE"))
        calls shouldBe 0
    }
    "지원하지 않는 매체와 위장 파일과 빈 사진을 거절한다" {
        mvc().perform(post(path).cookie(cookie).contentType("application/pdf").content(jpeg)).andExpect(status().isUnsupportedMediaType)
        mvc().perform(post(path).cookie(cookie).contentType("image/png").content(jpeg)).andExpect(status().isBadRequest)
        mvc().perform(post(path).cookie(cookie).contentType("image/jpeg").content(byteArrayOf())).andExpect(status().isBadRequest)
        calls shouldBe 0
    }
    "알 수 없는 길이도 최대 사진 길이보다 한 바이트만 더 읽는다" {
        var readCount = 0
        val source = object : java.io.InputStream() { override fun read(): Int { readCount++; return 0xff } }
        shouldThrow<ReceiptException> { service().extract(1, 10, 12, "image/jpeg", -1) { source } }.code shouldBe "IMAGE_TOO_LARGE"
        readCount shouldBe ReceiptPolicy.MAX_BYTES + 1; calls shouldBe 0
    }
    "명시한 길이가 초과하면 스트림을 열지 않는다" {
        shouldThrow<ReceiptException> { service().extract(1, 10, 12, "image/jpeg", ReceiptPolicy.MAX_BYTES + 1L) { error("열면 안 된다") } }.status.value() shouldBe 413
        calls shouldBe 0
    }
    "네 번째 요청은 Retry-After와 429를 반환한다" {
        val mvc = mvc()
        repeat(3) { mvc.perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg)).andExpect(status().isOk) }
        mvc.perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg))
            .andExpect(status().isTooManyRequests).andExpect(header().exists("Retry-After"))
        calls shouldBe 3
    }
    "모델이 사용 중이면 대기열 없이 503을 반환한다" {
        admission.enter(99).use {
            mvc().perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg))
                .andExpect(status().isServiceUnavailable).andExpect(header().string("Retry-After", "2"))
        }
        calls shouldBe 0
    }
    "추론 도중 상태가 변경되면 후보를 반환하지 않는다" {
        afterCallError = ApiException("SETTLEMENT_UNIT_NOT_OPEN", HttpStatus.CONFLICT, "OPEN 필요")
        mvc().perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg)).andExpect(status().isConflict)
        calls shouldBe 1; checks shouldBe 2
    }
    "모델 장애 뒤에도 단일 실행 슬롯은 반환한다" {
        failure = ReceiptException("OCR_TIMEOUT", 504)
        mvc().perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg)).andExpect(status().isGatewayTimeout)
        failure = null
        mvc().perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg)).andExpect(status().isOk)
        calls shouldBe 2
    }
})
