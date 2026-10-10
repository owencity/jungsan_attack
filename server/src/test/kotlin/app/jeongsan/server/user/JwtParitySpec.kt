package app.jeongsan.server.user

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import app.jeongsan.server.user.javaimpl.JwtService as JavaJwt

class JwtParitySpec : StringSpec({
    val secret="01234567890123456789012345678901"
    val k=JwtService(secret,30); val j=JavaJwt(secret,30)
    "WEB APP 토큰은 두 구현에서 상호 검증되고 클라이언트 구분을 유지한다" {
        listOf("WEB","APP").forEach { client ->
            listOf(k.issue(12,client),j.issue(12,client)).forEach { token ->
                val ki=k.identity(token)!!; val ji=j.identity(token)!!
                ki.userId shouldBe ji.userId(); ki.client shouldBe client; ji.client() shouldBe client
                ki.expiresAt shouldBe ji.expiresAt()
            }
        }
    }
    "각 로그인은 같은 사용자여도 다른 JWT를 발급한다" {
        (k.issue(12)==k.issue(12)) shouldBe false
        (j.issue(12,"APP")==j.issue(12,"APP")) shouldBe false
    }
    "만료된 토큰과 다른 키와 깨진 JWT는 양쪽이 거절한다" {
        listOf(JwtService(secret,-1).issue(12),JavaJwt(secret,-1).issue(12,"APP"),JwtService("x".repeat(32),30).issue(12),"broken").forEach {
            k.identity(it) shouldBe null; j.identity(it) shouldBe null
        }
    }
})
