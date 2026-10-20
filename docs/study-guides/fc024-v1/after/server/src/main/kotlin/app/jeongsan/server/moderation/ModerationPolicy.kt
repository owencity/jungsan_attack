package app.jeongsan.server.moderation

import java.text.Normalizer
import java.util.Locale

/** DB와 HTTP에 의존하지 않아 Java 학습 구현과 정책의 기대값을 각각 검증한다. */
object ModerationPolicy {
    private val reasons = setOf("ABUSE", "SEXUAL", "SPAM", "OTHER")

    fun normalize(text: String): String = buildString {
        Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT).codePoints()
            .filter { Character.isLetterOrDigit(it) }.forEach { appendCodePoint(it) }
    }

    fun messageViolation(text: String, banned: Boolean, forbiddenWords: List<String>): String? = when {
        banned -> "MESSAGE_BANNED"
        text.trim().isEmpty() || text.trim().length > 200 -> "MALFORMED_REQUEST"
        forbiddenWords.any { normalize(it).let { word -> word.isNotEmpty() && normalize(text).contains(word) } } ->
            "MESSAGE_NOT_ALLOWED"
        else -> null
    }

    fun reportAllowed(type: String, authorParticipantId: Long?, reporterParticipantId: Long, reason: String): Boolean =
        type == "MESSAGE" && authorParticipantId != null && authorParticipantId != reporterParticipantId && reason in reasons

    fun hideForEveryone(distinctReporterCount: Long): Boolean = distinctReporterCount >= 2

    fun blockAllowed(blockerUserId: Long, blockedUserId: Long?): Boolean =
        blockedUserId != null && blockedUserId != blockerUserId
}
