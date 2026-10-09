package app.jeongsan.server.user

import app.jeongsan.server.common.UnauthenticatedException
import app.jeongsan.server.gathering.GatheringStore
import app.jeongsan.server.gathering.PayoutCipher
import app.jeongsan.server.gathering.instant
import app.jeongsan.server.gathering.long
import app.jeongsan.server.gathering.text
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

data class AuthStart(val state: String, val browser: String, val location: String)
data class AuthFinish(val client: String, val returnTo: String, val credential: String)
data class AppExchangeRequest(val ticket: String, val codeVerifier: String)
data class AppExchangeResponse(val token: String, val expiresAt: Instant)

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
class AuthFlowService(private val db: GatheringStore, private val gateway: ProviderGateway, private val jwt: JwtService,
    @Value("\${auth.encryption-key}") key: String) {
    // 계좌 암호화와 키는 분리하되 AES GCM 형식은 공용 인프라로 사용한다.
    private val cipher = PayoutCipher(key)
    private fun random() = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
    fun start(provider: String, client: String, returnTo: String?, challenge: String?): AuthStart {
        AuthPolicy.client(client)
        val path=AuthPolicy.returnTo(returnTo); val app=AuthPolicy.challenge(client,challenge)
        val state=random(); val browser=random(); val nonce=random()
        val location=gateway.authorize(provider,state,nonce)
        db.update("INSERT INTO auth_challenges VALUES(:s,:p,:c,:r,:n,:b,:a,:e)","s" to AuthPolicy.hash(state),"p" to provider,
            "c" to client,"r" to path,"n" to nonce,"b" to AuthPolicy.hash(browser),"a" to app,"e" to Instant.now().plusSeconds(300))
        return AuthStart(state,browser,location)
    }
    fun finish(provider: String, state: String, browser: String?, code: String): AuthFinish {
        if(!Regex("[A-Za-z0-9_-]{43}").matches(state) || code.isBlank() || code.length>2048 || browser==null) throw UnauthenticatedException()
        val row=db.rows("SELECT * FROM auth_challenges WHERE state_hash=:s FOR UPDATE","s" to AuthPolicy.hash(state)).singleOrNull() ?: throw UnauthenticatedException()
        AuthPolicy.validUntil(checkNotNull(row.instant("expires_at")),Instant.now())
        if(row.text("provider")!=provider || row.text("browser_hash")!=AuthPolicy.hash(browser)) throw UnauthenticatedException()
        val identity=gateway.identity(provider,code,row.text("nonce"))
        if(identity.id.length !in 1..100) throw UnauthenticatedException()
        db.update("INSERT INTO users(provider,provider_id,nickname,profile_image_url,created_at) VALUES(:p,:id,:n,:i,:t) ON DUPLICATE KEY UPDATE nickname=VALUES(nickname),profile_image_url=VALUES(profile_image_url)",
            "p" to provider,"id" to identity.id,"n" to identity.nickname.take(50),"i" to identity.image,"t" to Instant.now())
        val uid=db.rows("SELECT id FROM users WHERE provider=:p AND provider_id=:id","p" to provider,"id" to identity.id).single().long("id")
        if(provider=="APPLE") {
            if(identity.refreshToken!=null) db.update("INSERT INTO auth_credentials VALUES(:u,:c,:t) ON DUPLICATE KEY UPDATE client_id=VALUES(client_id),token_encrypted=VALUES(token_encrypted)",
                "u" to uid,"c" to identity.clientId,"t" to cipher.encrypt(identity.refreshToken))
            else if(db.rows("SELECT user_id FROM auth_credentials WHERE user_id=:u","u" to uid).isEmpty()) throw UnauthenticatedException()
        }
        db.update("DELETE FROM auth_challenges WHERE state_hash=:s","s" to AuthPolicy.hash(state))
        if(row.text("client")=="web") return AuthFinish("web",row.text("return_to"),jwt.issue(uid))
        val ticket=random()
        db.update("INSERT INTO auth_tickets VALUES(:t,:u,:a,:e)","t" to AuthPolicy.hash(ticket),"u" to uid,"a" to row.text("app_challenge"),"e" to Instant.now().plusSeconds(60))
        return AuthFinish("app",row.text("return_to"),ticket)
    }
    fun exchange(request: AppExchangeRequest): AppExchangeResponse {
        if(!Regex("[A-Za-z0-9_-]{43}").matches(request.ticket)) throw UnauthenticatedException()
        val hash=AuthPolicy.hash(request.ticket)
        val owner=db.rows("SELECT user_id FROM auth_tickets WHERE ticket_hash=:t","t" to hash).singleOrNull() ?: throw UnauthenticatedException()
        // 탈퇴의 users→티켓 CASCADE와 반대 잠금을 잡지 않는다.
        val uid=owner.long("user_id")
        if(db.rows("SELECT id FROM users WHERE id=:u FOR SHARE","u" to uid).isEmpty()) throw UnauthenticatedException()
        val row=db.rows("SELECT * FROM auth_tickets WHERE ticket_hash=:t FOR UPDATE","t" to hash).singleOrNull() ?: throw UnauthenticatedException()
        AuthPolicy.validUntil(checkNotNull(row.instant("expires_at")),Instant.now())
        AuthPolicy.verifyVerifier(request.codeVerifier,row.text("app_challenge"))
        db.update("DELETE FROM auth_tickets WHERE ticket_hash=:t","t" to AuthPolicy.hash(request.ticket))
        val token=jwt.issue(uid,"APP")
        return AppExchangeResponse(token,checkNotNull(jwt.identity(token)).expiresAt)
    }
    @Transactional(readOnly=true)
    fun authenticated(token: String, client: String): Long {
        val who=jwt.identity(token) ?: throw UnauthenticatedException()
        if(who.client!=client || db.rows("SELECT id FROM users WHERE id=:u","u" to who.userId).isEmpty() ||
            db.rows("SELECT token_hash FROM auth_revocations WHERE token_hash=:h AND expires_at>:n","h" to AuthPolicy.hash(token),"n" to Instant.now()).isNotEmpty())
            throw UnauthenticatedException()
        return who.userId
    }
    fun logout(token: String) {
        val who=jwt.identity(token) ?: throw UnauthenticatedException()
        db.update("INSERT INTO auth_revocations VALUES(:h,:e) ON DUPLICATE KEY UPDATE expires_at=VALUES(expires_at)","h" to AuthPolicy.hash(token),"e" to who.expiresAt)
    }
}
