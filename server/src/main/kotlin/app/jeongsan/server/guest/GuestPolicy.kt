package app.jeongsan.server.guest

import app.jeongsan.server.common.ApiException
import app.jeongsan.server.common.MalformedRequestException
import org.springframework.http.HttpStatus

/** 이름은 표시 정보이며 본인 확인 수단이 아니다. Java 정책은 별도 구현해 대조한다. */
object GuestPolicy {
    fun displayName(value: String): String {
        val name = value.trim()
        if (name.codePointCount(0, name.length) !in 2..10 || name.any { it.isISOControl() })
            throw MalformedRequestException("성을 포함한 이름을 2~10자로 넣어주세요")
        return name
    }

    fun requireOrigin(origin: String?, frontendOrigin: String) {
        if (origin != frontendOrigin)
            throw ApiException("GUEST_ORIGIN_NOT_ALLOWED", HttpStatus.FORBIDDEN, "허용되지 않은 요청 출처입니다.")
    }

    fun requireSameName(previous: String, submitted: String) {
        if (previous != submitted)
            throw ApiException("GUEST_NAME_MISMATCH", HttpStatus.CONFLICT, "이 브라우저에서 이미 참여한 이름과 달라요")
    }
}
