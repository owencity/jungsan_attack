package app.jeongsan.core

/** FC-014 D2: 표시용 정수 근거도 서버가 별도 분배 공식을 만들지 않도록 Core에서 변환한다. */
object SettlementPresentation {
    data class Basis(val roundId: Long, val type: String, val amount: Long)

    fun basis(input: SettlementInput, result: SettlementResult, transfer: Transfer): List<Basis> {
        val ids = input.rounds.filter { it.payerId == transfer.toId }.map { it.id }.toSet()
        val rounds = result.breakdown.getValue(transfer.fromId).rounds.filter { it.roundId in ids }
        val values = rounds.map {
            Basis(it.roundId, when { it.exempt -> "EXEMPT"; !it.attended -> "ABSENT"; it.drank -> "DRANK"; else -> "SOBER" },
                it.amount.numerator.divide(it.amount.denominator).longValueExact())
        }.toMutableList()
        // 합산 후 올림·잔액 조정은 차수별 절단과 다를 수 있다. 차이는 마지막 차수의 근거에 명시한다.
        if (values.isNotEmpty()) {
            val last = values.last()
            values[values.lastIndex] = last.copy(amount = last.amount + transfer.amount - values.sumOf { it.amount })
        }
        return values
    }
}
