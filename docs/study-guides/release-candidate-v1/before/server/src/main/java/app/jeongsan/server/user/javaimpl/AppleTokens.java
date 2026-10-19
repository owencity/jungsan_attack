package app.jeongsan.server.user.javaimpl;

import app.jeongsan.server.common.UnauthenticatedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public final class AppleTokens {
    private AppleTokens() {}
    public record Key(String kid, String kty, String alg, String n, String e) {}
    public static String verify(String token, List<Key> keys, String audience, String nonce) {
        try {
            if (token.length() > 16000) throw new IllegalArgumentException();
            var header = new ObjectMapper().readTree(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
            if (!"RS256".equals(header.path("alg").asText())) throw new IllegalArgumentException();
            var matching = keys.stream().filter(k -> k.kid().equals(header.path("kid").asText()) && "RS256".equals(k.alg()) && "RSA".equals(k.kty())).toList();
            if (matching.size() != 1) throw new IllegalArgumentException();
            var k = matching.getFirst();
            var rsa = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                new BigInteger(1, Base64.getUrlDecoder().decode(k.n())), new BigInteger(1, Base64.getUrlDecoder().decode(k.e()))));
            var claims = Jwts.parser().verifyWith(rsa).requireIssuer("https://appleid.apple.com")
                .requireAudience(audience).require("nonce", nonce).build().parseSignedClaims(token).getPayload();
            if (claims.getExpiration() == null || claims.getSubject() == null || claims.getSubject().isEmpty() || claims.getSubject().length() > 100)
                throw new IllegalArgumentException();
            return claims.getSubject();
        } catch (Exception e) { throw new UnauthenticatedException(); }
    }
    public static String clientSecret(String team, String keyId, String client, String keyBase64, Instant now) {
        try {
            var key = (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyBase64)));
            return Jwts.builder().header().keyId(keyId).and().issuer(team).subject(client)
                .audience().add("https://appleid.apple.com").and().issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300))).signWith(key, Jwts.SIG.ES256).compact();
        } catch (Exception e) { throw new IllegalArgumentException("Apple 키 설정 오류", e); }
    }
}
