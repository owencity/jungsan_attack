package app.jeongsan.server.guest

import app.jeongsan.server.user.JwtService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.Date

class GuestTokensSpec : StringSpec({
    val secret="01234567890123456789012345678901"
    val tokens=GuestTokens(secret)
    val members=JwtService(secret,30)
    "비회원 브라우저는 재조회하면 같은 해시이고 새 발급은 다른 신원이다" {
        val token=tokens.issue()
        val identity=checkNotNull(tokens.identity(token))
        identity.browserHash.length shouldBe 64
        tokens.identity(token) shouldBe identity
        (tokens.identity(tokens.issue())==identity) shouldBe false
    }
    "회원과 비회원 토큰은 같은 서명 키를 써도 서로 인증되지 않는다" {
        tokens.identity(members.issue(42)) shouldBe null
        members.identity(tokens.issue()) shouldBe null
        members.parseUserId(tokens.issue()) shouldBe null
    }
    "위조 만료 잘못된 용도와 만료 시각 없는 자격은 거절한다" {
        val key=Keys.hmacShaKeyFor(secret.toByteArray())
        tokens.identity(null) shouldBe null
        tokens.identity("invalid") shouldBe null
        tokens.identity(tokens.issue()+"tampered") shouldBe null
        fun signed(purpose: String, expiration: Instant?): String {
            val builder=Jwts.builder().subject("guest:"+"a".repeat(43)).claim("purpose",purpose)
            expiration?.let{builder.expiration(Date.from(it))}
            return builder.signWith(key).compact()
        }
        tokens.identity(signed("GUEST_RESPONSE",Instant.now().minusSeconds(1))) shouldBe null
        tokens.identity(signed("WEB",Instant.now().plusSeconds(60))) shouldBe null
        tokens.identity(signed("GUEST_RESPONSE",null)) shouldBe null
    }
})
