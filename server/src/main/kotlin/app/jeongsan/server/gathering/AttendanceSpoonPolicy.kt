package app.jeongsan.server.gathering

/** 회수하지 않는 표시용 보상. 응답 재시도와 차수/단위 추가는 지급 사건을 새로 만들지 않는다. */
object AttendanceSpoonPolicy {
    const val AMOUNT = 10

    fun isAttendance(type: String, source: String): Boolean =
        type in setOf("SOBER", "DRANK") && source in setOf("SELF", "HOST")

    fun amount(hostParticipantId: Long, participantId: Long, hasAttendance: Boolean, alreadyAwarded: Boolean): Int =
        if (hostParticipantId != participantId && hasAttendance && !alreadyAwarded) AMOUNT else 0
}
