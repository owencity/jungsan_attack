package app.jeongsan.server.receipt

import app.jeongsan.server.common.LoginUser
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/gatherings/{gid}/settlement-units/{sid}/receipts")
class ReceiptController(private val service: ReceiptService) {
    @PostMapping("/extract")
    fun extract(@LoginUser userId: Long, @PathVariable gid: Long, @PathVariable sid: Long, request: HttpServletRequest): ResponseEntity<ReceiptExtractionResponse> =
        ResponseEntity.ok().header("Cache-Control", "no-store").body(
            service.extract(gid, sid, userId, request.contentType, request.contentLengthLong) { request.inputStream })
}
