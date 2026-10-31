package app.jeongsan.server.guest.javaimpl;

import app.jeongsan.server.common.ApiException;
import app.jeongsan.server.common.MalformedRequestException;
import org.springframework.http.HttpStatus;

/** Spring에 등록하지 않는 독립 정책 구현. 이름과 Origin은 Kotlin 결과와 별도로 검증한다. */
public final class GuestPolicy {
    private GuestPolicy() {}

    public static String displayName(String value) {
        int first = 0;
        int last = value.length();
        while (first < last && whitespace(value.codePointAt(first))) first += Character.charCount(value.codePointAt(first));
        while (last > first && whitespace(value.codePointBefore(last))) last -= Character.charCount(value.codePointBefore(last));
        String name = value.substring(first, last);
        int count = name.codePointCount(0, name.length());
        if (count < 2 || count > 10 || name.codePoints().anyMatch(Character::isISOControl))
            throw new MalformedRequestException("성을 포함한 이름을 2~10자로 넣어주세요");
        return name;
    }

    private static boolean whitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    public static void requireOrigin(String origin, String frontendOrigin) {
        if (!frontendOrigin.equals(origin))
            throw new ApiException("GUEST_ORIGIN_NOT_ALLOWED", HttpStatus.FORBIDDEN, "허용되지 않은 요청 출처입니다.");
    }

    public static void requireSameName(String previous, String submitted) {
        if (!previous.equals(submitted))
            throw new ApiException("GUEST_NAME_MISMATCH", HttpStatus.CONFLICT, "이 브라우저에서 이미 참여한 이름과 달라요");
    }
}
