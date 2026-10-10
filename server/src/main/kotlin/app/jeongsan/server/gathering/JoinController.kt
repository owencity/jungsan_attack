package app.jeongsan.server.gathering

import app.jeongsan.server.common.LoginUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/join/{token}")
class JoinController(private val service: GatheringService) {
    // 비로그인 링크 미리보기는 명단과 계좌를 공개하지 않는다.
    @GetMapping
    fun preview(@PathVariable token: String) = service.joinPreview(token)

    @PostMapping
    fun join(@PathVariable token: String, @LoginUser userId: Long, @Valid @RequestBody request: JoinRequest) = service.join(token,userId,request)
}
