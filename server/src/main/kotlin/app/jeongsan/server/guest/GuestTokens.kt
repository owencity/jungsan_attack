package app.jeongsan.server.guest

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date

data class GuestIdentity(val browserHash: String)

/** 기존 JWT 라이브러리를 쓰는 인증 인프라. 비숫자 subject로 회원 ID와 혼동하지 않는다. */
@Component
class GuestTokens(@Value("\${jwt.secret}") secret: String) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))
    private val random = SecureRandom()

    fun issue(): String {
        val now = Instant.now()
        val bytes = ByteArray(32).also { random.nextBytes(it) }
        val subject = "guest:" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return Jwts.builder().subject(subject).claim("purpose", "GUEST_RESPONSE")
            .issuedAt(Date.from(now)).expiration(Date.from(now.plus(30, ChronoUnit.DAYS)))
            .signWith(key).compact()
    }

    fun identity(token: String?): GuestIdentity? = try {
        if (token == null) null else {
            val claims = Jwts.parser().verifyWith(key).require("purpose", "GUEST_RESPONSE").build()
                .parseSignedClaims(token).payload
            if (!Regex("guest:[A-Za-z0-9_-]{43}").matches(claims.subject) || claims.expiration == null) null
            else GuestIdentity(MessageDigest.getInstance("SHA-256").digest(claims.subject.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) })
        }
    } catch (_: Exception) { null }
}
