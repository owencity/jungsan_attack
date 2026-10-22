package app.jeongsan.server.user

import app.jeongsan.server.common.MalformedRequestException
import app.jeongsan.server.common.UnauthenticatedException
import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

/** 같은 리다이렉트·티켓 규칙을 Java와 독립 비교한다. 시계·DB·HTTP는 호출부에서 제공한다. */
object AuthPolicy {
    // 실명은 L2에서 받으므로 Apple 이름·이메일 동의를 요청하지 않는다.
    fun appleAuthorizationParameters(nonce: String): Map<String, String> = mapOf(
        "response_mode" to "form_post",
        "nonce" to nonce,
    )

    fun providerAvailable(provider: String, kakaoSettings: List<String>, appleSettings: List<String>): Boolean = when (provider) {
        "KAKAO" -> kakaoSettings.size == 3 && kakaoSettings.all { it.isNotBlank() }
        "APPLE" -> appleSettings.size == 5 && appleSettings.all { it.isNotBlank() }
        else -> false
    }

    fun returnTo(value: String?): String {
        val path = value ?: "/jungsan/"
        if (path.length > 500 || !path.startsWith("/jungsan/") || path.contains("//") ||
            path.any { it <= ' ' || it == '\\' || it == '%' } || runCatching { URI(path).isAbsolute }.getOrDefault(true))
            throw MalformedRequestException("복귀 경로가 올바르지 않습니다.")
        return path
    }
    fun client(value: String): String = value.also {
        if (it != "web" && it != "app") throw MalformedRequestException("client는 web 또는 app입니다.")
    }
    fun challenge(client: String, value: String?): String? {
        if (client == "app" && (value == null || !Regex("[A-Za-z0-9_-]{43}").matches(value)))
            throw MalformedRequestException("앱 codeChallenge가 필요합니다.")
        return if (client == "app") value else null
    }
    fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
    fun verifyVerifier(value: String, expected: String) {
        if (!Regex("[A-Za-z0-9._~-]{43,128}").matches(value)) throw UnauthenticatedException()
        val actual = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.US_ASCII)))
        if (!MessageDigest.isEqual(actual.toByteArray(), expected.toByteArray())) throw UnauthenticatedException()
    }
    fun validUntil(expiry: Instant, now: Instant) { if (!now.isBefore(expiry)) throw UnauthenticatedException() }
    fun token(header: String?, cookie: String?): Pair<String, String> {
        if (header != null) {
            if (!header.startsWith("Bearer ", ignoreCase = true) || header.length <= 7 || header.substring(7).any { it.isWhitespace() })
                throw UnauthenticatedException()
            return header.substring(7) to "APP"
        }
        return (cookie?.takeIf { it.isNotBlank() } ?: throw UnauthenticatedException()) to "WEB"
    }
}
