package app.jeongsan.server.common

import app.jeongsan.server.user.AuthController
import app.jeongsan.server.user.JwtService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * 컨트롤러 파라미터에 **로그인한 사용자의 id** 를 주입한다.
 *
 * ```kotlin
 * @GetMapping("/groups")
 * fun list(@LoginUser userId: Long): List<GroupSummary>
 * ```
 *
 * **`User` 엔티티가 아니라 `Long` 을 주는 이유** — 대부분의 엔드포인트는 소유권
 * 검사(`group.createdByUserId == userId`)만 하면 된다. 엔티티를 주입하면 매 요청마다
 * DB 를 한 번 더 친다. 닉네임이 필요한 곳에서만 조회한다.
 *
 * **쿠키가 없거나 무효하면 401 을 던진다.** 컨트롤러마다 null 검사를 반복하지 않기
 * 위해서다. 로그인 없이도 되는 엔드포인트(`GET /gr/{token}` 등)는 이 애너테이션을
 * 쓰지 않고 직접 쿠키를 읽는다.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class LoginUser

@Component
class LoginUserArgumentResolver(
    private val jwtService: JwtService,
) : HandlerMethodArgumentResolver {

    /**
     * 원시 `long` 과 박싱된 `java.lang.Long` 을 **둘 다** 받는다.
     *
     * 코틀린의 non-null `Long` 은 원시 `long` 으로 컴파일된다. 박싱 타입만 비교하면
     * 이 resolver 가 후보에서 빠지고, Spring 이 `userId` 를 일반 요청 파라미터로
     * 처리하려다 `IllegalStateException` 으로 500 을 낸다 — 실제로 그렇게 터졌었다.
     */
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(LoginUser::class.java) &&
            (
                parameter.parameterType == Long::class.javaPrimitiveType ||
                    parameter.parameterType == Long::class.javaObjectType
                )

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Long {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: throw UnauthenticatedException()

        val token = request.cookies
            ?.firstOrNull { it.name == AuthController.COOKIE_NAME }
            ?.value
            ?: throw UnauthenticatedException()

        return jwtService.parseUserId(token) ?: throw UnauthenticatedException()
    }
}
