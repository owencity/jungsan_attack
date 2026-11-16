package app.jeongsan.server.receipt

import app.jeongsan.server.common.LoginUser
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 술자리를 만들지 않는 간단 정산도 같은 검토용 후보만 받는다. */
@RestController
@RequestMapping("/api/v1/receipts")
class ReceiptIntakeController(private val service: ReceiptService) {
    @PostMapping("/extract")
    fun extract(@LoginUser userId: Long, request: HttpServletRequest): ResponseEntity<ReceiptExtractionResponse> =
        ResponseEntity.ok().header("Cache-Control", "no-store").body(
            service.extract(userId, request.contentType, request.contentLengthLong) { request.inputStream })
}
