package app.jeongsan.server.receipt

import app.jeongsan.server.common.ApiException
import app.jeongsan.server.gathering.GatheringStore
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

fun interface ReceiptUnitAccess {
    fun requireHost(gatheringId: Long, settlementUnitId: Long, userId: Long)
}

@Component
class DatabaseReceiptUnitAccess(private val store: GatheringStore) : ReceiptUnitAccess {
    @Transactional(readOnly = true)
    override fun requireHost(gatheringId: Long, settlementUnitId: Long, userId: Long) {
        val row = store.rows("""
            SELECT settlement_unit.status unit_status, settlement_unit.host_participant_id,
                   participant.id participant_id, participant.status participant_status, unit_member.status member_status
            FROM settlement_units settlement_unit
            LEFT JOIN participants participant ON participant.gathering_id=settlement_unit.gathering_id AND participant.user_id=:userId
            LEFT JOIN settlement_unit_members unit_member ON unit_member.settlement_unit_id=settlement_unit.id
                AND unit_member.gathering_id=settlement_unit.gathering_id AND unit_member.participant_id=participant.id
            WHERE settlement_unit.id=:settlementUnitId AND settlement_unit.gathering_id=:gatheringId
        """.trimIndent(), "gatheringId" to gatheringId, "settlementUnitId" to settlementUnitId, "userId" to userId).singleOrNull()
        val code = ReceiptPolicy.access(row != null,
            row?.get("participant_status") == "ACTIVE" && row["member_status"] == "ACTIVE",
            row?.get("participant_id") != null && row["participant_id"] == row["host_participant_id"],
            row?.get("unit_status")?.toString().orEmpty()) ?: return
        val status = when (code) {
            "SETTLEMENT_UNIT_NOT_FOUND" -> HttpStatus.NOT_FOUND
            "SETTLEMENT_UNIT_NOT_OPEN" -> HttpStatus.CONFLICT
            else -> HttpStatus.FORBIDDEN
        }
        throw ApiException(code, status, "영수증을 입력할 수 있는 정산 단위인지 확인해주세요.")
    }
}
