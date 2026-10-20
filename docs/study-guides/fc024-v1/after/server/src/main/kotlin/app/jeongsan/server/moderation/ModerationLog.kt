package app.jeongsan.server.moderation

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

data class MessageReported(val entryId: Long, val gatheringId: Long, val reason: String)

/** 롤백된 신고를 접수 완료로 기록하지 않는다. 복구·일일 처리는 DB 미처리 조회가 기준이다. */
@Component
class ModerationLog {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun reported(event: MessageReported) {
        log.info("MESSAGE_REPORTED entryId={} gatheringId={} reason={}", event.entryId, event.gatheringId, event.reason)
    }
}
