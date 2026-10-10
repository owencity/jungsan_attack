package app.jeongsan.server.user

import app.jeongsan.server.common.ApiException
import app.jeongsan.server.common.UnauthenticatedException
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant

data class ProviderIdentity(val id: String, val nickname: String, val image: String?, val refreshToken: String? = null, val clientId: String? = null)
interface ProviderGateway {
    fun authorize(provider: String, state: String, nonce: String): String
    fun identity(provider: String, code: String, nonce: String): ProviderIdentity
    fun revokeApple(clientId: String, token: String)
}

/** 네트워크는 얇은 어댑터 하나로 제한해 자격 검증과 정책을 실제 키 없이 시험한다. */
@Component
class RestProviderGateway(
    @Value("\${kakao.client-id}") private val kakaoId: String,
    @Value("\${kakao.client-secret}") private val kakaoSecret: String,
    @Value("\${kakao.redirect-uri}") private val kakaoRedirect: String,
    @Value("\${apple.client-id}") private val appleId: String,
    @Value("\${apple.team-id}") private val appleTeam: String,
    @Value("\${apple.key-id}") private val appleKeyId: String,
    @Value("\${apple.private-key-base64}") private val appleKey: String,
    @Value("\${apple.redirect-uri}") private val appleRedirect: String,
    env: Environment,
) : ProviderGateway {
    private val kakaoSettings = listOf(kakaoId, kakaoSecret, kakaoRedirect)
    private val appleSettings = listOf(appleId, appleTeam, appleKeyId, appleKey, appleRedirect)
    private val http = RestClient.builder().requestFactory(SimpleClientHttpRequestFactory().apply {
        setConnectTimeout(5000); setReadTimeout(5000)
    }).build()
    init {
        if (env.activeProfiles.contains("prod")) {
            require(AuthPolicy.providerAvailable("KAKAO", kakaoSettings, appleSettings)) { "카카오 운영 설정이 비었습니다." }
            // Apple 키 발급 전에는 해당 제공자만 닫는다. 모두 설정했다면 잘못된 키는 기동 시 발견한다.
            if (AuthPolicy.providerAvailable("APPLE", kakaoSettings, appleSettings))
                AppleTokens.clientSecret(appleTeam, appleKeyId, appleId, appleKey, Instant.now())
        }
    }
    private fun available(provider: String) {
        val ready = AuthPolicy.providerAvailable(provider, kakaoSettings, appleSettings)
        if (!ready) throw ApiException("AUTH_PROVIDER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "로그인 설정을 확인해 주세요.")
    }
    override fun authorize(provider: String, state: String, nonce: String): String {
        available(provider)
        val apple = provider == "APPLE"
        val builder = UriComponentsBuilder.fromUriString(if (apple) "https://appleid.apple.com/auth/authorize" else "https://kauth.kakao.com/oauth/authorize")
            .queryParam("client_id", if(apple) appleId else kakaoId).queryParam("redirect_uri",if(apple) appleRedirect else kakaoRedirect)
            .queryParam("response_type","code").queryParam("state",state)
        if (apple) builder.queryParam("response_mode","form_post").queryParam("scope","name email").queryParam("nonce",nonce)
        return builder.build().encode().toUriString()
    }
    @Suppress("UNCHECKED_CAST")
    override fun identity(provider: String, code: String, nonce: String): ProviderIdentity {
        available(provider)
        try {
            val apple = provider == "APPLE"
            val response = form(if(apple) "https://appleid.apple.com/auth/token" else "https://kauth.kakao.com/oauth/token",
                "grant_type" to "authorization_code", "client_id" to if(apple) appleId else kakaoId,
                "client_secret" to if(apple) AppleTokens.clientSecret(appleTeam,appleKeyId,appleId,appleKey,Instant.now()) else kakaoSecret,
                "redirect_uri" to if(apple) appleRedirect else kakaoRedirect, "code" to code)
            if (apple) {
                val raw = http.get().uri("https://appleid.apple.com/auth/keys").retrieve().body(Map::class.java) ?: throw UnauthenticatedException()
                val keys = (raw["keys"] as List<Map<String,String>>).map { AppleKey(it.getValue("kid"),it.getValue("kty"),it.getValue("alg"),it.getValue("n"),it.getValue("e")) }
                val subject = AppleTokens.verify(response["id_token"] as String,keys,appleId,nonce)
                return ProviderIdentity(subject,"",null,response["refresh_token"] as? String,appleId)
            }
            val info = http.get().uri("https://kapi.kakao.com/v2/user/me").header(HttpHeaders.AUTHORIZATION,"Bearer ${response["access_token"]}")
                .retrieve().body(Map::class.java) ?: throw UnauthenticatedException()
            val profile = (info["kakao_account"] as? Map<String,Any?>)?.get("profile") as? Map<String,Any?>
            return ProviderIdentity((info["id"] as Number).toLong().toString(),(profile?.get("nickname") as? String).orEmpty(),profile?.get("profile_image_url") as? String)
        } catch (e: ApiException) { throw e }
        catch (_: Exception) { throw UnauthenticatedException() }
    }
    override fun revokeApple(clientId: String, token: String) {
        available("APPLE")
        form("https://appleid.apple.com/auth/revoke", "client_id" to clientId,
            "client_secret" to AppleTokens.clientSecret(appleTeam,appleKeyId,clientId,appleKey,Instant.now()),
            "token" to token, "token_type_hint" to "refresh_token")
    }
    private fun form(url: String, vararg fields: Pair<String,String>): Map<*,*> = http.post().uri(url)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(LinkedMultiValueMap<String,String>().apply { fields.forEach { add(it.first,it.second) } })
        .retrieve().body(Map::class.java).orEmpty()
}
