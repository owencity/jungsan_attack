# FC-025 PR diff 기반 학습 위치

기준 base `493d1a74e908ba3daa31e1f4b55cce0c250303b2`, 코드 head `0f3482b23f14902a1cc318e9d398a0dc08608ba0`. 정책·카탈로그는 base의 원문이다.
공유 MVC 인프라 수정이며 업무 정책·모델·계산 추가가 없어 Java 독립 정책 구현은 해당 없음이다.
사용자 학습 진도는 미기록이며 자체 리뷰·테스트 성공과 별개다.

### Java 기본기
- 해당 없음
### Effective Java
- 해당 없음
### 함수형 Java
- 해당 없음
### Kotlin 기본
- 해당 없음
### Kotlin 고급
- 해당 없음
### 기술별 학습 포인트
#### Spring MVC CORS
- URL 매핑의 첫 일치와 구체 경로 우선 등록 — `after/server/src/main/kotlin/app/jeongsan/server/config/WebConfig.kt:23`: 콜백 한 경로에만 Apple 출처가 적용되는 범위를 확인한다.
- 허용 Origin·메서드·credentials의 조합 — `WebConfig.addCorsMappings`: 다른 API까지 출처를 확장하지 않는 경계를 확인한다.
- 실제 HandlerMapping을 쓰는 회귀 테스트 — `after/server/src/test/kotlin/app/jeongsan/server/config/AppleCallbackCorsSpec.kt:30`: standaloneSetup이 놓친 실제 WebConfig 적용 경로를 확인한다.
#### HTTP·인증 쿠키
- SameSite=None·Secure와 Cookie Path — `AppleCallbackCorsSpec`의 WEB/APP 쿠키 테스트: 크로스 사이트 form POST에 필요한 속성과 서버 수신을 확인한다.
- CORS 403·인증 401·콜백 303 구분 — `after/scripts/verify-apple-callback-cors.py:44`: 인가 허용과 state 검증 성공이 다른 단계임을 확인한다.
- preflight와 단순 form POST의 구분 — `AppleCallbackCorsSpec`의 OPTIONS/POST 테스트: preflight 통과만으로 실제 콜백을 검증할 수 없는 이유를 확인한다.

참고: [Spring MVC CORS](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html), [Set-Cookie](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Set-Cookie).
