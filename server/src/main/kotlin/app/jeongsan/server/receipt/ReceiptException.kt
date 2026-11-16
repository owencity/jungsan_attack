package app.jeongsan.server.receipt

import app.jeongsan.server.common.ApiException
import org.springframework.http.HttpStatus

/** 내부 응답·사진·서비스 키는 오류 메시지에 포함하지 않는다. */
class ReceiptException(code: String, status: Int, val retryAfter: Long? = null) :
    ApiException(code, HttpStatus.valueOf(status), when (status) {
        429 -> "영수증 요청이 많습니다. 잠시 후 다시 시도해주세요."
        502, 503, 504 -> "영수증을 읽을 수 없습니다. 잠시 후 다시 시도해주세요."
        else -> "JPEG 또는 PNG 영수증 사진을 확인해주세요."
    })
