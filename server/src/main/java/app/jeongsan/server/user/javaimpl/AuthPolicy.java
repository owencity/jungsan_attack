package app.jeongsan.server.user.javaimpl;

import app.jeongsan.server.common.MalformedRequestException;
import app.jeongsan.server.common.UnauthenticatedException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/** Java 학습 구현. Kotlin 정책으로 위임하지 않는다. */
public final class AuthPolicy {
    public static void nativeKakaoToken(String value) {
        if (value == null || value.length() == 0 || value.length() > 2048 || value.codePoints().anyMatch(character -> character < 33 || character > 126))
            throw new MalformedRequestException("카카오 로그인 정보를 확인해 주세요.");
    }

    public static String nativeKakaoSubject(Long id, Long appId, Long expiresIn, long expectedAppId) {
        if (expectedAppId <= 0 || appId == null || appId != expectedAppId || id == null || id <= 0 || expiresIn == null || expiresIn <= 0)
            throw new UnauthenticatedException();
        return id.toString();
    }

    public static String appReturnMode(String client, String value) {
        String mode = value == null ? "page" : value;
        if ((!"page".equals(mode) && !"session".equals(mode)) ||
                (!"app".equals(client) && !"page".equals(mode)))
            throw new MalformedRequestException("appReturn은 앱에서 page 또는 session으로 지정합니다.");
        return mode;
    }

    public static String appSessionReturnLink(String ticket) {
        if (!ticket.matches("[A-Za-z0-9_-]{43}")) throw new UnauthenticatedException();
        return "jeongsan://auth?ticket=" + ticket;
    }

    public static Instant appTicketExpiresAt(Instant now) { return now.plusSeconds(300); }

    public static String appReturnLink(String ticket, String userAgent) {
        if (!ticket.matches("[A-Za-z0-9_-]{43}")) throw new UnauthenticatedException();
        if (userAgent != null && userAgent.toLowerCase(java.util.Locale.ROOT).contains("android"))
            return "intent://auth?ticket=" + ticket + "#Intent;scheme=jeongsan;package=app.jeongsan;end";
        return "jeongsan://auth?ticket=" + ticket;
    }

    // Apple이 첫 동의 때 제공한 이름을 재사용하며 서비스에 필요 없는 이메일은 요청하지 않는다.
    public static Map<String, String> appleAuthorizationParameters(String nonce) {
        return Map.of("response_mode", "form_post", "nonce", nonce, "scope", "name");
    }

    public static boolean providerAvailable(String provider, List<String> kakaoSettings, List<String> appleSettings) {
        if (!"KAKAO".equals(provider) && !"APPLE".equals(provider)) return false;
        List<String> settings = "KAKAO".equals(provider) ? kakaoSettings : appleSettings;
        int expectedSize = "KAKAO".equals(provider) ? 3 : 5;
        return settings.size() == expectedSize && settings.stream().allMatch(value ->
                value.codePoints().anyMatch(character -> !Character.isWhitespace(character) && !Character.isSpaceChar(character)));
    }

    private AuthPolicy() {}
    public record Credential(String token, String client) {}
    public static String returnTo(String value) {
        String path = value == null ? "/jungsan/" : value;
        try {
            if (path.length() > 500 || !path.startsWith("/jungsan/") || path.contains("//") ||
                path.chars().anyMatch(c -> c <= ' ' || c == '\\' || c == '%') || URI.create(path).isAbsolute())
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) { throw new MalformedRequestException("복귀 경로가 올바르지 않습니다."); }
        return path;
    }
    public static String client(String value) {
        if (!"web".equals(value) && !"app".equals(value)) throw new MalformedRequestException("client는 web 또는 app입니다.");
        return value;
    }
    public static String challenge(String client, String value) {
        if ("app".equals(client) && (value == null || !value.matches("[A-Za-z0-9_-]{43}")))
            throw new MalformedRequestException("앱 codeChallenge가 필요합니다.");
        return "app".equals(client) ? value : null;
    }
    private static byte[] digest(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static String hash(String value) { return HexFormat.of().formatHex(digest(value)); }
    public static void verifyVerifier(String value, String expected) {
        if (!value.matches("[A-Za-z0-9._~-]{43,128}")) throw new UnauthenticatedException();
        String actual = Base64.getUrlEncoder().withoutPadding().encodeToString(digest(value));
        if (!MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8)))
            throw new UnauthenticatedException();
    }
    public static void validUntil(Instant expiry, Instant now) { if (!now.isBefore(expiry)) throw new UnauthenticatedException(); }
    public static Credential token(String header, String cookie) {
        if (header != null) {
            if (header.length() <= 7 || !header.regionMatches(true, 0, "Bearer ", 0, 7) || header.substring(7).chars().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)))
                throw new UnauthenticatedException();
            return new Credential(header.substring(7), "APP");
        }
        if (cookie == null || cookie.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) throw new UnauthenticatedException();
        return new Credential(cookie, "WEB");
    }
}
