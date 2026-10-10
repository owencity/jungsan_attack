# Apple 이름·이메일 요청 제거 — 고정 스터디 가이드

기준 PR [#30](https://github.com/owencity/jungsan_attack/pull/30) · diff `a2586e9a462de52d5a30b2d53e45517935e7b700` → `250b9848df38e3d5766c7214bab4d12049f7b9bd`. 정책·목차는 작업 당시 origin/main `a2586e9a462de52d5a30b2d53e45517935e7b700`의 실제 MD를 보존했다.
[고정 diff](changes.patch)를 먼저 체크하고 before/after의 같은 파일·함수를 비교한다. 책 본문 설명이나 해결 코드는 추가하지 않는다.

## Java 기본기

- 자바의 정석 11장 1.1 컬렉션 프레임웍의 핵심 인터페이스 (p.608) — [server/src/main/java/app/jeongsan/server/user/javaimpl/AuthPolicy.java:18](https://github.com/owencity/jungsan_attack/blob/250b9848df38e3d5766c7214bab4d12049f7b9bd/server/src/main/java/app/jeongsan/server/user/javaimpl/AuthPolicy.java#L18) → `public static Map<String, String> appleAuthorizationParameters`: Map의 키가 실제 요청 인자를 정하는 부분을 diff에서 찾고, 이름·이메일 scope 키가 없는지 기대값 테스트와 대조한다.

## Effective Java

- 해당 없음

## 함수형 Java

- 해당 없음

## Kotlin 기본

- 15강, 코틀린에서 배열과 컬렉션을 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/user/AuthPolicy.kt:13](https://github.com/owencity/jungsan_attack/blob/250b9848df38e3d5766c7214bab4d12049f7b9bd/server/src/main/kotlin/app/jeongsan/server/user/AuthPolicy.kt#L13) → `fun appleAuthorizationParameters`: mapOf의 키·값 쌍을 Java Map.of와 비교하고 읽기 전용 Map 타입과 실제 요청 키 집합을 확인한다.

- 17강, 코틀린에서 람다를 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/user/ProviderGateway.kt:60](https://github.com/owencity/jungsan_attack/blob/250b9848df38e3d5766c7214bab4d12049f7b9bd/server/src/main/kotlin/app/jeongsan/server/user/ProviderGateway.kt#L60) → `AuthPolicy.appleAuthorizationParameters(nonce).forEach`: forEach 람다가 인자 이름·값을 받아 queryParam에 전달하는 변경 줄을 읽고 제거된 scope가 다른 경로로 다시 추가되는지 체크한다.

## Kotlin 고급

- 해당 없음

## 기술별 학습 포인트

### OAuth

- 요청 scope와 응답 방식 — [server/src/main/kotlin/app/jeongsan/server/user/ProviderGateway.kt:60](https://github.com/owencity/jungsan_attack/blob/250b9848df38e3d5766c7214bab4d12049f7b9bd/server/src/main/kotlin/app/jeongsan/server/user/ProviderGateway.kt#L60) → `AuthPolicy.appleAuthorizationParameters(nonce).forEach`: OAuth scope 생략과 form_post·nonce 유지가 서로 다른 요청 요소인지 before/after와 실제 Apple 인가 URL 테스트에서 확인한다.

### HTTP 로그인

- 상관 쿠키와 nonce 보존 — [server/src/test/kotlin/app/jeongsan/server/user/ApplePrivacySpec.kt:69](https://github.com/owencity/jungsan_attack/blob/250b9848df38e3d5766c7214bab4d12049f7b9bd/server/src/test/kotlin/app/jeongsan/server/user/ApplePrivacySpec.kt#L69) → `val bindingCookie =`: 웹·앱 로그인 인자의 변경 후에도 state·nonce·Secure·HttpOnly·SameSite=None을 확인하는 신규 테스트 줄을 읽는다.

### JWT

- 서명 검증과 업무 식별자 선택 — [server/src/test/kotlin/app/jeongsan/server/user/AppleTokensSpec.kt:33](https://github.com/owencity/jungsan_attack/blob/250b9848df38e3d5766c7214bab4d12049f7b9bd/server/src/test/kotlin/app/jeongsan/server/user/AppleTokensSpec.kt#L33) → `"이전에 동의한 이메일이 ID 토큰에 있어도`: 이메일 claim이 있는 서명 토큰을 입력해도 두 언어의 반환값은 sub인지 신규 테스트의 입력과 기대값을 대조한다.
