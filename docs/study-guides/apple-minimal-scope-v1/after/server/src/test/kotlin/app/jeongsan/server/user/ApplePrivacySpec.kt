package app.jeongsan.server.user

import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.gathering.GatheringStore
import app.jeongsan.server.user.javaimpl.AuthPolicy as JavaPolicy
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets
import java.util.Base64

class ApplePrivacySpec : StringSpec({
    fun decodedParameters(authorizationUrl: String) = UriComponentsBuilder.fromUriString(authorizationUrl).build()
        .queryParams.toSingleValueMap().mapValues { (_, parameterValue) -> UriUtils.decode(parameterValue, StandardCharsets.UTF_8) }

    fun configuredGateway() = RestProviderGateway(
        "kakao-client", "kakao-secret", "https://api.test/api/v1/auth/kakao/callback",
        "apple-service", "apple-team", "apple-key", "unused-local-key", "https://api.test/api/v1/auth/apple/callback",
        MockEnvironment().apply { setActiveProfiles("local") },
    )

    "Apple 전용 인가 인자는 양쪽 언어에서 이름·이메일 scope 없이 nonce와 POST 응답 방식만 포함한다" {
        val expectedParameters = mapOf("response_mode" to "form_post", "nonce" to "test-nonce")
        AuthPolicy.appleAuthorizationParameters("test-nonce") shouldBe expectedParameters
        JavaPolicy.appleAuthorizationParameters("test-nonce") shouldBe expectedParameters
    }

    "실제 Apple 인가 URL은 scope 없이 client·callback·state·nonce·code·form_post를 보존한다" {
        val authorizationUri = UriComponentsBuilder.fromUriString(configuredGateway().authorize("APPLE", "test-state", "test-nonce")).build()
        authorizationUri.host shouldBe "appleid.apple.com"
        authorizationUri.path shouldBe "/auth/authorize"
        decodedParameters(authorizationUri.toUriString()) shouldBe mapOf(
            "client_id" to "apple-service", "redirect_uri" to "https://api.test/api/v1/auth/apple/callback",
            "response_type" to "code", "state" to "test-state", "response_mode" to "form_post", "nonce" to "test-nonce",
        )
    }

    "카카오 인가 URL에는 Apple 전용 인자나 추가 scope가 섞이지 않는다" {
        val authorizationUri = UriComponentsBuilder.fromUriString(configuredGateway().authorize("KAKAO", "test-state", "unused-nonce")).build()
        authorizationUri.host shouldBe "kauth.kakao.com"
        decodedParameters(authorizationUri.toUriString()) shouldBe mapOf(
            "client_id" to "kakao-client", "redirect_uri" to "https://api.test/api/v1/auth/kakao/callback",
            "response_type" to "code", "state" to "test-state",
        )
    }

    listOf("web", "app").forEach { clientType ->
        "Apple $clientType 로그인은 scope 없이 리다이렉트하며 안전한 상관 쿠키를 유지한다" {
            val database = Mockito.mock(GatheringStore::class.java)
            val flow = AuthFlowService(database, configuredGateway(), JwtService("01234567890123456789012345678901", 30),
                Base64.getEncoder().encodeToString(ByteArray(32)))
            val controller = AuthController(flow, Mockito.mock(UserService::class.java), "https://web.test/jungsan", "https://web.test", true, 30)
            val mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(GlobalExceptionHandler()).build()
            val loginRequest = get("/api/v1/auth/apple/login").param("client", clientType)
            if (clientType == "app") loginRequest.param("codeChallenge", "a".repeat(43))
            val response = mvc.perform(loginRequest).andExpect(status().isFound)
                .andExpect(header().string("Cache-Control", "no-store")).andReturn().response
            val parameters = UriComponentsBuilder.fromUriString(checkNotNull(response.getHeader("Location"))).build().queryParams
            parameters.containsKey("scope") shouldBe false
            checkNotNull(parameters.getFirst("state")).length shouldBe 43
            checkNotNull(parameters.getFirst("nonce")).length shouldBe 43
            parameters.getFirst("response_mode") shouldBe "form_post"
            val bindingCookie = checkNotNull(response.getHeader("Set-Cookie"))
            listOf("Secure", "HttpOnly", "SameSite=None").forEach { attribute -> bindingCookie.contains(attribute) shouldBe true }
        }
    }
})
