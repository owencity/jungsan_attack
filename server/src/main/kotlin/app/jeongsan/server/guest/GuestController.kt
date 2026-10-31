package app.jeongsan.server.guest

import app.jeongsan.server.common.UnauthenticatedException
import app.jeongsan.server.gathering.AnswerRequest
import app.jeongsan.server.gathering.GatheringService
import app.jeongsan.server.gathering.ResponseRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration

data class GuestJoinRequest(@field:Size(max = 100) val displayName: String, val settlementUnitId: Long,
    @field:Size(max = 1000) val responses: List<AnswerRequest> = emptyList())

@RestController
@RequestMapping("/api/v1/guest")
class GuestController(private val service: GatheringService, private val tokens: GuestTokens,
    @param:Value("\${app.frontend-origin}") private val frontendOrigin: String,
    @param:Value("\${app.cookie-secure}") private val secure: Boolean) {

    @PostMapping("/session")
    fun startSession(request: HttpServletRequest): ResponseEntity<Unit> {
        GuestPolicy.requireOrigin(request.getHeader(HttpHeaders.ORIGIN), frontendOrigin)
        val previous = request.cookies?.firstOrNull { it.name == COOKIE_NAME }?.value
        if (previous != null && tokens.identity(previous) == null) throw UnauthenticatedException()
        val response = ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store")
        if (previous == null) response.header(HttpHeaders.SET_COOKIE,
            ResponseCookie.from(COOKIE_NAME, tokens.issue()).httpOnly(true).secure(secure).sameSite("Lax")
                .path("/api/v1/guest").maxAge(Duration.ofDays(30)).build().toString())
        return response.build()
    }

    @PostMapping("/join/{token}", consumes = ["application/json"])
    fun join(@PathVariable token: String, @GuestBrowser browser: GuestIdentity,
        @Valid @RequestBody body: GuestJoinRequest, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        GuestPolicy.requireOrigin(request.getHeader(HttpHeaders.ORIGIN), frontendOrigin)
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(service.guestJoin(token, browser.browserHash, body))
    }

    @GetMapping("/gatherings/{gatheringId}/me")
    fun me(@PathVariable gatheringId: Long, @GuestBrowser browser: GuestIdentity): ResponseEntity<Map<String, Any?>> =
        ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(service.guestMe(gatheringId, browser.browserHash))

    @PutMapping("/gatherings/{gatheringId}/settlement-units/{settlementUnitId}/responses/me", consumes = ["application/json"])
    fun respond(@PathVariable gatheringId: Long, @PathVariable settlementUnitId: Long, @GuestBrowser browser: GuestIdentity,
        @Valid @RequestBody body: ResponseRequest, request: HttpServletRequest): ResponseEntity<Unit> {
        GuestPolicy.requireOrigin(request.getHeader(HttpHeaders.ORIGIN), frontendOrigin)
        service.guestRespond(gatheringId, settlementUnitId, browser.browserHash, body)
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build()
    }

    @GetMapping("/gatherings/{gatheringId}/settlement-units/{settlementUnitId}/settlement")
    fun settlement(@PathVariable gatheringId: Long, @PathVariable settlementUnitId: Long,
        @GuestBrowser browser: GuestIdentity): ResponseEntity<Map<String, Any?>> = ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL,"no-store").body(service.guestSettlement(gatheringId,settlementUnitId,browser.browserHash))

    @PostMapping("/transfers/{transferId}/sent")
    fun sent(@PathVariable transferId: Long, @GuestBrowser browser: GuestIdentity, request: HttpServletRequest): ResponseEntity<Unit> {
        GuestPolicy.requireOrigin(request.getHeader(HttpHeaders.ORIGIN),frontendOrigin)
        service.guestSent(transferId,browser.browserHash)
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL,"no-store").build()
    }

    companion object { const val COOKIE_NAME = "jeongsan_guest" }
}
