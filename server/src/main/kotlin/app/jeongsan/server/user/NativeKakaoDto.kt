package app.jeongsan.server.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NativeKakaoLoginRequest(
    @field:NotBlank @field:Size(max = 2048) val accessToken: String,
) {
    override fun toString() = "NativeKakaoLoginRequest(accessToken=[redacted])"
}
