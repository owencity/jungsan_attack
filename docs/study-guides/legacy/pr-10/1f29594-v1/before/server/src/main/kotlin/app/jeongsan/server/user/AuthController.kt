package app.jeongsan.server.user

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.Duration

/**
 * 카카오 로그인 — 서버 사이드(REST API 방식). 뼈대 검증용 첫 조각(`GatheringController`와 같은 결).
 *
 * ⚠ Spring Security 를 안 썼다. `oauth2Login()` 오토컨피그는 인가 상태를 HttpSession 에 저장하는
 * 전제가 깔려 있는데, 세션은 안 쓰기로 했다(`JwtService` 주석 참조). 그래서 카카오 토큰/사용자
 * 정보 조회를 `RestClient` 로 직접 부르고, 우리 JWT 만 쿠키로 내려주는 얇은 구현으로 갔다.
 *
 * 흐름: /login → 카카오 인가 화면 → /callback(인가코드) → 카카오 토큰 교환 → 사용자 정보 조회 →
 * users 테이블 upsert(provider+provider_id 유니크 키) → JWT 발급 → httpOnly 쿠키로 내려주고
 * 프론트로 리다이렉트. /me 는 프론트가 "로그인돼 있나?"를 확인하는 용도.
 */
@RestController
class AuthController(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    @Value("\${kakao.client-id}") private val kakaoClientId: String,
    @Value("\${kakao.client-secret}") private val kakaoClientSecret: String,
    @Value("\${kakao.redirect-uri}") private val kakaoRedirectUri: String,
    @Value("\${app.login-success-url}") private val loginSuccessUrl: String,
    // 로컬(http)은 false, 운영(https)은 true — 프로파일이 정한다.
    // 하드코딩해두면 배포 때 바꾸는 걸 잊어서 쿠키가 평문으로 오간다.
    @Value("\${app.cookie-secure}") private val cookieSecure: Boolean,
) {
    private val kakaoAuth = RestClient.create("https://kauth.kakao.com")
    private val kakaoApi = RestClient.create("https://kapi.kakao.com")

    @GetMapping("/api/v1/auth/kakao/login")
    fun login(): ResponseEntity<Unit> {
        val authorizeUrl = UriComponentsBuilder
            .fromUriString("https://kauth.kakao.com/oauth/authorize")
            .queryParam("client_id", kakaoClientId)
            .queryParam("redirect_uri", kakaoRedirectUri)
            .queryParam("response_type", "code")
            .build()
            .toUriString()
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(authorizeUrl))
            .build()
    }

    @GetMapping("/api/v1/auth/kakao/callback")
    fun callback(@RequestParam code: String): ResponseEntity<Unit> {
        val accessToken = exchangeToken(code).accessToken
        val kakaoUser = fetchUserInfo(accessToken)

        val providerId = kakaoUser.id.toString()
        val user = userRepository.findByProviderAndProviderId("KAKAO", providerId)
            ?.apply {
                nickname = kakaoUser.kakaoAccount.profile.nickname
                profileImageUrl = kakaoUser.kakaoAccount.profile.profileImageUrl
            }
            ?: User(
                provider = "KAKAO",
                providerId = providerId,
                nickname = kakaoUser.kakaoAccount.profile.nickname,
                profileImageUrl = kakaoUser.kakaoAccount.profile.profileImageUrl,
            )
        userRepository.save(user)

        val jwt = jwtService.issue(user.id)
        val cookie = ResponseCookie.from(COOKIE_NAME, jwt)
            .httpOnly(true)
            .secure(cookieSecure)
            // 프론트(jungsan.devkdk.com)와 API(api.jungsan.devkdk.com)가 같은 등록 도메인
            // (devkdk.com) 아래라 same-site 로 취급된다 — Lax 로도 쿠키가 실린다.
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofDays(30))
            .build()

        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(loginSuccessUrl))
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build()
    }

    @GetMapping("/api/v1/auth/me")
    fun me(@CookieValue(name = COOKIE_NAME, required = false) token: String?): ResponseEntity<MeResponse> {
        val userId = token?.let { jwtService.parseUserId(it) }
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(MeResponse(user.id, user.nickname, user.profileImageUrl))
    }

    private fun exchangeToken(code: String): KakaoTokenResponse =
        kakaoAuth.post()
            .uri("/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                LinkedMultiValueMap<String, String>().apply {
                    add("grant_type", "authorization_code")
                    add("client_id", kakaoClientId)
                    add("client_secret", kakaoClientSecret)
                    add("redirect_uri", kakaoRedirectUri)
                    add("code", code)
                },
            )
            .retrieve()
            .body(KakaoTokenResponse::class.java)
            ?: error("카카오 토큰 응답이 비어있다")

    private fun fetchUserInfo(accessToken: String): KakaoUserInfo =
        kakaoApi.get()
            .uri("/v2/user/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .retrieve()
            .body(KakaoUserInfo::class.java)
            ?: error("카카오 사용자 정보 응답이 비어있다")

    companion object {
        const val COOKIE_NAME = "jeongsan_token"
    }
}

data class MeResponse(val id: Long, val nickname: String, val profileImageUrl: String?)
