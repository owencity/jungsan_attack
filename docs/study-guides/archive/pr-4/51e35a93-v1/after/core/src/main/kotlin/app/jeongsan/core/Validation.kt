package app.jeongsan.core

import java.math.BigInteger

/**
 * `CALC_RULES_V2.md` §5 — 검증 시점은 둘로 나뉜다.
 *
 * [SAVE] 시점에 전체 검증을 걸면 **아직 아무도 체크하지 않았으므로 참석자가 0명이라
 * 주최자가 차수를 입력조차 못 한다.**
 */
enum class ValidationPhase { SAVE, CONFIRM }

enum class ErrorCode {
    TOTAL_NOT_POSITIVE,
    ALCOHOL_NEGATIVE,
    ALCOHOL_EXCEEDS_TOTAL,
    PAYER_NOT_FOUND,
    INVALID_DRINK_ITEM,
    DRINK_ITEM_ROUND_NOT_FOUND,
    ATTENDANCE_REFERENCE_NOT_FOUND,
    MISSING_ATTENDANCE,
    DUPLICATE_ID,
    DUPLICATE_ROUND_SEQ,
    AMOUNT_TOO_LARGE,
    TOO_FEW_PARTICIPANTS,
    NO_ATTENDEE,
    NO_DRINKER_WITH_ALCOHOL,
    NEGATIVE_ADJUSTED_AMOUNT,
}

/**
 * `"입력이 잘못되었습니다"`는 차수를 5개 입력한 주최자에게 아무 도움이 되지 않는다.
 * **어느 차수, 어느 참여자가 문제인지 반드시 식별자를 담는다** — `CALC_RULES_V2.md` §5.
 */
data class ValidationError(
    val code: ErrorCode,
    val message: String,
    val roundId: Long? = null,
    val participantId: Long? = null,
)

object Validator {

    /**
     * 총액 상한. 값 자체는 임의지만 **상한이 존재하는 것**이 중요하다.
     *
     * 코드 리뷰 F2 — `Rational.ceilTo`가 `longValueExact()`로 끝나므로 이론상
     * `Long` 범위를 넘으면 `ArithmeticException`이 난다. "이 엔진은 예외를 던지지 않는다"는
     * 원칙에 구멍이 하나 있으면 호출자는 매번 방어를 고민해야 하므로 여기서 막는다.
     *
     * 실질적 이득은 오버플로 방어가 아니라 **오타 방어**다 —
     * 87,000원을 870,000,000원으로 잘못 입력하는 일은 실제로 난다.
     */
    const val MAX_AMOUNT: Long = 1_000_000_000_000L   // 1조 원
    private val MAX_AMOUNT_BIG: BigInteger = BigInteger.valueOf(MAX_AMOUNT)

    /**
     * 검증 오류를 하나 발견해도 즉시 끝내지 않고 가능한 오류를 모두 모은다. 여러 차수를
     * 입력한 사용자가 저장과 재시도를 반복하지 않고 한 번에 수정할 수 있게 하기 위해서다.
     */
    fun validate(input: SettlementInput, phase: ValidationPhase): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val rounds = input.effectiveRounds()          // F6 — 한 번만 계산해 넘긴다
        val ids = input.participants.map { it.id }.toSet()

        validateAlways(input, rounds, ids, errors)
        if (phase == ValidationPhase.CONFIRM) {
            validateOnConfirm(input, rounds, errors)
        }
        return errors
    }

    private fun <T> duplicatesOf(values: List<T>): Set<T> =
        values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

    /** 저장 시점에도 확정 시점에도 걸리는 형식 검증. */
    private fun validateAlways(
        input: SettlementInput,
        rounds: List<Round>,
        ids: Set<Long>,
        errors: MutableList<ValidationError>,
    ) {
        // ── 중복 id (F1) — 세 컬렉션 모두 검사하고 어느 id가 중복인지 담는다.
        // 참여자만 검사하고 있었는데, 같은 roundId 가 둘이면 attendance 조회가 두 차수에
        // 같은 값을 돌려주어 금액이 조용히 틀린다. 그런데 합계는 맞으므로 불변식으로도 안 잡힌다.
        duplicatesOf(input.participants.map { it.id }).forEach {
            errors += ValidationError(
                ErrorCode.DUPLICATE_ID, "참여자 id가 중복되었습니다: $it", participantId = it,
            )
        }
        duplicatesOf(input.rounds.map { it.id }).forEach {
            errors += ValidationError(
                ErrorCode.DUPLICATE_ID, "차수 id가 중복되었습니다: $it", roundId = it,
            )
        }
        duplicatesOf(input.rounds.map { it.seq }).forEach {
            errors += ValidationError(
                ErrorCode.DUPLICATE_ROUND_SEQ,
                "차수 순번이 중복되었습니다: ${it}차. 근거 화면의 차수 순서가 흔들립니다.",
            )
        }

        val roundIds = input.rounds.map { it.id }.toSet()
        input.drinkItems.forEach { item ->
            if (item.bottleCount <= 0 || item.unitPrice <= 0) {
                errors += ValidationError(
                    ErrorCode.INVALID_DRINK_ITEM,
                    "'${item.name}'의 병 수와 단가는 1 이상이어야 합니다. " +
                        "(${item.bottleCount}병 × ${item.unitPrice}원)",
                    roundId = item.roundId,
                )
            } else {
                val itemAmount = BigInteger.valueOf(item.bottleCount.toLong()) *
                    BigInteger.valueOf(item.unitPrice)
                if (itemAmount > MAX_AMOUNT_BIG) {
                    errors += ValidationError(
                        ErrorCode.AMOUNT_TOO_LARGE,
                        "'${item.name}'의 술값이 너무 큽니다. 병 수와 단가를 확인해주세요.",
                        roundId = item.roundId,
                    )
                }
            }
            if (item.roundId !in roundIds) {
                errors += ValidationError(
                    ErrorCode.DRINK_ITEM_ROUND_NOT_FOUND,
                    "'${item.name}'이 존재하지 않는 차수를 참조합니다.",
                    roundId = item.roundId,
                )
            }
        }

        // 술값은 DrinkItem으로 덮어써진 뒤의 값으로 검증해야 한다 (§2.1-b).
        rounds.forEach { round ->
            if (round.total <= 0) {
                errors += ValidationError(
                    ErrorCode.TOTAL_NOT_POSITIVE,
                    "${round.label}의 총액은 1원 이상이어야 합니다.",
                    roundId = round.id,
                )
            }
            if (round.alcohol < 0) {
                errors += ValidationError(
                    ErrorCode.ALCOHOL_NEGATIVE,
                    "${round.label}의 술값이 음수입니다.",
                    roundId = round.id,
                )
            }
            if (round.alcohol > round.total) {
                errors += ValidationError(
                    ErrorCode.ALCOHOL_EXCEEDS_TOTAL,
                    "${round.label}의 술값(${round.alcohol}원)이 총액(${round.total}원)보다 큽니다.",
                    roundId = round.id,
                )
            }
            if (round.total > MAX_AMOUNT) {
                errors += ValidationError(
                    ErrorCode.AMOUNT_TOO_LARGE,
                    "${round.label}의 총액이 너무 큽니다. 0을 하나 더 찍지 않았는지 확인해주세요.",
                    roundId = round.id,
                )
            }
            if (round.alcohol > MAX_AMOUNT) {
                errors += ValidationError(
                    ErrorCode.AMOUNT_TOO_LARGE,
                    "${round.label}의 술값이 너무 큽니다. 병 수와 단가를 확인해주세요.",
                    roundId = round.id,
                )
            }
            if (round.payerId !in ids) {
                errors += ValidationError(
                    ErrorCode.PAYER_NOT_FOUND,
                    "${round.label}의 결제자가 참여자 목록에 없습니다.",
                    roundId = round.id,
                    participantId = round.payerId,
                )
            }
        }

        input.attendance.keys.forEach { key ->
            if (key.participantId !in ids || key.roundId !in roundIds) {
                errors += ValidationError(
                    ErrorCode.ATTENDANCE_REFERENCE_NOT_FOUND,
                    "참여 응답이 존재하지 않는 참여자 또는 차수를 참조합니다.",
                    roundId = key.roundId,
                    participantId = key.participantId,
                )
            }
        }
        // 인원·배분 검증은 여기 없다. 저장 시점에는 아직 아무도 체크하지 않았을 수 있어서다.
    }

    /** 확정 시점에만 걸리는 인원·배분 검증. */
    private fun validateOnConfirm(
        input: SettlementInput,
        rounds: List<Round>,
        errors: MutableList<ValidationError>,
    ) {
        val grandTotal = rounds.fold(BigInteger.ZERO) { total, round ->
            total + BigInteger.valueOf(round.total)
        }
        if (grandTotal > MAX_AMOUNT_BIG) {
            errors += ValidationError(
                ErrorCode.AMOUNT_TOO_LARGE, "전체 총액이 너무 큽니다. (${grandTotal}원)",
            )
        }

        // 아래의 두 전역 조건이 실패하면 차수별 분모 검증도 같은 원인에서 파생된 오류를
        // 쏟아낸다. 사용자가 먼저 해결해야 할 대표 오류만 반환하도록 여기서 중단한다.
        if (input.participants.size < 2) {
            errors += ValidationError(
                ErrorCode.TOO_FEW_PARTICIPANTS,
                "참여자가 2명 이상이어야 정산할 수 있습니다.",
            )
            return
        }

        rounds.forEach { round ->
            val missing = input.participants.filter {
                AttendanceKey(it.id, round.id) !in input.attendance
            }
            missing.forEach { participant ->
                errors += ValidationError(
                    ErrorCode.MISSING_ATTENDANCE,
                    "${round.label}에 '${participant.name}'의 응답이 없습니다.",
                    roundId = round.id,
                    participantId = participant.id,
                )
            }

            val attendees = input.participants.filter {
                val attendance = input.attendanceOf(it.id, round.id)
                attendance.attended && !attendance.exempt
            }
            if (attendees.isEmpty()) {
                errors += ValidationError(
                    ErrorCode.NO_ATTENDEE,
                    "${round.label}에 비용을 부담할 참석자가 없습니다.",
                    roundId = round.id,
                )
                return@forEach
            }
            if (round.alcohol > 0) {
                val drinkers = input.participants.filter {
                    input.attendanceOf(it.id, round.id).drank
                }
                if (drinkers.isEmpty()) {
                    errors += ValidationError(
                        ErrorCode.NO_DRINKER_WITH_ALCOHOL,
                        "${round.label}에 술값 ${round.alcohol}원이 있는데 술 마신 사람이 없습니다. " +
                            "술값을 0으로 하거나 음주자를 지정해주세요.",
                        roundId = round.id,
                    )
                }
            }
        }

    }
}
