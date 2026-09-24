package app.jeongsan.core

/**
 * 계산 엔진의 결과. **예외를 던지지 않는다** — `CALC_RULES_V2.md` §5.
 * HTTP 상태 코드 매핑은 API 계층의 몫이다.
 */
sealed interface SettlementOutcome {
    data class Success(val result: SettlementResult) : SettlementOutcome
    data class Failure(val errors: List<ValidationError>) : SettlementOutcome
}

data class SettlementResult(
    /** 각 참여자의 최종 부담액. 결제액이 아니라 정산 후 그 사람이 부담해야 할 몫이다. */
    val amounts: Map<Long, Long>,
    /** 근거 화면(W2)용. **계산만 하고 버리지 말 것** — `CALC_RULES_V2.md` §4. */
    val breakdown: Map<Long, ParticipantBreakdown>,
    /** 차수 총무별 결제액·참여자 배정액·잔액 조정 결과. */
    val recipients: Map<Long, RecipientSettlement>,
    /** 서로 다른 총무 사이를 상계하지 않은 실제 송금 지시 목록. */
    val transfers: List<Transfer>,
    val grandTotal: Long,
)

data class ParticipantBreakdown(
    val participantId: Long,
    val name: String,
    val rounds: List<RoundBreakdown>,
    /** 1원 올림 전 차수 몫의 정확한 합이다. */
    val rawTotal: Rational,
    val finalAmount: Long,
    /** 이 사람이 차수 총무로 결제한 총액. */
    val paidTotal: Long,
)

data class RecipientSettlement(
    val recipientId: Long,
    val paidTotal: Long,
    val participantAmounts: Map<Long, Long>,
    val ownShare: Long,
    val incomingTotal: Long,
    val adjustmentParticipantId: Long,
)

/**
 * 차수 하나에 대한 근거. 불참한 차수도 `amount = 0`으로 포함된다 —
 * W2가 `2차 불참 → 0원`을 보여줘야 하기 때문이다.
 *
 * [foodTotal] / [attendeeCount] 같은 원재료를 함께 담는 이유는
 * 화면이 `안주값 52,000 ÷ 5명`처럼 **계산 과정을 그대로 보여줘야** 하기 때문이다.
 */
data class RoundBreakdown(
    val roundId: Long,
    val seq: Int,
    val label: String,
    val attended: Boolean,
    val drank: Boolean,
    /** 참석했지만 이 차수의 모든 부담과 분모에서 제외됐는지 나타낸다. */
    val exempt: Boolean,
    val foodTotal: Long,
    val attendeeCount: Int,
    val foodShare: Rational,
    val alcoholTotal: Long,
    val drinkerCount: Int,
    val alcoholShare: Rational,
) {
    val amount: Rational get() = foodShare + alcoholShare
}

/** `from`이 `to`에게 보낸다. */
data class Transfer(val fromId: Long, val toId: Long, val amount: Long)
