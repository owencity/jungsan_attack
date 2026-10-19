package app.jeongsan.server.gathering.javaimpl;

import java.util.List;

/** Kotlin 정책을 호출하지 않는 내부 알림 수신자 학습 구현이다. */
public final class NotificationPolicy {
    private NotificationPolicy() {}

    public static List<Long> recipients(List<Long> userIds, long actorUserId) {
        return userIds.stream().distinct()
                .filter(userId -> userId > 0 && userId != actorUserId)
                .toList();
    }
}
