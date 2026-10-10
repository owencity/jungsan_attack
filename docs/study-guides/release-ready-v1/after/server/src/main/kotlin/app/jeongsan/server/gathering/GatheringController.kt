package app.jeongsan.server.gathering

import app.jeongsan.server.common.LoginUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/gatherings")
class GatheringController(private val service: GatheringService) {
    @GetMapping
    fun list(@LoginUser userId: Long) = service.list(userId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@LoginUser userId: Long, @Valid @RequestBody(required=false) request: GatheringCreateRequest? = null) =
        service.create(userId, request?.headcount)

    @GetMapping("/{gid}")
    fun detail(@PathVariable gid: Long, @LoginUser userId: Long) = service.detail(gid,userId)

    @PatchMapping("/{gid}")
    fun edit(@PathVariable gid: Long, @LoginUser userId: Long, @Valid @RequestBody request: GatheringEditRequest) = service.edit(gid,userId,request)

    @PostMapping("/{gid}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    fun message(@PathVariable gid: Long, @LoginUser userId: Long, @Valid @RequestBody request: MessageRequest) = service.message(gid,userId,request)

}
