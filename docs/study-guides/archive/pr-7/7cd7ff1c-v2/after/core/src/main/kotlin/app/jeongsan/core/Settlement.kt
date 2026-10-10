package app.jeongsan.core

/**
 * 정산 계산 엔진. **순수 함수다** — DB도 UI도 시간도 모르고, 예외를 던지지 않는다.
 *
 * `CALC_RULES_V2.md` §3의 알고리즘을 그대로 구현한다.
 */
object Settlement {

    /**
     * 검증 → 정확한 원부담 계산 → 최종 원화 금액 확정의 유일한 공개 진입점이다.
     * 호출자는 내부 계산 함수를 우회할 수 없으므로 성공 결과는 항상 확정 단계 검증을 거친다.
     */
    fun settle(input: SettlementInput): SettlementOutcome {
        val errors = Validator.validate(input, ValidationPhase.CONFIRM)
        if (errors.isNotEmpty()) return SettlementOutcome.Failure(errors)

        val result = compute(input)
        val negative = result.recipients.values.firstOrNull {
            it.participantAmounts.values.any { amount -> amount < 0 }
        }
        if (negative != null) {
            return SettlementOutcome.Failure(listOf(negativeAmountError(negative)))
        }
        return SettlementOutcome.Success(result)
    }

    private fun negativeAmountError(recipient: RecipientSettlement): ValidationError {
        val victim = recipient.participantAmounts.entries.first { it.value < 0 }
        return ValidationError(
            ErrorCode.NEGATIVE_ADJUSTED_AMOUNT,
            "결제 금액이 참여 인원에 비해 너무 작아 잔액 조정자의 부담이 음수가 됩니다. " +
                "(${victim.key}: ${victim.value}원) 인원을 줄이거나 금액을 확인해주세요.",
            participantId = victim.key,
        )
    }

    private fun compute(input: SettlementInput): SettlementResult {
        val rounds = input.effectiveRounds()

        // ── 원부담 계산 — 정수 올림 전까지 정확한 유리수로만 누적한다.
        val roundLines = mutableMapOf<Long, MutableList<RoundBreakdown>>()
        input.participants.forEach { roundLines[it.id] = mutableListOf() }

        rounds.sortedBy { it.seq }.forEach { round ->
            val attendees = input.participants
                .filter {
                    val attendance = input.attendanceOf(it.id, round.id)
                    attendance.attended && !attendance.exempt
                }
                .map { it.id }.toSet()
            val drinkers = input.participants
                .filter { input.attendanceOf(it.id, round.id).drank }
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
                    exempt = a.exempt,
                    foodTotal = food,
                    attendeeCount = attendees.size,
                    foodShare = if (p.id in attendees) foodShare else Rational.ZERO,
                    alcoholTotal = round.alcohol,
                    drinkerCount = drinkers.size,
                    alcoholShare = if (p.id in drinkers) alcoholShare else Rational.ZERO,
                )
            }
        }

        // raw는 아직 원 단위로 올리지 않은 각 참여자의 정확한 부담액이다.
        val raw = input.participants.associate { p ->
            p.id to roundLines.getValue(p.id).map { it.amount }.sum()
        }

        val paid = input.participants.associate { it.id to 0L }.toMutableMap()
        rounds.forEach { paid[it.payerId] = paid.getValue(it.payerId) + it.total }
        val grandTotal = rounds.sumOf { it.total }

        // 수취인(차수 총무)별로 정확한 원부담을 합친 뒤 1원 단위로 한 번만 올린다.
        val recipients = rounds.groupBy { it.payerId }
            .toSortedMap()
            .mapValues { (recipientId, recipientRounds) ->
                val roundIds = recipientRounds.map { it.id }.toSet()
                val rawByParticipant = input.participants.associate { participant ->
                    participant.id to roundLines.getValue(participant.id)
                        .filter { it.roundId in roundIds }
                        .map { it.amount }
                        .sum()
                }
                val adjustmentId = if (rawByParticipant.getValue(recipientId).signum > 0) {
                    recipientId
                } else {
                    rawByParticipant.entries
                        .filter { it.value.signum > 0 }
                        .sortedWith(
                            compareByDescending<Map.Entry<Long, Rational>> { it.value }
                                .thenBy { it.key },
                        )
                        .first().key
                }
                val participantAmounts = input.participants.associate { participant ->
                    participant.id to if (participant.id == adjustmentId) {
                        0L
                    } else {
                        rawByParticipant.getValue(participant.id).ceilTo(1)
                    }
                }.toMutableMap()
                val paidTotal = recipientRounds.sumOf { it.total }
                participantAmounts[adjustmentId] = paidTotal - participantAmounts.values.sum()
                val incomingTotal = participantAmounts
                    .filterKeys { it != recipientId }
                    .values.sum()

                RecipientSettlement(
                    recipientId = recipientId,
                    paidTotal = paidTotal,
                    participantAmounts = participantAmounts,
                    ownShare = participantAmounts.getValue(recipientId),
                    incomingTotal = incomingTotal,
                    adjustmentParticipantId = adjustmentId,
                )
            }

        val amounts = input.participants.associate { participant ->
            participant.id to recipients.values.sumOf {
                it.participantAmounts.getValue(participant.id)
            }
        }

        val breakdown = input.participants.associate { p ->
            p.id to ParticipantBreakdown(
                participantId = p.id,
                name = p.name,
                rounds = roundLines.getValue(p.id),
                rawTotal = raw.getValue(p.id),
                finalAmount = amounts.getValue(p.id),
                paidTotal = paid.getValue(p.id),
            )
        }

        return SettlementResult(
            amounts = amounts,
            breakdown = breakdown,
            recipients = recipients,
            transfers = buildTransfers(recipients),
            grandTotal = grandTotal,
        )
    }

    /** 수취인별 배정액을 그대로 송금으로 만든다. 서로 다른 총무의 채권은 상계하지 않는다. */
    private fun buildTransfers(recipients: Map<Long, RecipientSettlement>): List<Transfer> =
        recipients.values.flatMap { recipient ->
            recipient.participantAmounts.entries
                .filter { (participantId, amount) -> participantId != recipient.recipientId && amount > 0 }
                .sortedBy { it.key }
                .map { (participantId, amount) ->
                    Transfer(fromId = participantId, toId = recipient.recipientId, amount = amount)
                }
        }
}
