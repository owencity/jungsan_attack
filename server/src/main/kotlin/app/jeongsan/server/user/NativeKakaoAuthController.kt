package app.jeongsan.server.user

import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.http.converter.HttpMessageNotReadableException
import app.jeongsan.server.common.ErrorResponse

@RestController
class NativeKakaoAuthController(private val service: NativeKakaoAuthService) {
    @PostMapping("/api/v1/auth/kakao/native", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun login(@Valid @RequestBody request: NativeKakaoLoginRequest): ResponseEntity<AppExchangeResponse> =
        ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(service.login(request))

    // 파서 예외 메시지는 제공자 토큰 본문 일부를 포함할 수 있어 공용 경고 로그로 넘기지 않는다.
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun malformed(): ResponseEntity<ErrorResponse> = ResponseEntity.badRequest()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(ErrorResponse("MALFORMED_REQUEST", "요청 형식이 올바르지 않습니다."))
}
