package app.jeongsan.server.common

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 오류 응답을 `API.md` §1.2 한 가지 모양으로 고정한다.
 *
 * ```
 * { "code": "...", "message": "...", "errors": null }
 * ```
 *
 * `errors` 는 **검증 실패에서만** 채워진다 — 차수를 5개 넣은 주최자에게 첫 오류만
 * 알려주면 5번 왕복하기 때문이다(§1.2).
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApi(e: ApiException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(e.status).body(ErrorResponse(e.code, e.message))

    /** `@Valid` 실패 — 필드별로 모아서 돌려준다. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.fieldErrors.map {
            FieldErrorDetail(
                code = it.code ?: "INVALID",
                message = it.defaultMessage ?: "값이 올바르지 않습니다.",
                field = it.field,
            )
        }
        return ResponseEntity.badRequest()
            .body(ErrorResponse("VALIDATION_FAILED", "입력을 확인해주세요.", errors))
    }

    /**
     * 본문을 못 읽음 — 깨진 JSON, 잘못된 인코딩, 타입 불일치.
     *
     * **클라이언트 잘못이므로 400 이다.** 이걸 안 잡으면 500 으로 나가서
     * "서버가 고장났다"로 오해하게 된다(실제로 그렇게 나왔었다).
     * 파서 메시지를 그대로 노출하지 않는다 — 내부 클래스명이 섞여 나온다.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        log.warn("요청 본문을 읽지 못했다: {}", e.message)
        return ResponseEntity.badRequest()
            .body(ErrorResponse("MALFORMED_REQUEST", "요청 형식이 올바르지 않습니다."))
    }

    /**
     * 잡히지 않은 예외. **내부 사정을 응답에 담지 않는다** — 스택트레이스나 SQL 이
     * 노출되면 공격자에게 정보를 준다. 로그에만 남긴다.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("처리되지 않은 예외", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("INTERNAL_ERROR", "일시적인 오류가 발생했습니다."))
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
    val errors: List<FieldErrorDetail>? = null,
)

data class FieldErrorDetail(
    val code: String,
    val message: String,
    val field: String? = null,
)
