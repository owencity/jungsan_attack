package app.jeongsan.server.common

import java.security.SecureRandom

/**
 * 공유 링크 토큰. `API.md` §3.4 — **12자 · 62종 · SecureRandom**.
 *
 * `Random` 이 아니라 `SecureRandom` 을 쓴다. 일반 `Random` 은 시드를 알면 다음 값을
 * 예측할 수 있어서, 토큰 하나가 노출되면 남의 모임 링크를 계산해낼 수 있다.
 *
 * 62^12 ≈ 3.2 × 10^21. 무작위 대입은 요청 제한(분당 30회, 연속 404 5회 → 10분)이
 * 별도로 막는다(§5.1).
 *
 * **로그에 남기지 않는다**(`SPEC.md` §10) — 접근 로그에 토큰이 찍히면 링크가 새는 것과 같다.
 */
object ShareToken {
    private const val ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private const val LENGTH = 12
    private val random = SecureRandom()

    fun generate(): String = buildString(LENGTH) {
        repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }
}
