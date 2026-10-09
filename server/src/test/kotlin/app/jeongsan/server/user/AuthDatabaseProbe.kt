package app.jeongsan.server.user

import app.jeongsan.server.common.ApiException
import app.jeongsan.server.common.UnauthenticatedException
import app.jeongsan.server.gathering.GatheringStore
import app.jeongsan.server.gathering.PayoutCipher
import app.jeongsan.server.gathering.long
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.TransactionTemplate
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Callable

private const val AUTH_KEY="AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="

class ProbeGateway : ProviderGateway {
    val subject=UUID.randomUUID().toString()
    var calls=0; var revokeCalls=0
    override fun authorize(provider: String,state: String,nonce: String)="https://provider.test/?state=$state"
    override fun identity(provider: String,code: String,nonce: String): ProviderIdentity {
        calls++
        if(code!="good") throw UnauthenticatedException()
        return ProviderIdentity(subject,"테스트",null,if(provider=="APPLE") "test-refresh" else null,if(provider=="APPLE") "test-client" else null)
    }
    override fun revokeApple(clientId: String,token: String) { revokeCalls++; check(clientId=="test-client"&&token=="test-refresh"); if(revokeCalls==1) error("retry") }
}
@Configuration
@EnableTransactionManagement
class AuthProbeConfig {
    @Bean fun source()=DriverManagerDataSource("jdbc:mysql://127.0.0.1:13306/jeongsan_auth_test?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC","root","local-v4-test")
    @Bean fun transactionManager()=DataSourceTransactionManager(source())
    @Bean fun db()=GatheringStore(source())
    @Bean fun gateway()=ProbeGateway()
    @Bean fun jwt()=JwtService("local-only-dev-secret-please-change-32b",30)
    @Bean fun flow()=AuthFlowService(db(),gateway(),jwt(),AUTH_KEY)
    @Bean fun deletion()=AccountDeletionService(db())
}

/** 서버 jar가 전용 DB에 migration을 적용한 뒤 실행한다. 외부 제공자만 가짜이며 DB·트랜잭션은 실제다. */
fun main() {
    AnnotationConfigApplicationContext(AuthProbeConfig::class.java).use { context ->
        val db=context.getBean(GatheringStore::class.java); val flow=context.getBean(AuthFlowService::class.java)
        val gateway=context.getBean(ProbeGateway::class.java); val deletion=context.getBean(AccountDeletionService::class.java)
        val jwt=context.getBean(JwtService::class.java)
        check(db.rows("SELECT DATABASE() d").single()["d"]=="jeongsan_auth_test")
        var count=0
        fun passed(label: String) { count++; println("PASS $count: $label") }
        fun rejects(code: String,action: ()->Unit) { try { action(); error("expected $code") } catch(e: ApiException) { check(e.code==code) {e.code} } }
        val verifier="a".repeat(43)
        val challenge=Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
        val start=flow.start("KAKAO","app",null,challenge)
        rejects("UNAUTHENTICATED") { flow.finish("KAKAO",start.state,null,"good") }
        rejects("UNAUTHENTICATED") { flow.finish("APPLE",start.state,start.browser,"good") }
        check(gateway.calls==0); passed("상관 쿠키·provider 불일치는 외부 교환 전 거절")
        rejects("UNAUTHENTICATED") { flow.finish("KAKAO",start.state,start.browser,"bad") }
        check(db.rows("SELECT state_hash FROM auth_challenges WHERE state_hash=:h","h" to AuthPolicy.hash(start.state)).size==1)
        passed("외부 인가 실패 시 상태 소비·가입을 rollback")
        val finish=flow.finish("KAKAO",start.state,start.browser,"good")
        rejects("UNAUTHENTICATED") { flow.finish("KAKAO",start.state,start.browser,"good") }; passed("완료된 OAuth state 재사용 거절")
        rejects("UNAUTHENTICATED") { flow.exchange(AppExchangeRequest(finish.credential,"b".repeat(43))) }; passed("탈취 티켓은 verifier 불일치로 거절")
        val pool=Executors.newFixedThreadPool(4)
        val results=try { pool.invokeAll((1..4).map { Callable { try { flow.exchange(AppExchangeRequest(finish.credential,verifier)) } catch(e: ApiException) { check(e.code=="UNAUTHENTICATED"); null } } }).map { it.get() } } finally { pool.shutdown() }
        check(results.count {it!=null}==1); val app=results.filterNotNull().single(); val uid=jwt.identity(app.token)!!.userId
        passed("동시 앱 티켓 교환 4건 중 정확히 1건 성공")
        check(flow.authenticated(app.token,"APP")==uid)
        rejects("UNAUTHENTICATED") {flow.authenticated(app.token,"WEB")}; passed("DB 검증도 APP/WEB 경계를 유지")
        flow.logout(app.token); rejects("UNAUTHENTICATED") {flow.authenticated(app.token,"APP")}; passed("로그아웃 JWT 복사본 재사용 차단")
        val expired=flow.start("KAKAO","web",null,null)
        // VM과 호스트 시계의 몇 초 차이를 만료 판정 오류로 오인하지 않도록 서버 시계를 사용한다.
        db.update("UPDATE auth_challenges SET expires_at=:n WHERE state_hash=:h","n" to Instant.now().minusSeconds(60),"h" to AuthPolicy.hash(expired.state))
        rejects("UNAUTHENTICATED") {flow.finish("KAKAO",expired.state,expired.browser,"good")}; passed("DB 만료 state 거절")
        val ticket="t".repeat(43)
        db.update("INSERT INTO auth_tickets VALUES(:h,:u,:c,:n)","h" to AuthPolicy.hash(ticket),"u" to uid,"c" to challenge,"n" to Instant.now().minusSeconds(1))
        rejects("UNAUTHENTICATED") {flow.exchange(AppExchangeRequest(ticket,verifier))}; passed("DB 만료 티켓 거절")
        // 실제 FK 자료를 만든 뒤 D7 거절과 완료 익명화를 검증한다.
        val other=db.insert("INSERT INTO users(provider,provider_id,nickname,display_name,created_at) VALUES('PROBE',:p,'남은사람','남은사람',UTC_TIMESTAMP())","p" to UUID.randomUUID().toString())
        val gid=db.insert("INSERT INTO gatherings(name,host_user_id,gathering_date,status,share_token,expected_count,created_at,last_activity_at) VALUES('검증',:u,CURRENT_DATE(),'OPEN',:s,0,UTC_TIMESTAMP(),UTC_TIMESTAMP())","u" to uid,"s" to UUID.randomUUID().toString().take(12))
        val pid=db.insert("INSERT INTO participants(gathering_id,user_id,name,status,created_at) VALUES(:g,:u,'테스트','ACTIVE',UTC_TIMESTAMP())","g" to gid,"u" to uid)
        db.insert("INSERT INTO participants(gathering_id,user_id,name,status,created_at) VALUES(:g,:u,'남은사람','ACTIVE',UTC_TIMESTAMP())","g" to gid,"u" to other)
        val sid=db.insert("INSERT INTO settlement_units(gathering_id,host_participant_id,created_at) VALUES(:g,:p,UTC_TIMESTAMP())","g" to gid,"p" to pid)
        rejects("ACTIVE_GATHERING_EXISTS") {deletion.delete(uid)}
        check(db.rows("SELECT id FROM users WHERE id=:u","u" to uid).size==1); passed("진행 중 탈퇴 거절은 계정과 FK를 보존")
        db.update("UPDATE gatherings SET status='COMPLETED' WHERE id=:g","g" to gid)
        db.update("UPDATE settlement_units SET status='COMPLETED' WHERE id=:s","s" to sid)
        db.insert("INSERT INTO timeline_entries(gathering_id,type,author_participant_id,body,created_at) VALUES(:g,'MESSAGE',:p,'개인글',UTC_TIMESTAMP())","g" to gid,"p" to pid)
        val web=jwt.issue(uid)
        deletion.delete(uid)
        check(db.rows("SELECT id FROM users WHERE id=:u","u" to uid).isEmpty())
        val anonymous=db.rows("SELECT * FROM participants WHERE id=:p","p" to pid).single()
        check(anonymous["user_id"]==null&&anonymous["name"]=="탈퇴한 사용자")
        check(db.rows("SELECT host_user_id FROM gatherings WHERE id=:g","g" to gid).single()["host_user_id"]==null)
        check(db.rows("SELECT id FROM timeline_entries WHERE author_participant_id=:p","p" to pid).isEmpty()); passed("완료 좌석·총무 FK 익명화, 계정·본인 글 삭제")
        rejects("UNAUTHENTICATED") {flow.authenticated(web,"WEB")}; passed("탈퇴 전 유효 JWT도 즉시 거절")
        val appleStart=flow.start("APPLE","web",null,null); flow.finish("APPLE",appleStart.state,appleStart.browser,"good")
        val appleUid=db.rows("SELECT id FROM users WHERE provider='APPLE' AND provider_id=:p","p" to gateway.subject).single().long("id")
        val encrypted=db.rows("SELECT token_encrypted FROM auth_credentials WHERE user_id=:u","u" to appleUid).single()["token_encrypted"] as ByteArray
        check(!encrypted.contentEquals("test-refresh".toByteArray())); passed("Apple refresh token은 암호문으로 저장")
        deletion.delete(appleUid)
        val job=db.rows("SELECT id FROM auth_revoke_jobs WHERE client_id='test-client' ORDER BY id DESC").first().long("id")
        val maintenance=AuthMaintenance(db,gateway,TransactionTemplate(context.getBean(DataSourceTransactionManager::class.java)),AUTH_KEY)
        maintenance.run()
        check(db.rows("SELECT attempts FROM auth_revoke_jobs WHERE id=:i","i" to job).single().long("attempts")==1L); passed("Apple 연결 해제 실패는 계정 복구 없이 재시도 보존")
        db.update("UPDATE auth_revoke_jobs SET next_attempt_at=:n WHERE id=:i","n" to Instant.now().minusSeconds(60),"i" to job)
        maintenance.run(); check(db.rows("SELECT id FROM auth_revoke_jobs WHERE id=:i","i" to job).isEmpty()); passed("Apple 연결 해제 성공 시 암호화 작업도 삭제")
        println("AUTH_DATABASE_PROBE checks=$count database=jeongsan_auth_test")
    }
}
