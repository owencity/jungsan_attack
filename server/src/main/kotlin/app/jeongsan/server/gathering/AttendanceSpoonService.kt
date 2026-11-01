package app.jeongsan.server.gathering

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** 스키마의 진실은 022-attendance-spoons.yaml. 응답을 롤백하면 보상도 함께 롤백해야 한다. */
@Service
class AttendanceSpoonService(private val db: GatheringStore) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun award(gatheringId: Long, unitId: Long, participantId: Long) {
        // 호출부가 단위→술자리를 잠근 뒤 호출한다. 수혜 계정 잠금은 가장 마지막이다.
        val host = db.rows("""
            SELECT host.id participant_id,host.user_id FROM settlement_units unit_entry
            JOIN participants host ON host.id=unit_entry.host_participant_id
            WHERE unit_entry.id=:unit AND unit_entry.gathering_id=:gathering
        """.trimIndent(), "unit" to unitId, "gathering" to gatheringId).single()
        val hostUserId = (host["user_id"] as? Number)?.toLong() ?: return
        if (host.long("participant_id") == participantId) return
        val hasAttendance = db.rows("""
            SELECT DISTINCT response_entry.type,response_entry.source FROM round_responses response_entry
            JOIN rounds round_entry ON round_entry.id=response_entry.round_id
            JOIN settlement_unit_members membership ON membership.settlement_unit_id=round_entry.settlement_unit_id
              AND membership.participant_id=response_entry.participant_id
            WHERE round_entry.settlement_unit_id=:unit AND membership.status='ACTIVE'
              AND response_entry.participant_id=:participant
        """.trimIndent(), "unit" to unitId, "participant" to participantId)
            .any { AttendanceSpoonPolicy.isAttendance(it.text("type"), it.text("source")) }
        if (!hasAttendance) return

        // 서로 다른 술자리도 같은 총무에게 동시에 지급할 수 있다. FK 공유 잠금보다 먼저 X 잠금을 잡는다.
        check(db.rows("SELECT id FROM users WHERE id=:host FOR UPDATE", "host" to hostUserId).size == 1)
        val alreadyAwarded = db.rows("""
            SELECT 1 FROM spoon_attendance_rewards
            WHERE gathering_id=:gathering AND host_user_id=:host AND participant_id=:participant
        """.trimIndent(), "gathering" to gatheringId, "host" to hostUserId, "participant" to participantId).isNotEmpty()
        val amount = AttendanceSpoonPolicy.amount(host.long("participant_id"), participantId, hasAttendance, alreadyAwarded)
        if (amount == 0) return
        db.update("""
            INSERT INTO spoon_attendance_rewards(gathering_id,host_user_id,participant_id,amount,awarded_at)
            VALUES(:gathering,:host,:participant,:amount,:now)
        """.trimIndent(), "gathering" to gatheringId, "host" to hostUserId, "participant" to participantId,
            "amount" to amount, "now" to Instant.now())
        check(db.update("UPDATE users SET spoon_count=spoon_count+:amount WHERE id=:host",
            "amount" to amount, "host" to hostUserId) == 1)
    }
}
