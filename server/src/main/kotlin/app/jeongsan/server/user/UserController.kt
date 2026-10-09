package app.jeongsan.server.user

import app.jeongsan.server.common.LoginUser
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.beans.factory.annotation.Value

@RestController
@RequestMapping("/api/v1/users/me")
class UserController(private val userService: UserService, private val deletion: AccountDeletionService? = null,
    @Value("\${app.cookie-secure}") private val secure: Boolean = false) {
    @DeleteMapping
    fun delete(@LoginUser userId: Long): ResponseEntity<Unit> {
        checkNotNull(deletion).delete(userId)
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE,AuthController.cookie("",secure,0).toString()).build()
    }
    @PutMapping("/display-name")
    fun registerDisplayName(@LoginUser userId: Long, @Valid @RequestBody request: DisplayNameRequest): MeResponse =
        userService.registerDisplayName(userId, request)
}
