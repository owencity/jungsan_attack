package app.jeongsan.server.gathering

import app.jeongsan.server.gathering.javaimpl.AutoSettlementPolicy as JavaPolicy
import app.jeongsan.server.gathering.javaimpl.NotificationText as JavaText
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import java.time.Instant

class AutoSettlementPolicySpec : StringSpec({
    val instant=Instant.parse("2026-10-10T06:00:00Z")
    val unit=UnitInput(10,3,"OPEN",4,listOf(1,2,3,4))
    val seats=listOf(MemberSeat(4,instant.plusSeconds(1)),MemberSeat(2,instant),MemberSeat(3,instant.plusSeconds(5)),MemberSeat(1,instant))
    val rounds=listOf(RoundInput(100,10,1,10000,3,emptyList()))
    val answers=listOf(1L,2L,3L,4L).map{ResponseInput(it,100,"SOBER","SELF")}
    fun checked(unit: UnitInput, headcount: Int?, rounds: List<RoundInput>, responses: List<ResponseInput>): AutoSettlementPlan? {
        val kotlin=AutoSettlementPolicy.plan(unit,headcount,seats,rounds,responses)
        val java=JavaPolicy.plan(unit.id,unit.hostId,unit.state,headcount,unit.participantIds,
            seats.map{JavaPolicy.Seat(it.participantId,it.joinedAt)},rounds.map{JavaPolicy.Round(it.id,it.unitId,it.payerId)},
            responses.map{JavaPolicy.Answer(it.participantId,it.roundId,it.source)})
        kotlin shouldBe java?.let{AutoSettlementPlan(it.includedIds(),it.excludedIds(),it.error())}
        return kotlin
    }
    "목표 인원 초과 응답자도 전부 계산하며 자동 제외하지 않는다" {
        checked(unit,3,rounds,answers) shouldBe AutoSettlementPlan(listOf(3,1,2,4),emptyList())
    }
    "목표 미입력과 미달도 전체 명단으로 계산하되 빈 차수와 확정 상태는 재평가하지 않는다" {
        for(count in listOf(null,5,50)) checked(unit,count,rounds,answers) shouldBe AutoSettlementPlan(listOf(3,1,2,4),emptyList())
        checked(unit,3,emptyList(),answers) shouldBe null
        for(state in listOf("SETTLING","COMPLETED")) checked(unit.copy(state=state),3,rounds,answers) shouldBe null
    }
    "목표 인원 밖 응답도 필요하며 AUTO와 다른 단위 응답을 재사용하지 않는다" {
        checked(unit,3,rounds,answers.dropLast(1)) shouldBe null
        checked(unit,3,rounds,answers.map{it.copy(source="AUTO")}) shouldBe null
        checked(unit,3,rounds+rounds[0].copy(id=101),answers) shouldBe null
        checked(unit,3,rounds+rounds[0].copy(id=102,unitId=20),answers) shouldBe AutoSettlementPlan(listOf(3,1,2,4),emptyList())
    }
    "목표 인원 밖 결제자도 현재 명단 그대로 포함한다" {
        checked(unit,3,rounds.map{it.copy(payerId=4)},answers) shouldBe AutoSettlementPlan(listOf(3,1,2,4),emptyList())
    }
    "총무가 제외한 공유 신원은 다시 계산에 끼워 넣지 않는다" {
        checked(unit.copy(participantIds=listOf(2,3,4)),2,rounds,answers) shouldBe AutoSettlementPlan(listOf(3,2,4),emptyList())
    }
    "인원 경계는 양쪽 구현이 각각 검증하며 계산 인원을 자르지 않는다" {
        for(value in listOf(1,51)) {
            shouldThrow<app.jeongsan.server.common.ApiException>{AutoSettlementPolicy.validateHeadcount(value)}.code shouldBe "MALFORMED_REQUEST"
            shouldThrow<IllegalArgumentException>{JavaPolicy.validateHeadcount(value)}.message shouldBe "MALFORMED_REQUEST"
        }
        checked(unit,2,rounds,answers)!!.includedIds shouldBe listOf(3,1,2,4)
        checked(unit,50,rounds,answers)!!.excludedIds shouldBe emptyList()
    }
    "금액 구분과 사람이 읽는 알림 제목은 코드 enum과 구분한다" {
        NotificationText.money(184000) shouldBe "184,000"
        JavaText.money(184000) shouldBe "184,000"
        for(type in listOf("SETTLED","SETTLED_HOST","HEADCOUNT_EXCEEDED","MEMBER_EXCLUDED","REMOVED","SENT","COMPLETED","PAYOUT_REGISTERED")) {
            NotificationText.title(type) shouldBe JavaText.title(type)
            (NotificationText.title(type)==type) shouldBe false
        }
        NotificationText.title("SETTLED_HOST") shouldBe "계산이 끝났어요"
    }
})
