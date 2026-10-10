package app.jeongsan.server.user

import app.jeongsan.server.common.LoginUser
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Duration

@RestController
class AuthController(private val flow: AuthFlowService, private val userService: UserService,
    @Value("\${app.login-success-url}") private val loginSuccessUrl: String,
    @Value("\${app.frontend-origin}") private val frontendOrigin: String,
    @Value("\${app.cookie-secure}") private val secure: Boolean,
    @Value("\${jwt.expiration-days}") private val expiryDays: Long) {
    @GetMapping("/api/v1/auth/kakao/login")
    fun login(@RequestParam(defaultValue="web") client: String, @RequestParam(required=false) returnTo: String?,
        @RequestParam(required=false) codeChallenge: String?) = start("KAKAO",client,returnTo,codeChallenge)
    @GetMapping("/api/v1/auth/apple/login")
    fun appleLogin(@RequestParam(defaultValue="web") client: String, @RequestParam(required=false) returnTo: String?,
        @RequestParam(required=false) codeChallenge: String?) = start("APPLE",client,returnTo,codeChallenge)
    private fun start(provider: String,client: String,path: String?,challenge: String?): ResponseEntity<Unit> {
        val result=flow.start(provider,client,path,challenge)
        val binding=ResponseCookie.from(bindingName(result.state),result.browser).httpOnly(true).secure(secure)
            .sameSite(if(provider=="APPLE"&&secure) "None" else "Lax").path("/api/v1/auth").maxAge(Duration.ofMinutes(5)).build()
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(result.location)).header(HttpHeaders.SET_COOKIE,binding.toString())
            .header(HttpHeaders.CACHE_CONTROL,"no-store").build()
    }
    @GetMapping("/api/v1/auth/kakao/callback")
    fun callback(@RequestParam code: String, @RequestParam state: String, request: HttpServletRequest) = finish("KAKAO",code,state,request)
    @PostMapping("/api/v1/auth/apple/callback", consumes=[MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun appleCallback(@RequestParam code: String, @RequestParam state: String, request: HttpServletRequest) = finish("APPLE",code,state,request)
    private fun finish(provider: String,code: String,state: String,request: HttpServletRequest): ResponseEntity<Unit> {
        if(!Regex("[A-Za-z0-9_-]{43}").matches(state)) throw app.jeongsan.server.common.UnauthenticatedException()
        val browser=request.cookies?.firstOrNull { it.name==bindingName(state) }?.value
        val result=flow.finish(provider,state,browser,code)
        val target=if(result.client=="app") "jeongsan://auth?ticket=${result.credential}" else
            if(result.returnTo=="/jungsan/") loginSuccessUrl else frontendOrigin.trimEnd('/')+result.returnTo
        // POST 콜백은 GET으로 복귀하도록 303을 사용한다.
        val response=ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(target))
            .header(HttpHeaders.CACHE_CONTROL,"no-store").header("Referrer-Policy","no-referrer")
            .header(HttpHeaders.SET_COOKIE,ResponseCookie.from(bindingName(state),"").httpOnly(true).secure(secure)
                .sameSite(if(provider=="APPLE"&&secure) "None" else "Lax").path("/api/v1/auth").maxAge(0).build().toString())
        if(result.client=="web") response.header(HttpHeaders.SET_COOKIE,cookie(result.credential,secure,expiryDays).toString())
        return response.build()
    }
    @PostMapping("/api/v1/auth/app/exchange")
    fun exchange(@RequestBody request: AppExchangeRequest): ResponseEntity<AppExchangeResponse> = ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL,"no-store").body(flow.exchange(request))
    @PostMapping("/api/v1/auth/logout")
    fun logout(@LoginUser userId: Long, request: HttpServletRequest): ResponseEntity<Unit> {
        flow.logout(AuthPolicy.token(request.getHeader(HttpHeaders.AUTHORIZATION),request.cookies?.firstOrNull { it.name==COOKIE_NAME }?.value).first)
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE,cookie("",secure,0).toString()).build()
    }
    @GetMapping("/api/v1/auth/me")
    fun me(@LoginUser userId: Long): MeResponse = userService.me(userId)
    companion object {
        const val COOKIE_NAME="jeongsan_token"
        fun bindingName(state: String)="jeongsan_auth_$state"
        fun cookie(value: String,secure: Boolean,days: Long)=ResponseCookie.from(COOKIE_NAME,value).httpOnly(true).secure(secure)
            .sameSite("Lax").path("/").maxAge(Duration.ofDays(days)).build()
    }
}
