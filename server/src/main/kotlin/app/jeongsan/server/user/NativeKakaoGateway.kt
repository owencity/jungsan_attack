package app.jeongsan.server.user

import app.jeongsan.server.common.ApiException
import app.jeongsan.server.common.UnauthenticatedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException

interface NativeKakaoGateway {
    fun identity(accessToken: String): ProviderIdentity
}

@Configuration
class NativeKakaoHttpConfig {
    @Bean("nativeKakaoHttp")
    fun nativeKakaoHttp(): RestClient = RestClient.builder().baseUrl("https://kapi.kakao.com")
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(5000); setReadTimeout(5000)
        }).build()
}

/** 토큰 소유만 확인하면 다른 개발자 앱의 토큰으로도 로그인할 수 있으므로 app_id를 먼저 대조한다. */
@Component
class RestNativeKakaoGateway(
    @param:Qualifier("nativeKakaoHttp") private val http: RestClient,
    @param:Value("\${kakao.app-id}") private val expectedAppId: Long,
    env: Environment,
) : NativeKakaoGateway {
    init {
        if (env.activeProfiles.contains("prod")) require(expectedAppId > 0) { "KAKAO_APP_ID는 양수여야 합니다." }
    }

    override fun identity(accessToken: String): ProviderIdentity {
        AuthPolicy.nativeKakaoToken(accessToken)
        if (expectedAppId <= 0) unavailable()
        try {
            val info = http.get().uri("/v1/user/access_token_info")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken").retrieve()
                .body(Map::class.java) ?: throw UnauthenticatedException()
            val subject = AuthPolicy.nativeKakaoSubject(info["id"].integer(), info["app_id"].integer(),
                info["expires_in"].integer(), expectedAppId)
            val profile = http.get().uri("/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken").retrieve()
                .body(Map::class.java) ?: throw UnauthenticatedException()
            if (profile["id"].integer()?.toString() != subject) throw UnauthenticatedException()
            val account = profile["kakao_account"] as? Map<*, *>
            val display = account?.get("profile") as? Map<*, *>
            return ProviderIdentity(subject, (display?.get("nickname") as? String).orEmpty(),
                display?.get("profile_image_url") as? String)
        } catch (error: ApiException) { throw error }
        catch (error: RestClientResponseException) {
            if (error.statusCode.value() >= 500 || error.statusCode.value() == 429) unavailable()
            throw UnauthenticatedException()
        } catch (_: ResourceAccessException) { unavailable() }
        catch (_: RestClientException) { unavailable() }
    }

    // 소수/문자열 ID를 강제로 Long으로 바꾸면 잘못된 주체를 인증할 수 있다.
    private fun Any?.integer(): Long? = when (this) { is Int -> toLong(); is Long -> this; else -> null }
    private fun unavailable(): Nothing = throw ApiException("AUTH_PROVIDER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
        "카카오 로그인을 잠시 후 다시 시도해 주세요.")
}
