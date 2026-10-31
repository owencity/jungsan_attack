package app.jeongsan.server.guest

import app.jeongsan.server.common.UnauthenticatedException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class GuestBrowser

@Component
class GuestBrowserResolver(private val tokens: GuestTokens) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(GuestBrowser::class.java) && parameter.parameterType == GuestIdentity::class.java

    override fun resolveArgument(parameter: MethodParameter, mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest, binderFactory: WebDataBinderFactory?): GuestIdentity {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java) ?: throw UnauthenticatedException()
        return tokens.identity(request.cookies?.firstOrNull { it.name == GuestController.COOKIE_NAME }?.value)
            ?: throw UnauthenticatedException()
    }
}
