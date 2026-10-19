package app.jeongsan.server.gathering

/** 로케일에 따라 금액 구분 기호가 달라지지 않게 고정한다. Java는 javaimpl에 독립 구현한다. */
object NotificationText {
    fun money(amount: Long): String = String.format(java.util.Locale.US, "%,d", amount)
    fun transfer(type: String, actorName: String): String = when (type) {
        "SENT" -> "${actorName}님이 보냈대요. 입금을 확인해주세요"
        "CONFIRMED" -> "${actorName}님이 입금을 확인했어요"
        "NOT_RECEIVED" -> "${actorName}님이 아직 입금을 확인 못 했대요"
        else -> throw IllegalArgumentException("송금 알림 종류가 아닙니다.")
    }
    fun payoutRegistered(payerName: String): String = "${payerName}님이 계좌를 등록했어요. 이제 보낼 수 있어요"
    fun completed(gatheringCompleted: Boolean): String =
        if (gatheringCompleted) "정산 완료! 7일 뒤 사라져요" else "담당 차수의 정산이 완료됐어요"
    fun removed(gatheringTitle: String): String = "${gatheringTitle}의 담당 정산에서 빠졌어요"
    fun title(type: String): String = when (type) {
        "JOINED", "RESPONDED" -> "참여 응답이 도착했어요"
        "SETTLED" -> "정산이 나왔어요! 입금액을 확인해주세요"
        "SETTLED_HOST" -> "계산이 끝났어요"
        "AUTO_RESPONDED" -> "자동응답으로 계산된 사람이 있어요"
        "SENT" -> "입금을 확인해주세요"
        "CONFIRMED" -> "입금이 확인됐어요"
        "NOT_RECEIVED" -> "아직 입금이 확인되지 않았어요"
        "COMPLETED" -> "정산 완료! 🎉"
        "SETTLEMENT_REVERTED" -> "정산이 되돌려졌어요"
        "PAYOUT_REGISTERED" -> "계좌가 등록됐어요"
        "PAYOUT_MISSING" -> "받을 계좌를 등록해주세요"
        "HEADCOUNT_EXCEEDED" -> "참여 인원을 확인해주세요"
        "MEMBER_EXCLUDED", "REMOVED" -> "이번 정산에서 빠졌어요"
        else -> "술자리 소식이 있어요"
    }
}
