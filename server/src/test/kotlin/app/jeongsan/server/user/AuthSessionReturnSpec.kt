package app.jeongsan.server.user

import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.common.MalformedRequestException
import app.jeongsan.server.common.UnauthenticatedException
import app.jeongsan.server.user.javaimpl.AuthPolicy as JavaPolicy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AuthSessionReturnSpec : StringSpec({
    val ticket = "t".repeat(43)
    val state = "s".repeat(43)
    val challenge = "c".repeat(43)
    val flow = Mockito.mock(AuthFlowService::class.java)
    val mvc = MockMvcBuilders.standaloneSetup(AuthController(flow, Mockito.mock(UserService::class.java),
        "https://web.test/jungsan", "https://web.test", true, 30))
        .setControllerAdvice(GlobalExceptionHandler()).build()
    beforeTest { Mockito.reset(flow) }

    "기존page가 기본이며session은app만 선택한다" {
        listOf(Triple("app", null, "page"), Triple("web", null, "page"),
            Triple("app", "page", "page"), Triple("web", "page", "page"), Triple("app", "session", "session"))
            .forEach { (client, input, expected) ->
                AuthPolicy.appReturnMode(client, input) shouldBe expected
                JavaPolicy.appReturnMode(client, input) shouldBe expected
            }
    }
    "잘못된 모드와 웹 session 조합은 양쪽 언어에서400 정책으로 거절한다" {
        listOf("", "SESSION", "https://evil.test", "session\n").forEach { input ->
            shouldThrow<MalformedRequestException> { AuthPolicy.appReturnMode("app", input) }
            shouldThrow<MalformedRequestException> { JavaPolicy.appReturnMode("app", input) }
        }
        shouldThrow<MalformedRequestException> { AuthPolicy.appReturnMode("web", "session") }
        shouldThrow<MalformedRequestException> { JavaPolicy.appReturnMode("web", "session") }
    }
    "session 복귀는 고정 스킴과43자 티켓만 허용한다" {
        AuthPolicy.appSessionReturnLink(ticket) shouldBe "jeongsan://auth?ticket=$ticket"
        JavaPolicy.appSessionReturnLink(ticket) shouldBe "jeongsan://auth?ticket=$ticket"
        listOf("", "x".repeat(42), "x".repeat(44), "x\r\nLocation:https://evil.test", "x&ticket=evil").forEach { input ->
            shouldThrow<UnauthenticatedException> { AuthPolicy.appSessionReturnLink(input) }
            shouldThrow<UnauthenticatedException> { JavaPolicy.appSessionReturnLink(input) }
        }
    }
    listOf("KAKAO", "APPLE").forEach { provider ->
        "$provider 로그인 시작은 선택한 session을 서비스에 전달한다" {
            Mockito.`when`(flow.start(provider, "app", null, challenge, "session"))
                .thenReturn(AuthStart(state, "browser", "https://provider.test/authorize"))
            mvc.perform(get("/api/v1/auth/${provider.lowercase()}/login").param("client", "app")
                .param("codeChallenge", challenge).param("appReturn", "session"))
                .andExpect(status().isFound).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Location", "https://provider.test/authorize"))
            Mockito.verify(flow).start(provider, "app", null, challenge, "session")
        }
        "$provider 인증 세션은 저장된 선택으로303을 반환하고 query와UA로 바꾸지 않는다" {
            Mockito.`when`(flow.finish(provider, state, "browser", "code"))
                .thenReturn(AuthFinish("app", "/jungsan/j/ignored", ticket, "session"))
            val request = if (provider == "APPLE") post("/api/v1/auth/apple/callback")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED) else get("/api/v1/auth/kakao/callback")
            val response = mvc.perform(request.param("code", "code").param("state", state).param("appReturn", "page")
                .header("User-Agent", "Android").cookie(Cookie(AuthController.bindingName(state), "browser")))
                .andExpect(status().isSeeOther).andExpect(header().string("Location", "jeongsan://auth?ticket=$ticket"))
                .andExpect(header().string("Cache-Control", "no-store")).andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(content().string("")).andReturn().response
            response.getHeaders("Set-Cookie").any { it.startsWith("jeongsan_token=") } shouldBe false
            response.getHeaders("Set-Cookie").any { it.contains("Max-Age=0") && it.contains("Secure") &&
                it.contains("HttpOnly") && it.contains("SameSite=${if (provider == "APPLE") "None" else "Lax"}") } shouldBe true
        }
    }
    "콜백 query에session을 붙여도 기존page 응답은303으로 바뀌지 않는다" {
        Mockito.`when`(flow.finish("KAKAO", state, "browser", "code"))
            .thenReturn(AuthFinish("app", "/jungsan/", ticket))
        mvc.perform(get("/api/v1/auth/kakao/callback").param("code", "code").param("state", state)
            .param("appReturn", "session").cookie(Cookie(AuthController.bindingName(state), "browser")))
            .andExpect(status().isOk).andExpect(header().doesNotExist("Location"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
    }
})
