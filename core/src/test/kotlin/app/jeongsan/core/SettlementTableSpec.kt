package app.jeongsan.core

import app.jeongsan.core.javaimpl.Model as JavaModel
import app.jeongsan.core.javaimpl.Settlement as JavaSettlement
import app.jeongsan.core.javaimpl.SettlementResult as JavaResult
import app.jeongsan.core.javaimpl.SettlementTable as JavaTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random

private fun SettlementInput.javaTableInput() = JavaModel.SettlementInput(
    participants.map { JavaModel.Participant(it.id,it.name) },
    rounds.map { JavaModel.Round(it.id,it.seq,it.label,it.total,it.alcohol,it.payerId) },
    attendance.mapKeys { JavaModel.AttendanceKey(it.key.participantId,it.key.roundId) }.mapValues { JavaModel.Attendance.valueOf(it.value.name) },
    drinkItems.map { JavaModel.DrinkItem(it.roundId,it.name,it.bottleCount,it.unitPrice) })

private fun JavaTable.View.kotlinView() = SettlementTable.View(
    lines().map { SettlementTable.Line(it.participantId(),it.name(),it.total(),it.paidTotal()) },
    rounds().map { row -> SettlementTable.RoundRow(row.roundId(),row.seq(),row.payerParticipantId(),row.total(),row.foodTotal(),row.alcoholTotal(),row.displayTotal(),
        row.cells().map { SettlementTable.Cell(it.participantId(),it.type(),it.food(),it.alcohol(),it.amount()) }) },
    adjustments().map { SettlementTable.Adjustment(it.recipientParticipantId(),it.cells()) },grandTotal())

private fun tableParity(input: SettlementInput): SettlementTable.View {
    val result=input.succeed()
    val view=SettlementTable.present(input,result)
    val javaInput=input.javaTableInput()
    val javaResult=(JavaSettlement.settle(javaInput) as JavaResult.Success).result()
    JavaTable.present(javaInput,javaResult).kotlinView() shouldBe view
    view.lines.sumOf { it.total } shouldBe view.grandTotal
    view.lines.forEach { line ->
        val cells=view.rounds.sumOf { row -> row.cells.first { it.participantId==line.participantId }.amount }
        val adjustment=view.adjustments.sumOf { it.cells.getValue(line.participantId) }
        cells+adjustment shouldBe line.total
    }
    return view
}

class SettlementTableSpec : StringSpec({
    val people=listOf(Participant(1,"김동명"),Participant(2,"김동명"),Participant(3,"김셋"))
    fun input(rounds: List<Round>, kind: (Long,Long)->Attendance = { _,_ -> Attendance.SOBER }) = SettlementInput(people,rounds,
        people.flatMap { participant -> rounds.map { round -> AttendanceKey(participant.id,round.id) to kind(participant.id,round.id) } }.toMap())

    "10000원 3인 표는 절단 셀과 음수 포함 조정으로 기존 최종 금액을 정확히 보존한다" {
        val view=tableParity(input(listOf(Round(1,1,"1차",10000,0,1))))
        view.rounds.single().cells.map { it.participantId } shouldBe listOf(1L,2L,3L)
        view.rounds.single().cells.map { it.amount } shouldBe listOf(3333L,3333L,3333L)
        view.rounds.single().displayTotal shouldBe 9999L
        view.lines.map { it.total } shouldBe listOf(3332L,3334L,3334L)
        view.adjustments.single().cells shouldBe mapOf(1L to -1L,2L to 1L,3L to 1L)
    }
    "술과 비주류를 분리하고 논알코올에게 술값을 표시하지 않는다" {
        val view=tableParity(input(listOf(Round(1,1,"1차",9000,3000,1))) { participantId,_ -> if(participantId==2L) Attendance.SOBER else Attendance.DRANK })
        view.rounds.single().cells.map { it.food } shouldBe listOf(2000L,2000L,2000L)
        view.rounds.single().cells.map { it.alcohol } shouldBe listOf(1500L,0L,1500L)
        view.adjustments shouldBe emptyList()
    }
    "불참과 면제는 셀 0원과 사유를 보존하고 수취인별 조정은 섞이지 않는다" {
        val view=tableParity(input(listOf(Round(2,2,"2차",10000,0,2),Round(1,1,"1차",10000,0,1))) { participantId,roundId ->
            when { participantId==3L && roundId==1L -> Attendance.ABSENT;participantId==3L -> Attendance.EXEMPT;else -> Attendance.SOBER }
        })
        view.rounds.map { it.roundId } shouldBe listOf(1L,2L)
        view.rounds.map { it.cells.last().type } shouldBe listOf("ABSENT","EXEMPT")
        view.rounds.map { it.cells.last().amount } shouldBe listOf(0L,0L)
    }
    "고정 seed의 여러 차수와 결제자에서도 표시 셀과 조정 합계가 두 언어에서 일치한다" {
        val random=Random(20261031)
        repeat(100) {
            val rounds=(1..random.nextInt(1,9)).map { sequence -> Round(sequence.toLong(),sequence,"${sequence}차",random.nextLong(10000,1000000),0,random.nextLong(1,4)) }
            tableParity(input(rounds))
        }
    }
    "스냅샷 합산은 오버플로를 거절하고 송금 비교는 순서만 무시한다" {
        val transfers=listOf(Transfer(2,1,5000),Transfer(3,1,7000))
        SettlementTable.transferTotal(transfers) shouldBe 12000L
        JavaTable.transferTotal(transfers.map { JavaResult.Transfer(it.fromId,it.toId,it.amount) }) shouldBe 12000L
        SettlementTable.matches(transfers,transfers.reversed()) shouldBe true
        SettlementTable.matches(transfers,transfers.drop(1)) shouldBe false
        SettlementTable.matches(transfers,transfers+transfers.first()) shouldBe false
        shouldThrow<ArithmeticException> { SettlementTable.transferTotal(listOf(Transfer(2,1,Long.MAX_VALUE),Transfer(3,1,1))) }
        shouldThrow<ArithmeticException> { JavaTable.transferTotal(listOf(JavaResult.Transfer(2,1,Long.MAX_VALUE),JavaResult.Transfer(3,1,1))) }
    }
})
