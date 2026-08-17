package app.jeongsan.core

/**
 * 계산 엔진의 결과. **예외를 던지지 않는다** — `CALC_RULES.md` §4.
 * HTTP 상태 코드 매핑은 API 계층의 몫이다.
 */
sealed interface SettlementOutcome {
    data class Success(val result: SettlementResult) : SettlementOutcome
    data class Failure(val errors: List<ValidationError>) : SettlementOutcome
}

data class SettlementResult(
    /** 면제자를 제외하고 결제 총액이 가장 큰 참여자. 동률이면 id 사전순 첫 번째. */
    val mainPayerId: Long,
    val amounts: Map<Long, Long>,
    /** 근거 화면(W2)용. **계산만 하고 버리지 말 것** — `CALC_RULES.md` §1. */
    val breakdown: Map<Long, ParticipantBreakdown>,
    val transfers: List<Transfer>,
    val grandTotal: Long,
    /** 실제로 적용된 반올림 단위. 대표결제자 음수를 피하려 강등됐을 수 있다. */
    val appliedRoundingUnit: Int,
    /** 요청한 단위가 대표결제자를 음수로 만들어 10원으로 강등된 경우 `true` — `CALC_RULES.md` T7. */
    val roundingUnitDowngraded: Boolean,
)

data class ParticipantBreakdown(
    val participantId: Long,
    val name: String,
    val isExempt: Boolean,
    val isMainPayer: Boolean,
    val rounds: List<RoundBreakdown>,
    val extras: List<ExtraBreakdown>,
    /** 반올림 전 원부담. 차수·기타 항목 몫의 정확한 합이다. */
    val rawTotal: Rational,
    val finalAmount: Long,
    /** 이 사람이 결제한 총액 (차수 + 기타 항목). */
    val paidTotal: Long,
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
    val foodTotal: Long,
    val attendeeCount: Int,
    val foodShare: Rational,
    val alcoholTotal: Long,
    val drinkerCount: Int,
    val alcoholShare: Rational,
) {
    val amount: Rational get() = foodShare + alcoholShare
}

data class ExtraBreakdown(
    val extraId: Long,
    val label: String,
    val amount: Long,
    val bearerCount: Int,
    val share: Rational,
)

/** `from`이 `to`에게 보낸다. */
data class Transfer(val fromId: Long, val toId: Long, val amount: Long)
