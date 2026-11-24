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
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.io.ByteArrayInputStream
import java.io.IOException

class ReceiptReceptionSpec : StringSpec({
    val jwt = JwtService("01234567890123456789012345678901", 30)
    val cookie = Cookie(AuthController.COOKIE_NAME, jwt.issue(12))
    val path = "/api/v1/gatherings/1/settlement-units/10/receipts/receive"
    val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0)
    val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
    val extract = Mockito.mock(ReceiptService::class.java)
    var checks = 0
    var accessFailure: ApiException? = null
    var changedAfterRead = false
    var admission = ReceiptAdmission()
    val photos = ReceiptPhotoReader()
    val access = ReceiptUnitAccess { gatheringId, settlementUnitId, userId ->
        listOf(gatheringId, settlementUnitId, userId) shouldBe listOf(1L, 10L, 12L)
        checks++
        accessFailure?.let { throw it }
        if (changedAfterRead && checks > 1) throw ApiException("SETTLEMENT_UNIT_NOT_OPEN", HttpStatus.CONFLICT, "OPEN 필요")
    }
    fun service() = ReceiptReceptionService(access, photos, admission)
    fun mvc() = MockMvcBuilders.standaloneSetup(ReceiptController(extract, service()))
        .setCustomArgumentResolvers(LoginUserArgumentResolver(jwt)).setControllerAdvice(GlobalExceptionHandler())
        .setMessageConverters(JacksonJsonHttpMessageConverter(jacksonMapperBuilder().build())).build()
    beforeTest { checks = 0; accessFailure = null; changedAfterRead = false; admission = ReceiptAdmission(); Mockito.reset(extract) }
    afterTest { Mockito.verifyNoInteractions(extract) }

    "카메라 메모리 JPEG는 파일명 없이 수신 메타데이터만 반환한다" {
        mvc().perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg))
            .andExpect(status().isOk).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.receivedBytes").value(jpeg.size)).andExpect(jsonPath("$.contentType").value("image/jpeg"))
            .andExpect(jsonPath("$.ocrProcessed").value(false)).andExpect(jsonPath("$.receiptId").doesNotExist())
            .andExpect(jsonPath("$.drinks").doesNotExist()).andExpect(jsonPath("$.url").doesNotExist())
        checks shouldBe 2
    }
    "갤러리 PNG도 같은 raw 사진 계약을 사용한다" {
        mvc().perform(post(path).cookie(cookie).contentType("image/png").content(png))
            .andExpect(status().isOk).andExpect(jsonPath("$.receivedBytes").value(png.size))
            .andExpect(jsonPath("$.contentType").value("image/png")).andExpect(jsonPath("$.ocrProcessed").value(false))
    }
    "앱 Bearer로 촬영 사진을 수신한다" {
        mvc().perform(post(path).header("Authorization", "Bearer " + jwt.issue(12, "APP")).contentType("image/jpeg").content(jpeg))
            .andExpect(status().isOk).andExpect(jsonPath("$.ocrProcessed").value(false))
    }
    "로그인 없는 촬영 요청은 사진 검사 전에 401이다" {
        mvc().perform(post(path).contentType("application/json").content(jpeg)).andExpect(status().isUnauthorized)
        checks shouldBe 0
    }
    "다른 총무는 사진 스트림을 열지 않고 403이다" {
        accessFailure = ApiException("NOT_SETTLEMENT_UNIT_HOST", HttpStatus.FORBIDDEN, "총무만 가능")
        var opened = false
        shouldThrow<ApiException> { service().receive(1, 10, 12, "image/jpeg", -1) { opened = true; ByteArrayInputStream(jpeg) } }.status.value() shouldBe 403
        opened shouldBe false
        mvc().perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg)).andExpect(status().isForbidden)
    }
    "정산 중 단위는 수신도 409다" {
        accessFailure = ApiException("SETTLEMENT_UNIT_NOT_OPEN", HttpStatus.CONFLICT, "OPEN 필요")
        mvc().perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg)).andExpect(status().isConflict)
    }
    "지원하지 않는 전송 형식과 위장 파일과 빈 사진을 거절한다" {
        val mvc = mvc()
        for (mediaType in listOf("application/json", "multipart/form-data", "application/pdf")) {
            mvc.perform(post(path).cookie(cookie).contentType(mediaType).content(jpeg)).andExpect(status().isUnsupportedMediaType)
        }
        mvc.perform(post(path).cookie(cookie).contentType("image/png").content(jpeg)).andExpect(status().isBadRequest)
        mvc.perform(post(path).cookie(cookie).contentType("image/jpeg").content(byteArrayOf())).andExpect(status().isBadRequest)
    }
    "길이가 명시된 큰 사진은 스트림을 열기 전에 거절한다" {
        var opened = false
        shouldThrow<ReceiptException> {
            service().receive(1, 10, 12, "image/jpeg", ReceiptPolicy.MAX_BYTES + 1L) { opened = true; ByteArrayInputStream(jpeg) }
        }.status.value() shouldBe 413
        opened shouldBe false
    }
    "수신 완료와 읽기 실패 모두 스트림을 닫는다" {
        var closed = false
        val source = object : ByteArrayInputStream(jpeg) { override fun close() { closed = true; super.close() } }
        service().receive(1, 10, 12, "image/jpeg", -1) { source }.receivedBytes shouldBe jpeg.size
        closed shouldBe true
        closed = false
        val broken = object : java.io.InputStream() {
            override fun read(): Int = throw IOException("private-image-error")
            override fun close() { closed = true }
        }
        shouldThrow<ReceiptException> { service().receive(1, 10, 12, "image/jpeg", -1) { broken } }.code shouldBe "IMAGE_INVALID"
        closed shouldBe true
        service().receive(1, 10, 12, "image/jpeg", jpeg.size.toLong()) { ByteArrayInputStream(jpeg) }.ocrProcessed shouldBe false
    }
    "전송 도중 단위 상태 변경은 수신 성공으로 응답하지 않는다" {
        changedAfterRead = true
        mvc().perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg)).andExpect(status().isConflict)
        checks shouldBe 2
    }
    "모델 준비 상태 조회 없이 네 번째 수신을 429로 제한한다" {
        val mvc = mvc()
        repeat(3) { mvc.perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg)).andExpect(status().isOk) }
        mvc.perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg))
            .andExpect(status().isTooManyRequests).andExpect(header().exists("Retry-After"))
    }
    "수신과 추출은 같은 사용자 한도를 공유한다" {
        repeat(3) { service().receive(1, 10, 12, "image/jpeg", -1) { ByteArrayInputStream(jpeg) } }
        val client = Mockito.mock(ReceiptOcrClient::class.java)
        Mockito.`when`(client.available()).thenReturn(true)
        val extraction = ReceiptService(access, client, admission, photos)
        shouldThrow<ReceiptException> { extraction.extract(1, 10, 12, "image/jpeg", -1) { ByteArrayInputStream(jpeg) } }.code shouldBe "OCR_REQUEST_LIMIT"
        Mockito.mockingDetails(client).invocations.count { it.method.name == "extract" } shouldBe 0
    }
    "사용 중인 수신 슬롯은 Retry-After와 503이다" {
        admission.enter(99).use {
            mvc().perform(post(path).cookie(cookie).contentType("image/jpeg").content(jpeg))
                .andExpect(status().isServiceUnavailable).andExpect(header().string("Retry-After", "2"))
        }
    }
})
