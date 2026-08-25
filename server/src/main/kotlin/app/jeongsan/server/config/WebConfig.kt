package app.jeongsan.server.config

import app.jeongsan.server.common.LoginUserArgumentResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 프론트가 별도 배포(jungsan.devkdk.com, 로컬은 localhost:5173)라 CORS 를 열어야 한다.
 * `/auth/me` 가 쿠키로 인증하므로 `allowCredentials(true)` 가 필수 — 이게 켜지면
 * `allowedOrigins("*")` 는 스펙상 금지라 정확한 프론트 origin 하나만 등록한다.
 */
@Configuration
class WebConfig(
    @Value("\${app.frontend-origin}") private val frontendOrigin: String,
    private val loginUserArgumentResolver: LoginUserArgumentResolver,
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(frontendOrigin)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowCredentials(true)
    }

    /** `@LoginUser Long` 파라미터를 쿠키에서 채운다. */
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(loginUserArgumentResolver)
    }
}
