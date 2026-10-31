package app.jeongsan.server.gathering.javaimpl;

/** Kotlin 정책을 호출하지 않는 학습용 독립 구현. JDBC/트랜잭션 인프라는 운영 서비스에서 공유한다. */
public final class AttendanceSpoonPolicy {
    private AttendanceSpoonPolicy() {}

    public static boolean isAttendance(String type, String source) {
        return ("SOBER".equals(type) || "DRANK".equals(type))
            && ("SELF".equals(source) || "HOST".equals(source));
    }

    public static int amount(long hostParticipantId, long participantId, boolean hasAttendance, boolean alreadyAwarded) {
        if (hostParticipantId == participantId || !hasAttendance || alreadyAwarded) return 0;
        return 10;
    }
}
