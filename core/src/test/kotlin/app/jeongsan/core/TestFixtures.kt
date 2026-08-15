package app.jeongsan.core

fun participants(vararg ids: String): List<Participant> = ids.map { Participant(it, it) }

fun Participant.exempted(): Participant = copy(exempt = true)

fun round(
    id: String,
    seq: Int,
    total: Long,
    alcohol: Long,
    payerId: String,
): Round = Round(id = id, seq = seq, label = "${seq}차", total = total, alcohol = alcohol, payerId = payerId)

/** `"A" at "r1" drank` 같은 형태로 출석을 적기 위한 헬퍼. */
class AttendanceBuilder {
    private val map = mutableMapOf<AttendanceKey, Attendance>()

    fun drank(roundId: String, vararg ids: String) = mark(roundId, Attendance.DRANK, ids)
    fun sober(roundId: String, vararg ids: String) = mark(roundId, Attendance.SOBER, ids)
    fun absent(roundId: String, vararg ids: String) = mark(roundId, Attendance.ABSENT, ids)

    private fun mark(roundId: String, value: Attendance, ids: Array<out String>) = apply {
        ids.forEach { map[AttendanceKey(it, roundId)] = value }
    }

    fun build(): Map<AttendanceKey, Attendance> = map.toMap()
}

fun attendance(block: AttendanceBuilder.() -> Unit): Map<AttendanceKey, Attendance> =
    AttendanceBuilder().apply(block).build()

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
