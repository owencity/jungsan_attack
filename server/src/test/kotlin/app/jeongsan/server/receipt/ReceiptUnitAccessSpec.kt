package app.jeongsan.server.receipt

import app.jeongsan.server.common.ApiException
import app.jeongsan.server.common.UnauthenticatedException
import app.jeongsan.server.gathering.GatheringStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito
import org.mockito.Answers

class ReceiptUnitAccessSpec : StringSpec({
    "삭제된 계정은 남아 있는 참여자 기록을 읽기 전에 거절한다" {
        var queries = 0
        val store = Mockito.mock(GatheringStore::class.java) { invocation ->
            if (invocation.method.name == "rows") {
                queries++
                (invocation.arguments[0] as String).contains("FROM users") shouldBe true
                emptyList<Map<String, Any?>>()
            } else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        shouldThrow<UnauthenticatedException> { DatabaseReceiptUnitAccess(store).requireHost(1, 10, 12) }
        queries shouldBe 1
    }

    "술자리와 단위가 일치하는 활성 총무만 허용하고 조회에 행 잠금을 사용하지 않는다" {
        var row: Map<String, Any?>? = mapOf("unit_status" to "OPEN", "participant_status" to "ACTIVE",
            "member_status" to "ACTIVE", "participant_id" to 12L, "host_participant_id" to 12L)
        val store = Mockito.mock(GatheringStore::class.java) { invocation ->
            if (invocation.method.name == "rows") {
                val query = invocation.arguments[0] as String
                query.uppercase().contains("FOR UPDATE") shouldBe false
                if (query.contains("FROM users")) listOf(mapOf("id" to 12L))
                else {
                    query.contains("settlement_unit.id=:settlementUnitId AND settlement_unit.gathering_id=:gatheringId") shouldBe true
                    listOfNotNull(row)
                }
            } else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        val access = DatabaseReceiptUnitAccess(store)
        access.requireHost(1, 10, 12)
        row = row!! + ("member_status" to "LEFT")
        shouldThrow<ApiException> { access.requireHost(1, 10, 12) }.code shouldBe "NOT_PARTICIPANT"
        row = row + ("member_status" to "ACTIVE") + ("host_participant_id" to 13L)
        shouldThrow<ApiException> { access.requireHost(1, 10, 12) }.code shouldBe "NOT_SETTLEMENT_UNIT_HOST"
        row = row + ("host_participant_id" to 12L) + ("unit_status" to "SETTLING")
        shouldThrow<ApiException> { access.requireHost(1, 10, 12) }.code shouldBe "SETTLEMENT_UNIT_NOT_OPEN"
        row = null
        shouldThrow<ApiException> { access.requireHost(1, 10, 12) }.code shouldBe "SETTLEMENT_UNIT_NOT_FOUND"
    }
})
