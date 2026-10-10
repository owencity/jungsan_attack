package app.jeongsan.server.common

import org.springframework.http.HttpStatus

/**
 * API 계층 오류. `API.md` §1.4 의 "API 계층에서 나오는 것" 표에 대응한다.
 *
 * 계산 엔진(`core`)의 오류와 성격이 다르다 — core 는 예외를 던지지 않고
 * `SettlementOutcome.Failure` 로 돌려준다(ADR-001). 이건 HTTP 계층 전용이다.
 */
open class ApiException(
    val code: String,
    val status: HttpStatus,
    override val message: String,
) : RuntimeException(message)

/** 401 — 쿠키가 없거나 서명이 안 맞거나 만료됨. */
class UnauthenticatedException :
    ApiException("UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.")

/**
 * 404 — 없거나, **있어도 알려주면 안 되는** 것.
 *
 * 남의 자원에 403 을 주면 "그건 존재한다"를 알려주는 셈이라 404 로 답한다(`API.md` §1.3).
 */
class NotFoundException(message: String = "찾을 수 없습니다.") :
    ApiException("NOT_FOUND", HttpStatus.NOT_FOUND, message)

/** 400 — 요청 형식이 계약과 다름. */
class MalformedRequestException(message: String) :
    ApiException("MALFORMED_REQUEST", HttpStatus.BAD_REQUEST, message)

/** 409 — 번개 모임은 술자리를 하나만 둔다(`API.md` §3-b.2). */
class FlashGroupHasGatheringException :
    ApiException(
        "FLASH_GROUP_HAS_GATHERING",
        HttpStatus.CONFLICT,
        "번개 모임에는 술자리를 하나만 만들 수 있습니다.",
    )
