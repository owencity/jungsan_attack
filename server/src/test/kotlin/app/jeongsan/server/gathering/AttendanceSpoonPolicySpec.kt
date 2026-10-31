package app.jeongsan.server.gathering

import app.jeongsan.server.gathering.javaimpl.AttendanceSpoonPolicy as JavaPolicy
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AttendanceSpoonPolicySpec : StringSpec({
    "참석 직접 응답과 총무 대리 응답만 인정하며 불참 면제 AUTO는 지급하지 않는다" {
        for (type in listOf("SOBER", "DRANK", "ABSENT", "EXEMPT", "UNKNOWN")) {
            for (source in listOf("SELF", "HOST", "AUTO", "UNKNOWN")) {
                val expected = (type == "SOBER" || type == "DRANK") && (source == "SELF" || source == "HOST")
                AttendanceSpoonPolicy.isAttendance(type, source) shouldBe expected
                JavaPolicy.isAttendance(type, source) shouldBe expected
            }
        }
    }
    "총무 본인은 지급 대상이 아니며 최초 참석 응답에만 10개를 지급한다" {
        for (participant in listOf(10L, 20L)) for (attending in listOf(true, false)) for (awarded in listOf(true, false)) {
            val expected = if (participant == 20L && attending && !awarded) 10 else 0
            AttendanceSpoonPolicy.amount(10, participant, attending, awarded) shouldBe expected
            JavaPolicy.amount(10, participant, attending, awarded) shouldBe expected
        }
    }
    "지급 후 불참 변경과 재참석은 회수하거나 추가 지급하지 않는다" {
        AttendanceSpoonPolicy.amount(10, 20, false, true) shouldBe 0
        JavaPolicy.amount(10, 20, false, true) shouldBe 0
        AttendanceSpoonPolicy.amount(10, 20, true, true) shouldBe 0
        JavaPolicy.amount(10, 20, true, true) shouldBe 0
    }
})
