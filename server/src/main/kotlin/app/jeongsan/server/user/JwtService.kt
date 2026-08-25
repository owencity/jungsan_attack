package app.jeongsan.server.user

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.SecretKey

/**
 * 세션을 안 쓰기로 했다(ADR-013 의 여러 서비스 방향과 공유 세션 스토어가 안 맞아서) —
 * 그래서 로그인 결과를 우리 자체 JWT 하나로 표현한다. 클레임은 subject(userId) 하나뿐이다 —
 * 닉네임 등은 위조돼도 상관없는 값이 아니라서 토큰에 안 싣고, 매번 `/auth/me` 로 DB에서 읽는다.
 */
@Component
class JwtService(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiration-days}") private val expirationDays: Long,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))

    fun issue(userId: Long): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expirationDays, ChronoUnit.DAYS)))
            .signWith(key)
            .compact()
    }

    /** 서명이 안 맞거나 만료됐으면 null — 호출부가 "로그인 안 됨"으로 취급한다. */
    fun parseUserId(token: String): Long? =
        try {
            Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).payload.subject.toLong()
        } catch (e: Exception) {
            null
        }
}
