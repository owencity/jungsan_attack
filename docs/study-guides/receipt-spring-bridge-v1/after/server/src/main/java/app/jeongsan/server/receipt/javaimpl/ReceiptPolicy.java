package app.jeongsan.server.receipt.javaimpl;

import java.util.Arrays;
import java.util.Locale;

/** 학습용 독립 구현. Spring 어댑터는 Kotlin 정책을 사용한다. */
public final class ReceiptPolicy {
    private ReceiptPolicy() {}

    public static String access(boolean exists, boolean activeMember, boolean host, String state) {
        if (!exists) return "SETTLEMENT_UNIT_NOT_FOUND";
        if (!activeMember) return "NOT_PARTICIPANT";
        if (!host) return "NOT_SETTLEMENT_UNIT_HOST";
        if (!"OPEN".equals(state)) return "SETTLEMENT_UNIT_NOT_OPEN";
        return null;
    }

    public static String mediaType(String header) {
        if (header == null) return null;
        String mediaType = header.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return mediaType.equals("image/jpeg") || mediaType.equals("image/png") ? mediaType : null;
    }

    public static String lengthError(long length) {
        if (length > 8L * 1024 * 1024) return "IMAGE_TOO_LARGE";
        return length == 0 || length < -1 ? "IMAGE_SIZE_INVALID" : null;
    }

    public static boolean matchesSignature(byte[] bytes, String mediaType) {
        if ("image/jpeg".equals(mediaType)) {
            return bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
        }
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        return "image/png".equals(mediaType) && bytes.length >= png.length && Arrays.equals(Arrays.copyOf(bytes, png.length), png);
    }

    public static long retryAfter(int usedCount, long elapsedNanos) {
        long windowNanos = 60_000_000_000L;
        if (usedCount < 3 || elapsedNanos >= windowNanos) return 0;
        return Math.max(1, (windowNanos - elapsedNanos + 999_999_999L) / 1_000_000_000L);
    }
}
