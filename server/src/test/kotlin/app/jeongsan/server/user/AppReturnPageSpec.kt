package app.jeongsan.server.user

import app.jeongsan.server.common.GlobalExceptionHandler
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
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

class AppReturnPageSpec : StringSpec({
    val ticket = "t".repeat(43)
    val state = "s".repeat(43)
    val flow = Mockito.mock(AuthFlowService::class.java)
    val mvc = MockMvcBuilders.standaloneSetup(AuthController(flow, Mockito.mock(UserService::class.java),
        "https://web.test/jungsan", "https://web.test", true, 30)).setControllerAdvice(GlobalExceptionHandler()).build()
    beforeTest { Mockito.reset(flow) }

    "Android intent와 다른 환경의 스킴은 정확한 기대값과 양쪽 언어 결과가 일치한다" {
        listOf("Mozilla Android" to "intent://auth?ticket=$ticket#Intent;scheme=jeongsan;package=app.jeongsan;end",
            "android" to "intent://auth?ticket=$ticket#Intent;scheme=jeongsan;package=app.jeongsan;end",
            "Mozilla iPhone" to "jeongsan://auth?ticket=$ticket", null to "jeongsan://auth?ticket=$ticket").forEach { (agent, expected) ->
            AuthPolicy.appReturnLink(ticket, agent) shouldBe expected
            JavaPolicy.appReturnLink(ticket, agent) shouldBe expected
        }
    }
    "HTML·링크에 주입할 수 있는 티켓은 양쪽 구현에서 거절한다" {
        listOf("", "x".repeat(42), "x".repeat(44), "\"<script>alert(1)</script>", "x&package=evil", "x\n").forEach { invalid ->
            shouldThrow<UnauthenticatedException> { AuthPolicy.appReturnLink(invalid, "Android") }
            shouldThrow<UnauthenticatedException> { JavaPolicy.appReturnLink(invalid, "Android") }
        }
    }
    "티켓은 양쪽 언어에서 정확히 5분이며 만료 시각부터 거절한다" {
        val now = Instant.parse("2026-10-22T00:00:00Z")
        val expiry = now.plusSeconds(300)
        AuthPolicy.appTicketExpiresAt(now) shouldBe expiry
        JavaPolicy.appTicketExpiresAt(now) shouldBe expiry
        AuthPolicy.validUntil(expiry, expiry.minusNanos(1))
        JavaPolicy.validUntil(expiry, expiry.minusNanos(1))
        shouldThrow<UnauthenticatedException> { AuthPolicy.validUntil(expiry, expiry) }
        shouldThrow<UnauthenticatedException> { JavaPolicy.validUntil(expiry, expiry) }
    }
    listOf("KAKAO", "APPLE").forEach { provider ->
        listOf("Android", "iPhone").forEach { agent ->
            "$provider 앱 $agent 콜백은 안전한 200 HTML과 수동 버튼을 반환한다" {
                Mockito.`when`(flow.finish(provider, state, "browser", "code"))
                    .thenReturn(AuthFinish("app", "/jungsan/", ticket))
                val request = if (provider == "APPLE") post("/api/v1/auth/apple/callback")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED) else get("/api/v1/auth/kakao/callback")
                val response = mvc.perform(request.param("code", "code").param("state", state)
                    .header("User-Agent", agent).cookie(Cookie(AuthController.bindingName(state), "browser")))
                    .andExpect(status().isOk).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(header().doesNotExist("Location")).andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(header().string("Referrer-Policy", "no-referrer"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff")).andReturn().response
                val html = response.contentAsString
                html.contains("href=\"${AuthPolicy.appReturnLink(ticket, agent)}\"") shouldBe true
                html.contains("정산어택 앱으로 돌아가기") shouldBe true
                html.contains("5분") shouldBe true
                html.contains("https://") shouldBe false
                html.contains("<img") shouldBe false
                html.contains("<script src") shouldBe false
                html.contains("code=code") shouldBe false
                html.contains(state) shouldBe false
                response.getHeaders("Set-Cookie").any { it.startsWith("jeongsan_token=") } shouldBe false
                response.getHeaders("Set-Cookie").any { it.contains("Max-Age=0") && it.contains("SameSite=${if (provider == "APPLE") "None" else "Lax"}") } shouldBe true
                val scriptBody = checkNotNull(Regex("<script>([\\s\\S]*?)</script>").find(html)).groupValues[1]
                val scriptHash = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(scriptBody.toByteArray()))
                response.getHeader("Content-Security-Policy") shouldBe
                    "default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'; style-src 'unsafe-inline'; script-src 'sha256-$scriptHash'"
            }
        }
    }
    "User-Agent는 HTML에 반영하지 않고 수동 링크만 선택한다" {
        val html = AppReturnPage.html(ticket, "Android\"><img src=https://evil.test>")
        html.contains("evil.test") shouldBe false
        html.contains("intent://auth?ticket=") shouldBe true
    }
    "Apple 완료 페이지를 새로고침해 소모된 state를 보내면 두 번째 티켓 대신 기존401이다" {
        Mockito.`when`(flow.finish("APPLE", state, "browser", "code"))
            .thenReturn(AuthFinish("app", "/jungsan/", ticket)).thenThrow(UnauthenticatedException())
        fun callback() = post("/api/v1/auth/apple/callback").contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("code", "code").param("state", state).cookie(Cookie(AuthController.bindingName(state), "browser"))
        mvc.perform(callback()).andExpect(status().isOk)
        val replay = mvc.perform(callback()).andExpect(status().isUnauthorized).andReturn().response
        replay.contentAsString.contains(ticket) shouldBe false
    }
    "웹 콜백은 계속303이며 웹 쿠키와 원래 복귀 경로를 유지한다" {
        Mockito.`when`(flow.finish("KAKAO", state, "browser", "code"))
            .thenReturn(AuthFinish("web", "/jungsan/j/link", "web-jwt"))
        val response = mvc.perform(get("/api/v1/auth/kakao/callback").param("state", state).param("code", "code")
            .cookie(Cookie(AuthController.bindingName(state), "browser")))
            .andExpect(status().isSeeOther).andExpect(header().string("Location", "https://web.test/jungsan/j/link"))
            .andReturn().response
        response.contentAsString shouldBe ""
        response.getHeaders("Set-Cookie").any { it.startsWith("jeongsan_token=web-jwt") } shouldBe true
    }
})
