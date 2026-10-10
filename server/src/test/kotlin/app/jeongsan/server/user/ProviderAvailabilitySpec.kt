package app.jeongsan.server.user

import app.jeongsan.server.common.ApiException
import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.gathering.GatheringStore
import app.jeongsan.server.user.javaimpl.AuthPolicy as JavaPolicy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpStatus
import org.springframework.mock.env.MockEnvironment
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class ProviderAvailabilitySpec : StringSpec({
    val kakaoSettings = listOf("kakao-client", "kakao-secret", "https://api.test/kakao/callback")
    val appleSettings = listOf("apple-service", "apple-team", "apple-key", "private-key", "https://api.test/apple/callback")
    fun gateway(kakao: List<String> = kakaoSettings, apple: List<String> = appleSettings) = RestProviderGateway(
        kakao[0], kakao[1], kakao[2], apple[0], apple[1], apple[2], apple[3], apple[4],
        MockEnvironment().apply { setActiveProfiles("prod") },
    )

    "Apple 필수 설정 하나라도 비면 양쪽 언어에서 Apple만 닫는다" {
        appleSettings.indices.forEach { index ->
            listOf("", " ", "\u00a0").forEach { blank ->
                val incomplete = appleSettings.toMutableList().apply { this[index] = blank }
                AuthPolicy.providerAvailable("APPLE", kakaoSettings, incomplete) shouldBe false
                JavaPolicy.providerAvailable("APPLE", kakaoSettings, incomplete) shouldBe false
                AuthPolicy.providerAvailable("KAKAO", kakaoSettings, incomplete) shouldBe true
                JavaPolicy.providerAvailable("KAKAO", kakaoSettings, incomplete) shouldBe true
            }
        }
        AuthPolicy.providerAvailable("APPLE", kakaoSettings, appleSettings) shouldBe true
        JavaPolicy.providerAvailable("APPLE", kakaoSettings, appleSettings) shouldBe true
    }

    "빈 설정 목록이나 알 수 없는 제공자를 사용 가능으로 보지 않는다" {
        listOf("KAKAO", "APPLE", "UNKNOWN").forEach { provider ->
            AuthPolicy.providerAvailable(provider, emptyList(), emptyList()) shouldBe false
            JavaPolicy.providerAvailable(provider, emptyList(), emptyList()) shouldBe false
        }
    }

    "운영에서 Apple 키가 비어도 카카오 인가 주소를 만들고 Apple 진입·교환·해제는 503이다" {
        val adapter = gateway(apple = appleSettings.toMutableList().apply { this[2] = ""; this[3] = "" })
        adapter.authorize("KAKAO", "state", "nonce").startsWith("https://kauth.kakao.com/oauth/authorize?") shouldBe true
        listOf<() -> Any?>(
            { adapter.authorize("APPLE", "state", "nonce") },
            { adapter.identity("APPLE", "code", "nonce") },
            { adapter.revokeApple("apple-service", "token") },
        ).forEach { operation ->
            val failure = shouldThrow<ApiException> { operation() }
            failure.code shouldBe "AUTH_PROVIDER_UNAVAILABLE"
            failure.status shouldBe HttpStatus.SERVICE_UNAVAILABLE
        }
    }

    "운영 카카오 설정 누락은 Apple 보류와 관계없이 기동을 막는다" {
        kakaoSettings.indices.forEach { index ->
            val incomplete = kakaoSettings.toMutableList().apply { this[index] = "" }
            AuthPolicy.providerAvailable("KAKAO", incomplete, appleSettings) shouldBe false
            JavaPolicy.providerAvailable("KAKAO", incomplete, appleSettings) shouldBe false
            shouldThrow<IllegalArgumentException> { gateway(incomplete, List(5) { "" }) }
        }
    }

    "모든 Apple 설정을 채웠지만 개인키가 잘못됐으면 운영 기동 시 거절한다" {
        shouldThrow<IllegalArgumentException> { gateway() }
    }

    "유효한 로그인용 P256 키를 모두 채우면 운영 Apple 인가를 다시 연다" {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val configured = appleSettings.toMutableList().apply { this[3] = Base64.getEncoder().encodeToString(keyPair.private.encoded) }
        gateway(apple = configured).authorize("APPLE", "state", "nonce").startsWith("https://appleid.apple.com/auth/authorize?") shouldBe true
    }

    "실제 인증 흐름의 Apple 진입은 503 JSON이며 DB에 로그인 시도를 만들지 않는다" {
        val database = Mockito.mock(GatheringStore::class.java)
        val flow = AuthFlowService(database, gateway(apple = List(5) { "" }),
            JwtService("01234567890123456789012345678901", 30), Base64.getEncoder().encodeToString(ByteArray(32)))
        val mvc = MockMvcBuilders.standaloneSetup(AuthController(flow, Mockito.mock(UserService::class.java),
            "https://web.test/jungsan", "https://web.test", true, 30)).setControllerAdvice(GlobalExceptionHandler()).build()
        mvc.perform(get("/api/v1/auth/apple/login")).andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.code").value("AUTH_PROVIDER_UNAVAILABLE"))
            .andExpect(header().doesNotExist("Location")).andExpect(header().doesNotExist("Set-Cookie"))
        Mockito.verifyNoInteractions(database)
    }

    "실제 인증 흐름은 Apple 미설정과 무관하게 카카오 state와 상관 쿠키를 발급한다" {
        val database = Mockito.mock(GatheringStore::class.java)
        val flow = AuthFlowService(database, gateway(apple = List(5) { "" }),
            JwtService("01234567890123456789012345678901", 30), Base64.getEncoder().encodeToString(ByteArray(32)))
        val mvc = MockMvcBuilders.standaloneSetup(AuthController(flow, Mockito.mock(UserService::class.java),
            "https://web.test/jungsan", "https://web.test", true, 30)).setControllerAdvice(GlobalExceptionHandler()).build()
        mvc.perform(get("/api/v1/auth/kakao/login")).andExpect(status().isFound)
            .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://kauth.kakao.com/oauth/authorize?")))
            .andExpect(header().exists("Set-Cookie"))
    }
})
