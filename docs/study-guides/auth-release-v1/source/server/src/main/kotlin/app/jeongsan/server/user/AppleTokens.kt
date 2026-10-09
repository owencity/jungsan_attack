package app.jeongsan.server.user

import app.jeongsan.server.common.UnauthenticatedException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.jsonwebtoken.Jwts
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.time.Instant
import java.util.Base64
import java.util.Date

data class AppleKey(val kid: String, val kty: String, val alg: String, val n: String, val e: String)

object AppleTokens {
    fun verify(token: String, keys: List<AppleKey>, audience: String, nonce: String): String = try {
        require(token.length <= 16000)
        val header = jacksonObjectMapper().readTree(Base64.getUrlDecoder().decode(token.split('.')[0]))
        require(header.path("alg").asText() == "RS256")
        val key = keys.single { it.kid == header.path("kid").asText() && it.alg == "RS256" && it.kty == "RSA" }
        val rsa = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(
            BigInteger(1, Base64.getUrlDecoder().decode(key.n)), BigInteger(1, Base64.getUrlDecoder().decode(key.e)))) as java.security.interfaces.RSAPublicKey
        val claims = Jwts.parser().verifyWith(rsa).requireIssuer("https://appleid.apple.com")
            .requireAudience(audience).require("nonce", nonce).build().parseSignedClaims(token).payload
        require(claims.expiration != null && claims.subject != null && claims.subject.length in 1..100)
        claims.subject
    } catch (_: Exception) { throw UnauthenticatedException() }

    fun clientSecret(team: String, keyId: String, client: String, keyBase64: String, now: Instant): String {
        val key = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyBase64))) as ECPrivateKey
        return Jwts.builder().header().keyId(keyId).and().issuer(team).subject(client)
            .audience().add("https://appleid.apple.com").and().issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(300))).signWith(key, Jwts.SIG.ES256).compact()
    }
}
