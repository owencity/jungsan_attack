package app.jeongsan.core

/** 원부담 절단과 최종 배정의 차이를 숨기지 않는 표시 전용 변환이다. 계산 결과를 바꾸지 않는다. */
object SettlementTable {
    data class Line(val participantId: Long, val name: String, val total: Long, val paidTotal: Long)
    data class Cell(val participantId: Long, val type: String, val food: Long, val alcohol: Long, val amount: Long)
    data class RoundRow(val roundId: Long, val seq: Int, val payerParticipantId: Long, val total: Long,
        val foodTotal: Long, val alcoholTotal: Long, val displayTotal: Long, val cells: List<Cell>)
    data class Adjustment(val recipientParticipantId: Long, val cells: Map<Long, Long>)
    data class View(val lines: List<Line>, val rounds: List<RoundRow>, val adjustments: List<Adjustment>, val grandTotal: Long)

    fun present(input: SettlementInput, result: SettlementResult): View {
        val participants=input.participants.sortedBy { it.id }
        val rows=input.rounds.sortedBy { it.seq }.map { round ->
            val breakdowns=participants.map { participant -> result.breakdown.getValue(participant.id).rounds.first { it.roundId==round.id } }
            val cells=participants.zip(breakdowns).map { (participant,breakdown) ->
                val food=breakdown.foodShare.numerator.divide(breakdown.foodShare.denominator).longValueExact()
                val alcohol=breakdown.alcoholShare.numerator.divide(breakdown.alcoholShare.denominator).longValueExact()
                Cell(participant.id,
                    when { breakdown.exempt -> "EXEMPT"; !breakdown.attended -> "ABSENT"; breakdown.drank -> "DRANK"; else -> "SOBER" },
                    food,alcohol,Math.addExact(food,alcohol))
            }
            val first=breakdowns.first()
            RoundRow(round.id,round.seq,round.payerId,round.total,first.foodTotal,first.alcoholTotal,cells.sumOf { it.amount },cells)
        }
        val adjustments=result.recipients.toSortedMap().mapNotNull { (recipientId,recipient) ->
            val recipientRows=rows.filter { it.payerParticipantId==recipientId }
            val amounts=participants.associate { participant ->
                val displayed=recipientRows.sumOf { row -> row.cells.first { it.participantId==participant.id }.amount }
                participant.id to recipient.participantAmounts.getValue(participant.id)-displayed
            }
            if(amounts.values.all { it==0L }) null else Adjustment(recipientId,amounts)
        }
        return View(participants.map { participant ->
            val breakdown=result.breakdown.getValue(participant.id)
            Line(participant.id,participant.name,breakdown.finalAmount,breakdown.paidTotal)
        },rows,adjustments,result.grandTotal)
    }

    /** 확정 후에는 스냅샷의 송금을 합산하며 분배를 다시 계산하지 않는다. */
    fun transferTotal(transfers: List<Transfer>): Long = transfers.fold(0L) { total,transfer -> Math.addExact(total,transfer.amount) }

    fun matches(transfers: List<Transfer>, stored: List<Transfer>): Boolean =
        transfers.sortedWith(compareBy({ it.toId },{ it.fromId })) == stored.sortedWith(compareBy({ it.toId },{ it.fromId }))
}
