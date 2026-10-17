package app.jeongsan.server.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/** `POST https://kauth.kakao.com/oauth/token` 응답. 필요한 필드만 옮긴다. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
)

/** `GET https://kapi.kakao.com/v2/user/me` 응답. 동의항목은 닉네임·프로필 사진뿐이다(SPEC). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoUserInfo(
    val id: Long,
    @JsonProperty("kakao_account") val kakaoAccount: KakaoAccount,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class KakaoAccount(val profile: Profile)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Profile(
        val nickname: String,
        @JsonProperty("profile_image_url") val profileImageUrl: String?,
    )
}
