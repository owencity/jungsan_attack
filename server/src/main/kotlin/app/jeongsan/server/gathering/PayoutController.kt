package app.jeongsan.server.gathering

import app.jeongsan.server.common.LoginUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users/me/payout")
class PayoutController(private val service: GatheringService) {
    @PutMapping
    fun payout(@LoginUser userId: Long, @Valid @RequestBody request: PayoutRequest) = service.payout(userId,request)

}
