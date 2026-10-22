package app.jeongsan.server.user

import app.jeongsan.server.common.ApiException
import app.jeongsan.server.common.UnauthenticatedException
import app.jeongsan.server.gathering.GatheringStore
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.sql.Timestamp
import java.time.Instant
import java.util.Base64

class AuthCallbackLogSpec : StringSpec({
    val logger = LoggerFactory.getLogger(AuthCallbackLog::class.java) as Logger
    val appender = ListAppender<ILoggingEvent>().apply { start() }
    beforeSpec { logger.addAppender(appender) }
    afterSpec { logger.detachAppender(appender); appender.stop() }
    val database = Mockito.mock(GatheringStore::class.java)
    val gateway = Mockito.mock(ProviderGateway::class.java)
    val flow = AuthFlowService(database, gateway, JwtService("01234567890123456789012345678901", 30),
        Base64.getEncoder().encodeToString(ByteArray(32)))
    val state = "s".repeat(43)
    val cookie = "DUMMY_PRIVATE_COOKIE"
    val code = "DUMMY_PRIVATE_CODE"
    val query = "SELECT * FROM auth_challenges WHERE state_hash=:s FOR UPDATE"
    fun challenge(expired: Boolean = false, provider: String = "KAKAO", browser: String = cookie) = mapOf<String, Any?>(
        "expires_at" to Timestamp.from(Instant.now().plusSeconds(if (expired) -60 else 300)),
        "provider" to provider, "browser_hash" to AuthPolicy.hash(browser), "nonce" to "DUMMY_PRIVATE_NONCE",
    )
    fun expectReason(reason: AuthCallbackLog.Failure) {
        appender.list.map { it.formattedMessage } shouldBe listOf("AUTH_CALLBACK_FAILED provider=KAKAO reason=${reason.name}")
        appender.list.all { it.throwableProxy == null } shouldBe true
    }
    beforeTest { appender.list.clear(); Mockito.reset(database, gateway) }

    "잘못된 state와 누락 쿠키는 개인정보 없이 분류하며 DB와 제공자를 호출하지 않는다" {
        shouldThrow<UnauthenticatedException> { flow.finish("KAKAO", "invalid", cookie, code) }
        expectReason(AuthCallbackLog.Failure.INVALID_STATE)
        appender.list.clear()
        shouldThrow<UnauthenticatedException> { flow.finish("KAKAO", state, null, code) }
        expectReason(AuthCallbackLog.Failure.MISSING_BROWSER_COOKIE)
        Mockito.verifyNoInteractions(database, gateway)
    }
    "빈 코드와 과대 코드는 외부 교환 전에 거절한다" {
        listOf("", " ", "x".repeat(2049)).forEach { invalidCode ->
            appender.list.clear()
            shouldThrow<UnauthenticatedException> { flow.finish("KAKAO", state, cookie, invalidCode) }
            expectReason(AuthCallbackLog.Failure.INVALID_CODE)
        }
        Mockito.verifyNoInteractions(database, gateway)
    }
    "없는 state와 만료 state를 구분하며 원문과 해시를 로그에 쓰지 않는다" {
        shouldThrow<UnauthenticatedException> { flow.finish("KAKAO", state, cookie, code) }
        expectReason(AuthCallbackLog.Failure.STATE_NOT_FOUND)
        appender.list.clear()
        Mockito.`when`(database.rows(query, "s" to AuthPolicy.hash(state))).thenReturn(listOf(challenge(expired = true)))
        shouldThrow<UnauthenticatedException> { flow.finish("KAKAO", state, cookie, code) }
        expectReason(AuthCallbackLog.Failure.STATE_EXPIRED)
        Mockito.verifyNoInteractions(gateway)
    }
    "다른 provider와 다른 브라우저 쿠키는 제공자 호출 전에 거절한다" {
        Mockito.`when`(database.rows(query, "s" to AuthPolicy.hash(state))).thenReturn(listOf(challenge(provider = "APPLE")))
        shouldThrow<UnauthenticatedException> { flow.finish("KAKAO", state, cookie, code) }
        expectReason(AuthCallbackLog.Failure.PROVIDER_MISMATCH)
        appender.list.clear()
        Mockito.`when`(database.rows(query, "s" to AuthPolicy.hash(state))).thenReturn(listOf(challenge(browser = "other")))
        shouldThrow<UnauthenticatedException> { flow.finish("KAKAO", state, cookie, code) }
        expectReason(AuthCallbackLog.Failure.BROWSER_MISMATCH)
        Mockito.verifyNoInteractions(gateway)
    }
    "제공자 거절의 예외 본문과 인증 값은 남기지 않고 기존 HTTP 오류를 보존한다" {
        Mockito.`when`(database.rows(query, "s" to AuthPolicy.hash(state))).thenReturn(listOf(challenge()))
        Mockito.`when`(gateway.identity("KAKAO", code, "DUMMY_PRIVATE_NONCE"))
            .thenThrow(ApiException("AUTH_PROVIDER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "DUMMY_PRIVATE_EXCEPTION"))
        val failure = shouldThrow<ApiException> { flow.finish("KAKAO", state, cookie, code) }
        failure.status shouldBe HttpStatus.SERVICE_UNAVAILABLE
        expectReason(AuthCallbackLog.Failure.PROVIDER_REJECTED)
    }
    "성공 로그는 provider와 client만 기록하며 입력 문자열을 로그에 주입할 수 없다" {
        AuthCallbackLog.started("KAKAO", "app")
        AuthCallbackLog.completed("KAKAO", "app")
        AuthCallbackLog.completed("DUMMY_PRIVATE_PROVIDER\n", "DUMMY_PRIVATE_CLIENT")
        appender.list.map { it.formattedMessage } shouldBe listOf(
            "AUTH_LOGIN_STARTED provider=KAKAO client=app",
            "AUTH_CALLBACK_COMPLETED provider=KAKAO client=app",
            "AUTH_CALLBACK_COMPLETED provider=UNKNOWN client=UNKNOWN",
        )
    }
})
