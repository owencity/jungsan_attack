package app.jeongsan.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * REST API 서비스 (ADR-013 의 3개 서비스 중 ①).
 * 채팅 게이트웨이(②)·알림 워커(③)는 별도 모듈로 만든다 — 이 모듈에 합치지 않는다.
 */
@SpringBootApplication
class JeongsanServerApplication

fun main(args: Array<String>) {
    runApplication<JeongsanServerApplication>(*args)
}
