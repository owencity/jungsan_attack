package app.jeongsan.server.gathering

/** 탈퇴 사용자·행동자·중복 수신자를 제외하는 내부 알림 정책이다. */
object NotificationPolicy {
    fun recipients(userIds: List<Long>, actorUserId: Long): List<Long> =
        userIds.distinct().filter { userId -> userId > 0 && userId != actorUserId }
}
