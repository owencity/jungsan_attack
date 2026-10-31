package app.jeongsan.server.gathering

import java.time.Instant

data class MemberSeat(val participantId: Long, val joinedAt: Instant)
data class AutoSettlementPlan(val includedIds: List<Long>, val excludedIds: List<Long>, val error: String? = null)

/** FC-029: 전원 응답 후 계산 검증만 한다. 목표 인원은 명단 선택·확정의 조건이 아니다. */
object AutoSettlementPolicy {
    fun validateHeadcount(headcount: Int?) {
        if (headcount != null && headcount !in 2..50)
            SettlementWorkflow.fail("MALFORMED_REQUEST", status = org.springframework.http.HttpStatus.BAD_REQUEST)
    }

    fun plan(unit: UnitInput, headcount: Int?, seats: List<MemberSeat>, rounds: List<RoundInput>, responses: List<ResponseInput>): AutoSettlementPlan? {
        validateHeadcount(headcount)
        if (unit.state != "OPEN") return null
        val ordered = seats.filter { it.participantId in unit.participantIds }
            .sortedWith(compareBy<MemberSeat> { it.participantId != unit.hostId }.thenBy { it.joinedAt }.thenBy { it.participantId })
            .map { it.participantId }
        val selectedRounds = rounds.filter { it.unitId == unit.id }
        if (selectedRounds.isEmpty() || unit.hostId !in ordered) return null
        val included = ordered
        val answered = responses.filter { it.source == "SELF" || it.source == "HOST" }
            .map { it.participantId to it.roundId }.toSet()
        if (included.any { participantId -> selectedRounds.any { participantId to it.id !in answered } }) return null
        return AutoSettlementPlan(included, emptyList())
    }
}

