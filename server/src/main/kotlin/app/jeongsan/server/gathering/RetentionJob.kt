package app.jeongsan.server.gathering

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import java.time.Instant

@Component
class RetentionJob(private val db: GatheringStore, private val service: GatheringService) {
    private val log=LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay=60000)
    fun deleteCompleted() {
        val now=Instant.now()
        // 후보 하나의 실패가 다른 술자리의 개인정보 삭제를 막지 않도록 트랜잭션을 방별로 둔다.
        db.rows("SELECT id FROM gatherings WHERE (status='COMPLETED' AND delete_scheduled_at<=:n) OR last_activity_at<=:old ORDER BY id LIMIT 100", "n" to now,"old" to now.minusSeconds(30*86400L)).forEach {
            try { service.deleteExpired(it.long("id"),now) }
            catch(e: Exception) { log.error("완료 술자리 삭제 실패: {}",it["id"],e) }
        }
    }
}
