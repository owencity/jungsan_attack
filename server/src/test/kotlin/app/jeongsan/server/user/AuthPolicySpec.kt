package app.jeongsan.server.user

import app.jeongsan.server.common.ApiException
import app.jeongsan.server.user.javaimpl.AuthPolicy as JavaPolicy
import app.jeongsan.server.user.javaimpl.AccountPolicy as JavaAccount
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class AuthPolicySpec : StringSpec({
    fun code(block: () -> Any?): String = try { block(); "OK" } catch(e: ApiException) { e.code }
    "웹 복귀 경로와 인코딩 우회가 양쪽 언어에서 같은 결과를 낸다" {
        listOf(null,"/jungsan/j/abc","/jungsan/?tab=me","https://evil.test","//evil.test","/jungsan//evil.test",
            "/jungsan/\\evil","/jungsan/%2f%2fevil","/jungsan/\r\nLocation:evil","/jungsan/"+"x".repeat(500)).forEach {
            val k=code{AuthPolicy.returnTo(it)}; k shouldBe code{JavaPolicy.returnTo(it)}
            if(k=="OK") AuthPolicy.returnTo(it) shouldBe JavaPolicy.returnTo(it)
        }
    }
    "잘못된 Bearer는 유효한 쿠키로 우회하지 않는다" {
        listOf("Basic abc","Bearer ","Bearer x y","Bearer \u00a0","").forEach { h ->
            code{AuthPolicy.token(h,"cookie")} shouldBe "UNAUTHENTICATED"
            code{JavaPolicy.token(h,"cookie")} shouldBe "UNAUTHENTICATED"
        }
        AuthPolicy.token("bearer abc","cookie") shouldBe ("abc" to "APP")
        JavaPolicy.token("bearer abc","cookie").client() shouldBe "APP"
        AuthPolicy.token(null,"cookie") shouldBe ("cookie" to "WEB")
        JavaPolicy.token(null,"cookie").client() shouldBe "WEB"
        code{AuthPolicy.token(null,"\u00a0")} shouldBe "UNAUTHENTICATED"
        code{JavaPolicy.token(null,"\u00a0")} shouldBe "UNAUTHENTICATED"
    }
    "앱 verifier는 RFC 7636 S256 예제와 일치하고 다른 앱 값은 거절한다" {
        val verifier="dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val challenge="E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
        code{AuthPolicy.verifyVerifier(verifier,challenge)} shouldBe "OK"
        code{JavaPolicy.verifyVerifier(verifier,challenge)} shouldBe "OK"
        listOf("wrong","x".repeat(43),verifier+" ").forEach {
            code{AuthPolicy.verifyVerifier(it,challenge)} shouldBe "UNAUTHENTICATED"
            code{JavaPolicy.verifyVerifier(it,challenge)} shouldBe "UNAUTHENTICATED"
        }
        AuthPolicy.hash(verifier) shouldBe JavaPolicy.hash(verifier)
    }
    "티켓 만료 경계는 정확한 만료 시각부터 거절한다" {
        val now=Instant.parse("2026-10-09T00:00:00Z")
        listOf(-1L,0L,1L).forEach { offset ->
            val expected=if(offset>0) "OK" else "UNAUTHENTICATED"
            code{AuthPolicy.validUntil(now.plusSeconds(offset),now)} shouldBe expected
            code{JavaPolicy.validUntil(now.plusSeconds(offset),now)} shouldBe expected
        }
    }
    "짧은 verifier는 해시가 일치해도 RFC 최소 길이에 미달하면 거절한다" {
        val verifier="v".repeat(42)
        val hash=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
        code{AuthPolicy.verifyVerifier(verifier,hash)} shouldBe "UNAUTHENTICATED"
        code{JavaPolicy.verifyVerifier(verifier,hash)} shouldBe "UNAUTHENTICATED"
    }
    "진행 중 술자리 하나라도 있으면 탈퇴를 거절하고 완료만 있으면 허용한다" {
        listOf("OPEN","SETTLING","COLLECTING","CONFIRMED").forEach {
            code{AccountPolicy.deletable(listOf("COMPLETED",it))} shouldBe "ACTIVE_GATHERING_EXISTS"
            code{JavaAccount.deletable(listOf("COMPLETED",it))} shouldBe "ACTIVE_GATHERING_EXISTS"
        }
        code{AccountPolicy.deletable(listOf("COMPLETED"))} shouldBe "OK"
        code{JavaAccount.deletable(emptyList())} shouldBe "OK"
    }
    "잠금 도중 참여·단위 목록이 바뀌면 계정을 삭제하지 않는다" {
        code{AccountPolicy.unchanged(listOf(1),listOf(1,2))} shouldBe "ACCOUNT_STATE_CHANGED"
        code{JavaAccount.unchanged(listOf(1),listOf(1,2))} shouldBe "ACCOUNT_STATE_CHANGED"
    }
    "알 수 없는 상태를 완료로 간주해 계정을 지우지 않는다" {
        code{AccountPolicy.deletable(listOf("UNKNOWN"))} shouldBe "ACCOUNT_STATE_CHANGED"
        code{JavaAccount.deletable(listOf("UNKNOWN"))} shouldBe "ACCOUNT_STATE_CHANGED"
    }
})
