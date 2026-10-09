package app.jeongsan.server.user

import app.jeongsan.server.common.LoginUser
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me")
class UserController(private val userService: UserService) {
    @PutMapping("/display-name")
    fun registerDisplayName(@LoginUser userId: Long, @Valid @RequestBody request: DisplayNameRequest): MeResponse =
        userService.registerDisplayName(userId, request)
}
