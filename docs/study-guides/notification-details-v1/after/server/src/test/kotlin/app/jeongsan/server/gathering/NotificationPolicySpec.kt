package app.jeongsan.server.gathering

import app.jeongsan.server.gathering.javaimpl.NotificationPolicy as JavaNotificationPolicy
import app.jeongsan.server.gathering.javaimpl.NotificationText as JavaNotificationText
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class NotificationPolicySpec : StringSpec({
    "행동자·탈퇴 사용자·중복 대상을 제외하고 최초 순서를 유지한다" {
        val userIds=listOf(12L,0L,14L,12L,-1L,15L,14L)
        NotificationPolicy.recipients(userIds,14) shouldBe listOf(12L,15L)
        JavaNotificationPolicy.recipients(userIds,14) shouldBe listOf(12L,15L)
        userIds shouldBe listOf(12L,0L,14L,12L,-1L,15L,14L)
    }
    "서버가 자동 처리한 소식은 유효 사용자 전원에게 한 번씩 보낸다" {
        NotificationPolicy.recipients(listOf(12,12,13,0),0) shouldBe listOf(12,13)
        JavaNotificationPolicy.recipients(listOf(12,12,13,0),0) shouldBe listOf(12,13)
    }
    "송금 상태 알림은 행동자의 이름과 각 상태의 의미를 전달한다" {
        val expected=mapOf("SENT" to "김하나님이 보냈대요. 입금을 확인해주세요",
            "CONFIRMED" to "김하나님이 입금을 확인했어요", "NOT_RECEIVED" to "김하나님이 아직 입금을 확인 못 했대요")
        expected.forEach { (type,body) ->
            NotificationText.transfer(type,"김하나") shouldBe body
            JavaNotificationText.transfer(type,"김하나") shouldBe body
        }
    }
    "계좌 등록 알림은 예금주 대신 사용자 실명을 보여준다" {
        NotificationText.payoutRegistered("김하나") shouldBe "김하나님이 계좌를 등록했어요. 이제 보낼 수 있어요"
        JavaNotificationText.payoutRegistered("김하나") shouldBe NotificationText.payoutRegistered("김하나")
    }
    "다른 단위가 남으면 7일 삭제를 예고하지 않고 술자리 전체 완료에만 예고한다" {
        NotificationText.completed(false) shouldBe "담당 차수의 정산이 완료됐어요"
        JavaNotificationText.completed(false) shouldBe "담당 차수의 정산이 완료됐어요"
        NotificationText.completed(true) shouldBe "정산 완료! 7일 뒤 사라져요"
        JavaNotificationText.completed(true) shouldBe "정산 완료! 7일 뒤 사라져요"
    }
    "단위에서 내보내진 사람에게 전체 술자리에서 제거됐다고 안내하지 않는다" {
        NotificationText.removed("9/28 술자리") shouldBe "9/28 술자리의 담당 정산에서 빠졌어요"
        JavaNotificationText.removed("9/28 술자리") shouldBe NotificationText.removed("9/28 술자리")
    }
})
