package app.jeongsan.core

/**
 * `CALC_RULES.md` §4 — 검증 시점은 둘로 나뉜다.
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
    BEARER_NOT_FOUND,
    INVALID_DRINK_ITEM,
    INVALID_ROUNDING_UNIT,
    DRANK_WITHOUT_ATTEND,
    DUPLICATE_ID,
    TOO_FEW_PARTICIPANTS,
    NO_ATTENDEE,
    NO_NON_EXEMPT_ATTENDEE,
    NO_DRINKER_WITH_ALCOHOL,
    NO_NON_EXEMPT_BEARER,
    ALL_EXEMPT,
    NEGATIVE_FINAL_AMOUNT,
}

/**
 * `"입력이 잘못되었습니다"`는 차수를 5개 입력한 주최자에게 아무 도움이 되지 않는다.
 * **어느 차수, 어느 항목이 문제인지 반드시 식별자를 담는다** — `CALC_RULES.md` §4.
 */
data class ValidationError(
    val code: ErrorCode,
    val message: String,
    val roundId: String? = null,
    val extraId: String? = null,
    val participantId: String? = null,
)

object Validator {

    fun validate(input: SettlementInput, phase: ValidationPhase): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val ids = input.participants.map { it.id }.toSet()

        validateAlways(input, ids, errors)
        if (phase == ValidationPhase.CONFIRM) {
            validateOnConfirm(input, errors)
        }
        return errors
    }

    /** 저장 시점에도 확정 시점에도 걸리는 형식 검증. */
    private fun validateAlways(
        input: SettlementInput,
        ids: Set<String>,
        errors: MutableList<ValidationError>,
    ) {
        if (input.roundingUnit != 10 && input.roundingUnit != 100) {
            errors += ValidationError(
                ErrorCode.INVALID_ROUNDING_UNIT,
                "반올림 단위는 10원 또는 100원만 허용합니다. (입력: ${input.roundingUnit})",
            )
        }

        if (ids.size != input.participants.size) {
            errors += ValidationError(ErrorCode.DUPLICATE_ID, "참여자 id가 중복되었습니다.")
        }

        input.drinkItems.forEach { item ->
            if (item.bottleCount <= 0 || item.unitPrice <= 0) {
                errors += ValidationError(
                    ErrorCode.INVALID_DRINK_ITEM,
                    "'${item.name}'의 병 수와 단가는 1 이상이어야 합니다. " +
                        "(${item.bottleCount}병 × ${item.unitPrice}원)",
                    roundId = item.roundId,
                )
            }
        }

        // 술값은 DrinkItem으로 덮어써진 뒤의 값으로 검증해야 한다 (§2.1-b).
        input.effectiveRounds().forEach { round ->
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
            if (round.payerId !in ids) {
                errors += ValidationError(
                    ErrorCode.PAYER_NOT_FOUND,
                    "${round.label}의 결제자가 참여자 목록에 없습니다.",
                    roundId = round.id,
                    participantId = round.payerId,
                )
            }
        }

        input.extras.forEach { extra ->
            if (extra.amount <= 0) {
                errors += ValidationError(
                    ErrorCode.TOTAL_NOT_POSITIVE,
                    "'${extra.label}'의 금액은 1원 이상이어야 합니다.",
                    extraId = extra.id,
                )
            }
            if (extra.payerId !in ids) {
                errors += ValidationError(
                    ErrorCode.PAYER_NOT_FOUND,
                    "'${extra.label}'의 결제자가 참여자 목록에 없습니다.",
                    extraId = extra.id,
                    participantId = extra.payerId,
                )
            }
            extra.bearerIds.filterNot { it in ids }.forEach { unknown ->
                errors += ValidationError(
                    ErrorCode.BEARER_NOT_FOUND,
                    "'${extra.label}'의 부담자 중 참여자 목록에 없는 사람이 있습니다.",
                    extraId = extra.id,
                    participantId = unknown,
                )
            }
        }

        input.attendance.forEach { (key, value) ->
            if (!value.attended && value.drank) {
                errors += ValidationError(
                    ErrorCode.DRANK_WITHOUT_ATTEND,
                    "불참으로 표시됐는데 음주로 표시되어 있습니다.",
                    roundId = key.roundId,
                    participantId = key.participantId,
                )
            }
        }
        // 인원·배분 검증은 여기 없다. 저장 시점에는 아직 아무도 체크하지 않았을 수 있어서다.
    }

    /** 확정 시점에만 걸리는 인원·배분 검증. */
    private fun validateOnConfirm(input: SettlementInput, errors: MutableList<ValidationError>) {
        if (input.participants.size < 2) {
            errors += ValidationError(
                ErrorCode.TOO_FEW_PARTICIPANTS,
                "참여자가 2명 이상이어야 정산할 수 있습니다.",
            )
            return
        }

        if (input.participants.all { it.exempt }) {
            errors += ValidationError(
                ErrorCode.ALL_EXEMPT,
                "참여자 전원이 면제자라 부담할 사람이 없습니다.",
            )
            return
        }

        input.effectiveRounds().forEach { round ->
            val attendees = input.participants.filter {
                input.attendanceOf(it.id, round.id).attended
            }
            if (attendees.isEmpty()) {
                errors += ValidationError(
                    ErrorCode.NO_ATTENDEE,
                    "${round.label}에 참석한 사람이 없습니다.",
                    roundId = round.id,
                )
                return@forEach
            }
            if (attendees.none { !it.exempt }) {
                errors += ValidationError(
                    ErrorCode.NO_NON_EXEMPT_ATTENDEE,
                    "${round.label}의 참석자가 전원 면제자라 나눌 대상이 없습니다.",
                    roundId = round.id,
                )
            }
            if (round.alcohol > 0) {
                val drinkers = input.participants.filter {
                    !it.exempt && input.attendanceOf(it.id, round.id).drank
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

        val exemptIds = input.participants.filter { it.exempt }.map { it.id }.toSet()
        input.extras.forEach { extra ->
            if (extra.bearerIds.none { it !in exemptIds }) {
                errors += ValidationError(
                    ErrorCode.NO_NON_EXEMPT_BEARER,
                    "'${extra.label}'을 부담할 사람이 없습니다. (전원 면제이거나 부담자가 비어 있음)",
                    extraId = extra.id,
                )
            }
        }
    }
}
