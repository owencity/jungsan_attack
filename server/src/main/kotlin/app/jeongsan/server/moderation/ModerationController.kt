package app.jeongsan.server.moderation

import app.jeongsan.server.common.LoginUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class ModerationController(private val service: ModerationService) {
    @PostMapping("/gatherings/{gid}/messages/{entryId}/reports")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun report(@PathVariable gid: Long, @PathVariable entryId: Long, @LoginUser userId: Long,
               @Valid @RequestBody request: MessageReportRequest) {
        service.report(gid, entryId, userId, request.reason)
    }

    @PostMapping("/me/blocks")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun block(@LoginUser userId: Long, @Valid @RequestBody request: UserBlockRequest) {
        service.block(userId, request)
    }

    @GetMapping("/me/blocks")
    fun blocks(@LoginUser userId: Long): List<UserBlockResponse> = service.blocks(userId)

    @DeleteMapping("/me/blocks/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unblock(@PathVariable userId: Long, @LoginUser blockerUserId: Long) {
        service.unblock(blockerUserId, userId)
    }
}
