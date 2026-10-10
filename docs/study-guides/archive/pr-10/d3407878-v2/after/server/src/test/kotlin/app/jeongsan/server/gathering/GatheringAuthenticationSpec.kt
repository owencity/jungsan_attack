package app.jeongsan.server.gathering

import app.jeongsan.server.common.GlobalExceptionHandler
import app.jeongsan.server.common.LoginUserArgumentResolver
import app.jeongsan.server.user.AuthController
import app.jeongsan.server.user.JwtService
import io.kotest.core.spec.style.StringSpec
import jakarta.servlet.http.Cookie
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class GatheringAuthenticationSpec : StringSpec({
    val repository = Mockito.mock(GatheringRepository::class.java)
    val jwt = JwtService("01234567890123456789012345678901", 30)
    val mvc = MockMvcBuilders.standaloneSetup(GatheringController(repository))
        .setCustomArgumentResolvers(LoginUserArgumentResolver(jwt))
        .setControllerAdvice(GlobalExceptionHandler()).build()
    beforeTest { Mockito.reset(repository) }

    "술자리 목록은 로그인 없이 조회할 수 없다" {
        mvc.perform(get("/api/v1/gatherings")).andExpect(status().isUnauthorized)
        Mockito.verifyNoInteractions(repository)
    }
    "로그인한 총무 본인의 목록 조회만 호출한다" {
        Mockito.`when`(repository.findByHostUserIdOrderByGatheringDateDesc(12)).thenReturn(emptyList())
        mvc.perform(get("/api/v1/gatherings").cookie(Cookie(AuthController.COOKIE_NAME, jwt.issue(12))))
            .andExpect(status().isOk)
        Mockito.verify(repository).findByHostUserIdOrderByGatheringDateDesc(12)
        Mockito.verifyNoMoreInteractions(repository)
    }
})
