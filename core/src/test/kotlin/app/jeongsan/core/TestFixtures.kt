package app.jeongsan.core

/**
 * `id`가 `Long`이 되면서 테스트에 `mapOf(1L to 3_320L)` 같은 단언이 늘어난다.
 * 그러면 **어느 참여자인지 읽을 수 없다.**
 *
 * 그래서 픽스처가 **이름 → id**를 만들고, 단언은 [amountsByName]·[transfersByName]으로
 * 다시 이름으로 되돌린다. 테스트는 읽히고, id 매핑까지 함께 검증된다.
 *
 * id는 **등록 순서대로 1, 2, 3…** 이므로 `CALC_RULES.md` §2.2의
 * "동률이면 id가 작은 사람" = "먼저 등록된 사람"이 테스트에서도 그대로 성립한다.
 */
fun participants(vararg names: String): List<Participant> =
    names.mapIndexed { i, name -> Participant(id = (i + 1).toLong(), name = name) }

fun Participant.exempted(): Participant = copy(exempt = true)

/** 이름으로 id를 찾는다. 픽스처 밖에서는 쓰지 않는다. */
fun List<Participant>.id(name: String): Long =
    first { it.name == name }.id

fun List<Participant>.name(id: Long): String =
    first { it.id == id }.name

fun round(
    id: Long,
    seq: Int,
    total: Long,
    alcohol: Long,
    payerId: Long,
): Round = Round(id = id, seq = seq, label = "${seq}차", total = total, alcohol = alcohol, payerId = payerId)

/** `sober("r1", "A", "B")` 처럼 이름으로 출석을 적는다. */
class AttendanceBuilder(private val ps: List<Participant>) {
    private val map = mutableMapOf<AttendanceKey, Attendance>()

    fun drank(roundId: Long, vararg names: String) = mark(roundId, Attendance.DRANK, names)
    fun sober(roundId: Long, vararg names: String) = mark(roundId, Attendance.SOBER, names)
    fun absent(roundId: Long, vararg names: String) = mark(roundId, Attendance.ABSENT, names)

    private fun mark(roundId: Long, value: Attendance, names: Array<out String>) = apply {
        names.forEach { map[AttendanceKey(ps.id(it), roundId)] = value }
    }

    fun build(): Map<AttendanceKey, Attendance> = map.toMap()
}

fun List<Participant>.attendance(block: AttendanceBuilder.() -> Unit): Map<AttendanceKey, Attendance> =
    AttendanceBuilder(this).apply(block).build()

/** 성공을 기대하는 테스트용. 실패하면 검증 오류를 그대로 드러내며 터진다. */
fun SettlementInput.succeed(): SettlementResult =
    when (val outcome = Settlement.settle(this)) {
        is SettlementOutcome.Success -> outcome.result
        is SettlementOutcome.Failure -> error("검증에 걸렸다: ${outcome.errors}")
    }

/** 실패를 기대하는 테스트용. */
fun SettlementInput.fail(): List<ValidationError> =
    when (val outcome = Settlement.settle(this)) {
        is SettlementOutcome.Success -> error("통과하면 안 되는 입력이 통과했다: ${outcome.result.amounts}")
        is SettlementOutcome.Failure -> outcome.errors
    }

fun List<ValidationError>.codes(): Set<ErrorCode> = map { it.code }.toSet()

// ── 단언을 이름으로 읽기 위한 변환 ────────────────────────────

fun SettlementResult.amountsByName(ps: List<Participant>): Map<String, Long> =
    ps.associate { it.name to amounts.getValue(it.id) }

fun SettlementResult.mainPayerName(ps: List<Participant>): String = ps.name(mainPayerId)

/** `"B→A 3340"` 형태. 순서까지 검증하려면 리스트를 그대로 비교한다. */
fun SettlementResult.transfersByName(ps: List<Participant>): List<String> =
    transfers.map { "${ps.name(it.fromId)}→${ps.name(it.toId)} ${it.amount}" }
