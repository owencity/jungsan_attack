package app.jeongsan.server.guest

import app.jeongsan.server.common.ApiException
import app.jeongsan.server.guest.javaimpl.GuestPolicy as JavaGuestPolicy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GuestPolicySpec : StringSpec({
    "성이름은 Unicode 코드포인트로 검증하고 양쪽 구현 모두 주변 공백만 제거한다" {
        for ((submitted, expected) in listOf(" 김하나 " to "김하나", "\u00a0김하나\u2003" to "김하나", "😀😀" to "😀😀", "가".repeat(10) to "가".repeat(10))) {
            GuestPolicy.displayName(submitted) shouldBe expected
            JavaGuestPolicy.displayName(submitted) shouldBe expected
        }
    }
    "빈 이름과 한 글자와 초과 길이와 중간 제어문자는 양쪽 모두 400이다" {
        for (submitted in listOf("", " ", "김", "가".repeat(11), "김\n하나")) {
            shouldThrow<ApiException> { GuestPolicy.displayName(submitted) }.code shouldBe "MALFORMED_REQUEST"
            shouldThrow<ApiException> { JavaGuestPolicy.displayName(submitted) }.status.value() shouldBe 400
        }
    }
    "Origin은 정확히 일치해야 하며 누락 null 형제 호스트 접미사 위조를 거절한다" {
        val allowed="https://web.test"
        GuestPolicy.requireOrigin(allowed,allowed);JavaGuestPolicy.requireOrigin(allowed,allowed)
        for (origin in listOf(null,"null","https://sibling.test","https://web.test.evil","https://web.test/")) {
            shouldThrow<ApiException> { GuestPolicy.requireOrigin(origin,allowed) }.code shouldBe "GUEST_ORIGIN_NOT_ALLOWED"
            shouldThrow<ApiException> { JavaGuestPolicy.requireOrigin(origin,allowed) }.status.value() shouldBe 403
        }
    }
    "이미 참여한 이름이 다르면 새 신원을 만들거나 다른 사람을 선택하지 않는다" {
        GuestPolicy.requireSameName("김하나","김하나");JavaGuestPolicy.requireSameName("김하나","김하나")
        shouldThrow<ApiException> { GuestPolicy.requireSameName("김하나","김둘") }.code shouldBe "GUEST_NAME_MISMATCH"
        shouldThrow<ApiException> { JavaGuestPolicy.requireSameName("김하나","김둘") }.status.value() shouldBe 409
    }
})
