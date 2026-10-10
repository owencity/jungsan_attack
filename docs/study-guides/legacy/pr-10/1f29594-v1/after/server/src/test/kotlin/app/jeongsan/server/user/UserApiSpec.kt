package app.jeongsan.server.user

import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.common.LoginUserArgumentResolver
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.StringSpec
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import jakarta.servlet.http.Cookie
import java.util.Optional

class UserApiSpec : StringSpec({
    val jwt = JwtService("01234567890123456789012345678901", 30)
    val cookie = Cookie(AuthController.COOKIE_NAME, jwt.issue(12))
    val repository = Mockito.mock(UserRepository::class.java)
    val service = UserService(repository)
    val auth = AuthController(repository, jwt, service, "", "", "", "http://localhost", false)
    val mvc = MockMvcBuilders.standaloneSetup(UserController(service), auth)
        .setCustomArgumentResolvers(LoginUserArgumentResolver(jwt))
        .setControllerAdvice(GlobalExceptionHandler())
        .setMessageConverters(MappingJackson2HttpMessageConverter(jacksonObjectMapper()))
        .build()

    beforeTest { Mockito.reset(repository) }

    fun request(value: String) = put("/api/v1/users/me/display-name")
        .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
        .content(jacksonObjectMapper().writeValueAsString(DisplayNameRequest(value)))

    "미등록 사용자 조회는 실명 null과 needsName true를 반환한다" {
        Mockito.`when`(repository.findById(12)).thenReturn(Optional.of(User(id = 12, nickname = "봄이")))
        mvc.perform(get("/api/v1/auth/me").cookie(cookie)).andExpect(status().isOk)
            .andExpect(jsonPath("$.needsName").value(true))
            .andExpect(jsonPath("$.nickname").value("봄이"))
            .andExpect(jsonPath("$.displayName").isEmpty)
    }

    "실명을 공백 제거해 최초 등록하고 갱신된 내 정보를 반환한다" {
        Mockito.`when`(repository.registerDisplayName(12, "김동규")).thenReturn(1)
        Mockito.`when`(repository.findById(12)).thenReturn(Optional.of(User(id = 12, displayName = "김동규")))
        mvc.perform(request("  김동규  ")).andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("김동규"))
            .andExpect(jsonPath("$.needsName").value(false))
        Mockito.verify(repository).registerDisplayName(12, "김동규")
    }

    "같은 실명 재요청은 성공한다" {
        Mockito.`when`(repository.findById(12)).thenReturn(Optional.of(User(id = 12, displayName = "김동규")))
        mvc.perform(request("김동규")).andExpect(status().isOk)
    }

    "이미 등록한 실명과 다르면 409를 반환한다" {
        Mockito.`when`(repository.findById(12)).thenReturn(Optional.of(User(id = 12, displayName = "김동규")))
        mvc.perform(request("박동규")).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("DISPLAY_NAME_ALREADY_SET"))
    }

    "빈 이름과 한 글자와 11자는 안내 문구와 400을 반환한다" {
        listOf("  " to "이름을 넣어주세요", "김" to "성까지 적어주세요", "12345678901" to "이름은 10자까지예요")
            .forEach { (name, message) ->
                mvc.perform(request(name)).andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.message").value(message))
            }
        Mockito.verifyNoInteractions(repository)
    }

    "이모지 10개는 코드포인트 10자로 허용한다" {
        val name = "🍺".repeat(10)
        Mockito.`when`(repository.findById(12)).thenReturn(Optional.of(User(id = 12, displayName = name)))
        mvc.perform(request(name)).andExpect(status().isOk)
        Mockito.verify(repository).registerDisplayName(12, name)
    }

    "이모지 하나는 한 글자로 거절한다" {
        mvc.perform(request("🍺")).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("성까지 적어주세요"))
    }

    "로그인 없는 등록과 내 정보 조회는 401이다" {
        mvc.perform(put("/api/v1/users/me/display-name").contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"김동규\"}")).andExpect(status().isUnauthorized)
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized)
        Mockito.verifyNoInteractions(repository)
    }

    "위조된 쿠키로 등록할 수 없다" {
        mvc.perform(put("/api/v1/users/me/display-name").contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"김동규\"}").cookie(Cookie(AuthController.COOKIE_NAME, "invalid")))
            .andExpect(status().isUnauthorized)
        Mockito.verifyNoInteractions(repository)
    }

    "삭제된 사용자 토큰은 401이다" {
        Mockito.`when`(repository.findById(12)).thenReturn(Optional.empty())
        mvc.perform(request("김동규")).andExpect(status().isUnauthorized)
    }

    "누락 null 깨진 JSON은 400이다" {
        listOf("{}", "{\"displayName\":null}", "{").forEach { body ->
            mvc.perform(put("/api/v1/users/me/display-name").cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest)
        }
    }
})
