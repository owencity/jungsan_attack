package app.jeongsan.server.gathering

import app.jeongsan.server.common.LoginUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/transfers/{id}")
class TransferController(private val service: GatheringService) {
    @PostMapping("/sent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun sent(@PathVariable id: Long, @LoginUser userId: Long) = service.transfer(id,userId,"sent")

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun confirm(@PathVariable id: Long, @LoginUser userId: Long) = service.transfer(id,userId,"confirm")

    @PostMapping("/not-received")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun notReceived(@PathVariable id: Long, @LoginUser userId: Long) = service.transfer(id,userId,"not-received")

}
