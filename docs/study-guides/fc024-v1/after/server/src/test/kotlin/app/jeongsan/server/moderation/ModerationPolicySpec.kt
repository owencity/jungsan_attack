package app.jeongsan.server.moderation

import app.jeongsan.server.moderation.javaimpl.ModerationPolicy as JavaPolicy
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ModerationPolicySpec : StringSpec({
    val words = listOf("씨발", "병신", "fuck")
    for (text in listOf("씨발", "씨 ! 발", "씨\u200b발", "병.신", "ＦＵＣＫ", "F.u c-k")) {
        "정규화된 금칙어를 양쪽 언어가 거절: $text" {
            ModerationPolicy.messageViolation(text, false, words) shouldBe "MESSAGE_NOT_ALLOWED"
            JavaPolicy.messageViolation(text, false, words) shouldBe "MESSAGE_NOT_ALLOWED"
        }
    }
    for (text in listOf("감사합니다", "정산했어요 12,000원", "🍻", "부족하면 알려주세요", "a".repeat(200))) {
        "정상 메시지는 양쪽 언어가 허용: $text" {
            ModerationPolicy.messageViolation(text, false, words) shouldBe null
            JavaPolicy.messageViolation(text, false, words) shouldBe null
        }
    }
    "빈칸·최대 길이·메시지 금지의 기대 오류가 일치한다" {
        for (text in listOf("", "   ", "\u00a0\u3000", "a".repeat(201))) {
            ModerationPolicy.messageViolation(text, false, words) shouldBe "MALFORMED_REQUEST"
            JavaPolicy.messageViolation(text, false, words) shouldBe "MALFORMED_REQUEST"
        }
        ModerationPolicy.messageViolation("씨발", true, words) shouldBe "MESSAGE_BANNED"
        JavaPolicy.messageViolation("씨발", true, words) shouldBe "MESSAGE_BANNED"
    }
    "SYSTEM·SPOON·본인 글·작성자 없는 글·잘못된 사유는 신고할 수 없다" {
        data class Case(val type: String, val author: Long?, val reporter: Long, val reason: String, val expected: Boolean)
        for (case in listOf(Case("MESSAGE", 1, 2, "ABUSE", true), Case("MESSAGE", 1, 2, "SEXUAL", true),
            Case("MESSAGE", 1, 2, "SPAM", true), Case("MESSAGE", 1, 2, "OTHER", true),
            Case("MESSAGE", 1, 1, "ABUSE", false), Case("SYSTEM", 1, 2, "ABUSE", false),
            Case("SPOON", 1, 2, "ABUSE", false), Case("MESSAGE", null, 2, "OTHER", false),
            Case("MESSAGE", 1, 2, "abuse", false))) {
            ModerationPolicy.reportAllowed(case.type, case.author, case.reporter, case.reason) shouldBe case.expected
            JavaPolicy.reportAllowed(case.type, case.author, case.reporter, case.reason) shouldBe case.expected
        }
    }
    "서로 다른 두 신고자부터 전체 숨김, 자기 자신과 탈퇴 계정은 차단 불가" {
        for (count in 0L..4L) {
            ModerationPolicy.hideForEveryone(count) shouldBe (count >= 2)
            JavaPolicy.hideForEveryone(count) shouldBe (count >= 2)
        }
        for (target in listOf<Long?>(null, 1, 2)) {
            ModerationPolicy.blockAllowed(1, target) shouldBe (target == 2L)
            JavaPolicy.blockAllowed(1, target) shouldBe (target == 2L)
        }
    }
})
