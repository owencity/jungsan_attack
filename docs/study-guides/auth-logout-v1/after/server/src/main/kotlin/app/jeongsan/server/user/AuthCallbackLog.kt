package app.jeongsan.server.user

import org.slf4j.LoggerFactory

/** 브라우저→서버→앱 중 어디에서 멈췄는지만 기록한다. 요청 값·예외 본문은 기록하지 않는다. */
internal object AuthCallbackLog {
    enum class Failure {
        INVALID_STATE, INVALID_CODE, MISSING_BROWSER_COOKIE, STATE_NOT_FOUND, STATE_EXPIRED,
        PROVIDER_MISMATCH, BROWSER_MISMATCH, PROVIDER_REJECTED,
    }

    private val log = LoggerFactory.getLogger(AuthCallbackLog::class.java)

    fun started(provider: String, client: String) {
        log.info("AUTH_LOGIN_STARTED provider={} client={}", safeProvider(provider), safeClient(client))
    }

    fun failed(provider: String, reason: Failure) {
        log.warn("AUTH_CALLBACK_FAILED provider={} reason={}", safeProvider(provider), reason.name)
    }

    fun completed(provider: String, client: String) {
        log.info("AUTH_CALLBACK_COMPLETED provider={} client={}", safeProvider(provider), safeClient(client))
    }

    private fun safeProvider(provider: String) = if (provider in setOf("KAKAO", "APPLE")) provider else "UNKNOWN"
    private fun safeClient(client: String) = if (client in setOf("web", "app")) client else "UNKNOWN"
}
