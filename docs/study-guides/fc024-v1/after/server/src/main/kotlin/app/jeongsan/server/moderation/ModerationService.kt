package app.jeongsan.server.moderation

import app.jeongsan.server.common.ApiException
import app.jeongsan.server.common.MalformedRequestException
import app.jeongsan.server.common.NotFoundException
import app.jeongsan.server.common.UnauthenticatedException
import app.jeongsan.server.gathering.GatheringStore
import app.jeongsan.server.gathering.instant
import app.jeongsan.server.gathering.long
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** 스키마의 진실은 019-ugc-safety.yaml. 정산 데이터와 분리한 메시지 안전 처리. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
class ModerationService(private val db: GatheringStore, private val events: ApplicationEventPublisher) {
    private val forbiddenWords = ClassPathResource("moderation/forbidden-words.txt").inputStream
        .bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        }.also { require(it.isNotEmpty()) { "메시지 금칙어 목록이 비어 있습니다." } }

    private fun lockGathering(gatheringId: Long) {
        if (db.rows("SELECT id FROM gatherings WHERE id=:id FOR UPDATE", "id" to gatheringId).isEmpty())
            throw NotFoundException()
    }

    private fun participant(gatheringId: Long, userId: Long): Long =
        db.rows("SELECT id FROM participants WHERE gathering_id=:gatheringId AND user_id=:userId AND status='ACTIVE'",
            "gatheringId" to gatheringId, "userId" to userId).singleOrNull()?.long("id")
            ?: throw ApiException("NOT_PARTICIPANT", HttpStatus.FORBIDDEN, "술자리 참여자만 사용할 수 있어요")

    private fun user(userId: Long, lock: Boolean = false): Map<String, Any?> =
        db.rows("SELECT id,message_banned_at FROM users WHERE id=:id" + if (lock) " FOR SHARE" else "",
            "id" to userId).singleOrNull() ?: throw UnauthenticatedException()

    fun report(gatheringId: Long, entryId: Long, reporterUserId: Long, reason: String) {
        // 기존 만료·탈퇴의 Gathering→users 순서와 맞추고 같은 방의 신고 경합을 직렬화한다.
        lockGathering(gatheringId)
        user(reporterUserId, true)
        val reporterParticipantId = participant(gatheringId, reporterUserId)
        val entry = db.rows("SELECT type,author_participant_id FROM timeline_entries WHERE id=:entryId AND gathering_id=:gatheringId FOR UPDATE",
            "entryId" to entryId, "gatheringId" to gatheringId).singleOrNull() ?: throw NotFoundException()
        val authorParticipantId = (entry["author_participant_id"] as? Number)?.toLong()
        if (!ModerationPolicy.reportAllowed(entry["type"].toString(), authorParticipantId, reporterParticipantId, reason))
            throw MalformedRequestException("다른 참여자의 메시지만 신고할 수 있어요")
        if (db.rows("SELECT id FROM message_reports WHERE entry_id=:entryId AND reporter_user_id=:userId",
                "entryId" to entryId, "userId" to reporterUserId).isNotEmpty()) return

        val now = Instant.now()
        db.insert("INSERT INTO message_reports(entry_id,reporter_user_id,reason,created_at) VALUES(:entryId,:userId,:reason,:now)",
            "entryId" to entryId, "userId" to reporterUserId, "reason" to reason, "now" to now)
        // 각 행은 유일한 신고자에게서 생성됐다. 탈퇴로 ID가 NULL이 되어도 이미 접수한 신고는 센다.
        val reporterCount = db.rows("SELECT COUNT(*) reporter_count FROM message_reports WHERE entry_id=:entryId",
            "entryId" to entryId).single().long("reporter_count")
        if (ModerationPolicy.hideForEveryone(reporterCount))
            db.update("UPDATE timeline_entries SET hidden_at=COALESCE(hidden_at,:now) WHERE id=:entryId",
                "now" to now, "entryId" to entryId)
        events.publishEvent(MessageReported(entryId, gatheringId, reason))
    }

    fun block(blockerUserId: Long, request: UserBlockRequest) {
        lockGathering(request.gatheringId)
        val blockerParticipantId = participant(request.gatheringId, blockerUserId)
        val target = db.rows("SELECT user_id FROM participants WHERE id=:participantId AND gathering_id=:gatheringId AND status='ACTIVE'",
            "participantId" to request.participantId, "gatheringId" to request.gatheringId).singleOrNull()
            ?: throw MalformedRequestException("해당 술자리의 참여자를 선택해주세요")
        val blockedUserId = (target["user_id"] as? Number)?.toLong()
        if (blockerParticipantId == request.participantId || !ModerationPolicy.blockAllowed(blockerUserId, blockedUserId))
            throw MalformedRequestException("자신이나 탈퇴한 사용자는 차단할 수 없어요")
        // 두 계정의 삭제와 차단 FK 입력을 교차하지 않도록 고정된 순서로 확인한다.
        val users = db.rows("SELECT id FROM users WHERE id IN (:ids) ORDER BY id FOR SHARE",
            "ids" to listOf(blockerUserId, checkNotNull(blockedUserId)).sorted())
        if (users.size != 2) throw MalformedRequestException("탈퇴한 사용자는 차단할 수 없어요")
        db.update("INSERT INTO user_blocks(blocker_user_id,blocked_user_id,created_at) VALUES(:blocker,:blocked,:now) ON DUPLICATE KEY UPDATE created_at=created_at",
            "blocker" to blockerUserId, "blocked" to blockedUserId, "now" to Instant.now())
    }

    @Transactional(readOnly = true)
    fun blocks(blockerUserId: Long): List<UserBlockResponse> {
        user(blockerUserId)
        return db.rows("SELECT b.blocked_user_id,COALESCE(u.display_name,u.nickname) display_name,b.created_at FROM user_blocks b JOIN users u ON u.id=b.blocked_user_id WHERE b.blocker_user_id=:userId ORDER BY b.created_at DESC,b.blocked_user_id",
            "userId" to blockerUserId).map { row ->
            UserBlockResponse(row.long("blocked_user_id"), row["display_name"].toString(), checkNotNull(row.instant("created_at")))
        }
    }

    fun unblock(blockerUserId: Long, blockedUserId: Long) {
        user(blockerUserId, true)
        db.update("DELETE FROM user_blocks WHERE blocker_user_id=:blocker AND blocked_user_id=:blocked",
            "blocker" to blockerUserId, "blocked" to blockedUserId)
    }

    fun checkMessage(userId: Long, text: String) {
        val violation = ModerationPolicy.messageViolation(text, user(userId, true)["message_banned_at"] != null, forbiddenWords)
        when (violation) {
            "MESSAGE_BANNED" -> throw ApiException(violation, HttpStatus.FORBIDDEN, "메시지를 보낼 수 없는 계정이에요")
            "MESSAGE_NOT_ALLOWED" -> throw ApiException(violation, HttpStatus.UNPROCESSABLE_ENTITY, "부적절한 표현이 있어요")
            "MALFORMED_REQUEST" -> throw MalformedRequestException("메시지는 1~200자까지 입력해주세요")
        }
    }
}
