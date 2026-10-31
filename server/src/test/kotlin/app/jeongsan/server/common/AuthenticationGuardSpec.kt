package app.jeongsan.server.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

class AuthenticationGuardSpec : StringSpec({
    "공개 진입점 외 모든 컨트롤러 핸들러가 회원 또는 비회원 인증을 받는다" {
        // 로그인 전 카카오 인가 진입점과 콜백만 공개한다. 내 정보 조회도 인증 대상이다.
        val publicMethods = setOf(
            "app.jeongsan.server.user.AuthController#login",
            "app.jeongsan.server.user.AuthController#callback",
            "app.jeongsan.server.user.AuthController#appleLogin",
            "app.jeongsan.server.user.AuthController#appleCallback",
            "app.jeongsan.server.user.AuthController#exchange",
            "app.jeongsan.server.gathering.JoinController#preview",
            // 무인증으로는 브라우저 자격만 만든다. 참여·조회·수정은 GuestBrowser와 DB 범위 검사를 요구한다.
            "app.jeongsan.server.guest.GuestController#startSession",
            // 로그인 이전의 고정 앱 데모 안내만 공개한다. 인증·사용자 데이터를 발급하지 않는다.
            "app.jeongsan.server.review.ReviewDemoController#demo",
        )
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AnnotationTypeFilter(RestController::class.java))
        val handlers = scanner.findCandidateComponents("app.jeongsan.server").flatMap { definition ->
            Class.forName(definition.beanClassName!!).declaredMethods.filter {
                AnnotatedElementUtils.hasAnnotation(it, RequestMapping::class.java)
            }
        }
        val unauthenticated = handlers.filter { method ->
            method.parameters.none { it.isAnnotationPresent(LoginUser::class.java) || it.isAnnotationPresent(app.jeongsan.server.guest.GuestBrowser::class.java) }
        }.map { "${it.declaringClass.name}#${it.name}" }
        unauthenticated.shouldContainExactlyInAnyOrder(publicMethods)
    }
})
