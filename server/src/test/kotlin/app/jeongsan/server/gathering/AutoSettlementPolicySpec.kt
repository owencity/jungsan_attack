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
    val answers=listOf(1L,2L,3L).map{ResponseInput(it,100,"SOBER","SELF")}
    fun checked(unit: UnitInput, headcount: Int?, rounds: List<RoundInput>, responses: List<ResponseInput>): AutoSettlementPlan? {
        val kotlin=AutoSettlementPolicy.plan(unit,headcount,seats,rounds,responses)
        val java=JavaPolicy.plan(unit.id,unit.hostId,unit.state,headcount,unit.participantIds,
            seats.map{JavaPolicy.Seat(it.participantId,it.joinedAt)},rounds.map{JavaPolicy.Round(it.id,it.unitId,it.payerId)},
            responses.map{JavaPolicy.Answer(it.participantId,it.roundId,it.source)})
        kotlin shouldBe java?.let{AutoSettlementPlan(it.includedIds(),it.excludedIds(),it.error())}
        return kotlin
    }
    "총무는 먼저, 나머지는 가입 시각과 id로 선택하며 인원 밖 무응답은 막지 않는다" {
        checked(unit,3,rounds,answers) shouldBe AutoSettlementPlan(listOf(3,1,2),listOf(4))
    }
    "NULL 인원·미달·빈 차수·이미 정산된 단위는 자동 정산하지 않는다" {
        checked(unit,null,rounds,answers) shouldBe null
        checked(unit,5,rounds,answers) shouldBe null
        checked(unit,3,emptyList(),answers) shouldBe null
        for(state in listOf("SETTLING","COMPLETED")) checked(unit.copy(state=state),3,rounds,answers) shouldBe null
    }
    "인원 안 모든 차수의 실제 응답이 필요하며 AUTO와 다른 단위 응답을 재사용하지 않는다" {
        checked(unit,3,rounds,answers.dropLast(1)) shouldBe null
        checked(unit,3,rounds,answers.map{it.copy(source="AUTO")}) shouldBe null
        checked(unit,3,rounds+rounds[0].copy(id=101),answers) shouldBe null
        checked(unit,3,rounds+rounds[0].copy(id=102,unitId=20),answers) shouldBe AutoSettlementPlan(listOf(3,1,2),listOf(4))
    }
    "인원 밖 결제자는 보류하고 명단을 임의로 늘리지 않는다" {
        checked(unit,3,rounds.map{it.copy(payerId=4)},answers) shouldBe AutoSettlementPlan(listOf(3,1,2),listOf(4),"REMOVE_PAYER")
    }
    "제외된 공유 신원은 다시 계산에 끼워 넣지 않는다" {
        checked(unit.copy(participantIds=listOf(2,3,4)),2,rounds,answers) shouldBe AutoSettlementPlan(listOf(3,2),listOf(4))
    }
    "인원 경계는 양쪽 구현이 각각 검증한다" {
        for(value in listOf(1,51)) {
            shouldThrow<app.jeongsan.server.common.ApiException>{AutoSettlementPolicy.validateHeadcount(value)}.code shouldBe "MALFORMED_REQUEST"
            shouldThrow<IllegalArgumentException>{JavaPolicy.validateHeadcount(value)}.message shouldBe "MALFORMED_REQUEST"
        }
        checked(unit,2,rounds,answers)!!.includedIds shouldBe listOf(3,1)
        checked(unit,50,rounds,answers) shouldBe null
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
