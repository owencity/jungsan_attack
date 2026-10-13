package app.jeongsan.server.common

import app.jeongsan.server.user.AuthController
import app.jeongsan.server.user.JwtService
import app.jeongsan.server.user.UserController
import app.jeongsan.server.user.UserRepository
import app.jeongsan.server.user.UserService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.StringSpec
import jakarta.servlet.http.Cookie
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.mock.web.MockServletContext
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

class GlobalExceptionHandlerSpec : StringSpec({
    fun missingRouteContext(configuration: Class<*>): AnnotationConfigWebApplicationContext =
        AnnotationConfigWebApplicationContext().apply {
            servletContext = MockServletContext()
            register(configuration)
            refresh()
        }

    "핸들러가 없는 실제 MVC 요청은 404 NOT_FOUND를 반환한다" {
        missingRouteContext(NoRoutesConfiguration::class.java).use { context ->
            MockMvcBuilders.webAppContextSetup(context).build()
                .perform(get("/api/v1/me").param("token", "DUMMY_SECRET"))
                .andExpect(status().isNotFound)
                .andExpect(content().json("""{"code":"NOT_FOUND","message":"찾을 수 없습니다.","errors":null}""", JsonCompareMode.STRICT))
        }
    }

    "정적 리소스 핸들러에 도달한 없는 API도 404 NOT_FOUND를 반환한다" {
        // 운영 Boot의 /** 리소스 매핑을 재현해야 NoResourceFoundException 회귀를 잡는다.
        missingRouteContext(MissingResourceConfiguration::class.java).use { context ->
            MockMvcBuilders.webAppContextSetup(context).build()
                .perform(get("/api/v1/me").param("token", "DUMMY_SECRET"))
                .andExpect(status().isNotFound)
                .andExpect(content().json("""{"code":"NOT_FOUND","message":"찾을 수 없습니다.","errors":null}""", JsonCompareMode.STRICT))
        }
    }

    val jwt = JwtService("01234567890123456789012345678901", 30)
    val cookie = Cookie(AuthController.COOKIE_NAME, jwt.issue(12))
    val repository = Mockito.mock(UserRepository::class.java)
    val mvc = MockMvcBuilders.standaloneSetup(UserController(UserService(repository)))
        .setCustomArgumentResolvers(LoginUserArgumentResolver(jwt))
        .setControllerAdvice(GlobalExceptionHandler())
        .setMessageConverters(MappingJackson2HttpMessageConverter(jacksonObjectMapper()))
        .build()

    beforeTest { Mockito.reset(repository) }

    "존재하는 보호 API의 미인증 요청은 기존 401을 유지한다" {
        mvc.perform(put("/api/v1/users/me/display-name")
            .contentType(MediaType.APPLICATION_JSON).content("""{"displayName":"김동규"}"""))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }

    "깨진 JSON은 기존 400 MALFORMED_REQUEST를 유지한다" {
        mvc.perform(put("/api/v1/users/me/display-name").cookie(cookie)
            .contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
    }

    "실제 서버 예외는 내부 메시지 없이 기존 500을 유지한다" {
        Mockito.`when`(repository.registerDisplayName(12, "김동규"))
            .thenThrow(IllegalStateException("DUMMY_INTERNAL_FAILURE"))
        mvc.perform(put("/api/v1/users/me/display-name").cookie(cookie)
            .contentType(MediaType.APPLICATION_JSON).content("""{"displayName":"김동규"}"""))
            .andExpect(status().isInternalServerError)
            .andExpect(content().json("""{"code":"INTERNAL_ERROR","message":"일시적인 오류가 발생했습니다.","errors":null}""", JsonCompareMode.STRICT))
    }
}) {
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    class NoRoutesConfiguration {
        @Bean
        fun exceptionHandler() = GlobalExceptionHandler()
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    class MissingResourceConfiguration : WebMvcConfigurer {
        @Bean
        fun exceptionHandler() = GlobalExceptionHandler()

        override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
            registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/http-not-found-spec-missing-resources/")
        }
    }
}
