package app.jeongsan.server.gathering

/** 로케일에 따라 금액 구분 기호가 달라지지 않게 고정한다. Java는 javaimpl에 독립 구현한다. */
object NotificationText {
    fun money(amount: Long): String = String.format(java.util.Locale.US, "%,d", amount)
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
