package app.jeongsan.server.config

import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.common.LoginUserArgumentResolver
import app.jeongsan.server.user.AuthController
import app.jeongsan.server.user.AuthFinish
import app.jeongsan.server.user.AuthFlowService
import app.jeongsan.server.user.AuthStart
import app.jeongsan.server.user.JwtService
import app.jeongsan.server.user.UserService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockServletContext
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc

class AppleCallbackCorsSpec : StringSpec({
    // standaloneSetup은 운영 WebConfig의 CORS 처리를 실행하지 않아 이 장애를 놓친다.
    val context = AnnotationConfigWebApplicationContext().apply {
        servletContext = MockServletContext()
        environment = MockEnvironment().withProperty("app.frontend-origin", "https://web.test")
        register(CorsConfiguration::class.java)
        refresh()
    }
    afterSpec { context.close() }
    val mvc = MockMvcBuilders.webAppContextSetup(context).build()
    val flow = context.getBean(AuthFlowService::class.java)
    val callbackPath = "/api/v1/auth/apple/callback"
    val appleOrigin = "https://appleid.apple.com"
    val state = "s".repeat(43)
    beforeTest { Mockito.reset(flow) }

    listOf("web", "app").forEach { client ->
        "Apple $client POST는 발급한 상태 쿠키를 전달하고 기존 303 콜백을 실행한다" {
            val challenge = if (client == "app") "a".repeat(43) else null
            Mockito.`when`(flow.start("APPLE", client, null, challenge))
                .thenReturn(AuthStart(state, "browser-binding", "https://appleid.apple.com/auth/authorize"))
            val loginRequest = get("/api/v1/auth/apple/login").param("client", client)
            if (challenge != null) loginRequest.param("codeChallenge", challenge)
            val loginResponse = mvc.perform(loginRequest).andExpect(status().isFound).andReturn().response
            val bindingCookie = checkNotNull(loginResponse.getCookie(AuthController.bindingName(state)))
            val cookieHeader = checkNotNull(loginResponse.getHeader("Set-Cookie"))
            listOf("SameSite=None", "Secure", "HttpOnly", "Path=/api/v1/auth", "Max-Age=300")
                .forEach { attribute -> cookieHeader.contains(attribute) shouldBe true }
            bindingCookie.value shouldBe "browser-binding"
            Mockito.`when`(flow.finish("APPLE", state, bindingCookie.value, "code"))
                .thenReturn(AuthFinish(client, "/jungsan/", "credential"))
            val target = if (client == "app") "jeongsan://auth?ticket=credential" else "https://web.test/jungsan"
            val response = mvc.perform(post(callbackPath).header("Origin", appleOrigin)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("code", "code").param("state", state)
                .cookie(Cookie(bindingCookie.name, bindingCookie.value)))
                .andExpect(status().isSeeOther).andExpect(header().string("Location", target))
                .andExpect(header().string("Access-Control-Allow-Origin", appleOrigin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andReturn().response
            response.getHeaders("Set-Cookie").any {
                it.startsWith(bindingCookie.name + "=") && it.contains("Max-Age=0") && it.contains("SameSite=None") && it.contains("Secure")
            } shouldBe true
            Mockito.verify(flow).finish("APPLE", state, "browser-binding", "code")
        }
    }
    "운영 재현의 잘못된 state는 CORS 403 대신 기존 401을 반환한다" {
        mvc.perform(post(callbackPath).header("Origin", appleOrigin)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("code", "x").param("state", "y"))
            .andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
            .andExpect(header().string("Access-Control-Allow-Origin", appleOrigin))
        Mockito.verifyNoInteractions(flow)
    }
    "Apple 출처는 다른 API와 카카오 콜백에 접근할 수 없다" {
        listOf("/api/v1/auth/me", "/api/v1/auth/kakao/callback").forEach { path ->
            mvc.perform(get(path).header("Origin", appleOrigin)).andExpect(status().isForbidden)
                .andExpect(content().string("Invalid CORS request"))
        }
        mvc.perform(post("/api/v1/auth/app/exchange").header("Origin", appleOrigin)
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden).andExpect(content().string("Invalid CORS request"))
        Mockito.verifyNoInteractions(flow)
    }
    "다른 출처와 유사 Apple 도메인은 콜백에서도 거절한다" {
        listOf("https://evil.test", "https://appleid.apple.com.evil.test", "http://appleid.apple.com", "https://web.test")
            .forEach { origin ->
                mvc.perform(post(callbackPath).header("Origin", origin)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("code", "x").param("state", "y"))
                    .andExpect(status().isForbidden).andExpect(content().string("Invalid CORS request"))
            }
        Mockito.verifyNoInteractions(flow)
    }
    "콜백의 Apple POST preflight만 허용하며 GET과 다른 API preflight는 거절한다" {
        mvc.perform(options(callbackPath).header("Origin", appleOrigin).header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk).andExpect(header().string("Access-Control-Allow-Methods", "POST"))
        mvc.perform(options(callbackPath).header("Origin", appleOrigin).header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden)
        mvc.perform(options("/api/v1/auth/me").header("Origin", appleOrigin).header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden)
    }
    "기존 프론트의 다른 API CORS와 인증 401은 유지한다" {
        mvc.perform(get("/api/v1/auth/me").header("Origin", "https://web.test"))
            .andExpect(status().isUnauthorized).andExpect(header().string("Access-Control-Allow-Origin", "https://web.test"))
        Mockito.verifyNoInteractions(flow)
    }
}) {
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(WebConfig::class)
    class CorsConfiguration {
        @Bean fun flow(): AuthFlowService = Mockito.mock(AuthFlowService::class.java)
        @Bean fun userService(): UserService = Mockito.mock(UserService::class.java)
        @Bean fun authController(flow: AuthFlowService, userService: UserService) =
            AuthController(flow, userService, "https://web.test/jungsan", "https://web.test", true, 30)
        @Bean fun loginUserArgumentResolver() = LoginUserArgumentResolver(JwtService("01234567890123456789012345678901", 30))
        @Bean fun exceptionHandler() = GlobalExceptionHandler()
    }
}
