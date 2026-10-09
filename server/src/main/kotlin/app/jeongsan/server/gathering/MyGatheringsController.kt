package app.jeongsan.server.gathering

import app.jeongsan.server.common.LoginUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/me")
class MyGatheringsController(private val service: GatheringService) {
    @GetMapping("/gatherings")
    fun list(@LoginUser userId: Long) = service.list(userId)

    @GetMapping("/notifications")
    fun notifications(@LoginUser userId: Long) = service.notifications(userId)

    @PostMapping("/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun read(@PathVariable id: Long, @LoginUser userId: Long) = service.readNotifications(userId,id)

    @PostMapping("/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun readAll(@LoginUser userId: Long) = service.readNotifications(userId,null)

}
