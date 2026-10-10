package app.jeongsan.server.user.javaimpl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/** Bean은 등록하지 않고 Kotlin JWT와 상호 검증하는 학습용 구현이다. */
public final class JwtService {
    private final SecretKey key;
    private final long expirationDays;
    public record Identity(long userId, String client, Instant expiresAt) {}
    public JwtService(String secret, long expirationDays) {
        this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationDays=expirationDays;
    }
    public String issue(long userId, String client) {
        var now=Instant.now();
        return Jwts.builder().subject(Long.toString(userId)).claim("client",client).id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now)).expiration(Date.from(now.plus(expirationDays,ChronoUnit.DAYS))).signWith(key).compact();
    }
    public Identity identity(String token) {
        try {
            var claims=Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return new Identity(Long.parseLong(claims.getSubject()),claims.get("client",String.class)==null?"WEB":claims.get("client",String.class),claims.getExpiration().toInstant());
        } catch (Exception e) { return null; }
    }
}
