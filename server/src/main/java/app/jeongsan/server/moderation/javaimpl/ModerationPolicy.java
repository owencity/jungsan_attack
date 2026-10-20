package app.jeongsan.server.moderation.javaimpl;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Spring Bean을 등록하지 않는 독립 Java 학습 구현. Kotlin 정책을 호출하지 않는다. */
public final class ModerationPolicy {
    private static final Set<String> REASONS = Set.of("ABUSE", "SEXUAL", "SPAM", "OTHER");

    private ModerationPolicy() {}

    public static String normalize(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        normalized.codePoints().filter(Character::isLetterOrDigit).forEach(result::appendCodePoint);
        return result.toString();
    }

    public static String messageViolation(String text, boolean banned, List<String> forbiddenWords) {
        if (banned) return "MESSAGE_BANNED";
        // Kotlin trim과 같이 유니코드 공백도 경계에서 제거한다.
        int start = 0;
        int end = text.length();
        while (start < end && isSpace(text.codePointAt(start))) start += Character.charCount(text.codePointAt(start));
        while (end > start && isSpace(text.codePointBefore(end))) end -= Character.charCount(text.codePointBefore(end));
        String trimmed = text.substring(start, end);
        if (trimmed.isEmpty() || trimmed.length() > 200) return "MALFORMED_REQUEST";
        String normalizedText = normalize(text);
        for (String forbiddenWord : forbiddenWords) {
            String normalizedWord = normalize(forbiddenWord);
            if (!normalizedWord.isEmpty() && normalizedText.contains(normalizedWord)) return "MESSAGE_NOT_ALLOWED";
        }
        return null;
    }

    public static boolean reportAllowed(String type, Long authorParticipantId, long reporterParticipantId, String reason) {
        return "MESSAGE".equals(type) && authorParticipantId != null
            && authorParticipantId.longValue() != reporterParticipantId && REASONS.contains(reason);
    }

    public static boolean hideForEveryone(long distinctReporterCount) {
        return distinctReporterCount >= 2;
    }

    public static boolean blockAllowed(long blockerUserId, Long blockedUserId) {
        return blockedUserId != null && blockedUserId.longValue() != blockerUserId;
    }

    private static boolean isSpace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }
}
