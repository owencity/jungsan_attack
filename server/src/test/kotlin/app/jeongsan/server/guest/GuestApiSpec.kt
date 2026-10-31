package app.jeongsan.server.guest

import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.common.LoginUserArgumentResolver
import app.jeongsan.server.gathering.*
import app.jeongsan.server.user.JwtService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.module.kotlin.jacksonMapperBuilder

class GuestApiSpec : StringSpec({
    val service=Mockito.mock(GatheringService::class.java)
    val tokens=GuestTokens("01234567890123456789012345678901")
    val members=JwtService("01234567890123456789012345678901",30)
    val mvc=MockMvcBuilders.standaloneSetup(GuestController(service,tokens,"https://web.test",true),GatheringController(service))
        .setCustomArgumentResolvers(GuestBrowserResolver(tokens),LoginUserArgumentResolver(members))
        .setControllerAdvice(GlobalExceptionHandler())
        .setMessageConverters(JacksonJsonHttpMessageConverter(jacksonMapperBuilder().build())).build()
    val cookie=Cookie(GuestController.COOKIE_NAME,tokens.issue())
    val browserHash=checkNotNull(tokens.identity(cookie.value)).browserHash
    val joinPath="/api/v1/guest/join/shared-link"
    val joinBody="""{"displayName":"김하나","settlementUnitId":10,"responses":[]}"""
    val responsePath="/api/v1/guest/gatherings/1/settlement-units/10/responses/me"
    beforeTest { Mockito.reset(service) }

    "세션 초기화는 httpOnly Secure Lax 경로 제한 쿠키만 발급하고 회원을 만들지 않는다" {
        val response=mvc.perform(post("/api/v1/guest/session").header("Origin","https://web.test"))
            .andExpect(status().isNoContent).andExpect(header().string("Cache-Control","no-store")).andReturn().response
        val setCookie=checkNotNull(response.getHeader("Set-Cookie"))
        for (attribute in listOf("HttpOnly","Secure","SameSite=Lax","Path=/api/v1/guest","Max-Age=2592000")) setCookie.contains(attribute) shouldBe true
        response.contentAsString shouldBe ""
        Mockito.verifyNoInteractions(service)
    }
    "유효 세션 초기화 재시도는 쿠키를 회전하지 않고 무효 쿠키는 401이다" {
        mvc.perform(post("/api/v1/guest/session").cookie(cookie).header("Origin","https://web.test"))
            .andExpect(status().isNoContent).andExpect(header().doesNotExist("Set-Cookie"))
        mvc.perform(post("/api/v1/guest/session").cookie(Cookie(GuestController.COOKIE_NAME,"bad")).header("Origin","https://web.test"))
            .andExpect(status().isUnauthorized)
        Mockito.verifyNoInteractions(service)
    }
    "세션 발급도 누락되거나 다른 Origin을 허용하지 않는다" {
        mvc.perform(post("/api/v1/guest/session")).andExpect(status().isForbidden)
        mvc.perform(post("/api/v1/guest/session").header("Origin","https://sibling.test")).andExpect(status().isForbidden)
    }
    "비회원 참여는 브라우저 해시를 전달하며 본문에 토큰을 반환하지 않는다" {
        Mockito.`when`(service.guestJoin("shared-link",browserHash,GuestJoinRequest("김하나",10)))
            .thenReturn(mapOf("gatheringId" to 1,"participantId" to 2))
        mvc.perform(post(joinPath).cookie(cookie).header("Origin","https://web.test").contentType(MediaType.APPLICATION_JSON).content(joinBody))
            .andExpect(status().isOk).andExpect(jsonPath("$.participantId").value(2)).andExpect(jsonPath("$.token").doesNotExist())
        Mockito.verify(service).guestJoin("shared-link",browserHash,GuestJoinRequest("김하나",10))
    }
    "본인 조회와 수정은 서버 인증한 브라우저만 전달한다" {
        mvc.perform(get("/api/v1/guest/gatherings/1/me").cookie(cookie)).andExpect(status().isOk)
        Mockito.verify(service).guestMe(1,browserHash)
        mvc.perform(put(responsePath).cookie(cookie).header("Origin","https://web.test").contentType(MediaType.APPLICATION_JSON)
            .content("""{"answers":[{"roundId":7,"type":"SOBER"}]}""" )).andExpect(status().isNoContent)
        Mockito.verify(service).guestRespond(1,10,browserHash,ResponseRequest(listOf(AnswerRequest(7,"SOBER"))))
    }
    "모든 비회원 업무 API는 자격이 없으면 401이며 회원 토큰으로도 우회하지 않는다" {
        for (wrongCookie in listOf<Cookie?>(null,Cookie(GuestController.COOKIE_NAME,members.issue(42)))) {
            val requests=listOf(get("/api/v1/guest/gatherings/1/me"),post(joinPath).content(joinBody),put(responsePath).content("""{"answers":[]}"""))
            for (request in requests) {
                wrongCookie?.let{request.cookie(it)}
                mvc.perform(request.header("Origin","https://web.test").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized)
            }
        }
        Mockito.verifyNoInteractions(service)
    }
    "출처 위조와 깨진 JSON은 비회원 서비스를 실행하지 않는다" {
        mvc.perform(post(joinPath).cookie(cookie).header("Origin","https://evil.test").contentType(MediaType.APPLICATION_JSON).content(joinBody))
            .andExpect(status().isForbidden)
        mvc.perform(put(responsePath).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("""{"answers":[]}"""))
            .andExpect(status().isForbidden)
        mvc.perform(post(joinPath).cookie(cookie).header("Origin","https://web.test").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest)
        Mockito.verifyNoInteractions(service)
    }
    "비회원 쿠키를 회원 쿠키로 바꿔도 총무 API에 진입하지 않는다" {
        mvc.perform(post("/api/v1/gatherings").cookie(Cookie("jeongsan_token",cookie.value)))
            .andExpect(status().isUnauthorized)
        Mockito.verifyNoInteractions(service)
    }
})
