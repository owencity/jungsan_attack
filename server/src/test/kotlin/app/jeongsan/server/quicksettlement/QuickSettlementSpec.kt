package app.jeongsan.server.quicksettlement

import app.jeongsan.server.common.DatabaseActiveUserAccess
import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.common.LoginUserArgumentResolver
import app.jeongsan.server.gathering.GatheringStore
import app.jeongsan.server.user.JwtService
import app.jeongsan.server.quicksettlement.javaimpl.QuickSettlementCalculator as JavaCalculator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mockito.Answers
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.module.kotlin.jacksonMapperBuilder

class QuickSettlementSpec : StringSpec({
    "1만 원을 3명이 나누면 결제자 부담에 잔액을 반영하고 합계를 보존한다" {
        val expected = QuickSettlementResponse(10000, 3, 3334, 3332, 6668)
        QuickSettlementCalculator.calculate(10000, 3) shouldBe expected
        JavaCalculator.calculate(10000, 3) shouldBe expected
    }
    "인원 경계와 큰 금액에서도 core 결과가 정확하고 재현된다" {
        for (count in listOf(2, 3, 7, 50)) {
            for (amount in listOf(10000L, 123456L, 10_000_000L)) {
                val result = QuickSettlementCalculator.calculate(amount, count)
                JavaCalculator.calculate(amount, count) shouldBe result
                result.participantAmount * (count - 1) + result.payerAmount shouldBe amount
                result.incomingTotal + result.payerAmount shouldBe amount
                QuickSettlementCalculator.calculate(amount, count) shouldBe result
            }
        }
    }
    "결제자 부담이 음수가 되는 소액은 양 어댑터가 core 오류를 그대로 전달한다" {
        shouldThrow<QuickSettlementValidationException> { QuickSettlementCalculator.calculate(1, 3) }
            .details.single().code.name shouldBe "NEGATIVE_ADJUSTED_AMOUNT"
        shouldThrow<QuickSettlementValidationException> { JavaCalculator.calculate(1, 3) }
            .details.single().code.name shouldBe "NEGATIVE_ADJUSTED_AMOUNT"
        QuickSettlementCalculator.calculate(2, 3) shouldBe QuickSettlementResponse(2, 3, 1, 0, 2)
    }

    val jwt = JwtService("01234567890123456789012345678901", 30)
    val appToken = jwt.issue(12, "APP")
    val path = "/api/v1/settlements/quick"
    var accountExists = true
    var queries = 0
    val store = Mockito.mock(GatheringStore::class.java) { invocation ->
        if (invocation.method.name == "rows") {
            (invocation.arguments[0] as String) shouldBe "SELECT id FROM users WHERE id=:userId"
            queries++
            if (accountExists) listOf(mapOf("id" to 12L)) else emptyList<Map<String, Any?>>()
        } else if (invocation.method.name in setOf("insert", "update")) {
            error("간단 정산에서 DB를 변경하면 안 된다")
        } else Answers.RETURNS_DEFAULTS.answer(invocation)
    }
    val service = QuickSettlementService(DatabaseActiveUserAccess(store))
    val mvc = MockMvcBuilders.standaloneSetup(QuickSettlementController(service))
        .setCustomArgumentResolvers(LoginUserArgumentResolver(jwt)).setControllerAdvice(GlobalExceptionHandler())
        .setMessageConverters(JacksonJsonHttpMessageConverter(jacksonMapperBuilder().build())).build()
    beforeTest { accountExists = true; queries = 0 }

    "로그인한 앱은 술자리를 저장하지 않고 표시용 원화 5개 필드를 받는다" {
        mvc.perform(post(path).header("Authorization", "Bearer $appToken").contentType(MediaType.APPLICATION_JSON)
            .content("""{"total":10000,"headcount":3}"""))
            .andExpect(status().isOk).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.length()").value(5)).andExpect(jsonPath("$.total").value(10000))
            .andExpect(jsonPath("$.headcount").value(3)).andExpect(jsonPath("$.participantAmount").value(3334))
            .andExpect(jsonPath("$.payerAmount").value(3332)).andExpect(jsonPath("$.incomingTotal").value(6668))
        queries shouldBe 1
    }
    "로그인 없음과 삭제된 계정은 간단 정산을 사용할 수 없다" {
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("""{"total":10000,"headcount":3}"""))
            .andExpect(status().isUnauthorized)
        queries shouldBe 0
        accountExists = false
        mvc.perform(post(path).header("Authorization", "Bearer $appToken").contentType(MediaType.APPLICATION_JSON)
            .content("""{"total":10000,"headcount":3}"""))
            .andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        queries shouldBe 1
    }
    "HTTP에서 총액과 인원 경계를 검사하고 음수 잔액은 422로 알린다" {
        for (body in listOf("""{"total":10000,"headcount":1}""", """{"total":10000,"headcount":51}""",
            """{"total":0,"headcount":3}""", """{"total":10000001,"headcount":3}""",
            """{"total":10000.9,"headcount":3}""", """{"total":10000,"headcount":3.7}""",
            """{"total":"10000","headcount":3}""", """{"total":null,"headcount":3}""", "{}",
            """{"total":10000,"headcount":18446744073709551619}""")) {
            mvc.perform(post(path).header("Authorization", "Bearer $appToken").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest)
        }
        queries shouldBe 0
        mvc.perform(post(path).header("Authorization", "Bearer $appToken").contentType(MediaType.APPLICATION_JSON)
            .content("""{"total":1,"headcount":3}"""))
            .andExpect(status().`is`(422)).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].code").value("NEGATIVE_ADJUSTED_AMOUNT"))
    }
})
