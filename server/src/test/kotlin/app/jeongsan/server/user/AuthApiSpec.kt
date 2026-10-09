package app.jeongsan.server.user

import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.common.LoginUserArgumentResolver
import app.jeongsan.server.common.UnauthenticatedException
import app.jeongsan.server.common.ApiException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

class AuthApiSpec : StringSpec({
    val flow=Mockito.mock(AuthFlowService::class.java); val service=Mockito.mock(UserService::class.java)
    val deletion=Mockito.mock(AccountDeletionService::class.java)
    val jwt=JwtService("01234567890123456789012345678901",30)
    val mvc=MockMvcBuilders.standaloneSetup(AuthController(flow,service,"https://web.test/jungsan","https://web.test",true,30),UserController(service,deletion,true))
        .setCustomArgumentResolvers(LoginUserArgumentResolver(jwt)).setControllerAdvice(GlobalExceptionHandler())
        .setMessageConverters(MappingJackson2HttpMessageConverter(jacksonObjectMapper().findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))).build()
    val web=Cookie(AuthController.COOKIE_NAME,jwt.issue(12)); val app=jwt.issue(12,"APP")
    val state="s".repeat(43)
    beforeTest { Mockito.reset(flow,service,deletion) }
    "Apple 인가의 상관 쿠키는 HTTPS form_post에 맞춰 Secure HttpOnly SameSite None이다" {
        Mockito.`when`(flow.start("APPLE","web",null,null)).thenReturn(AuthStart(state,"browser","https://appleid.apple.com/auth/authorize"))
        val response=mvc.perform(get("/api/v1/auth/apple/login")).andExpect(status().isFound).andReturn().response
        val cookie=response.getHeader("Set-Cookie")!!
        cookie.contains("Secure") shouldBe true; cookie.contains("HttpOnly") shouldBe true; cookie.contains("SameSite=None") shouldBe true
    }
    "Apple POST 콜백은 웹 쿠키를 발급하고 원래 링크로 GET 복귀한다" {
        Mockito.`when`(flow.finish("APPLE",state,"browser","code")).thenReturn(AuthFinish("web","/jungsan/j/link","signed"))
        mvc.perform(post("/api/v1/auth/apple/callback").contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("code","code").param("state",state).cookie(Cookie(AuthController.bindingName(state),"browser")))
            .andExpect(status().isSeeOther).andExpect(header().string("Location","https://web.test/jungsan/j/link"))
            .andExpect(header().string("Cache-Control","no-store"))
        Mockito.verify(flow).finish("APPLE",state,"browser","code")
    }
    "앱 콜백에는 JWT 대신 티켓만 딥링크에 담는다" {
        Mockito.`when`(flow.finish("KAKAO",state,"browser","code")).thenReturn(AuthFinish("app","/jungsan/","ticket"))
        val response=mvc.perform(get("/api/v1/auth/kakao/callback").param("code","code").param("state",state)
            .cookie(Cookie(AuthController.bindingName(state),"browser"))).andExpect(status().isSeeOther)
            .andExpect(header().string("Location","jeongsan://auth?ticket=ticket")).andReturn().response
        response.getHeaders("Set-Cookie").any { it.startsWith("jeongsan_token=") } shouldBe false
    }
    "콜백의 잘못된 state는 서비스·제공자를 호출하지 않는다" {
        mvc.perform(get("/api/v1/auth/kakao/callback").param("code","code").param("state","invalid"))
            .andExpect(status().isUnauthorized)
        Mockito.verifyNoInteractions(flow)
    }
    "공개 앱 교환은 티켓과 verifier를 전달하며 만료 시각을 반환한다" {
        val input=AppExchangeRequest("ticket","verifier")
        Mockito.`when`(flow.exchange(input)).thenReturn(AppExchangeResponse("token",Instant.parse("2026-10-11T00:00:00Z")))
        mvc.perform(post("/api/v1/auth/app/exchange").contentType(MediaType.APPLICATION_JSON)
            .content("{\"ticket\":\"ticket\",\"codeVerifier\":\"verifier\"}"))
            .andExpect(status().isOk).andExpect(jsonPath("$.token").value("token"))
            .andExpect(jsonPath("$.expiresAt").value("2026-10-11T00:00:00Z"))
    }
    "앱 Bearer 로그아웃은 204와 쿠키 만료를 반환한다" {
        mvc.perform(post("/api/v1/auth/logout").header("Authorization","Bearer $app"))
            .andExpect(status().isNoContent).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("Max-Age=0")))
        Mockito.verify(flow).logout(app)
    }
    "웹 토큰을 Bearer로 쓰거나 앱 토큰을 쿠키로 쓰면 거절한다" {
        mvc.perform(post("/api/v1/auth/logout").header("Authorization","Bearer ${web.value}")).andExpect(status().isUnauthorized)
        mvc.perform(post("/api/v1/auth/logout").cookie(Cookie(AuthController.COOKIE_NAME,app))).andExpect(status().isUnauthorized)
        Mockito.verifyNoInteractions(flow)
    }
    "탈퇴 성공과 진행 중 정산 거절이 계약대로 응답한다" {
        mvc.perform(delete("/api/v1/users/me").cookie(web)).andExpect(status().isNoContent)
        Mockito.doThrow(ApiException("ACTIVE_GATHERING_EXISTS",HttpStatus.CONFLICT,"진행 중" )).`when`(deletion).delete(12)
        mvc.perform(delete("/api/v1/users/me").cookie(web)).andExpect(status().isConflict).andExpect(jsonPath("$.code").value("ACTIVE_GATHERING_EXISTS"))
    }
    "로그인 없는 로그아웃과 탈퇴는 401이다" {
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isUnauthorized)
        mvc.perform(delete("/api/v1/users/me")).andExpect(status().isUnauthorized)
        Mockito.verifyNoInteractions(flow,deletion)
    }
    "깨진 앱 교환 JSON은 400이다" {
        mvc.perform(post("/api/v1/auth/app/exchange").contentType(MediaType.APPLICATION_JSON).content("{\"ticket\":\"x\"}"))
            .andExpect(status().isBadRequest)
        Mockito.verifyNoInteractions(flow)
    }
    "필수 콜백 query와 form 파라미터 누락은 400이다" {
        mvc.perform(get("/api/v1/auth/kakao/callback").param("code","code")).andExpect(status().isBadRequest)
        mvc.perform(post("/api/v1/auth/apple/callback").contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("state",state)).andExpect(status().isBadRequest)
        Mockito.verifyNoInteractions(flow)
    }
})
