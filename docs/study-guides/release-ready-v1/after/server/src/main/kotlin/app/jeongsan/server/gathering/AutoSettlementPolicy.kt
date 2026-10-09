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

