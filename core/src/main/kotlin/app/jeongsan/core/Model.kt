package app.jeongsan.core

import java.math.BigInteger

/**
 * 계산 엔진의 입력. DB도 UI도 모른다. `CALC_RULES_V2.md` §2 참조.
 *
 * `id`는 `Long`이다. DB의 `BIGINT`와 1:1로 맞고, 잔액 조정자 동률 규칙에서
 * **"id가 작은 사람" = "먼저 등록된 사람"** 으로 자연스럽게 성립한다.
 *
 * 처음에는 `String`이었다. "엔진이 저장소 키 타입에 묶이지 않게" 하려는 것이었는데,
 * MySQL `BIGINT` 식별자를 문자열로 넘기면 사전순 비교가 `"10" < "2"` 가 되어
 * **10번 참여자가 2번보다 앞선다.** 결정론은 지켜지지만 설명할 수 없는 동작이 남는다.
 * DB가 MySQL로 확정된 이 프로젝트에서 그 유연성은 값을 하지 못했다.
 */
data class SettlementInput(
    val participants: List<Participant>,
    val rounds: List<Round>,
    val attendance: Map<AttendanceKey, Attendance> = emptyMap(),
    val drinkItems: List<DrinkItem> = emptyList(),
) {
    /**
     * `CALC_RULES_V2.md` §2.2 — 해당 차수에 [DrinkItem]이 하나라도 있으면
     * `alcohol`을 무시하고 `Σ(bottleCount × unitPrice)`로 덮어쓴다.
     *
     * 검증도 이 값을 기준으로 해야 한다. 원본 `alcohol`은 이미 의미가 없다.
     */
    fun effectiveRounds(): List<Round> {
        // 술병 상세가 없는 차수까지 복사하지 않는다. 입력 모델은 불변이므로 원본을 그대로
        // 돌려줘도 안전하고, 호출자는 항상 이 함수의 결과만 검증·계산에 사용하면 된다.
        if (drinkItems.isEmpty()) return rounds
        val byRound = drinkItems.groupBy { it.roundId }
        return rounds.map { round ->
            val items = byRound[round.id] ?: return@map round
            val alcohol = items.fold(BigInteger.ZERO) { total, item ->
                total + BigInteger.valueOf(item.bottleCount.toLong()) * BigInteger.valueOf(item.unitPrice)
            }
            round.copy(
                alcohol = when {
                    alcohol > LONG_MAX -> Long.MAX_VALUE
                    alcohol < LONG_MIN -> Long.MIN_VALUE
                    else -> alcohol.longValueExact()
                },
            )
        }
    }

    /**
     * 출석 행이 없다는 것은 아직 체크하지 않았거나 불참한 상태다. 계산에서는 둘 다 부담이
     * 없으므로 [Attendance.ABSENT]로 해석하고, 확정 가능 여부는 [Validator]가 별도로 판단한다.
     */
    fun attendanceOf(participantId: Long, roundId: Long): Attendance =
        attendance[AttendanceKey(participantId, roundId)] ?: Attendance.ABSENT

    private companion object {
        val LONG_MAX: BigInteger = BigInteger.valueOf(Long.MAX_VALUE)
        val LONG_MIN: BigInteger = BigInteger.valueOf(Long.MIN_VALUE)
    }
}

data class Participant(
    val id: Long,
    val name: String,
)

data class Round(
    val id: Long,
    val seq: Int,
    val label: String,
    val total: Long,
    val alcohol: Long,
    val payerId: Long,
)

data class DrinkItem(
    val roundId: Long,
    val name: String,
    val bottleCount: Int,
    val unitPrice: Long,
)

data class AttendanceKey(val participantId: Long, val roundId: Long)

/**
 * 차수별 참여 상태. [EXEMPT]는 참석 사실은 남기되 해당 차수의 모든 부담과 분모에서
 * 제외한다. 불가능한 `불참 + 음주` 조합을 타입으로 만들 수 없게 네 상태로 닫는다.
 */
enum class Attendance(
    val attended: Boolean,
    val drank: Boolean,
    val exempt: Boolean,
) {
    ABSENT(attended = false, drank = false, exempt = false),
    EXEMPT(attended = true, drank = false, exempt = true),
    SOBER(attended = true, drank = false, exempt = false),
    DRANK(attended = true, drank = true, exempt = false),
}
