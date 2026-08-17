package app.jeongsan.core

/**
 * 정산 계산 엔진. **순수 함수다** — DB도 UI도 시간도 모르고, 예외를 던지지 않는다.
 *
 * `CALC_RULES.md` §2의 알고리즘을 그대로 구현한다.
 */
object Settlement {

    fun settle(input: SettlementInput): SettlementOutcome {
        val errors = Validator.validate(input, ValidationPhase.CONFIRM)
        if (errors.isNotEmpty()) return SettlementOutcome.Failure(errors)

        val first = compute(input, input.roundingUnit)

        // T7 — 대표결제자가 음수면 예외 처리 대신 단위를 10원으로 강등해 재계산한다.
        if (first.amounts.values.any { it < 0 } && input.roundingUnit != 10) {
            val retried = compute(input, unit = 10)
            if (retried.amounts.values.any { it < 0 }) {
                return SettlementOutcome.Failure(listOf(negativeAmountError(retried)))
            }
            return SettlementOutcome.Success(retried.copy(roundingUnitDowngraded = true))
        }

        if (first.amounts.values.any { it < 0 }) {
            return SettlementOutcome.Failure(listOf(negativeAmountError(first)))
        }
        return SettlementOutcome.Success(first)
    }

    private fun negativeAmountError(result: SettlementResult): ValidationError {
        val victim = result.amounts.entries.first { it.value < 0 }
        return ValidationError(
            ErrorCode.NEGATIVE_FINAL_AMOUNT,
            "1인당 금액이 너무 작아 대표결제자의 부담이 음수가 됩니다. " +
                "(${victim.key}: ${victim.value}원) 인원을 줄이거나 금액을 확인해주세요.",
            participantId = victim.key,
        )
    }

    private fun compute(input: SettlementInput, unit: Int): SettlementResult {
        val rounds = input.effectiveRounds()
        val exemptIds = input.participants.filter { it.exempt }.map { it.id }.toSet()

        // ── §2.1 원부담 계산 — 반올림 없음. 정확한 유리수로만 누적한다.
        val roundLines = mutableMapOf<String, MutableList<RoundBreakdown>>()
        input.participants.forEach { roundLines[it.id] = mutableListOf() }

        rounds.sortedBy { it.seq }.forEach { round ->
            val attendees = input.participants
                .filter { !it.exempt && input.attendanceOf(it.id, round.id).attended }
                .map { it.id }.toSet()
            val drinkers = input.participants
                .filter { !it.exempt && input.attendanceOf(it.id, round.id).drank }
                .map { it.id }.toSet()

            val food = round.total - round.alcohol
            val foodShare = if (attendees.isEmpty()) Rational.ZERO
            else Rational.of(food) / attendees.size
            val alcoholShare = if (drinkers.isEmpty()) Rational.ZERO
            else Rational.of(round.alcohol) / drinkers.size

            input.participants.forEach { p ->
                val a = input.attendanceOf(p.id, round.id)
                roundLines.getValue(p.id) += RoundBreakdown(
                    roundId = round.id,
                    seq = round.seq,
                    label = round.label,
                    attended = a.attended,
                    drank = a.drank,
                    foodTotal = food,
                    attendeeCount = attendees.size,
                    foodShare = if (p.id in attendees) foodShare else Rational.ZERO,
                    alcoholTotal = round.alcohol,
                    drinkerCount = drinkers.size,
                    alcoholShare = if (p.id in drinkers) alcoholShare else Rational.ZERO,
                )
            }
        }

        // ── §2.1-d 기타 항목 — 부담자(면제자 제외)끼리 균등 분담.
        val extraLines = mutableMapOf<String, MutableList<ExtraBreakdown>>()
        input.participants.forEach { extraLines[it.id] = mutableListOf() }

        input.extras.forEach { extra ->
            val bearers = extra.bearerIds.filter { it !in exemptIds }.toSet()
            if (bearers.isEmpty()) return@forEach
            val share = Rational.of(extra.amount) / bearers.size
            bearers.forEach { id ->
                extraLines.getValue(id) += ExtraBreakdown(
                    extraId = extra.id,
                    label = extra.label,
                    amount = extra.amount,
                    bearerCount = bearers.size,
                    share = share,
                )
            }
        }

        val raw = input.participants.associate { p ->
            p.id to (
                roundLines.getValue(p.id).map { it.amount } +
                    extraLines.getValue(p.id).map { it.share }
                ).sum()
        }

        // ── §2.2 대표결제자 결정 — 면제자 제외, 결제총액 최대, 동률이면 id 사전순.
        val paid = input.participants.associate { it.id to 0L }.toMutableMap()
        rounds.forEach { paid[it.payerId] = paid.getValue(it.payerId) + it.total }
        input.extras.forEach { paid[it.payerId] = paid.getValue(it.payerId) + it.amount }

        val grandTotal = rounds.sumOf { it.total } + input.extras.sumOf { it.amount }

        val mainPayerId = input.participants
            .filter { !it.exempt }
            .sortedWith(compareByDescending<Participant> { paid.getValue(it.id) }.thenBy { it.id })
            .first().id

        // ── §2.3 반올림. 대표결제자는 잔액을 가져가므로 구조적으로 합계가 원금과 일치한다.
        val amounts = mutableMapOf<String, Long>()
        input.participants.forEach { p ->
            when {
                p.exempt -> amounts[p.id] = 0L
                p.id == mainPayerId -> Unit
                else -> amounts[p.id] = raw.getValue(p.id).ceilTo(unit)
            }
        }
        amounts[mainPayerId] = grandTotal - amounts.values.sum()

        val breakdown = input.participants.associate { p ->
            p.id to ParticipantBreakdown(
                participantId = p.id,
                name = p.name,
                isExempt = p.exempt,
                isMainPayer = p.id == mainPayerId,
                rounds = roundLines.getValue(p.id),
                extras = extraLines.getValue(p.id),
                rawTotal = if (p.exempt) Rational.ZERO else raw.getValue(p.id),
                finalAmount = amounts.getValue(p.id),
                paidTotal = paid.getValue(p.id),
            )
        }

        return SettlementResult(
            mainPayerId = mainPayerId,
            amounts = amounts,
            breakdown = breakdown,
            transfers = buildTransfers(input, paid, amounts),
            grandTotal = grandTotal,
            appliedRoundingUnit = unit,
            roundingUnitDowngraded = false,
        )
    }

    /**
     * §2.4 송금 목록. 채권자·채무자를 각각 금액 내림차순(동률이면 id 사전순)으로 정렬하고
     * 큰 쪽부터 greedy 상계한다.
     *
     * **최소 송금 횟수의 최적해가 아니다. 최적화하지 말 것.**
     * 결과가 예측 가능한 것이 더 중요하다 — `CALC_RULES.md` §2.4.
     */
    private fun buildTransfers(
        input: SettlementInput,
        paid: Map<String, Long>,
        amounts: Map<String, Long>,
    ): List<Transfer> {
        val balance = input.participants.associate { p ->
            p.id to (paid.getValue(p.id) - amounts.getValue(p.id))
        }
        val order = compareByDescending<Pair<String, Long>> { it.second }.thenBy { it.first }

        val creditors = balance.filter { it.value > 0 }
            .map { it.key to it.value }.sortedWith(order).toMutableList()
        val debtors = balance.filter { it.value < 0 }
            .map { it.key to -it.value }.sortedWith(order).toMutableList()

        val transfers = mutableListOf<Transfer>()
        var ci = 0
        var di = 0
        while (ci < creditors.size && di < debtors.size) {
            val (creditorId, credit) = creditors[ci]
            val (debtorId, debt) = debtors[di]
            val amount = minOf(credit, debt)
            transfers += Transfer(fromId = debtorId, toId = creditorId, amount = amount)
            creditors[ci] = creditorId to (credit - amount)
            debtors[di] = debtorId to (debt - amount)
            if (creditors[ci].second == 0L) ci++
            if (debtors[di].second == 0L) di++
        }
        return transfers
    }
}
