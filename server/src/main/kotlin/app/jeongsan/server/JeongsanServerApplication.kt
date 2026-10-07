package app.jeongsan.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * 술자리 API와 내부 알림·타임라인을 제공하는 모놀리스(ADR-014).
 */
@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
class JeongsanServerApplication

fun main(args: Array<String>) {
    runApplication<JeongsanServerApplication>(*args)
}
