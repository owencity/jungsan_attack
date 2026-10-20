package app.jeongsan.server.moderation

import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.common.LoginUserArgumentResolver
import app.jeongsan.server.user.AuthController
import app.jeongsan.server.user.JwtService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import org.mockito.Mockito
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.Instant

class ModerationApiSpec : StringSpec({
    val service = Mockito.mock(ModerationService::class.java)
    val jwt = JwtService("01234567890123456789012345678901", 30)
    val mvc = MockMvcBuilders.standaloneSetup(ModerationController(service))
        .setCustomArgumentResolvers(LoginUserArgumentResolver(jwt)).setControllerAdvice(GlobalExceptionHandler())
        .setMessageConverters(JacksonJsonHttpMessageConverter(jacksonMapperBuilder().build())).build()
    val cookie = Cookie(AuthController.COOKIE_NAME, jwt.issue(12))
    data class Case(val method: String, val path: String, val body: String? = null, val expected: Int = 204)
    val cases = listOf(Case("POST", "/api/v1/gatherings/1/messages/2/reports", """{"reason":"ABUSE"}"""),
        Case("POST", "/api/v1/me/blocks", """{"gatheringId":1,"participantId":3}"""),
        Case("GET", "/api/v1/me/blocks", expected = 200), Case("DELETE", "/api/v1/me/blocks/3"))
    beforeTest { Mockito.reset(service) }
    for (case in cases) {
        "${case.method} ${case.path}: 인증 후 계약 상태로 한 번만 호출한다" {
            val builder = request(HttpMethod.valueOf(case.method), case.path).cookie(cookie).contentType(MediaType.APPLICATION_JSON)
            case.body?.let { builder.content(it) }
            mvc.perform(builder).andExpect(status().`is`(case.expected))
            Mockito.mockingDetails(service).invocations.size shouldBe 1
        }
        "${case.method} ${case.path}: 인증 없으면 서비스 진입 전 401" {
            val builder = request(HttpMethod.valueOf(case.method), case.path).contentType(MediaType.APPLICATION_JSON)
            case.body?.let { builder.content(it) }
            mvc.perform(builder).andExpect(status().isUnauthorized)
            Mockito.verifyNoInteractions(service)
        }
    }
    "사유·양수 ID·JSON 검증 실패는 저장하지 않는다" {
        for (case in listOf(Case("POST", cases[0].path, """{"reason":"UNKNOWN"}"""),
            Case("POST", cases[0].path, """{}"""), Case("POST", cases[0].path, "{"),
            Case("POST", "/api/v1/me/blocks", """{"gatheringId":0,"participantId":3}"""),
            Case("POST", "/api/v1/me/blocks", """{"gatheringId":1,"participantId":-1}"""))) {
            mvc.perform(request(HttpMethod.POST, case.path).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(case.body!!))
                .andExpect(status().isBadRequest)
        }
        Mockito.verifyNoInteractions(service)
    }
    "차단 목록은 문서의 userId displayName blockedAt을 반환한다" {
        Mockito.`when`(service.blocks(12)).thenReturn(listOf(UserBlockResponse(3, "김하나", Instant.parse("2026-10-20T00:00:00Z"))))
        mvc.perform(request(HttpMethod.GET, "/api/v1/me/blocks").cookie(cookie)).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].userId").value(3)).andExpect(jsonPath("$[0].displayName").value("김하나"))
            .andExpect(jsonPath("$[0].blockedAt").value("2026-10-20T00:00:00Z"))
    }
})
