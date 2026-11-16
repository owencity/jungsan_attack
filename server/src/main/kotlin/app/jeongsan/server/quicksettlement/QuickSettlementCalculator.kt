package app.jeongsan.server.quicksettlement

import app.jeongsan.core.Attendance
import app.jeongsan.core.AttendanceKey
import app.jeongsan.core.Participant
import app.jeongsan.core.Round
import app.jeongsan.core.Settlement
import app.jeongsan.core.SettlementInput
import app.jeongsan.core.SettlementOutcome
import app.jeongsan.server.common.MalformedRequestException

/** 계산용 번호와 이름은 메모리에서만 쓰며 회원·술자리·참여자 기록을 만들지 않는다. */
object QuickSettlementCalculator {
    fun calculate(total: Long, headcount: Int): QuickSettlementResponse {
        if (total !in 1..10_000_000) throw MalformedRequestException("총액은 1원부터 1천만 원까지입니다.")
        if (headcount !in 2..50) throw MalformedRequestException("총 정산인원은 결제자를 포함해 2명부터 50명까지입니다.")
        val people = (1L..headcount.toLong()).map { Participant(it, if (it == 1L) "결제자" else "참여자") }
        val input = SettlementInput(people, listOf(Round(1, 1, "간단 정산", total, 0, 1)),
            people.associate { AttendanceKey(it.id, 1) to Attendance.SOBER })
        return when (val outcome = Settlement.settle(input)) {
            is SettlementOutcome.Failure -> throw QuickSettlementValidationException(outcome.errors)
            is SettlementOutcome.Success -> {
                val recipient = outcome.result.recipients.getValue(1)
                QuickSettlementResponse(outcome.result.grandTotal, headcount, recipient.participantAmounts.getValue(2),
                    recipient.ownShare, recipient.incomingTotal)
            }
        }
    }
}
