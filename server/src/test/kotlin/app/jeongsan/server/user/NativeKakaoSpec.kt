package app.jeongsan.server.user

import app.jeongsan.server.common.ApiException
import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.common.UnauthenticatedException
import app.jeongsan.server.gathering.GatheringStore
import app.jeongsan.server.user.javaimpl.AuthPolicy as JavaPolicy
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito
import org.springframework.core.env.StandardEnvironment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.match.MockRestRequestMatchers.header as requestHeader
import org.springframework.test.web.client.response.MockRestResponseCreators.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.client.RestClient
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.Instant

class NativeKakaoSpec : StringSpec({
    fun code(block: () -> Any?): String = try { block(); "OK" } catch (error: ApiException) { error.code }

    "다른 앱 토큰과 만료·없는 ID를 양쪽 정책에서 거절한다" {
        val cases: List<List<Long?>> = listOf(
            listOf<Long?>(42, 1234, 60), listOf(42, 4321, 60), listOf(42, 1234, 0),
            listOf(0, 1234, 60), listOf(null, 1234, 60), listOf(42, null, 60), listOf(42, 1234, null),
        )
        cases.forEachIndexed { index, values ->
            val expected = if (index == 0) "OK" else "UNAUTHENTICATED"
            code { AuthPolicy.nativeKakaoSubject(values[0], values[1], values[2], 1234) } shouldBe expected
            code { JavaPolicy.nativeKakaoSubject(values[0], values[1], values[2], 1234) } shouldBe expected
        }
        listOf("", "x y", "x\r\nAuthorization: other", "\u00a0", "x".repeat(2049)).forEach {
            code { AuthPolicy.nativeKakaoToken(it) } shouldBe "MALFORMED_REQUEST"
            code { JavaPolicy.nativeKakaoToken(it) } shouldBe "MALFORMED_REQUEST"
        }
        code { AuthPolicy.nativeKakaoToken("native-token._~+/=") } shouldBe "OK"
        NativeKakaoLoginRequest("private-token").toString().contains("private-token") shouldBe false
    }

    fun verifyProvider(info: String, profile: String? = null, statusCode: HttpStatus = HttpStatus.OK): Pair<String, String?> {
        val builder = RestClient.builder().baseUrl("https://kapi.kakao.com")
        val server = MockRestServiceServer.bindTo(builder).build()
        val gateway = RestNativeKakaoGateway(builder.build(), 1234, StandardEnvironment())
        server.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
            .andExpect(requestHeader(HttpHeaders.AUTHORIZATION, "Bearer native-token"))
            .andRespond(withStatus(statusCode).contentType(MediaType.APPLICATION_JSON).body(info))
        if (profile != null) server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
            .andExpect(requestHeader(HttpHeaders.AUTHORIZATION, "Bearer native-token"))
            .andRespond(withSuccess(profile, MediaType.APPLICATION_JSON))
        var subject: String? = null
        val result = code { subject = gateway.identity("native-token").id }
        server.verify()
        return result to subject
    }

    "카카오에서 검증한 앱 ID와 두 사용자 ID가 모두 맞으면 주체를 반환한다" {
        verifyProvider("""{"id":42,"app_id":1234,"expires_in":60}""",
            """{"id":42,"kakao_account":{"profile":{"nickname":"총무"}}}""") shouldBe ("OK" to "42")
    }
    "다른 앱 토큰·실수 ID·만료는 사용자 정보 조회 전에 거절한다" {
        listOf("""{"id":42,"app_id":4321,"expires_in":60}""",
            """{"id":42.5,"app_id":1234,"expires_in":60}""",
            """{"id":42,"app_id":1234,"expires_in":0}""").forEach {
            verifyProvider(it) shouldBe ("UNAUTHENTICATED" to null)
        }
    }
    "토큰 정보와 프로필 ID 불일치도 인증하지 않는다" {
        verifyProvider("""{"id":42,"app_id":1234,"expires_in":60}""", """{"id":99}""") shouldBe ("UNAUTHENTICATED" to null)
    }
    "제공자 인증 거절과 일시 실패를 구분하고 예외 본문을 전달하지 않는다" {
        verifyProvider("""{"msg":"private-token"}""", statusCode = HttpStatus.UNAUTHORIZED) shouldBe ("UNAUTHENTICATED" to null)
        verifyProvider("""{"msg":"private-token"}""", statusCode = HttpStatus.SERVICE_UNAVAILABLE) shouldBe ("AUTH_PROVIDER_UNAVAILABLE" to null)
        verifyProvider("bad JSON") shouldBe ("AUTH_PROVIDER_UNAVAILABLE" to null)
    }
    "공개 교환 API는 검증 후 APP 토큰과 만료만 주고 웹 쿠키를 발급하지 않는다" {
        val service = Mockito.mock(NativeKakaoAuthService::class.java)
        Mockito.`when`(service.login(NativeKakaoLoginRequest("native-token")))
            .thenReturn(AppExchangeResponse("app-token", Instant.parse("2026-12-01T00:00:00Z")))
        val mvc = MockMvcBuilders.standaloneSetup(NativeKakaoAuthController(service))
            .setControllerAdvice(GlobalExceptionHandler())
            .setMessageConverters(JacksonJsonHttpMessageConverter(jacksonMapperBuilder().build())).build()
        mvc.perform(post("/api/v1/auth/kakao/native").contentType(MediaType.APPLICATION_JSON)
            .content("""{"accessToken":"native-token"}"""))
            .andExpect(status().isOk).andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE)).andExpect(jsonPath("$.token").value("app-token"))
        Mockito.verify(service).login(NativeKakaoLoginRequest("native-token"))
        Mockito.clearInvocations(service)
        mvc.perform(post("/api/v1/auth/kakao/native").contentType(MediaType.APPLICATION_JSON)
            .content("""{"accessToken":""}"""))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        Mockito.verifyNoInteractions(service)
        listOf("{}", "{\"accessToken\":null}", "{\"accessToken\":\"private-token\"").forEach { body ->
            mvc.perform(post("/api/v1/auth/kakao/native").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest)
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않습니다."))
        }
        Mockito.verifyNoInteractions(service)
        Mockito.`when`(service.login(NativeKakaoLoginRequest("invalid"))).thenThrow(UnauthenticatedException())
        mvc.perform(post("/api/v1/auth/kakao/native").contentType(MediaType.APPLICATION_JSON)
            .content("""{"accessToken":"invalid"}"""))
            .andExpect(status().isUnauthorized).andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
    }
    "제공자 검증 실패에서는 계정 DB와 토큰 발급을 호출하지 않는다" {
        val gateway = Mockito.mock(NativeKakaoGateway::class.java)
        val flow = Mockito.mock(AuthFlowService::class.java)
        Mockito.`when`(gateway.identity("invalid")).thenThrow(UnauthenticatedException())
        code { NativeKakaoAuthService(gateway, flow).login(NativeKakaoLoginRequest("invalid")) } shouldBe "UNAUTHENTICATED"
        Mockito.verifyNoInteractions(flow)
    }
    "네이티브 경로도 기존 KAKAO 계정을 조회하고 APP 전용 JWT를 발급한다" {
        val db = Mockito.mock(GatheringStore::class.java)
        val provider = Mockito.mock(ProviderGateway::class.java)
        val jwt = JwtService("01234567890123456789012345678901", 30)
        Mockito.`when`(db.rows("SELECT id FROM users WHERE provider=:p AND provider_id=:id", "p" to "KAKAO", "id" to "42"))
            .thenReturn(listOf(mapOf("id" to 7L)))
        val flow = AuthFlowService(db, provider, jwt, "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=")
        val result = flow.completeNativeKakao(ProviderIdentity("42", "총무", null))
        jwt.identity(result.token)!!.userId shouldBe 7
        jwt.identity(result.token)!!.client shouldBe "APP"
        Mockito.verify(db).rows("SELECT id FROM users WHERE provider=:p AND provider_id=:id", "p" to "KAKAO", "id" to "42")
        Mockito.verifyNoInteractions(provider)
    }
})
