package app.jeongsan.server.gathering

import app.jeongsan.server.common.ApiException
import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.common.LoginUserArgumentResolver
import app.jeongsan.server.moderation.ModerationService
import app.jeongsan.server.user.JwtService
import app.jeongsan.server.gathering.javaimpl.SettlementWorkflow as JavaWorkflow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mockito.Answers
import org.mockito.Mockito
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.module.kotlin.jacksonMapperBuilder

class RoundRevisionSpec : StringSpec({
    val mapper = jacksonMapperBuilder().build()
    val jwt = JwtService("01234567890123456789012345678901", 30)
    val token = jwt.issue(12, "APP")
    val service = Mockito.mock(GatheringService::class.java)
    val mvc = MockMvcBuilders.standaloneSetup(SettlementUnitController(service))
        .setCustomArgumentResolvers(LoginUserArgumentResolver(jwt)).setControllerAdvice(GlobalExceptionHandler())
        .setMessageConverters(JacksonJsonHttpMessageConverter(mapper)).build()
    val path = "/api/v1/gatherings/1/settlement-units/10/rounds/1"
    beforeTest { Mockito.reset(service) }

    "구버전의 누락과 명시 null은 유지하고 정수 버전만 서비스로 전달한다" {
        for ((field, expected) in listOf("" to null, ",\"expectedInputRevision\":null" to null,
            ",\"expectedInputRevision\":0" to 0L, ",\"expectedInputRevision\":7" to 7L,
            ",\"expectedInputRevision\":9223372036854775807" to Long.MAX_VALUE)) {
            Mockito.reset(service)
            mvc.perform(put(path).header("Authorization", "Bearer $token").contentType("application/json")
                .content("""{"total":10000,"payerParticipantId":12,"drinks":[]$field}"""))
                .andExpect(status().isOk)
            Mockito.verify(service).saveRound(1, 10, 12, 1, RoundRequest(10000, 12, expectedInputRevision = expected))
        }
    }
    "음수 소수 문자 불리언 범위초과 버전은 저장 호출 전에400으로 거절한다" {
        for (value in listOf("-1", "7.9", "\"7\"", "true", "9223372036854775808", "{}", "[]")) {
            mvc.perform(put(path).header("Authorization", "Bearer $token").contentType("application/json")
                .content("""{"total":10000,"payerParticipantId":12,"expectedInputRevision":$value}"""))
                .andExpect(status().isBadRequest)
        }
        Mockito.verifyNoInteractions(service)
    }
    "양 언어 정책은 최신 버전과 미지정만 허용한다" {
        val unit = UnitInput(10, 12, "OPEN", 7, listOf(12))
        val javaUnit = JavaWorkflow.UnitInput(10, 12, "OPEN", 7, listOf(12L))
        for (expected in listOf(null, 7L)) {
            SettlementWorkflow.expectedRevision(unit, expected)
            JavaWorkflow.expectedRevision(javaUnit, expected)
        }
        for (expected in listOf(0L, 6L, 8L, Long.MAX_VALUE)) {
            shouldThrow<ApiException> { SettlementWorkflow.expectedRevision(unit, expected) }.code shouldBe "SETTLEMENT_INPUT_CHANGED"
            shouldThrow<ApiException> { JavaWorkflow.expectedRevision(javaUnit, expected) }.code shouldBe "SETTLEMENT_INPUT_CHANGED"
        }
        shouldThrow<ApiException> { SettlementWorkflow.expectedRevision(unit, -1) }.status.value() shouldBe 400
        shouldThrow<ApiException> { JavaWorkflow.expectedRevision(javaUnit, -1) }.status.value() shouldBe 400
    }
    "저장은 단위 잠금에서 읽은 버전을 검사하고 충돌 시 차수와 술과 이력을 바꾸지 않는다" {
        var locked = false
        val store = Mockito.mock(GatheringStore::class.java) { invocation ->
            if (invocation.method.name in setOf("update", "insert")) error("충돌 시 DB를 변경하면 안 된다")
            if (invocation.method.name == "rows") {
                val sql = invocation.arguments[0] as String
                when {
                    sql.startsWith("SELECT * FROM settlement_units") -> {
                        sql.endsWith("FOR UPDATE") shouldBe true
                        locked = true
                        listOf(mapOf("id" to 10L, "host_participant_id" to 12L, "status" to "OPEN", "input_revision" to 7L))
                    }
                    sql.startsWith("SELECT participant_id FROM settlement_unit_members") -> listOf(mapOf("participant_id" to 12L))
                    sql.startsWith("SELECT * FROM users") -> listOf(mapOf("id" to 12L))
                    sql.startsWith("SELECT p.id,p.user_id") -> listOf(mapOf("id" to 12L, "user_id" to 12L, "name" to "총무"))
                    else -> error("충돌 후 다른 자료를 읽으면 안 된다: $sql")
                }
            } else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        val real = GatheringService(store, mapper, Mockito.mock(PayoutCipher::class.java),
            Mockito.mock(ModerationService::class.java), Mockito.mock(AttendanceSpoonService::class.java))
        shouldThrow<ApiException> {
            real.saveRound(1, 10, 12, 1, RoundRequest(10000, 12, expectedInputRevision = 6))
        }.code shouldBe "SETTLEMENT_INPUT_CHANGED"
        locked shouldBe true
    }
})
