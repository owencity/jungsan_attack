package app.jeongsan.server.user

import app.jeongsan.server.common.UnauthenticatedException
import app.jeongsan.server.gathering.GatheringStore
import app.jeongsan.server.gathering.long
import app.jeongsan.server.gathering.text
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AccountDeletionService(private val db: GatheringStore) {
    private fun rooms(uid: Long)=db.rows("SELECT DISTINCT g.id FROM gatherings g LEFT JOIN participants p ON p.gathering_id=g.id WHERE g.host_user_id=:u OR p.user_id=:u ORDER BY g.id","u" to uid).map { it.long("id") }
    private fun units(ids: List<Long>)=if(ids.isEmpty()) emptyList() else db.rows("SELECT id FROM settlement_units WHERE gathering_id IN (:g) ORDER BY id","g" to ids).map { it.long("id") }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    fun delete(uid: Long) {
        val gids=rooms(uid); val sids=units(gids)
        sids.forEach { db.rows("SELECT id FROM settlement_units WHERE id=:s FOR UPDATE","s" to it) }
        gids.forEach { db.rows("SELECT id FROM gatherings WHERE id=:g FOR UPDATE","g" to it) }
        val user=db.rows("SELECT * FROM users WHERE id=:u FOR UPDATE","u" to uid).singleOrNull() ?: throw UnauthenticatedException()
        AccountPolicy.unchanged(gids,rooms(uid)); AccountPolicy.unchanged(sids,units(gids))
        if(gids.isNotEmpty()) AccountPolicy.deletable(db.rows("SELECT status FROM gatherings WHERE id IN (:g)","g" to gids).map { it.text("status") })
        if(sids.isNotEmpty()) AccountPolicy.deletable(db.rows("SELECT status FROM settlement_units WHERE id IN (:s)","s" to sids).map { it.text("status") })
        db.rows("SELECT * FROM auth_credentials WHERE user_id=:u","u" to uid).forEach {
            db.insert("INSERT INTO auth_revoke_jobs(client_id,token_encrypted,next_attempt_at) VALUES(:c,:t,:n)","c" to it["client_id"],"t" to it["token_encrypted"],"n" to Instant.now())
        }
        val pids=db.rows("SELECT id FROM participants WHERE user_id=:u","u" to uid).map { it.long("id") }
        if(pids.isNotEmpty()) {
            db.update("DELETE FROM timeline_entries WHERE author_participant_id IN (:p)","p" to pids)
            db.update("UPDATE participants SET user_id=NULL,name='탈퇴한 사용자',payout_bank_name=NULL,payout_account_no=NULL,payout_account_holder=NULL WHERE id IN (:p)","p" to pids)
        }
        // 과거 시스템 소식에는 작성자 FK가 없으므로 탈퇴자의 이름이 포함된 소식·알림을 제거한다.
        val names=listOfNotNull(user["display_name"] as? String,user["nickname"] as? String).filter { it.isNotBlank() }.distinct()
        if(gids.isNotEmpty()) names.forEach { name ->
            db.update("DELETE FROM timeline_entries WHERE gathering_id IN (:g) AND type='SYSTEM' AND LOCATE(:n,body)>0","g" to gids,"n" to name)
            db.update("DELETE FROM notifications WHERE gathering_id IN (:g) AND (LOCATE(:n,body)>0 OR LOCATE(:n,title)>0)","g" to gids,"n" to name)
        }
        db.update("UPDATE gatherings SET host_user_id=NULL,name='완료된 술자리' WHERE host_user_id=:u","u" to uid)
        db.update("DELETE FROM notifications WHERE user_id=:u","u" to uid)
        db.update("DELETE FROM settlement_unit_requests WHERE user_id=:u","u" to uid)
        db.update("DELETE FROM notification_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(payload,'$.userId'))=:u OR JSON_UNQUOTE(JSON_EXTRACT(payload,'$.recipientUserId'))=:u OR JSON_UNQUOTE(JSON_EXTRACT(payload,'$.actorUserId'))=:u","u" to uid.toString())
        if(gids.isNotEmpty()) db.update("DELETE FROM notification_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(payload,'$.gatheringId')) IN (:g)","g" to gids.map { it.toString() })
        db.update("DELETE FROM group_members WHERE user_id=:u","u" to uid)
        db.update("UPDATE user_groups SET created_by_user_id=NULL,name='삭제된 모임' WHERE created_by_user_id=:u","u" to uid)
        db.update("DELETE FROM users WHERE id=:u","u" to uid)
    }
}
