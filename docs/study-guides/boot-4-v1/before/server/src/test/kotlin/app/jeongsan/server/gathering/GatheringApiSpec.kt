package app.jeongsan.server.gathering

import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.common.LoginUserArgumentResolver
import app.jeongsan.server.user.JwtService
import app.jeongsan.server.user.AuthController
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import org.mockito.Mockito
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class GatheringApiSpec : StringSpec({
    val service=Mockito.mock(GatheringService::class.java)
    val jwt=JwtService("01234567890123456789012345678901",30)
    val mapper=jacksonObjectMapper().findAndRegisterModules()
    val mvc=MockMvcBuilders.standaloneSetup(GatheringController(service),SettlementUnitController(service),JoinController(service),
        MyGatheringsController(service),PayoutController(service),TransferController(service))
        .setCustomArgumentResolvers(LoginUserArgumentResolver(jwt)).setControllerAdvice(GlobalExceptionHandler())
        .setMessageConverters(MappingJackson2HttpMessageConverter(mapper)).build()
    val cookie=Cookie(AuthController.COOKIE_NAME,jwt.issue(12))
    val root="/api/v1/gatherings/1/settlement-units/10"
    data class Case(val verb: String,val url: String,val body: String?=null,val code: Int=200)
    val round="""{"total":10000,"payerParticipantId":1,"drinks":[]}"""
    val answers="""{"answers":[{"roundId":1,"type":"SOBER"}]}"""
    val cases=listOf(
        Case("GET","/api/v1/gatherings"),Case("POST","/api/v1/gatherings",code=201),Case("GET","/api/v1/gatherings/1"),
        Case("PATCH","/api/v1/gatherings/1","""{"title":"테스트 술자리"}"""),
        Case("POST","/api/v1/gatherings/1/messages","""{"text":"안녕하세요"}""",201),
        Case("GET","/api/v1/me/gatherings"),Case("GET","/api/v1/me/notifications"),
        Case("POST","/api/v1/me/notifications/1/read",code=204),Case("POST","/api/v1/me/notifications/read-all",code=204),
        Case("PUT","/api/v1/users/me/payout","""{"bank":"카카오뱅크","accountNo":"1234567890","holder":"김하나"}"""),
        Case("POST","/api/v1/transfers/1/sent",code=204),Case("POST","/api/v1/transfers/1/confirm",code=204),Case("POST","/api/v1/transfers/1/not-received",code=204),
        Case("POST","/api/v1/gatherings/1/settlement-units","""{"requestId":"00000000-0000-0000-0000-000000000001","participantIds":[1,2]}""",201),
        Case("PUT",root+"/participants/2",code=204),Case("DELETE",root+"/participants/2",code=204),
        Case("PUT",root+"/headcount","""{"headcount":3}""",204),
        Case("POST",root+"/rounds",round,201),Case("PUT",root+"/rounds/1",round),Case("DELETE",root+"/rounds/1",code=204),
        Case("PUT",root+"/responses/me",answers,204),Case("PUT",root+"/participants/2/responses",answers,204),
        Case("GET",root+"/settlement/preview"),Case("POST",root+"/settlement","""{"inputRevision":1,"inputHash":"${"a".repeat(64)}"}"""),
        Case("DELETE",root+"/settlement",code=204),Case("POST",root+"/settlement/viewed",code=204),Case("POST",root+"/complete",code=204),
        Case("POST","/api/v1/join/abcdefghijkl","""{"settlementUnitId":10,"responses":[]}"""))
    beforeTest { Mockito.reset(service) }
    cases.forEach { c ->
        "${c.verb} ${c.url}는 로그인 후 계약 HTTP 상태로 서비스를 호출한다" {
            val req=request(HttpMethod.valueOf(c.verb),c.url).cookie(cookie).contentType(MediaType.APPLICATION_JSON)
            c.body?.let{req.content(it)}
            mvc.perform(req).andExpect(status().`is`(c.code))
            Mockito.mockingDetails(service).invocations.size shouldBe 1
        }
        "${c.verb} ${c.url}는 로그인 없으면 서비스를 호출하지 않는다" {
            val req=request(HttpMethod.valueOf(c.verb),c.url).contentType(MediaType.APPLICATION_JSON)
            c.body?.let{req.content(it)}
            mvc.perform(req).andExpect(status().isUnauthorized)
            Mockito.verifyNoInteractions(service)
        }
    }
    "공개 링크 미리보기는 인증 없이 호출한다" {
        Mockito.`when`(service.joinPreview("abcdefghijkl")).thenReturn(mapOf("title" to "술자리","participantCount" to 2))
        mvc.perform(request(HttpMethod.GET,"/api/v1/join/abcdefghijkl")).andExpect(status().isOk).andExpect(jsonPath("$.title").value("술자리"))
        Mockito.verify(service).joinPreview("abcdefghijkl")
    }
    "금액 0과 깨진 JSON은 400이며 서비스에 진입하지 않는다" {
        for(body in listOf("""{"total":0,"payerParticipantId":1}""","{")) {
            mvc.perform(request(HttpMethod.POST,root+"/rounds").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest)
        }
        Mockito.verifyNoInteractions(service)
    }
    "인원은 2~50이며 본문 없는 술자리 생성도 유지한다" {
        for (body in listOf("""{"headcount":1}""", """{"headcount":51}""", "{}")) {
            mvc.perform(request(HttpMethod.PUT,root+"/headcount").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest)
        }
        Mockito.verifyNoInteractions(service)
        mvc.perform(request(HttpMethod.POST,"/api/v1/gatherings").cookie(cookie)).andExpect(status().isCreated)
        Mockito.verify(service).create(12,null)
    }
})
