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
    @param:Value("\${app.login-success-url}") private val loginSuccessUrl: String,
    @param:Value("\${app.frontend-origin}") private val frontendOrigin: String,
    @param:Value("\${app.cookie-secure}") private val secure: Boolean,
    @param:Value("\${jwt.expiration-days}") private val expiryDays: Long) {
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
        AuthCallbackLog.started(provider, client)
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(result.location)).header(HttpHeaders.SET_COOKIE,binding.toString())
            .header(HttpHeaders.CACHE_CONTROL,"no-store").build()
    }
    @GetMapping("/api/v1/auth/kakao/callback")
    fun callback(@RequestParam code: String, @RequestParam state: String, request: HttpServletRequest) = finish("KAKAO",code,state,request)
    @PostMapping("/api/v1/auth/apple/callback", consumes=[MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun appleCallback(@RequestParam code: String, @RequestParam state: String, request: HttpServletRequest) = finish("APPLE",code,state,request)
    private fun finish(provider: String,code: String,state: String,request: HttpServletRequest): ResponseEntity<String> {
        if(!Regex("[A-Za-z0-9_-]{43}").matches(state)) {
            AuthCallbackLog.failed(provider, AuthCallbackLog.Failure.INVALID_STATE)
            throw app.jeongsan.server.common.UnauthenticatedException()
        }
        val browser=request.cookies?.firstOrNull { it.name==bindingName(state) }?.value
        val result=flow.finish(provider,state,browser,code)
        val response=if(result.client=="app") ResponseEntity.ok() else {
            val target=if(result.returnTo=="/jungsan/") loginSuccessUrl else frontendOrigin.trimEnd('/')+result.returnTo
            ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(target))
        }
        response
            .header(HttpHeaders.CACHE_CONTROL,"no-store").header("Referrer-Policy","no-referrer")
            .header(HttpHeaders.SET_COOKIE,ResponseCookie.from(bindingName(state),"").httpOnly(true).secure(secure)
                .sameSite(if(provider=="APPLE"&&secure) "None" else "Lax").path("/api/v1/auth").maxAge(0).build().toString())
        if(result.client=="web") response.header(HttpHeaders.SET_COOKIE,cookie(result.credential,secure,expiryDays).toString())
        // 서비스의 트랜잭션이 커밋된 뒤다. 브라우저가 실제로 앱을 열었다는 의미는 아니다.
        AuthCallbackLog.completed(provider, result.client)
        return if(result.client=="app") response.contentType(MediaType("text", "html", Charsets.UTF_8))
            .header("Content-Security-Policy",AppReturnPage.contentSecurityPolicy)
            .header("X-Content-Type-Options","nosniff")
            .body(AppReturnPage.html(result.credential,request.getHeader(HttpHeaders.USER_AGENT)))
        else response.build()
    }
    @PostMapping("/api/v1/auth/app/exchange")
    fun exchange(@RequestBody request: AppExchangeRequest): ResponseEntity<AppExchangeResponse> = ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL,"no-store").body(flow.exchange(request))
    @PostMapping("/api/v1/auth/logout")
    fun logout(@LoginUser userId: Long, request: HttpServletRequest): ResponseEntity<Unit> {
        flow.logout(AuthPolicy.token(request.getHeader(HttpHeaders.AUTHORIZATION),request.cookies?.firstOrNull { it.name==COOKIE_NAME }?.value).first)
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE,cookie("",secure,0).toString())
            .header(HttpHeaders.CACHE_CONTROL,"no-store").build()
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
