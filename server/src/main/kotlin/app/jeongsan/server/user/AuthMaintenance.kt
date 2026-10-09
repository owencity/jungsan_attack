package app.jeongsan.server.user

import app.jeongsan.server.gathering.GatheringStore
import app.jeongsan.server.gathering.PayoutCipher
import app.jeongsan.server.gathering.long
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

/** 외부 연결 해제 실패가 이미 삭제된 계정을 되살리지 않도록 독립 재시도한다. */
@Component
class AuthMaintenance(private val db: GatheringStore, private val gateway: ProviderGateway, private val tx: TransactionTemplate,
    @Value("\${auth.encryption-key}") key: String) {
    private val cipher=PayoutCipher(key)
    private val log=LoggerFactory.getLogger(javaClass)
    @Scheduled(fixedDelay=60000)
    fun run() {
        tx.executeWithoutResult {
            for(table in listOf("auth_challenges","auth_tickets","auth_revocations"))
                db.update("DELETE FROM $table WHERE expires_at<=:n","n" to Instant.now())
        }
        val jobs=db.rows("SELECT id FROM auth_revoke_jobs WHERE next_attempt_at<=:n ORDER BY id LIMIT 20","n" to Instant.now())
        jobs.forEach { job ->
            tx.executeWithoutResult {
                val row=db.rows("SELECT * FROM auth_revoke_jobs WHERE id=:id AND next_attempt_at<=:n FOR UPDATE SKIP LOCKED","id" to job.long("id"),"n" to Instant.now()).singleOrNull()
                if(row!=null) try {
                    gateway.revokeApple(row["client_id"].toString(),cipher.decrypt(row["token_encrypted"] as ByteArray))
                    db.update("DELETE FROM auth_revoke_jobs WHERE id=:id","id" to job.long("id"))
                } catch (_: Exception) {
                    val attempts=row.long("attempts")+1
                    db.update("UPDATE auth_revoke_jobs SET attempts=:a,next_attempt_at=:n WHERE id=:id","a" to attempts,
                        "n" to Instant.now().plusSeconds(minOf(3600L,60L*attempts)),"id" to job.long("id"))
                    // 제공자 예외에는 토큰·요청 본문이 들어갈 수 있어 작업 ID만 남긴다.
                    log.warn("Apple 연결 해제 재시도 필요: job={}",job.long("id"))
                }
            }
        }
    }
}
