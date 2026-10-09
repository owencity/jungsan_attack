package app.jeongsan.server.gathering

import java.time.Instant

data class MemberSeat(val participantId: Long, val joinedAt: Instant)
data class AutoSettlementPlan(val includedIds: List<Long>, val excludedIds: List<Long>, val error: String? = null)

/** 인원 밖 결제자를 삭제하기 전에 보류한다. DB 잠금·스냅샷 저장은 서비스의 책임이다. */
object AutoSettlementPolicy {
    fun validateHeadcount(headcount: Int?) {
        if (headcount != null && headcount !in 2..50)
            SettlementWorkflow.fail("MALFORMED_REQUEST", status = org.springframework.http.HttpStatus.BAD_REQUEST)
    }

    fun plan(unit: UnitInput, headcount: Int?, seats: List<MemberSeat>, rounds: List<RoundInput>, responses: List<ResponseInput>): AutoSettlementPlan? {
        validateHeadcount(headcount)
        if (unit.state != "OPEN" || headcount == null) return null
        val ordered = seats.filter { it.participantId in unit.participantIds }
            .sortedWith(compareBy<MemberSeat> { it.participantId != unit.hostId }.thenBy { it.joinedAt }.thenBy { it.participantId })
            .map { it.participantId }
        val selectedRounds = rounds.filter { it.unitId == unit.id }
        if (ordered.size < headcount || selectedRounds.isEmpty() || unit.hostId !in ordered) return null
        val included = ordered.take(headcount)
        val answered = responses.filter { it.source == "SELF" || it.source == "HOST" }
            .map { it.participantId to it.roundId }.toSet()
        if (included.any { participantId -> selectedRounds.any { participantId to it.id !in answered } }) return null
        val excluded = ordered.drop(headcount)
        val error = if (selectedRounds.any { it.payerId in excluded }) "REMOVE_PAYER" else null
        return AutoSettlementPlan(included, excluded, error)
    }
}

/** 로케일에 따라 금액 구분 기호가 달라지지 않게 고정한다. */
object NotificationText {
    fun money(amount: Long): String = String.format(java.util.Locale.US, "%,d", amount)
    fun title(type: String): String = when (type) {
        "JOINED", "RESPONDED" -> "참여 응답이 도착했어요"
        "SETTLED" -> "정산이 나왔어요! 입금액을 확인해주세요"
        "SETTLED_HOST" -> "계산이 끝났어요"
        "AUTO_RESPONDED" -> "자동응답으로 계산된 사람이 있어요"
        "SENT" -> "입금을 확인해주세요"
        "CONFIRMED" -> "입금이 확인됐어요"
        "NOT_RECEIVED" -> "아직 입금이 확인되지 않았어요"
        "COMPLETED" -> "정산 완료! 🎉"
        "SETTLEMENT_REVERTED" -> "정산이 되돌려졌어요"
        "PAYOUT_REGISTERED" -> "계좌가 등록됐어요"
        "PAYOUT_MISSING" -> "받을 계좌를 등록해주세요"
        "HEADCOUNT_EXCEEDED" -> "참여 인원을 확인해주세요"
        "MEMBER_EXCLUDED", "REMOVED" -> "이번 정산에서 빠졌어요"
        else -> "술자리 소식이 있어요"
    }
}
