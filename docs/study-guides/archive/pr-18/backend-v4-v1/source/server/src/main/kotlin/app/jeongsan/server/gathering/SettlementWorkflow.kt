package app.jeongsan.server.gathering

import app.jeongsan.core.*
import app.jeongsan.server.common.ApiException
import org.springframework.http.HttpStatus
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class UnitInput(val id: Long, val hostId: Long, val state: String, val revision: Long, val participantIds: List<Long>)
data class PersonInput(val id: Long, val userId: Long, val name: String)
data class DrinkInput(val name: String, val unitPrice: Long, val quantity: Int)
data class RoundInput(val id: Long, val unitId: Long, val seq: Int, val total: Long, val payerId: Long, val drinks: List<DrinkInput>)
data class ResponseInput(val participantId: Long, val roundId: Long, val type: String, val source: String)
data class PreviewLine(val participantId: Long, val total: Long, val auto: Boolean, val rounds: List<SettlementPresentation.Basis>)
data class PreviewTransfer(val from: Long, val to: Long, val amount: Long, val basis: List<SettlementPresentation.Basis>)
data class Preview(val settlementUnitId: Long, val inputRevision: Long, val inputHash: String,
    val lines: List<PreviewLine>, val transfers: List<PreviewTransfer>, val grandTotal: Long)

/** DB와 HTTP에서 분리한 단위 인가·입력 조립·확정 상태 규칙. Java는 javaimpl의 별도 구현이다. */
object SettlementWorkflow {
    fun fail(code: String, message: String = code, status: HttpStatus = HttpStatus.CONFLICT): Nothing =
        throw ApiException(code, status, message)

    fun host(unit: UnitInput, person: PersonInput) {
        if (unit.hostId != person.id) fail("NOT_SETTLEMENT_UNIT_HOST", status = HttpStatus.FORBIDDEN)
    }
    fun open(unit: UnitInput) { if (unit.state != "OPEN") fail("SETTLEMENT_UNIT_NOT_OPEN") }
    fun member(unit: UnitInput, participantId: Long) {
        if (participantId !in unit.participantIds) fail("NOT_SETTLEMENT_UNIT_MEMBER", status = HttpStatus.FORBIDDEN)
    }
    fun remove(unit: UnitInput, participantId: Long, rounds: List<RoundInput>) {
        open(unit)
        if (participantId == unit.hostId) fail("REMOVE_HOST")
        if (rounds.any { it.unitId == unit.id && it.payerId == participantId }) fail("REMOVE_PAYER")
    }
    fun response(unit: UnitInput, personId: Long, roundId: Long, type: String, rounds: List<RoundInput>) {
        open(unit); member(unit, personId)
        if (rounds.none { it.id == roundId && it.unitId == unit.id }) fail("ROUND_NOT_FOUND", status = HttpStatus.NOT_FOUND)
        if (type !in setOf("ABSENT", "SOBER", "DRANK")) fail("MALFORMED_REQUEST", status = HttpStatus.BAD_REQUEST)
    }
    fun input(unit: UnitInput, people: List<PersonInput>, rounds: List<RoundInput>, responses: List<ResponseInput>): SettlementInput {
        val persons = people.filter { it.id in unit.participantIds }.sortedBy { it.id }
        val selected = rounds.filter { it.unitId == unit.id }.sortedBy { it.seq }
        if (selected.isEmpty()) fail("NO_ROUNDS")
        val answers = responses.associateBy { it.participantId to it.roundId }
        return SettlementInput(
            persons.map { Participant(it.id, it.name) },
            selected.map { Round(it.id, it.seq, "${it.seq}차", it.total, 0, it.payerId) },
            persons.flatMap { p -> selected.map { r -> AttendanceKey(p.id, r.id) to
                Attendance.valueOf(answers[p.id to r.id]?.type ?: "DRANK") } }.toMap(),
            selected.flatMap { r -> r.drinks.map { DrinkItem(r.id, it.name, it.quantity, it.unitPrice) } },
        )
    }
    fun hash(unit: UnitInput, input: SettlementInput): String {
        // 이름·조회 시각·다른 단위·계좌는 계산을 바꾸지 않는다. 길이 접두사로 문자열 충돌을 막는다.
        val bytes = buildString {
            fun add(value: Any) { val text = value.toString(); append(text.length).append(':').append(text) }
            add("v4");add(unit.id);add("participants");add(input.participants.size)
            input.participants.sortedBy { it.id }.forEach { add(it.id) }
            add("rounds");add(input.rounds.size)
            input.rounds.sortedBy { it.id }.forEach { add(it.id); add(it.seq); add(it.total); add(it.alcohol); add(it.payerId) }
            add("attendance");add(input.attendance.size)
            input.attendance.entries.sortedWith(compareBy({ it.key.roundId }, { it.key.participantId }))
                .forEach { add(it.key.roundId); add(it.key.participantId); add(it.value.name) }
            add("drinks");add(input.drinkItems.size)
            input.drinkItems.sortedWith(compareBy({ it.roundId }, { it.name }, { it.bottleCount }, { it.unitPrice }))
                .forEach { add(it.roundId); add(it.name); add(it.bottleCount); add(it.unitPrice) }
        }.toByteArray(StandardCharsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
    fun preview(unit: UnitInput, people: List<PersonInput>, rounds: List<RoundInput>, responses: List<ResponseInput>): Preview {
        val input = input(unit, people, rounds, responses)
        val result = when (val outcome = Settlement.settle(input)) {
            is SettlementOutcome.Success -> outcome.result
            is SettlementOutcome.Failure -> throw SettlementValidationException(outcome.errors)
        }
        val selected = rounds.filter { it.unitId == unit.id }
        val answers = responses.map { it.participantId to it.roundId }.toSet()
        return Preview(unit.id, unit.revision, hash(unit, input), result.breakdown.values.map { b ->
            PreviewLine(b.participantId, b.finalAmount, selected.any { b.participantId to it.id !in answers },
                b.rounds.map { r -> SettlementPresentation.Basis(r.roundId,
                    input.attendance.getValue(AttendanceKey(b.participantId, r.roundId)).name,
                    r.amount.numerator.divide(r.amount.denominator).longValueExact()) })
        }, result.transfers.map { PreviewTransfer(it.fromId, it.toId, it.amount, SettlementPresentation.basis(input, result, it)) }, result.grandTotal)
    }
    fun creationRoster(ids: List<Long>, host: Long): List<Long> {
        val sorted=ids.distinct().sorted()
        if(sorted.size!=ids.size || host !in sorted) fail("MALFORMED_REQUEST",status=HttpStatus.BAD_REQUEST)
        return sorted
    }
    fun payout(bank: String, account: String, holder: String): Map<String,String> {
        val banks=setOf("카카오뱅크","토스뱅크","국민","신한","우리","하나","농협","기업","케이뱅크","SC제일","새마을금고","우체국","수협","신협","부산","대구")
        val digits=account.replace("-","");val name=holder.trim()
        if(bank !in banks || !account.matches(Regex("[0-9-]+")) || !digits.matches(Regex("[0-9]{8,16}")) || name.codePointCount(0,name.length) !in 1..20)
            fail("MALFORMED_REQUEST",status=HttpStatus.BAD_REQUEST)
        return mapOf("bank" to bank,"accountNo" to digits,"holder" to name)
    }
    fun expired(states: List<String>, due: java.time.Instant?, last: java.time.Instant, now: java.time.Instant): Boolean =
        (states.all{it=="COMPLETED"} && due!=null && due<=now) || last<=now.minusSeconds(30*86400L)

    fun summary(states: List<String>): String = when { "OPEN" in states -> "OPEN"; "SETTLING" in states -> "SETTLING"; else -> "COMPLETED" }
    fun transfer(state: String, action: String, actorId: Long, sender: Long, recipient: Long, unitState: String, hasPayout: Boolean): String {
        val permitted = if (action == "sent") sender else recipient
        if (actorId != permitted) fail("NOT_TRANSFER_OWNER", status = HttpStatus.FORBIDDEN)
        val target = when (action) { "sent" -> "SENT"; "confirm" -> "CONFIRMED"; "not-received" -> "WAITING"; else -> fail("MALFORMED_REQUEST") }
        if (state == target || (state == "CONFIRMED" && action == "sent")) return state
        if (unitState != "SETTLING") fail("SETTLEMENT_UNIT_NOT_SETTLING")
        if (action == "sent" && !hasPayout) fail("PAYOUT_MISSING")
        if ((action == "not-received" && state != "SENT") || state == "CONFIRMED") fail("INVALID_TRANSFER_TRANSITION")
        return target
    }
}

class SettlementValidationException(val details: List<ValidationError>) :
    ApiException("VALIDATION_FAILED", HttpStatus.CONFLICT, "정산 입력을 확인해주세요.")
