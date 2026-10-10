package app.jeongsan.server.user

import app.jeongsan.server.common.UnauthenticatedException
import app.jeongsan.server.user.javaimpl.AppleTokens as JavaApple
import io.jsonwebtoken.Jwts
import io.kotest.core.spec.style.StringSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.security.interfaces.ECPublicKey
import java.time.Instant
import java.util.Base64
import java.util.Date

class AppleTokensSpec : StringSpec({
    val pair=KeyPairGenerator.getInstance("RSA").apply{initialize(2048)}.generateKeyPair()
    val rsa=pair.public as RSAPublicKey
    fun number(value: java.math.BigInteger)=Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray().let { if(it[0]==0.toByte()) it.copyOfRange(1,it.size) else it })
    val key=AppleKey("test-key","RSA","RS256",number(rsa.modulus),number(rsa.publicExponent))
    val jkey=JavaApple.Key(key.kid,key.kty,key.alg,key.n,key.e)
    fun token(aud: String="client",nonce: String="nonce",issuer: String="https://appleid.apple.com",expiry: Instant=Instant.now().plusSeconds(60),kid: String="test-key")=
        Jwts.builder().header().keyId(kid).and().issuer(issuer).subject("apple-subject").audience().add(aud).and()
            .claim("nonce",nonce).expiration(Date.from(expiry)).signWith(pair.private,Jwts.SIG.RS256).compact()
    fun reject(raw: String) {
        shouldThrow<UnauthenticatedException>{AppleTokens.verify(raw,listOf(key),"client","nonce")}
        shouldThrow<UnauthenticatedException>{JavaApple.verify(raw,listOf(jkey),"client","nonce")}
    }
    "Apple RS256 서명을 양쪽 언어에서 검증한다" {
        AppleTokens.verify(token(),listOf(key),"client","nonce") shouldBe "apple-subject"
        JavaApple.verify(token(),listOf(jkey),"client","nonce") shouldBe "apple-subject"
    }
    "이전에 동의한 이메일이 ID 토큰에 있어도 양쪽 언어는 사용자 식별자만 반환한다" {
        val identityToken = Jwts.builder().header().keyId("test-key").and().issuer("https://appleid.apple.com")
            .subject("apple-subject").audience().add("client").and().claim("nonce", "nonce")
            .claim("email", "unused@example.test").claim("email_verified", true)
            .expiration(Date.from(Instant.now().plusSeconds(60))).signWith(pair.private, Jwts.SIG.RS256).compact()
        AppleTokens.verify(identityToken, listOf(key), "client", "nonce") shouldBe "apple-subject"
        JavaApple.verify(identityToken, listOf(jkey), "client", "nonce") shouldBe "apple-subject"
    }
    "다른 앱 audience와 다른 nonce와 issuer를 거절한다" {
        reject(token(aud="other")); reject(token(nonce="other")); reject(token(issuer="https://evil.test"))
    }
    "만료 토큰과 모르는 kid와 깨진 서명을 거절한다" {
        reject(token(expiry=Instant.now().minusSeconds(60))); reject(token(kid="missing"))
        val raw=token(); val chunks=raw.split('.'); val signature=Base64.getUrlDecoder().decode(chunks[2]); signature[0]=(signature[0].toInt() xor 1).toByte()
        reject(chunks.take(2).joinToString(".")+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(signature))
    }
    "HMAC 알고리즘으로 바꾼 Apple 토큰은 서명 검증 전에 거절한다" {
        reject(Jwts.builder().header().keyId("test-key").and().subject("apple-subject").signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(ByteArray(32))).compact())
    }
    "ES256 client secret의 팀·대상·5분 만료는 양쪽 언어가 동일하다" {
        val ec=KeyPairGenerator.getInstance("EC").apply{initialize(java.security.spec.ECGenParameterSpec("secp256r1"))}.generateKeyPair()
        val encoded=Base64.getEncoder().encodeToString(ec.private.encoded); val now=Instant.now()
        val raw=listOf(AppleTokens.clientSecret("TEAM","KEY","client",encoded,now),JavaApple.clientSecret("TEAM","KEY","client",encoded,now))
        raw.forEach {
            val parsed=Jwts.parser().verifyWith(ec.public as ECPublicKey).build().parseSignedClaims(it)
            parsed.header.algorithm shouldBe "ES256"; parsed.header.keyId shouldBe "KEY"
            parsed.payload.issuer shouldBe "TEAM"; parsed.payload.subject shouldBe "client"
            parsed.payload.audience shouldBe setOf("https://appleid.apple.com")
            parsed.payload.expiration.toInstant().epochSecond shouldBe now.plusSeconds(300).epochSecond
        }
    }
})
