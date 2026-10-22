# FC-026 앱 복귀 수정의 확정 diff 학습 위치

base `f8e4e133807559bef31a2d6ba8407115331dbee3`, code head `38150caa0f725507a2d56a740ef969cd775b7351`. 정책·카탈로그는 main560fee3 원문이다. 최초 진단/로그아웃 기록은 auth-logout-v1에 보존하고 이 가이드는 실제 앱 복귀 변경만 다룬다.

### Java 기본기
- 해당 없음
### Effective Java
- Item49, 매개변수가 유효한지 검사하라 — `after/server/src/main/java/app/jeongsan/server/user/javaimpl/AuthPolicy.java`의 `appReturnLink`: HTML 링크에 들어갈 티켓의 허용 문자·길이를 먼저 검사하는 경계를 확인한다.
### 함수형 Java
- 해당 없음
### Kotlin 기본
- 2강, 코틀린에서 null을 다루는 방법 — `after/server/src/main/kotlin/app/jeongsan/server/user/AuthPolicy.kt`의 `appReturnLink`: UA가 없을 때 기존 스킴으로 내려가는 분기를 확인한다.
### Kotlin 고급
- 해당 없음
### 기술별 학습 포인트
#### HTTP·Android 브라우저 복귀
- 사용자 동작을 남기는200 HTML·수동 Intent 링크 — `after/server/src/main/kotlin/app/jeongsan/server/user/AuthController.kt`의 `finish`, `AppReturnPage.html`: 자동 전환 실패 뒤 사용자가 직접 앱을 열 수 있는 경계를 확인한다.
- 앱200과 웹303의 분리 — `after/server/src/test/kotlin/app/jeongsan/server/user/AppReturnPageSpec.kt`: 같은 콜백의 client별 응답과 Apple POST 재전송 거절을 확인한다.
#### CSP·개인정보
- 고정 인라인 스크립트의 SHA256 허용 목록 — `after/server/src/main/kotlin/app/jeongsan/server/user/AppReturnPage.kt`, `AppReturnPageSpec`: 실제 HTML 스크립트의 바이트와 CSP 해시가 일치해야 자동 이동이 실행되는 이유를 확인한다.
- no-store·no-referrer·인증 query 제거 — `AppReturnPage`의 SCRIPT와 `AuthController.finish`: 링크의 티켓과 콜백 code/state가 외부 자원·로그·이동에 흘러가지 않도록 제한한 지점을 확인한다.
#### 인증 티켓·MySQL
- 5분 만료와 일회용·PKCE 유지 — `AuthPolicy.appTicketExpiresAt`, `AuthFlowService.finish`, `after/server/src/test/kotlin/app/jeongsan/server/user/AuthDatabaseProbe.kt`: 정책의 기대 시각과 실제 DB 저장 만료를 분리해 검증한다.

사용자 학습 완료는 미기록이다. 브라우저/실기기 성공은 테스트와 별도다.
