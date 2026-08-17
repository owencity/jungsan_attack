package app.jeongsan.core

/**
 * 계산 엔진의 입력. DB도 UI도 모른다. `CALC_RULES.md` §1 참조.
 *
 * `id`는 문자열이다. `CALC_RULES.md` §2.2가 대표결제자 동률 시 **사전순** 결정을 요구하기 때문에
 * 전순서가 있는 타입이어야 하고, 저장소의 키 타입에 엔진이 묶이지 않게 하려는 것이다.
 */
data class SettlementInput(
    val participants: List<Participant>,
    val rounds: List<Round>,
    val attendance: Map<AttendanceKey, Attendance> = emptyMap(),
    val drinkItems: List<DrinkItem> = emptyList(),
    val extras: List<ExtraItem> = emptyList(),
    val roundingUnit: Int = 10,
) {
    /**
     * `CALC_RULES.md` §2.1-b — 해당 차수에 [DrinkItem]이 하나라도 있으면
     * `alcohol`을 무시하고 `Σ(bottleCount × unitPrice)`로 덮어쓴다.
     *
     * 검증도 이 값을 기준으로 해야 한다. 원본 `alcohol`은 이미 의미가 없다.
     */
    fun effectiveRounds(): List<Round> {
        if (drinkItems.isEmpty()) return rounds
        val byRound = drinkItems.groupBy { it.roundId }
        return rounds.map { round ->
            val items = byRound[round.id] ?: return@map round
            round.copy(alcohol = items.sumOf { it.bottleCount.toLong() * it.unitPrice })
        }
    }

    fun attendanceOf(participantId: String, roundId: String): Attendance =
        attendance[AttendanceKey(participantId, roundId)] ?: Attendance.ABSENT
}

data class Participant(
    val id: String,
    val name: String,
    /** `CALC_RULES.md` §2.1-c — 부담 0이며 **모든 분모에서 제외**된다. 대표결제자가 될 수 없다. */
    val exempt: Boolean = false,
)

data class Round(
    val id: String,
    val seq: Int,
    val label: String,
    val total: Long,
    val alcohol: Long,
    val payerId: String,
)

data class DrinkItem(
    val roundId: String,
    val name: String,
    val bottleCount: Int,
    val unitPrice: Long,
)

data class ExtraItem(
    val id: String,
    val label: String,
    val amount: Long,
    val payerId: String,
    /** 이 항목을 부담할 사람들. 면제자는 여기 있어도 분모에서 빠진다. */
    val bearerIds: List<String>,
)

data class AttendanceKey(val participantId: String, val roundId: String)

/**
 * `attended == false && drank == true`는 모순이지만 **생성자에서 막지 않는다.**
 * `CALC_RULES.md` §4가 이것을 *검증 대상*으로 규정하기 때문이다.
 * 저장소에 이미 그런 행이 있다면 예외로 터지는 대신 검증 오류로 보고돼야 한다.
 */
data class Attendance(val attended: Boolean, val drank: Boolean) {

    companion object {
        val ABSENT = Attendance(attended = false, drank = false)
        val SOBER = Attendance(attended = true, drank = false)
        val DRANK = Attendance(attended = true, drank = true)
    }
}
