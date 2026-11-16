package app.jeongsan.server.quicksettlement

import app.jeongsan.server.common.LoginUser
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/settlements")
class QuickSettlementController(private val service: QuickSettlementService) {
    @PostMapping("/quick")
    fun calculate(@LoginUser userId: Long, @Valid @RequestBody request: QuickSettlementRequest): ResponseEntity<QuickSettlementResponse> =
        ResponseEntity.ok().header("Cache-Control", "no-store").body(service.calculate(userId, request))
}
